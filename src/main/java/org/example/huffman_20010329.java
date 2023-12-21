package org.example;

import java.io.*;
import java.util.*;

public class huffman_20010329 {
    private final String filePath;
    private final int chunkSize;

    public huffman_20010329(String filePath, int chunkSize) {
        this.filePath = filePath;
        this.chunkSize = chunkSize;
    }


    private static String convertByteToBitString(boolean isLastByte, byte b) {
        int byteValue = b & 0xFF; // Convert to unsigned
        String binaryString = Integer.toBinaryString(byteValue);
        return String.format("%8s", binaryString).replace(' ', '0'); // Always pad to 8 bits
    }

    public void decompress(String inputFilePath, String outputFilePath) throws IOException {
        decompressData(inputFilePath, outputFilePath);
    }


    private TreeNode buildHuffmanTree(Map<ByteArray, Integer> freq) {
        PriorityQueue<TreeNode> queue = new PriorityQueue<>(Comparator.comparingInt(TreeNode::getFreq));
        freq.forEach((key, value) -> queue.add(new TreeNode(value, null, null, key)));

        while (queue.size() > 1) {
            TreeNode left = queue.poll();
            TreeNode right = queue.poll();
            TreeNode parent = new TreeNode(left.getFreq() + right.getFreq(), left, right, null);
            queue.add(parent);
        }
        return queue.poll();
    }

    private Map<ByteArray, Integer> calculateFreq() throws IOException {
        Map<ByteArray, Integer> freq = new HashMap<>();
        // Set a larger buffer size that is a multiple of chunkSize
        int bufferSize = chunkSize * 1024; // where N is an integer factor

        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Process the buffer in smaller chunks of size chunkSize
                for (int i = 0; i < bytesRead; i += chunkSize) {
                    int end = Math.min(i + chunkSize, bytesRead);
                    byte[] data = Arrays.copyOfRange(buffer, i, end);
                    ByteArray key = new ByteArray(data);
                    freq.put(key, freq.getOrDefault(key, 0) + 1);
                }
            }
        }
        return freq;
    }

    private Map<ByteArray, String> generateCodeWords(TreeNode root) {
        Map<ByteArray, String> codeWords = new HashMap<>();
        generateCodeWordsDFS(root, "", codeWords);
        return codeWords;
    }

    private void generateCodeWordsDFS(TreeNode node, String code, Map<ByteArray, String> codeWords) {
        if (node == null) {
            return;
        }
        if (node.getLeftChild() == null && node.getRightChild() == null) {
            codeWords.put(node.getByteArray(), code);
            return;
        }
        generateCodeWordsDFS(node.getLeftChild(), code + "0", codeWords);
        generateCodeWordsDFS(node.getRightChild(), code + "1", codeWords);
    }

    public void compress() throws IOException {
        long startTime = System.currentTimeMillis();
        Map<ByteArray, Integer> freq = calculateFreq();
        long endTime = System.currentTimeMillis();
        System.out.println("Calculate freq time: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        TreeNode root = buildHuffmanTree(freq);
        endTime = System.currentTimeMillis();
        System.out.println("Build Huffman tree time: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        Map<ByteArray, String> codeWords = generateCodeWords(root);
        endTime = System.currentTimeMillis();
        System.out.println("Generate code words time: " + (endTime - startTime) + "ms");

        // Count the number of chunks
        File file = new File(filePath);
        long fileSize = file.length();
        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);

        startTime = System.currentTimeMillis();
        compressData(filePath, codeWords, chunkCount); // Pass the chunkCount to the method
        endTime = System.currentTimeMillis();
        System.out.println("Compress data time: " + (endTime - startTime) + "ms");
    }

    private void writeBits(BufferedOutputStream bos, String bitString) throws IOException {
        int index = 0;
        while (index < bitString.length()) {
            int nextIndex = Math.min(index + 8, bitString.length());
            byte b = (byte) Integer.parseInt(bitString.substring(index, nextIndex), 2);
            bos.write(b);
            index = nextIndex;
        }
    }

    private void compressData(String filePath, Map<ByteArray, String> codeWords, int chunkCount) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
             FileOutputStream fos = new FileOutputStream(filePath + ".huffman");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(bos)) {

            // Write the number of chunks as the first piece of header information
            dos.writeInt(chunkCount);

            // Write the rest of the header
            dos.writeInt(codeWords.size());
            for (Map.Entry<ByteArray, String> entry : codeWords.entrySet()) {
                byte[] byteArray = entry.getKey().getData();
                String code = entry.getValue();
                dos.writeInt(byteArray.length);
                dos.write(byteArray);
                dos.writeUTF(code);
            }

            // Adjusted buffer size - a factor of 1024 times the chunkSize
            int bufferSize = 1024 * chunkSize;
            byte[] largeBuffer = new byte[bufferSize];
            int bytesRead;
            StringBuilder sb = new StringBuilder();

            while ((bytesRead = bis.read(largeBuffer)) != -1) {
                // Process the large buffer in smaller chunks of size chunkSize
                for (int start = 0; start < bytesRead; start += chunkSize) {
                    int end = Math.min(start + chunkSize, bytesRead);
                    byte[] chunk = Arrays.copyOfRange(largeBuffer, start, end);
                    ByteArray key = new ByteArray(chunk);
                    String code = codeWords.get(key);
                    if (code != null) {
                        sb.append(code);
                    }
                }

                // Write if buffer is sufficiently filled and handle partial codes at the end
                if (sb.length() >= bufferSize * 8) { // Assuming 8 bits per character
                    int bitsToWrite = (sb.length() / 8) * 8; // Write full bytes only
                    writeBits(bos, sb.substring(0, bitsToWrite));
                    sb.delete(0, bitsToWrite); // Keep the remaining bits in the StringBuilder
                }
            }

            // Write any remaining compressed data
            if (sb.length() > 0) {
                writeBits(bos, sb.toString());
            }
        }
    }

    private void decompressData(String inputFilePath, String outputFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFilePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis);
             FileOutputStream fos = new FileOutputStream(outputFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // Read the number of chunks from the header
            int chunkCount = dis.readInt();

            // Read the rest of the header to reconstruct Huffman codes
            int size = dis.readInt();
            Map<String, ByteArray> invertedCodeWords = new HashMap<>();
            for (int i = 0; i < size; i++) {
                int length = dis.readInt();
                byte[] byteArray = new byte[length];
                dis.readFully(byteArray);
                String code = dis.readUTF();
                invertedCodeWords.put(code, new ByteArray(byteArray));
            }

            // Decompress the data
            StringBuilder currentCode = new StringBuilder();
            int chunksDecompressed = 0;
            byte[] readBuffer = new byte[2048]; // Use a buffer to read chunks
            int bytesRead;
            long time = System.currentTimeMillis();
            while ((bytesRead = dis.read(readBuffer)) != -1 && chunksDecompressed < chunkCount) {
                for (int i = 0; i < bytesRead; i++) {
                    String bitString = convertByteToBitString(false, readBuffer[i]);
                    for (char bit : bitString.toCharArray()) {
                        currentCode.append(bit);
                        ByteArray data = invertedCodeWords.get(currentCode.toString());
                        if (data != null) {
                            bos.write(data.getData());
                            currentCode.setLength(0); // Reset current code
                            chunksDecompressed++;
                            if (chunksDecompressed == chunkCount) {
                                // Finish writing and exit if the last chunk is reached
                                bos.flush();
                                long endTime = System.currentTimeMillis();
                                System.out.println("Decompress time Loop: " + (endTime - time) + "ms");
                                return;
                            }
                        }
                    }
                }
            }
            bos.flush();
        }
    }


}
