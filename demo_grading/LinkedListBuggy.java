public class LinkedListBuggy {
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    Node head;
    int size;

    // LỖI 1: Hàm addHead làm mất các Node phía sau
    public void addHead(int value) {
        Node node = new Node(value);
        head = node; // SAI: Gán thẳng head bằng node mới mà quên nối node.next = head cũ. Toàn bộ danh sách cũ bị mất!
        size++;
    }

    // LỖI 2: Hàm addTail bị lỗi NullPointerException hoặc lặp vô hạn
    public void addTail(int value) {
        Node node = new Node(value);
        if (head == null) {
            head = node;
        } else {
            Node current = head;
            // SAI: Điều kiện dừng là current != null thay vì current.next != null
            while (current != null) { 
                current = current.next;
            }
            // Khi thoát vòng lặp, current đã bằng null. 
            // Gọi null.next sẽ gây ra lỗi NullPointerException (sập chương trình)
            current.next = node; 
        }
        size--; // LỖI 3: Thêm phần tử nhưng lại giảm size (size--)
    }

    // LỖI 4: Hàm display phá hủy cấu trúc danh sách
    public void display() {
        // SAI: Dùng trực tiếp con trỏ 'head' để duyệt thay vì dùng biến tạm 'current'
        while (head != null) {
            System.out.print(head.value + " -> ");
            head = head.next; // Sau khi hàm này chạy xong, head sẽ bằng null. Danh sách bị xóa sạch!
        }
        System.out.println("null");
    }

    public int size() {
        return size; 
    }
}
