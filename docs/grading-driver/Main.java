import java.util.Scanner;

/** Instructor driver. Adapt these calls to the assignment's published MyBST API. */
public class Main {
    public static void main(String[] args) {
        MyBST tree = new MyBST();
        Scanner input = new Scanner(System.in);
        while (input.hasNext()) {
            String command = input.next();
            if ("insert".equals(command)) tree.insert(input.nextInt());
            else if ("search".equals(command)) System.out.println(tree.search(input.nextInt()));
            else if ("delete".equals(command)) tree.delete(input.nextInt());
            else if ("inorder".equals(command)) System.out.println(tree.inorder());
            else if ("height".equals(command)) System.out.println(tree.height());
            else throw new IllegalArgumentException("Unknown command");
        }
    }
}
