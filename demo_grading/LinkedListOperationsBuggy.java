public class LinkedListOperationsBuggy {
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    Node head;
    int size;

    public int count() {
        int elementCount = 0;
        while (head != null) {
            elementCount++;
            head = head.next;
        }
        return elementCount;
    }

    public int getFirst() {
        return head.next.value;
    }

    public int getLast() {
        Node current = head;
        while (current != null) {
            current = current.next;
        }
        return current.value;
    }

    public void clear() {
        size = 0;
    }
}
