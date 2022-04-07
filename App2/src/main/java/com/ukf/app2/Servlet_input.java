package com.ukf.app2;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@WebServlet(name = "Servlet_input")
@MultipartConfig(maxFileSize = 16177215)
public class Servlet_input extends HttpServlet {
    protected SessionFactory sessionFactory;

    protected void dajSpojenie() {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception ex) {
            System.out.println(ex);
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private void createHeader(PrintWriter out, HttpServletRequest request){
        HttpSession ses = request.getSession();
        String vypis = ses.getAttribute("meno") + " " + ses.getAttribute("priezvisko");
        out.println("<h1 align='center'>To-Do List</h1><br />");
        out.println("<b><i><p align=right>"+vypis+"</i></b>");
        out.println("<form method='post' action='mainServlet' align=right>");
        out.println("<button type='submit' class='btn btn-primary' align=right>Hlavná stránka</button>");
        out.println("<input type='hidden' name='operacia' value=' '></form>");
        out.println("<form action='mainServlet' method='post' align=right>");
        out.println("<input type='hidden' name='operacia' value='logout'>");
        out.println("<button type='submit' class='btn btn-primary' align=right>Odhlásiť</button>");
        out.println("</form><hr>");
    }

    private void createBody(PrintWriter out) {
        try {
            out.println("<form action='mainServlet' method='post' enctype='multipart/form-data' >");
            out.println("<p>Zadaj názov aktivity: </p>");
            out.println("<input type='text' name='nazov'><br>");
            out.println("<p>Vyber dátum aktivity: </p>");
            out.println("<input type='date' name='date' value='2022-01-01'><br>");
            out.println("<p>Zadaj popis aktivity: </p>");
            out.println("<textarea name='popis' rows='3' cols='30'></textarea><br>");
            out.println("<p>Vyber obrázok k aktivite: </p>");
            out.println("<input type='file' name='photo'><br>");
            out.println("<input type='hidden' name='operacia' value='pridanie'>");
            out.print("<br/>");
            out.println("<button type='submit' class='btn btn-primary' align=right>Pridaj</button>");
            out.println("</form>");
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        dajSpojenie();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<title>To-Do-List</title>");
        out.println("<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css' />");
        out.println("<script src='https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js'></script>'");
        out.println("<script src='https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js'></script>");
        out.println("<script src='https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js'></script>");
        out.println("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css' type='text/css' />");
        out.println("<link rel='stylesheet' type='text/css' href='style.css' />");
        createHeader(out, request);
        createBody(out);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }
}
