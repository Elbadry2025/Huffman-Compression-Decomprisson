package org.example;

import java.io.*;
import java.util.*;

public class huffman_20010329 {
    //“I acknowledge that I am aware of the academic integrity guidelines of this course,
    // and that I worked on this assignment independently without any unauthorized help”.
    private final String filePath;
    private final int n; // chunck size

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
        return freq;
    }

    private TreeNode buildHuffmanTree(Map<ByteArray, Integer> freq) {
        PriorityQueue<TreeNode> queue = new PriorityQueue<>(new Comparator<TreeNode>() {
            @Override
            public int compare(TreeNode n1, TreeNode n2) {
                return Integer.compare(n1.getFreq(), n2.getFreq());
            }
        });
        int alphabetSize = freq.size();
        for (ByteArray key : freq.keySet()) {
            TreeNode node = new TreeNode(freq.get(key), null, null, key);
            queue.add(node);
        }

        for (int i = 0; i < alphabetSize - 1; i++) {
            TreeNode node1 = queue.poll();
            TreeNode node2 = queue.poll();
            TreeNode parent = new TreeNode(node1.getFreq() + node2.getFreq(), node1, node2, null);
            queue.add(parent);
        }
        return queue.poll();
    }

    private Map<ByteArray, BitSet> generateCodeWords(TreeNode root) {
        Map<ByteArray, BitSet> codeWords = new HashMap<>();
        DFS(root, new BitSet(), 0, codeWords);
        return codeWords;
    }

    private void DFS(TreeNode node, BitSet code, int length, Map<ByteArray, BitSet> codeWords) {
        if (node == null) {
            return;
        }
        if (node.getLeftChild() == null && node.getRightChild() == null) {
            BitSet leafCode = (BitSet) code.clone();
            codeWords.put(node.getByteArray(), Trimmer(leafCode, length));
            return;
        }
        if (node.getLeftChild() != null) {
            BitSet NodeCode = (BitSet) code.clone();
            DFS(node.getLeftChild(), NodeCode, length + 1, codeWords);
        }

        if (node.getRightChild() != null) {
            BitSet NodeCode = (BitSet) code.clone();
            NodeCode.set(length);
            DFS(node.getRightChild(), NodeCode, length + 1, codeWords);
        }
    }

    private BitSet Trimmer(BitSet bitSet, int length) {
        BitSet helper = new BitSet(length);
        for (int i = 0; i < length; i++) {
            helper.set(i, bitSet.get(i));
        }
        return helper;
    }


    private void writeHeader(Map<ByteArray, BitSet> codeWords) throws IOException {
        String outputFile = filePath + ".huffmanCompressed.bin";
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos)) {
            dos.writeInt(codeWords.size());

            for (Map.Entry<ByteArray, BitSet> entry : codeWords.entrySet()) {
                byte[] byteArray = entry.getKey().getData();
                dos.writeInt(byteArray.length);
                dos.write(byteArray);

                BitSet bitSet = entry.getValue();
                int length = bitSet.length();
                dos.writeInt(length);
                for (int i = 0; i < length; i++) {
                    dos.writeBoolean(bitSet.get(i));
                }
            }
        }
    }

    private void compressData(Map<ByteArray, BitSet> codeWords) throws IOException {
        String outputFilePath = filePath + ".huffmanCompressed.bin";
        try (FileInputStream fis = new FileInputStream(filePath);
             FileOutputStream fos = new FileOutputStream(outputFilePath, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] buffer = new byte[n];
            int numberOfBytesRead;
            BitSet compressedData = new BitSet();
            int bitLength = 0;
            while ((numberOfBytesRead = fis.read(buffer)) != -1) {
                byte[] data = (numberOfBytesRead == n) ? buffer : Arrays.copyOf(buffer, numberOfBytesRead);
                BitSet codeword = codeWords.get(new ByteArray(data));
                if (codeword == null) {
                    throw new IOException("Codeword not found for given ByteArray.");
                }

                // Append the codeword to the compressed data
                for (int i = 0; i < codeword.length(); i++) {
                    if (codeword.get(i)) {
                        compressedData.set(bitLength);
                    }
                    bitLength++;
                }

                // Write out compressed data in chunks
                if (bitLength >= 8 * 1024) { // For example, when we have 1KB of data
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

    private void writeBitSet(BufferedOutputStream bos, BitSet bitSet, int bitLength) throws IOException {
        byte[] bytes = bitSet.toByteArray();
        bos.write(bytes, 0, (int) Math.ceil(bitLength / 8.0));
    }

    public void compress() throws IOException {
        Map<ByteArray, Integer> freq = calculateFreq();
        TreeNode root = buildHuffmanTree(freq);
        Map<ByteArray, BitSet> codeWords = generateCodeWords(root);
        writeHeader(codeWords);
        compressData(codeWords);
    }


    //private compressData(inputFilePath, outputFile, codewordMap)
    //private readHeader(inputFile)
    //private decompressData(inputFile, outputFilePath, huffmanTree)


}
