//“I acknowledge that I am aware of the academic integrity guidelines of this course,
// and that I worked on this assignment independently without any unauthorized help”.
package org.example;

import java.io.File;
import java.io.IOException;

public class Main {

    private static final String ID = "20010329";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Invalid arguments. Usage: java -jar huffman_<id>.jar [c|d] <file_path> [chunk_size]");
            return;
        }

        String mode = args[0];
        String filePath = args[1];

        if ("c".equals(mode) && args.length == 3) {
            int chunkSize = Integer.parseInt(args[2]);
            File inputFile = new File(filePath);
            String baseName = inputFile.getName();
            String outputFilePath = inputFile.getParent() + File.separator + ID + "." + chunkSize + "." + baseName + ".hc";

            huffman_20010329 huffman = new huffman_20010329(filePath, chunkSize);
            long startTime = System.currentTimeMillis();
            huffman.compress(outputFilePath);
            long endTime = System.currentTimeMillis();

            long originalSize = inputFile.length();
            long compressedSize = new File(outputFilePath).length();
            double compressionRatio = (double) compressedSize / (double) originalSize;

            System.out.println("Compression ratio: " + compressionRatio);
            System.out.println("Compression time: " + (endTime - startTime) + "ms");
        } else if ("d".equals(mode)) {
            File inputFile = new File(filePath);
            String outputFilePath = inputFile.getParent() + File.separator + "extracted." + inputFile.getName().replaceAll("\\.hc$", "");

            huffman_20010329 huffman = new huffman_20010329(filePath, 0); // Chunk size not needed for decompression
            long startTime = System.currentTimeMillis();
            huffman.decompress(filePath, outputFilePath);
            long endTime = System.currentTimeMillis();

            System.out.println("Decompression time: " + (endTime - startTime) + "ms");
        } else {
            System.out.println("Invalid mode. Use 'c' for compression and 'd' for decompression.");
        }
    }
}
