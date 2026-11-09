public class LinkedListRubricTest {
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    Node head;
    int size;

    public void addFirst(int value) {
        Node node = new Node(value);
        node.next = head;
        head = node;
        size++;
    }

    public void addLast(int value) {
        
    }

    public boolean isEmpty() {
        return 0;
    }

    public int size() {
        return value;
    }

    public int search(int value) {
        
        return -1;
    }
}