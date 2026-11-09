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
        Node node = new Node(value);
        if (head == null) {
            head = node;
        } else {
            Node current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = node;
        }
        size++;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public int search(int value) {
        Node current = head;
        int index = 0;
        while (current != null) {
            if (current.value == value) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }
}