package org.example;

import java.io.IOException;

public class Main {
    //“I acknowledge that I am aware of the academic integrity guidelines of this course,
    // and that I worked on this assignment independently without any unauthorized help”.
    public static void main(String[] args) throws IOException {
        huffman_20010329 H = new huffman_20010329("D:\\Algorithms_Assignment2\\Operating system internals 9th edition.pdf", 1);
        H.compress();


    }
}