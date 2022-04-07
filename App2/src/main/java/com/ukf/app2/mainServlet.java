package com.ukf.app2;

import org.hibernate.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import org.apache.commons.io.IOUtils;

@WebServlet(name = "mainServlet")
@MultipartConfig(maxFileSize = 16177215)
public class mainServlet extends HttpServlet {
    String errorMessage = "";
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

    private boolean badConnection(PrintWriter out) {
        if (errorMessage.length() > 0) {
            out.println(errorMessage);
            return true;
        }
        return false;
    }

    protected boolean uspesneOverenie(HttpServletRequest request, PrintWriter out) {
        try {
            String login = request.getParameter("login");
            String pwd = request.getParameter("pswd");
            Transaction transaction = null;
            Users user = null;
            dajSpojenie();
            try(Session session = sessionFactory.openSession()){
                transaction = session.beginTransaction();
                user = (Users) session.createQuery("FROM Users U WHERE email = :login").setParameter("login", login).uniqueResult();
                if (user != null && user.getPassword().equals(pwd)) {
                    request.getSession().setAttribute("ID", user.getId());
                    request.getSession().setAttribute("meno", user.getMeno());
                    request.getSession().setAttribute("priezvisko", user.getPriezvisko());
                    return true;
                }
                else {
                    out.println("Zle meno a/alebo heslo");
                }
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                e.printStackTrace();
            }
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
            dajSpojenie();
            Transaction transaction = null;
            Users user = null;
            try(Session session = sessionFactory.openSession()){
                transaction = session.beginTransaction();
                user = (Users) session.createQuery("FROM Users U WHERE email = :login").setParameter("login", login).uniqueResult();
                if (user != null) {
                    out.println("<h2 class='center'>Login uz je obsadeny</h2>");
                    out.println("<META http-equiv='refresh' content='2;URL=index.html'>");
                    return false;
                }
                transaction.commit();
                transaction = session.beginTransaction();
                user = new Users();
                user.setMeno(meno);
                user.setPriezvisko(priezvisko);
                user.setEmail(login);
                user.setPassword(pwd);
                session.save(user);
                transaction.commit();
                session.close();
                request.getSession().setAttribute("ID", user.getId());
                request.getSession().setAttribute("meno", user.getMeno());
                request.getSession().setAttribute("priezvisko", user.getPriezvisko());
                return true;
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                e.printStackTrace();
            }
        } catch (Exception e) {
            out.println(e.getMessage());
        }
        return false;
    }

