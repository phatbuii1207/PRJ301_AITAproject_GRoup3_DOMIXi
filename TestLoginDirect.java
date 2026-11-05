import com.fpt.aita.dal.UserDAO;
import com.fpt.aita.model.User;

public class TestLoginDirect {
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        User u = dao.login("gv_namhn", "123456");
        if (u != null) {
            System.out.println("LOGIN SUCCESS: " + u.getFullName() + ", role=" + u.getRole());
        } else {
            System.out.println("LOGIN FAILED!");
        }
    }
}

