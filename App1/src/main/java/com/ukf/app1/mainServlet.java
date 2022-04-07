package com.ukf.app1;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "mainServlet")
@MultipartConfig(maxFileSize = 16177215)
public class mainServlet extends HttpServlet {
    Connection con = null;
    String errorMessage = "";
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

    private boolean badConnection(PrintWriter out) {
        if (errorMessage.length() > 0) {
            out.println(errorMessage);
            return true;
        }
        return false;
    }

    private int getLogedUser(HttpServletRequest request, PrintWriter out) {
        HttpSession ses = request.getSession();
        int id = (Integer)ses.getAttribute("ID");
        if (id == 0) {
            out.println("Neprihlásený user");
            vypisNeopravnenyPristup(out);
        }
        return id;
    }

    private void vypisNeopravnenyPristup(PrintWriter out) {
        out.println("Nemas opravnenie, prihlas sa!");
        out.println("<META http-equiv='refresh' content='2;URL=index.html'>");
    }

    private boolean badOperation(String operacia, PrintWriter out) {
        if (operacia == null) {
            vypisNeopravnenyPristup(out);
            return true;
        }
        return false;
    }

    private void Pridaj(int id_user, PrintWriter out, HttpServletRequest request) {
        try {
            con = dajSpojenie(request);
            InputStream inputStream = null;
            Part filePart = request.getPart("photo");
            if (filePart != null) inputStream = filePart.getInputStream();
            String nazov = request.getParameter("nazov");
            String datum = request.getParameter("date");
            String popis = request.getParameter("popis");
            String sql = "INSERT INTO activity (nazov, datum, popis, obr, user_id) values (?, ?, ?, ?, ?)";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, nazov);
            stmt.setString(2, datum);
            stmt.setString(3, popis);
            if (inputStream != null) stmt.setBlob(4, inputStream);
            else stmt.setNull(4, java.sql.Types.BLOB);
            stmt.setInt(5, id_user);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    private void editujPolozku(PrintWriter out, String id, String nazov, String datum, String popis, Part photo) {
        try {
            InputStream inputStream = null;
            if (photo.getSize()>0) inputStream = photo.getInputStream();
            if (inputStream != null){
                String sql = "UPDATE activity SET nazov = ?, datum = ?, popis = ?, obr = ? WHERE id=?";
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setString(1, nazov);
                stmt.setString(2, datum);
                stmt.setString(3, popis);
                stmt.setBlob(4, inputStream);
                stmt.setString(5, id);
                stmt.executeUpdate();
                stmt.close();
            }
            else {
                String sql = "UPDATE activity SET nazov = ?, datum = ?, popis = ? WHERE id=?";
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setString(1, nazov);
                stmt.setString(2, datum);
                stmt.setString(3, popis);
                stmt.setString(4, id);
                stmt.executeUpdate();
                stmt.close();
            }

        } catch (Exception e) { out.println(e);}
    }

    private void createHeader(PrintWriter out, HttpServletRequest request){
        HttpSession ses = request.getSession();
        String vypis = ses.getAttribute("meno") + " " + ses.getAttribute("priezvisko");
        out.println("<h1 align='center'>To-Do List</h1><br />");
        out.println("<b><i><p align=right>"+vypis+"</i></b><br />");
        out.println("<a class='btn btn-primary' href='Servlet_input' role='button'>Pridaj</a>");
        out.println("<form action='mainServlet' method='post' align=right>");
        out.println("<input type='hidden' name='operacia' value='logout'>");
        out.println("<button type='submit' class='btn btn-primary' >Odhlásiť</button>");
        out.println("</form><hr>");
    }

    private void createBody(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        HttpSession ses = request.getSession();
        try {
            String sql = "SELECT * FROM activity WHERE user_id = ? ORDER by datum";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, ses.getAttribute("ID").toString());
            ResultSet rs = stmt.executeQuery();
            Blob image = null;
            out.print("<table>");
            while (rs.next()) {
                out.print("<tr>");
                out.print("<td>"+rs.getString("nazov")+ "&emsp;</td>");
                out.print("<td>"+rs.getString("datum") + "&emsp;</td>");
                out.print("<td>" +rs.getString("popis") + "&emsp;</td>");
                image = rs.getBlob("obr");
                if (image != null && image.length() > 0) {
                    out.print("<td><form action='Servlet_image' method='post' enctype='multipart/form-data'>");
                    out.println("<input type=hidden name ='id' value='"+rs.getString("id")+"'>");
                    out.println("<input type=hidden name ='photo' value='"+rs.getBlob("obr")+"'>");
                    out.println("<input type=hidden name='operacia' value='obrazok'>");
                    out.println("<button type='submit' class='btn btn-primary' align=right>Ukáž obrázok</button></form></td>");
                }
                else out.print("<td></td>");
                out.println("<td><form action='Servlet_update' method='post'>");
                out.println("<input type=hidden name ='id' value='"+rs.getString("id")+"'>");
                out.println("<input type=hidden name ='nazov' value='"+rs.getString("nazov")+"'>");
                out.println("<input type=hidden name ='datum' value='"+rs.getString("datum")+"'>");
                out.println("<input type=hidden name ='popis' value='"+rs.getString("popis")+"'>");
                out.println("<input type=hidden name='operacia' value='zobrFormedit'>");
                out.println("<button type='submit' class='btn btn-primary' align=right>Editácia</button></form></td>");
                out.print("<td><form action='mainServlet' method='post'>");
                out.println("<input type=hidden name ='id' value='"+rs.getString("id")+"'>");
                out.println("<input type=hidden name='operacia' value='mazanie'>");
                out.println("<button type='submit' class='btn btn-danger' align=right>Vymaž</button></form></td>");
                out.print("</tr>");
            }
            out.print("</table>");
            rs.close();
            stmt.close();
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    private void odhlas(PrintWriter out, HttpServletRequest request) {
        out.println("<div style='text-align: center;'>");
        out.println("<h2 class='center'>Dovidenia</h2>");
        out.println("</div>");
        out.println("<META http-equiv='refresh' content='2;URL=index.html'>");
        HttpSession ses = request.getSession();
        ses.invalidate();
    }

    protected boolean uspesneOverenie(HttpServletRequest request, PrintWriter out) {
        try {
            String login = request.getParameter("login");
            String pwd = request.getParameter("pswd");
            con = dajSpojenie(request);
            Statement stmt = con.createStatement();
            String sql = "SELECT count(*) as cnt FROM users "+" WHERE `email` = '"+login+"' AND password = '"+pwd+"'";
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            if (rs.getInt("cnt") == 1) {
                HttpSession session = request.getSession();
                sql = "SELECT ID, meno, priezvisko FROM users WHERE `email` = '"+login+"'";
                rs = stmt.executeQuery(sql);
                rs.next();
                session.setAttribute("ID", rs.getInt("id"));
                session.setAttribute("meno", rs.getString("meno"));
                session.setAttribute("priezvisko", rs.getString("priezvisko"));
                rs.close();
                stmt.close();
                return true;
            }
            if (rs.getInt("cnt") == 0) {out.println("Zle meno a/alebo heslo");}
            if (rs.getInt("cnt") > 1) {out.println("Zle meno a/alebo heslo - velkost");}
            rs.close();
            stmt.close();
            return false;
        } catch (Exception e) {
            out.println(e.getMessage());
        }
        return false;
    }

    protected boolean registruj(HttpServletRequest request, PrintWriter out) {
        try {
            String login = request.getParameter("login");
            String pwd = request.getParameter("pswd");
            String pwd2 = request.getParameter("pswd2");
            String meno = request.getParameter("meno");
            String priezvisko = request.getParameter("priezv");
            String[] arr = login.split("@", 2);
            if (!login.contains("@") || !arr[1].contains(".") || !pwd.equals(pwd2) || login.equals("") || meno.equals("") || priezvisko.equals("")){
                out.println("<h2 class='center'>Zle vyplnené údaje</h2>");
                out.println("<META http-equiv='refresh' content='2;URL=index.html'>");
                return false;
            }
            con = dajSpojenie(request);
            Statement stmt = con.createStatement();
            String sql = "SELECT count(*) as cnt FROM users "+" WHERE `email` = '"+login+"'";
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            if (rs.getInt("cnt") == 0) {
                HttpSession session = request.getSession();
                sql = "INSERT INTO `users`(`meno`, `priezvisko`, `email`, `password`) VALUES ('"+meno+"', '"+priezvisko+"', '"+login+"', '"+pwd+"')";
                stmt.executeUpdate(sql);
                sql = "SELECT ID, meno, priezvisko FROM users WHERE `email` = '"+login+"'";
                rs = stmt.executeQuery(sql);
                rs.next();
                session.setAttribute("ID", rs.getInt("id"));
                session.setAttribute("meno", rs.getString("meno"));
                session.setAttribute("priezvisko", rs.getString("priezvisko"));
                return true;
            }
            else out.println("<h2 class='center'>Login uz je obsadeny</h2>");
            out.println("<META http-equiv='refresh' content='2;URL=index.html'>");
            rs.close();
            stmt.close();
        } catch (Exception e) {
            out.println(e.getMessage());
        }
        return false;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<title>To-Do-List</title>");
        out.println("<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css' />");
        out.println("<script src='https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js'></script>'");
        out.println("<script src='https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js'></script>");
        out.println("<script src='https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js'></script>");
        out.println("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css' type='text/css' />");
        out.println("<link rel='stylesheet' type='text/css' href='style.css' />");
        try {
            String operacia = request.getParameter("operacia");
            if (badConnection(out) || badOperation(operacia,out)) return;
            if (operacia.equals("register")) if (!registruj(request, out)) return;
            if (operacia.equals("login")) if(!uspesneOverenie(request,out)){
                vypisNeopravnenyPristup(out);
                return;
            }
            int id = getLogedUser(request,out);
            if (id == 0) return;
            if (operacia.equals("mazanie")) {
                vymazPolozku(out, request.getParameter("id"));
            }
            if (operacia.equals("pridanie")) Pridaj(id, out, request);
            if (operacia.equals("edit")) {
                editujPolozku(out,
                        request.getParameter("id"),
                        request.getParameter("nazov"),
                        request.getParameter("datum"),
                        request.getParameter("popis"),
                        request.getPart("photo"));
            }
            if (operacia.equals("logout")) {
                odhlas(out, request);
                return;
            }
            createHeader(out, request);
            createBody(request, response);
        }
        catch (Exception e){
            out.println(e);
        }
    }

    private void vymazPolozku(PrintWriter out, String id) {
        try {
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM activity WHERE id = " + id;
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (Exception e) { out.println(e);}
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}