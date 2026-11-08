public class MyBST {
    private static class Node {
        int value; Node left, right;
        Node(int value) { this.value = value; }
    }
    private Node root;
    public void insert(int value) { root = insert(root, value); }
    private Node insert(Node node, int value) {
        if (node == null) return new Node(value);
        if (value < node.value) node.left = insert(node.left, value);
        else if (value > node.value) node.right = insert(node.right, value);
        return node;
    }
    public boolean search(int value) {
        Node node = root;
        while (node != null) {
            if (value == node.value) return true;
            node = value < node.value ? node.left : node.right;
        }
        return false;
    }
    public void delete(int value) { root = delete(root, value); }
    private Node delete(Node node, int value) {
        if (node == null) return null;
        if (value < node.value) node.left = delete(node.left, value);
        else if (value > node.value) node.right = delete(node.right, value);
        else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            Node successor = node.right;
            while (successor.left != null) successor = successor.left;
            node.value = successor.value;
            node.right = delete(node.right, successor.value);
        }
        return node;
    }
    public String inorder() { StringBuilder result = new StringBuilder(); inorder(root,result); return result.toString().trim(); }
    private void inorder(Node node, StringBuilder result) {
        if (node == null) return;
        inorder(node.left,result);result.append(node.value).append(' ');inorder(node.right,result);
    }
    public int height() { return height(root); }
    private int height(Node node) { return node == null ? 0 : 1 + Math.max(height(node.left),height(node.right)); }
}
