package org.example;

import java.io.*;
import java.util.*;

public class huffman_20010329 {
    //“I acknowledge that I am aware of the academic integrity guidelines of this course,
    // and that I worked on this assignment independently without any unauthorized help”.
    private final String filePath;
    private final int n; // chunck size

    private String fileExtension;
    private int chunkSize;

    public huffman_20010329(String filePath, int n) {
        this.filePath = filePath;
        this.n = n;
    }

    private Map<ByteArray, Integer> calculateFreq() throws IOException {
        Map<ByteArray, Integer> freq = new HashMap<>();

        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[n];
            int numberOfBytesRead;

            while ((numberOfBytesRead = inputStream.read(buffer)) != -1) {
                byte[] data = numberOfBytesRead == n ? buffer : Arrays.copyOf(buffer, numberOfBytesRead); // handle the case of last chunck in the file if its < n bytes
                ByteArray key = new ByteArray(data);
                freq.put(key, freq.getOrDefault(key, 0) + 1);
            }
        }
        freq.forEach((key, value) -> System.out.println("Byte: " + Arrays.toString(key.getData()) + ", Freq: " + value));
        return freq;
    }

    private void printTree(TreeNode root, String indent) {
        if (root == null) {
            return;
        }
        System.out.println(indent + "Node: " + (root.getByteArray() == null ? "Internal" : Arrays.toString(root.getByteArray().getData())) + ", Freq: " + root.getFreq());
        printTree(root.getLeftChild(), indent + "    ");
        printTree(root.getRightChild(), indent + "    ");
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


    private Map<ByteArray, bitSet_Extended> generateCodeWords(TreeNode root) {
        Map<ByteArray, bitSet_Extended> codeWords = new HashMap<>();
        DFS(root, new bitSet_Extended(), 0, codeWords);
        return codeWords;
    }

    private void DFS(TreeNode node, BitSet code, int length, Map<ByteArray, bitSet_Extended> codeWords) {
        if (node == null) {
            return;
        }
        if (node.getLeftChild() == null && node.getRightChild() == null) {
            bitSet_Extended leafCode = (bitSet_Extended) code.clone();
            leafCode.setSize(length);
            codeWords.put(node.getByteArray(), leafCode);
            System.out.println("Code Length: " + length);
            System.out.println("Byte Array: " + Arrays.toString(node.getByteArray().getData()) + ", Code: " + bitSetToString(codeWords.get(node.getByteArray()), codeWords.get(node.getByteArray()).length()));
            return;
        }
        if (node.getLeftChild() != null) {
            bitSet_Extended NodeCode = (bitSet_Extended) code.clone();
            NodeCode.clear(length);
            DFS(node.getLeftChild(), NodeCode, length + 1, codeWords);
        }

        if (node.getRightChild() != null) {
            bitSet_Extended NodeCode = (bitSet_Extended) code.clone();
            NodeCode.set(length);
            DFS(node.getRightChild(), NodeCode, length + 1, codeWords);
        }
    }

    private String bitSetToString(BitSet bitSet, int codeLength) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < codeLength; ++i) {
            builder.append(bitSet.get(i) ? "1" : "0");
        }
        return builder.toString();
    }

    private String bitSetToString(BitSet bitSet) {
        if (bitSet.isEmpty()) {
            return "0";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bitSet.length() || (i == 0 && bitSet.length() == 0); ++i) {
            builder.append(bitSet.get(i) ? "1" : "0");
        }
        return builder.toString();
    }


    private BitSet Trimmer(BitSet bitSet, int length) {
        BitSet helper = new BitSet(length);
        for (int i = 0; i < length; i++) {
            helper.set(i, bitSet.get(i));
        }
        return helper;
    }


    private void writeHeader(Map<ByteArray, bitSet_Extended> codeWords) throws IOException {
        String outputFile = filePath + ".huffmanCompressed.bin";
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos)) {

            String fileExtension = "";
            int j = filePath.lastIndexOf('.');
            if (j > 0) {
                fileExtension = filePath.substring(j + 1);
            }
            dos.writeUTF(fileExtension);
            dos.writeInt(n);

            dos.writeInt(codeWords.size());
            for (Map.Entry<ByteArray, bitSet_Extended> entry : codeWords.entrySet()) {
                byte[] byteArray = entry.getKey().getData();
                dos.writeInt(byteArray.length);
                dos.write(byteArray);

                bitSet_Extended bitSet = entry.getValue();
                int length = bitSet.getSize();
                dos.writeInt(length);
                for (int i = 0; i < length; i++) {
                    dos.writeBoolean(bitSet.get(i));
                }
            }
        }
    }

    private void compressData(Map<ByteArray, bitSet_Extended> codeWords) throws IOException {
        String outputFilePath = filePath + ".huffmanCompressed.bin";
        try (FileInputStream fis = new FileInputStream(filePath);
             FileOutputStream fos = new FileOutputStream(outputFilePath, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] buffer = new byte[n];
            int numberOfBytesRead;
            bitSet_Extended compressedData = new bitSet_Extended();
            int bitLength = 0;
            while ((numberOfBytesRead = fis.read(buffer)) != -1) {
                byte[] data = (numberOfBytesRead == n) ? buffer : Arrays.copyOf(buffer, numberOfBytesRead);
                bitSet_Extended codeword = codeWords.get(new ByteArray(data));
                if (codeword == null) {
                    throw new IOException("Codeword not found for given ByteArray.");
                }

                // Append the codeword to the compressed data
                for (int i = 0; i < codeword.getSize(); i++) {
                    if (codeword.get(i)) {
                        compressedData.set(bitLength);
                    }
                    bitLength++;
                }

                // Write out compressed data in chunks
                if (bitLength >= 8) { // For example, when we have 1KB of data
                    writeBitSet(bos, compressedData, bitLength);
                    compressedData.clear();
                    bitLength = 0;
                }
            }

            // Write any remaining compressed data
            if (bitLength > 0) {
                writeBitSet(bos, compressedData, bitLength);
            }
        }
    }

    private void writeBitSet(BufferedOutputStream bos, bitSet_Extended bitSet, int bitLength) throws IOException {
        byte[] bytes = bitSet.toByteArray();
        bos.write(bytes, 0, (int) Math.ceil(bitLength / 8.0));
    }

    void printCodeWords(Map<ByteArray, BitSet> codeWords) {
        for (Map.Entry<ByteArray, BitSet> entry : codeWords.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    public void compress() throws IOException {
        Map<ByteArray, Integer> freq = calculateFreq();
        TreeNode root = buildHuffmanTree(freq);
        Map<ByteArray, bitSet_Extended> codeWords = generateCodeWords(root);
        writeHeaderDataToTextFile_Compress(codeWords, filePath);
        writeHeader(codeWords);
        compressData(codeWords);
        //printCodeWords(codeWords);
        printTree(root, "");


    }


    // Helper method to invert the codeWords map
    private Map<bitSet_Extended, ByteArray> invertCodeWordsMap(Map<ByteArray, bitSet_Extended> codeWords) {
        Map<bitSet_Extended, ByteArray> invertedMap = new HashMap<>();
        for (Map.Entry<ByteArray, bitSet_Extended> entry : codeWords.entrySet()) {
            invertedMap.put(entry.getValue(), entry.getKey());
        }
        return invertedMap;
    }

    // Method to decompress the data using the codeWords map
    private void decompressDataUsingMap(String inputFile, String outputFile) throws IOException {
        Map<bitSet_Extended, ByteArray> invertedCodeWords;
        Map<ByteArray, bitSet_Extended> codeWords = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(inputFile);
             DataInputStream dis = new DataInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // Reading header
            fileExtension = dis.readUTF();
            chunkSize = dis.readInt();
            int codeWordSize = dis.readInt();

            // Reading code words
            for (int i = 0; i < codeWordSize; i++) {
                int byteArrayLength = dis.readInt();
                byte[] byteArray = new byte[byteArrayLength];
                dis.readFully(byteArray);
                int bitSetLength = dis.readInt();
                bitSet_Extended bitSet = new bitSet_Extended();
                bitSet.setSize(bitSetLength);
                for (int j = 0; j < bitSetLength; j++) {
                    bitSet.set(j, dis.readBoolean());
                }
                codeWords.put(new ByteArray(byteArray), bitSet);
            }

            invertedCodeWords = invertCodeWordsMap(codeWords);

            bitSet_Extended currentBitSet = new bitSet_Extended();
            int bitIndex = 0;

            // Reading compressed data

        }
    }


    // Main decompress method
    public void decompress(String inputFile, String outputFile) throws IOException {
        decompressDataUsingMap(inputFile, outputFile);
    }

    private void writeHeaderDataToTextFile_Compress(Map<ByteArray, bitSet_Extended> codeWords, String outputPath) throws IOException {
        String headerFilePath = outputPath + "_header_compress.txt";
        try (FileWriter writer = new FileWriter(headerFilePath)) {
            writer.write("File Extension: " + fileExtension + "\n");
            writer.write("Chunk Size: " + n + "\n");
            writer.write("Code Words Size: " + codeWords.size() + "\n");

            for (Map.Entry<ByteArray, bitSet_Extended> entry : codeWords.entrySet()) {
                writer.write("Byte Array: " + Arrays.toString(entry.getKey().getData()) + ", ");
                writer.write("BitSet: " + bitSetToString(entry.getValue(), entry.getValue().getSize()) + "     length = " + entry.getValue().getSize() + "\n");
            }
        }
    }

    private void writeHeaderDataToTextFile_Decompress(Map<ByteArray, bitSet_Extended> codeWords, String outputPath) throws IOException {
        String headerFilePath = outputPath + "_header_decompress.txt";
        try (FileWriter writer = new FileWriter(headerFilePath)) {
            writer.write("File Extension: " + fileExtension + "\n");
            writer.write("Chunk Size: " + n + "\n");
            writer.write("Code Words Size: " + codeWords.size() + "\n");

            for (Map.Entry<ByteArray, bitSet_Extended> entry : codeWords.entrySet()) {
                writer.write("Byte Array: " + Arrays.toString(entry.getKey().getData()) + ", ");
                writer.write("BitSet: " + bitSetToString(entry.getValue(), entry.getValue().getSize()) + "     length = " + entry.getValue().getSize() + "\n");
            }
        }
    }


    //private readHeader(inputFile)
    //private decompressData(inputFile, outputFilePath, huffmanTree)


}
