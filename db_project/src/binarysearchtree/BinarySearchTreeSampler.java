package binarysearchtree;

import common.WeightedSampler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class BinarySearchTreeSampler implements WeightedSampler {

    private Node root;

    public BinarySearchTreeSampler(Map<String, Integer> weightByItem) {
        buildBalancedTree(weightByItem);
    }

    private void buildBalancedTree(Map<String, Integer> weightByItem) {
        List<String> itemsInKeyOrder = new ArrayList<>(new TreeMap<>(weightByItem).keySet());
        Deque<Range> rangesWaitingForInsert = new ArrayDeque<>();
        rangesWaitingForInsert.add(new Range(0, itemsInKeyOrder.size() - 1));
        while (!rangesWaitingForInsert.isEmpty()) {
            Range range = rangesWaitingForInsert.poll();
            if (range.from() > range.to()) {
                continue;
            }
            int middle = (range.from() + range.to()) / 2;
            String middleItem = itemsInKeyOrder.get(middle);
            insert(middleItem, weightByItem.get(middleItem));
            rangesWaitingForInsert.add(new Range(range.from(), middle - 1));
            rangesWaitingForInsert.add(new Range(middle + 1, range.to()));
        }
    }

    @Override
    public void insert(String item, int weight) {
        Node newNode = new Node(item, weight);
        if (root == null) {
            root = newNode;
            return;
        }
        Node node = root;
        while (true) {
            if (item.compareTo(node.item) < 0) {
                node.leftWeight += weight;
                if (node.left == null) {
                    node.left = newNode;
                    return;
                }
                node = node.left;
            } else {
                node.rightWeight += weight;
                if (node.right == null) {
                    node.right = newNode;
                    return;
                }
                node = node.right;
            }
        }
    }

    @Override
    public void delete(String item) {
        Node removed = findNode(item);
        if (removed == null) {
            return;
        }
        Node parent = subtractWeightOnPathAndReturnParent(root, item, removed.weight);
        if (removed.left == null) {
            replaceChild(parent, removed, removed.right);
            return;
        }
        if (removed.right == null) {
            replaceChild(parent, removed, removed.left);
            return;
        }
        Node successor = smallestNodeIn(removed.right);
        Node successorParent = subtractWeightOnPathAndReturnParent(removed, successor.item, successor.weight);
        replaceChild(successorParent, successor, successor.right);
        removed.item = successor.item;
        removed.weight = successor.weight;
    }

    @Override
    public String sample(Random random) {
        long totalWeight = root.leftWeight + root.weight + root.rightWeight;
        long target = random.nextLong(totalWeight);
        Node node = root;
        while (true) {
            if (target < node.leftWeight) {
                node = node.left;
            } else if (target < node.leftWeight + node.weight) {
                return node.item;
            } else {
                target -= node.leftWeight + node.weight;
                node = node.right;
            }
        }
    }

    private Node findNode(String item) {
        Node node = root;
        while (node != null && !node.item.equals(item)) {
            if (item.compareTo(node.item) < 0) {
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return node;
    }

    private Node subtractWeightOnPathAndReturnParent(Node start, String item, int weight) {
        Node parent = null;
        Node node = start;
        while (!node.item.equals(item)) {
            parent = node;
            if (item.compareTo(node.item) < 0) {
                node.leftWeight -= weight;
                node = node.left;
            } else {
                node.rightWeight -= weight;
                node = node.right;
            }
        }
        return parent;
    }

    private void replaceChild(Node parent, Node oldChild, Node newChild) {
        if (parent == null) {
            root = newChild;
        } else if (parent.left == oldChild) {
            parent.left = newChild;
        } else {
            parent.right = newChild;
        }
    }

    private Node smallestNodeIn(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }
}
