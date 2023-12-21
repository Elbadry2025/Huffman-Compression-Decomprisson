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

//        // Count the number of chunks
//        File file = new File(filePath);
//        long fileSize = file.length();
//        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);

        startTime = System.currentTimeMillis();
        compressData(filePath, codeWords); // Pass the chunkCount to the method
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

    private void compressData(String filePath, Map<ByteArray, String> codeWords) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
             FileOutputStream fos = new FileOutputStream(filePath + ".huffman");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(bos)) {

            // Reserve space for padding information at the beginning of the header
            dos.writeByte(0); // Initial placeholder for padding

            // Write the number of code words and each code word with its corresponding byte array
            dos.writeInt(codeWords.size());
            for (Map.Entry<ByteArray, String> entry : codeWords.entrySet()) {
                byte[] byteArray = entry.getKey().getData();
                String code = entry.getValue();
                dos.writeInt(byteArray.length);
                dos.write(byteArray);
                dos.writeUTF(code);
            }

            // Initialize variables for compression
            int bufferSize = 1024 * chunkSize;
            byte[] largeBuffer = new byte[bufferSize];
            int bytesRead;
            StringBuilder sb = new StringBuilder();
            int totalBits = 0; // Keep track of the total number of bits

            // Process the file data and create Huffman encoded string
            while ((bytesRead = bis.read(largeBuffer)) != -1) {
                for (int start = 0; start < bytesRead; start += chunkSize) {
                    int end = Math.min(start + chunkSize, bytesRead);
                    byte[] chunk = Arrays.copyOfRange(largeBuffer, start, end);
                    ByteArray key = new ByteArray(chunk);
                    String code = codeWords.get(key);
                    if (code != null) {
                        sb.append(code);
                        totalBits += code.length();
                    }
                }

                // Write the bits to the output stream
                while (sb.length() >= 8) {
                    writeBits(bos, sb.substring(0, 8));
                    sb.delete(0, 8);
                }
            }

            // Handle the last byte with padding if necessary
            int paddingBits = 8 - (totalBits % 8);
            if (paddingBits != 8) { // If padding is required
                for (int i = 0; i < paddingBits; i++) {
                    sb.append("0"); // Manually append each zero
                }

                writeBits(bos, sb.toString());
            }

            // Go back and write the actual padding information at the beginning of the header
            bos.flush(); // Ensure all data is written before seeking
            fos.getChannel().position(0); // Seek to the beginning of the file
            dos.writeByte(paddingBits); // Write the padding information
        }
    }


    private void decompressData(String inputFilePath, String outputFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFilePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis);
             FileOutputStream fos = new FileOutputStream(outputFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // Read padding information
            int paddingBits = dis.readByte();

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
            byte[] readBuffer = new byte[2048];
            int bytesRead;
            boolean isLastByte = false;
            while ((bytesRead = dis.read(readBuffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    // Check if processing the last byte
                    isLastByte = (bytesRead == i + 1) && (dis.available() == 0);

                    String bitString = convertByteToBitString(isLastByte, readBuffer[i], paddingBits);
                    for (char bit : bitString.toCharArray()) {
                        currentCode.append(bit);
                        ByteArray data = invertedCodeWords.get(currentCode.toString());
                        if (data != null) {
                            bos.write(data.getData());
                            currentCode.setLength(0);
                        }
                    }
                }
            }
            bos.flush();
        }
    }

    private String convertByteToBitString(boolean isLastByte, byte b, int paddingBits) {
        int byteValue = b & 0xFF;
        String binaryString = Integer.toBinaryString(byteValue);
        binaryString = String.format("%8s", binaryString).replace(' ', '0');

        if (isLastByte && paddingBits != 0) {
            return binaryString.substring(0, binaryString.length() - paddingBits);
        }
        return binaryString;
    }


}