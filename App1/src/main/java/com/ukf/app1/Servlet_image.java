package com.ukf.app1;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

@WebServlet(name = "Servlet_image")
@MultipartConfig(maxFileSize = 16177215)
public class Servlet_image extends HttpServlet {
    Connection con = null;
    Guard g;

    protected Connection dajSpojenie(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();
            Connection c = (Connection)session.getAttribute("spojenie");
            if (c == null) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                c = DriverManager.getConnection("jdbc:mysql://localhost/to_do_list", "root", "");
                session.setAttribute("spojenie", c);
                g = new Guard(c);
            }
            return c;
        } catch(Exception e) {
            System.out.println("CHYBA");
            return null;
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        con = dajSpojenie(request);
        try {
            String sql = "SELECT obr FROM activity WHERE ID = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, request.getParameter("id"));
            ResultSet rs = stmt.executeQuery();
            //System.out.println(stmt);
            Blob image;
            while (rs.next()) {
                image = rs.getBlob("obr");
                byte [] rb = new byte[(int)image.length()];
                InputStream in = image.getBinaryStream();
                int index = in.read(rb, 0, (int) image.length());

                response.reset();
                response.setContentType("image/jpg");
                response.getOutputStream().write(rb,0, (int) image.length());
                response.getOutputStream().flush();
                response.setContentType("text/html;charset=UTF-8");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }
}