    private int getLogedUser(HttpServletRequest request, PrintWriter out) {
        HttpSession ses = request.getSession();
        int id = ((Long) ses.getAttribute("ID")).intValue();
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

    private void Pridaj(PrintWriter out, HttpServletRequest request) {
        try {
            dajSpojenie();
            InputStream inputStream = null;
            Part filePart = request.getPart("photo");
            if (filePart != null) inputStream = filePart.getInputStream();
            String nazov = request.getParameter("nazov");
            String datum = request.getParameter("date");
            String popis = request.getParameter("popis");
            Activity activity = new Activity();
            activity.setNazov(nazov);
            activity.setDatum(datum);
            activity.setPopis(popis);
            byte[] bytes = IOUtils.toByteArray(inputStream);
            activity.setObr(bytes);
            activity.setUser_id((Long) request.getSession().getAttribute("ID"));
            Session session = sessionFactory.openSession();
            session.beginTransaction();
            session.save(activity);
            session.getTransaction().commit();
            session.close();
        }
        catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    private void editujPolozku(HttpServletRequest request, PrintWriter out, String id, String nazov, String datum, String popis, Part photo) {
        try {
            InputStream inputStream = null;
            if (photo.getSize()>0) inputStream = photo.getInputStream();
            byte[] bytes;
            Activity activity;
            if (inputStream != null){
                bytes = IOUtils.toByteArray(inputStream);
            }
            else {
                Session session = sessionFactory.openSession();
                activity = session.get(Activity.class, Long.parseLong(id));
                bytes = activity.getObr();
                session.close();
            }
            activity = new Activity();
            activity.setId(Long.parseLong(id));
            activity.setNazov(nazov);
            activity.setDatum(datum);
            activity.setPopis(popis);
            activity.setObr(bytes);
            activity.setUser_id((Long) request.getSession().getAttribute("ID"));
            Session session = sessionFactory.openSession();
            session.beginTransaction();
            session.update(activity);
            session.getTransaction().commit();
            session.close();
        } catch (Exception e) { out.println(e);}
    }

    private void vymazPolozku(PrintWriter out, String id) {
        try {
            Activity activity = new Activity();
            activity.setId(Long.parseLong(id));
            Session session = sessionFactory.openSession();
            session.beginTransaction();
            session.delete(activity);
            session.getTransaction().commit();
            session.close();
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
            Session session = sessionFactory.openSession();
            String hql = "FROM Activity WHERE user_id = :user_id ORDER by datum";
            Query query = session.createQuery(hql);
            query.setParameter("user_id", ses.getAttribute("ID"));
            List results = query.list();
            byte[] image = null;
            out.print("<table>");
            for (int i = 0; i < results.size(); i++){
                out.print("<tr>");
                out.print("<td>" + ((Activity)results.get(i)).getNazov() + "&emsp;</td>");
                out.print("<td>" + ((Activity)results.get(i)).getDatum() + "&emsp;</td>");
                out.print("<td>" + ((Activity)results.get(i)).getPopis() + "&emsp;</td>");
                image = ((Activity)results.get(i)).getObr();
                if (image != null && image.length > 0) {
                    out.print("<td><form action='Servlet_image' method='post' enctype='multipart/form-data'>");
                    out.println("<input type=hidden name ='id' value='" + ((Activity)results.get(i)).getId() + "'>");
                    out.println("<input type=hidden name ='photo' value='" + ((Activity)results.get(i)).getObr() + "'>");
                    out.println("<input type=hidden name='operacia' value='obrazok'>");
                    out.println("<button type='submit' class='btn btn-primary' align=right>Ukáž obrázok</button></form></td>");
                }
                else out.print("<td></td>");
                out.println("<td><form action='Servlet_update' method='post'>");
                out.println("<input type=hidden name ='id' value='" + ((Activity)results.get(i)).getId() + "'>");
                out.println("<input type=hidden name ='nazov' value='" + ((Activity)results.get(i)).getNazov() + "'>");
                out.println("<input type=hidden name ='datum' value='" + ((Activity)results.get(i)).getDatum() + "'>");
                out.println("<input type=hidden name ='popis' value='" + ((Activity)results.get(i)).getPopis() + "'>");
                out.println("<input type=hidden name='operacia' value='zobrFormedit'>");
                out.println("<button type='submit' class='btn btn-primary' align=right>Editácia</button></form></td>");
                out.print("<td><form action='mainServlet' method='post'>");
                out.println("<input type=hidden name ='id' value='" + ((Activity)results.get(i)).getId() + "'>");
                out.println("<input type=hidden name='operacia' value='mazanie'>");
                out.println("<button type='submit' class='btn btn-danger' align=right>Vymaž</button></form></td>");
                out.print("</tr>");
            }
            out.print("</table>");
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    private void odhlas(PrintWriter out) {
        out.println("<div style='text-align: center;'>");
        out.println("<h2 class='center'>Dovidenia</h2>");
        out.println("</div>");
        out.println("<META http-equiv='refresh' content='2;URL=index.html'>");
        sessionFactory.close();
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
            if (operacia.equals("pridanie")) Pridaj(out, request);
            if (operacia.equals("edit")) {
                editujPolozku(request, out,
                        request.getParameter("id"),
                        request.getParameter("nazov"),
                        request.getParameter("datum"),
                        request.getParameter("popis"),
                        request.getPart("photo"));
            }
            if (operacia.equals("mazanie")) {
                vymazPolozku(out, request.getParameter("id"));
            }
            if (operacia.equals("logout")) {
                odhlas(out);
                return;
            }
            createHeader(out, request);
            createBody(request, response);
        }
        catch (Exception e){
            out.println(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}