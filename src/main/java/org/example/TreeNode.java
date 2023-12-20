package org.example;

public class TreeNode {
    private int freq;
    private TreeNode leftChild;
    private TreeNode rightChild;
    private ByteArray byteArray;


    public TreeNode(int freq, TreeNode leftChild, TreeNode rightChild, ByteArray byteArray) {
        this.freq = freq;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.byteArray = byteArray;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public TreeNode getLeftChild() {
        return leftChild;
    }

    public void setLeftChild(TreeNode leftChild) {
        this.leftChild = leftChild;
    }

    public TreeNode getRightChild() {
        return rightChild;
    }

    public void setRightChild(TreeNode rightChild) {
        this.rightChild = rightChild;
    }

    public ByteArray getByteArray() {
        return byteArray;
    }

    public void setByteArray(ByteArray byteArray) {
        this.byteArray = byteArray;
    }
}
