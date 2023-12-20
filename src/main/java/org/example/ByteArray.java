package org.example;

import java.util.Arrays;

public class ByteArray {
    //“I acknowledge that I am aware of the academic integrity guidelines of this course,
    // and that I worked on this assignment independently without any unauthorized help”.
    private final byte[] data;

    public ByteArray(byte[] data) {
        this.data = data.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        if (this == other) return true;
        ByteArray that = (ByteArray) other;
        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    public byte[] getData() {
        return data.clone();
    }
}
