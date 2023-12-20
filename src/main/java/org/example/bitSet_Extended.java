package org.example;

public class bitSet_Extended extends java.util.BitSet {

    private int size = 0;

    private void incrementSize() {
        size++;
    }

    @Override
    public byte[] toByteArray() {
        int len = this.size();
        int numBytes = (len + 7) / 8; // Calculate the number of bytes needed
        byte[] byteArray = new byte[numBytes];

        for (int i = 0; i < len; i++) {
            if (this.get(i)) {
                int byteIndex = i / 8;
                int bitIndex = 7 - (i % 8);
                byteArray[byteIndex] |= (1 << bitIndex);
            }
        }

        return byteArray;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
