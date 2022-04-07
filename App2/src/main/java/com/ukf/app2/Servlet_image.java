package com.ukf.app2;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(name = "Servlet_image")
@MultipartConfig(maxFileSize = 16177215)
public class Servlet_image extends HttpServlet {
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       dajSpojenie();
        Session session = sessionFactory.openSession();
        Activity activity = session.get(Activity.class, Long.parseLong(request.getParameter("id")));
        session.close();
        response.reset();
        response.setContentType("image/jpg");
        response.getOutputStream().write(activity.getObr(),0, activity.getObr().length);
        response.getOutputStream().flush();
        response.setContentType("text/html;charset=UTF-8");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }
}
