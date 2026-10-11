package com.fpt.aita;
import com.fpt.aita.dal.UserDAO;
import com.fpt.aita.model.User;
public class TestLogin {
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        User u = dao.login("gv_namhn", "123456");
        if (u != null) {
            System.out.println("Login Success: " + u.getFullName() + ", Role: " + u.getRole());
        } else {
            System.out.println("Login Failed!");
        }
    }
}
