/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gamefriendzone;

/**
 *
 * @author GAMING LAPTOP
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    // Cấu hình kết nối
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "friendzone_db";
    private static final String USER = "root"; 
    private static final String PASS = ""; // XAMPP mặc định rỗng

    // Chuỗi kết nối (Có cấu hình Unicode và Timezone)
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME 
            + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";

    public static Connection getConnection() {
        try {
            // Load Driver (Cho bản Java cũ, bản mới tự nhận nhưng viết cho chắc)
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Hàm test thử xem kết nối được chưa
    public static void main(String[] args) {
        if (getConnection() != null) {
            System.out.println("Kết nối Database THÀNH CÔNG! Chiến thôi!");
        } else {
            System.out.println("Kết nối THẤT BẠI. Kiểm tra lại XAMPP/Port/User/Pass.");
        }
    }
}
