/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dsa_withme;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author FOTZS41
 */
public class MyBST {

    Node root;

    public MyBST() {
        this.root = null;
    }

    public void insert(int data) {
        this.root = insertNode(data, this.root);
    }

    public Node insertNode(int data, Node r) {

        if (r == null) {
            return new Node(data);
        }

        if (data > r.data) {
            r.right = insertNode(data, r.right);
        } else if (data < r.data) {
            r.left = insertNode(data, r.left);
        }

        return r;
    }

    public int height(Node r) {
        if (r == null) {
            return 0;
        }

        int hleft = height(r.left);
        int hright = height(r.right);

        return 1 + Math.max(hleft, hright);
    }

    public int count(Node r) {
        if (r == null) {
            return 0;
        }

        return 1 + count(r.left) + count(r.right);
    }

    public void nLR(Node node) {
        if (node == null) {
            return;
        }

        System.out.print(node.data + " ");
        nLR(node.left);
        nLR(node.right);
    }

    public void lNR(Node node) {
        if (node == null) {
            return;
        }

        lNR(node.left);
        System.out.print(node.data + " ");
        lNR(node.right);
    }

    public void lRN(Node node) {
        if (node == null) {
            return;
        }

        lRN(node.left);
        lRN(node.right);
        System.out.print(node.data + " ");
    }

    public Node min(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    public Node deleteByCopying(Node node, int value) {
        if (node == null) {
            return null;
        }

        if (value < node.data) {
            node.left = deleteByCopying(node.left, value);
        } else if (value > node.data) {
            node.right = deleteByCopying(node.right, value);
        } else {
            // node khong co con
            if (node.left == null && node.right == null) {
                return null;
            }

            // node co 1 con
            // neu con nam ben left thi return right
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }

            // node co 2 con => tim node nho nhat ben phai roi thay vao root
            Node res = min(node.right);
            node.data = res.data;
            node.right = deleteByCopying(node.right, res.data);
        }
        return node;
    }

    public Node deleteByMergingLeft(Node r, int value) {
        //neu r == null => return;
        if (r == null) {
            return null;
        }

        if (r.data < value) {
            r.right = deleteByMergingLeft(r.right, value);
        } else if (r.data > value) {
            r.left = deleteByMergingLeft(r.left, value);
        } else {
            if (r.right == null && r.left == null) {
                return null;
            }

            //node khong co con o ben phai => tra ve con ben trai
            if (r.right == null) {
                return r.left;
            }

            if (r.left == null) {
                return r.right;
            }

            // node co 2 con => tim gia tri lon nhat o nhanh ben trai
            Node leftTree = r.left;
            Node rightTree = r.right;

            Node maxLeft = leftTree;

            while (maxLeft.right != null) {
                maxLeft = maxLeft.right;
            }

            maxLeft.right = rightTree;
            //  this.root = leftTree;
            return leftTree;
        }

        return r;
    }

    public Node deleteByMergingRight(Node r, int value) {
        //neu r == null => return;
        if (r == null) {
            return null;

        }

        if (r.data < value) {
            r.right = deleteByMergingRight(r.right, value);
        } else if (r.data > value) {
            r.left = deleteByMergingRight(r.left, value);
        } else {
            if (r.right == null && r.left == null) {
                return null;
            }

            //node khong co con o ben phai => tra ve con ben trai
            if (r.right == null) {
                return r.left;
            }

            if (r.left == null) {
                return r.right;
            }

            // node co 2 con => tim gia tri lon nhat o nhanh ben trai
            Node leftTree = r.left;
            Node rightTree = r.right;

            Node minRight = rightTree;

            while (minRight.left != null) {
                minRight = minRight.left;
            }

            minRight.left = leftTree;
            //this.root = rightTree;
            return rightTree;
        }

        return r;
    }

    public void bFS(Node node) {
        if (node == null) {
            return;
        }

        Queue<Node> queue = new LinkedList<>();
        queue.offer(node);  // cho node vao cuoi queue

        while (!queue.isEmpty()) {   // neu queue con phan tu
            Node current = queue.poll();

            System.out.print(current.data + " ");

            if (current.left != null) {
                queue.offer(current.left);
            }

            if (current.right != null) {
                queue.offer(current.right);
            }
        }
    }
}
