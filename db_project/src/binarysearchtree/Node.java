package binarysearchtree;

class Node {
    String item;
    int weight;
    Node left;
    Node right;
    long leftWeight;
    long rightWeight;

    Node(String item, int weight) {
        this.item = item;
        this.weight = weight;
    }
}
