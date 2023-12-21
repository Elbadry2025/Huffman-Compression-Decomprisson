package org.example;

import java.io.IOException;

public class Main {
    //“I acknowledge that I am aware of the academic integrity guidelines of this course,
    // and that I worked on this assignment independently without any unauthorized help”.
    public static void main(String[] args) throws IOException {
        huffman_20010329 H = new huffman_20010329("D:\\Algorithms_Assignment2\\gbbct10.seq", 1);
        long startTime = System.currentTimeMillis();
        H.compress();
        long endTime = System.currentTimeMillis();
        System.out.println("Compress time: " + (endTime - startTime) / 1000 + "s");
        startTime = System.currentTimeMillis();
        H.decompress("D:\\Algorithms_Assignment2\\gbbct10.seq.huffman", "D:\\Algorithms_Assignment2\\gbbct10.seq.huffman.decompressed.seq");
        endTime = System.currentTimeMillis();
        System.out.println("Decompress time: " + (endTime - startTime) / 1000 + "s");

    }
}