public class LinkedListUtils {
    static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    Node head;
    int size;

    // 1. Kiểm tra danh sách có rỗng hay không (ĐÚNG)
    public boolean isEmpty() {
        return size == 0; // Hoặc return head == null;
    }

    // 2. Tìm kiếm giá trị trong danh sách (ĐÚNG)
    // Trả về chỉ số (index) đầu tiên tìm thấy, hoặc -1 nếu không có
    public int search(int value) {
        Node current = head;
        int index = 0;
        while (current != null) {
            if (current.value == value) {
                return index; // Tìm thấy, trả về vị trí
            }
            current = current.next;
            index++;
        }
        return -1; // Không tìm thấy
    }
}
