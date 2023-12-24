//“I acknowledge that I am aware of the academic integrity guidelines of this course,
// and that I worked on this assignment independently without any unauthorized help”.
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

        int bufferSize = chunkSize * 2048;

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

    public void compress(String outPutFilePath) throws IOException {
        Map<ByteArray, Integer> freq = calculateFreq();
        TreeNode root = buildHuffmanTree(freq);
        Map<ByteArray, String> codeWords = generateCodeWords(root);
        compressData(outPutFilePath, codeWords);
    }

    private void compressData(String filePathOut, Map<ByteArray, String> codeWords) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
             FileOutputStream fos = new FileOutputStream(filePathOut);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(bos)) {

            dos.writeByte(0); // Placeholder for padding

            dos.writeInt(codeWords.size());
            for (Map.Entry<ByteArray, String> entry : codeWords.entrySet()) {
                byte[] byteArray = entry.getKey().getData();
                dos.writeInt(byteArray.length);
                dos.write(byteArray);
                dos.writeUTF(entry.getValue());
            }

            int bufferSize = 1024 * chunkSize;
            byte[] largeBuffer = new byte[bufferSize];
            int bytesRead;
            int currentByte = 0; // Stores the current byte being formed
            int bitCount = 0; // Counts the number of bits added to currentByte

            while ((bytesRead = bis.read(largeBuffer)) != -1) {
                for (int start = 0; start < bytesRead; start += chunkSize) {
                    int end = Math.min(start + chunkSize, bytesRead);
                    ByteArray key = new ByteArray(Arrays.copyOfRange(largeBuffer, start, end));
                    String code = codeWords.get(key);
                    if (code != null) {
                        for (char bit : code.toCharArray()) {
                            currentByte = (currentByte << 1) | (bit == '1' ? 1 : 0);
                            bitCount++;
                            if (bitCount == 8) {
                                dos.writeByte(currentByte);
                                currentByte = 0;
                                bitCount = 0;
                            }
                        }
                    }
                }
            }

            // Handle the last byte with padding if necessary
            int paddingBits = 8 - bitCount;
            if (bitCount > 0) {
                currentByte <<= paddingBits;
                dos.writeByte(currentByte);
            }

            // Write the actual padding information
            bos.flush();
            fos.getChannel().position(0);
            dos.writeByte(paddingBits);
        }
    }


    private void decompressData(String inputFilePath, String outputFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFilePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis);
             FileOutputStream fos = new FileOutputStream(outputFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            int paddingBits = dis.readByte();
            int size = dis.readInt();
            Map<String, ByteArray> invertedCodeWords = new HashMap<>();
            for (int i = 0; i < size; i++) {
                int length = dis.readInt();
                byte[] byteArray = new byte[length];
                dis.readFully(byteArray);
                String code = dis.readUTF();
                invertedCodeWords.put(code, new ByteArray(byteArray));
            }

            StringBuilder currentCode = new StringBuilder();
            byte[] readBuffer = new byte[2048];
            int bytesRead;
            boolean isLastByteProcessed = false;

            while (!isLastByteProcessed && (bytesRead = dis.read(readBuffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    byte currentByte = readBuffer[i];
                    boolean isLastByte = (i == bytesRead - 1) && (dis.available() == 0);

                    for (int bit = 7; bit >= 0; bit--) {
                        if (isLastByte && bit < paddingBits) {
                            isLastByteProcessed = true;
                            break;
                        }
                        int currentBit = (currentByte >> bit) & 1;
                        currentCode.append(currentBit);

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


}