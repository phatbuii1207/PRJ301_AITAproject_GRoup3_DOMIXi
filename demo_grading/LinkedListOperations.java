public class LinkedListOperations {
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    Node head;
    int size;

    // 1. Hàm đếm số lượng phần tử (Duyệt thủ công qua danh sách)
    public int count() {
        int elementCount = 0;
        Node current = head;
        while (current != null) {
            elementCount++;
            current = current.next;
        }
        return elementCount;
    }

    // 2. Lấy giá trị của phần tử đầu tiên
    public int getFirst() {
        if (head == null) {
            throw new java.util.NoSuchElementException("Danh sách đang trống!");
        }
        return head.value;
    }

    // 3. Lấy giá trị của phần tử cuối cùng
    public int getLast() {
        if (head == null) {
            throw new java.util.NoSuchElementException("Danh sách đang trống!");
        }
        Node current = head;
        while (current.next != null) {
            current = current.next;
        }
        return current.value;
    }

    // 4. Xóa toàn bộ danh sách liên kết
    public void clear() {
        head = null; // Hủy liên kết gốc, Java Garbage Collector sẽ tự động giải phóng bộ nhớ của các Node cũ
        size = 0;    // Đưa kích thước về 0
    }

    // --- Hàm hỗ trợ thêm phần tử để bạn có thể test chạy thử độc lập ---
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
        size++;
    }
}
