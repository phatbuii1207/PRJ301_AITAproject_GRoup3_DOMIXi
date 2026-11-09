public class LinkedListCorrect {
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    Node head;
    int size;

    // 1. Thêm vào đầu danh sách
    public void addHead(int value) {
        Node node = new Node(value);
        node.next = head;
        head = node;
        size++; // Tăng kích thước danh sách
    }

    // 2. Thêm vào cuối danh sách
    public void addTail(int value) {
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
        size++; // Tăng kích thước danh sách
    }

    // 3. Hiển thị danh sách liên kết
    public void display() {
        Node current = head;
        while (current != null) {
            System.out.print(current.value + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }

    // 4. Trả về kích thước danh sách
    public int size() {
        return size;
    }
}
