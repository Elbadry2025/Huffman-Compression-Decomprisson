package org.example;

import java.io.IOException;

public class Main {
    //“I acknowledge that I am aware of the academic integrity guidelines of this course,
    // and that I worked on this assignment independently without any unauthorized help”.
    public static void main(String[] args) throws IOException {
        huffman_20010329 H = new huffman_20010329("D:\\Algorithms_Assignment2\\Test.txt", 1);
        H.compress();
        H.decompress("D:\\Algorithms_Assignment2\\Test.txt.huffmanCompressed.bin", "D:\\Algorithms_Assignment2\\Test.txt.Decompressed.txt");


    }
}