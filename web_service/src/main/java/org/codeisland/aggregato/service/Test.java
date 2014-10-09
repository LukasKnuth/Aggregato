package org.codeisland.aggregato.service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Lukas Knuth
 * @version 1.0
 */
public class Test extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        String test_param = req.getParameter("test");
        if (test_param == null){
            resp.getWriter().println("<h1>Nothing given!</h1>");
        } else {
            resp.getWriter().println("<h1>You gave us: '"+test_param+"' !</h1>");

        }
    }

}
