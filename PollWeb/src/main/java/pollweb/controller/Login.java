/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pollweb.controller;

import framework.data.DataException;
import framework.data.dao.PollDataLayer;
import framework.data.proxy.ResponsibleUserProxy;
import framework.result.FailureResult;
import framework.result.TemplateManagerException;
import framework.result.TemplateResult;
import framework.security.SecurityLayer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pollweb.data.impl.ResponsibleUserImpl;
import pollweb.data.model.ResponsibleUser;

/**
 *
 * @author achissimo
 */
public class Login extends PollBaseController {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    String ref;
    
    private void action_error(HttpServletRequest request, HttpServletResponse response) {
        if (request.getAttribute("exception") != null) {
            (new FailureResult(getServletContext())).activate((Exception) request.getAttribute("exception"), request, response);
        } else {
            (new FailureResult(getServletContext())).activate((String) request.getAttribute("message"), request, response);
        }
    }

    private void action_default(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, TemplateManagerException {
            TemplateResult res = new TemplateResult(getServletContext());
            this.ref = request.getHeader("referer");
            request.setAttribute("page_title", "Login");
            
            res.activate("login.ftl.html", request, response);
        
    }
    
     private void action_default1(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, TemplateManagerException {
            TemplateResult res = new TemplateResult(getServletContext());
            
            request.setAttribute("page_title", "index");
           
            res.activate("index.ftl.html", request, response);
        
    }

    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            
            if(request.getParameter("login")!=null){
                action_login(request, response);

                }
            else{
                
            action_default(request, response);
            }

        } catch (IOException ex) {
            request.setAttribute("exception", ex);
            action_error(request, response);

        } catch (TemplateManagerException ex) {
            request.setAttribute("exception", ex);
            action_error(request, response);

        } catch (DataException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    private void action_login(HttpServletRequest request, HttpServletResponse response) throws IOException, TemplateManagerException, DataException {
         String username = request.getParameter("email");
        String password = request.getParameter("password");
        //... VALIDAZIONE IDENTITA'...
        //... IDENTITY CHECKS ...
      //  UserProxy up = new UserProxy();
        ResponsibleUserImpl ru = new ResponsibleUserImpl();
         ru.setEmail(username);
         ru.setPwd(password);
    

        if (!username.isEmpty() && !password.isEmpty()) {
            //se la validazione ha successo
            //if the identity validation succeeds
            //carichiamo lo userid dal database utenti
            //load userid from user database
            if(((PollDataLayer)request.getAttribute("datalayer")).getResponsibleUserDAO().checkResponsible(ru)){
                
                 int userid = 12;
            SecurityLayer.createSession(request, username, userid);
            response.sendRedirect("ResponsiblePage");
            ServletContext context = getServletContext( );
                context.log("è andato bene il cazzo di login");
            }
            else {
                ServletContext context = getServletContext( );
                context.log("è andato di merda il login");
            }
            
       //     response.sen

           
            //se � stato trasmesso un URL di origine, torniamo a quell'indirizzo
            //if an origin URL has been transmitted, return to it
            /*
            String referrer = request.getHeader("referer"); // Yes, with the legendary misspelling.

            if (referrer != null) {
                ServletContext context = getServletContext( );
                response.sendRedirect(ref);
            } else {
                response.sendRedirect("Home");
            }
*/


        } else {
            request.setAttribute("exception", new Exception("Login failed"));
            action_error(request, response);
        }
    }

 
    @Override
    public String getServletInfo() {
        return "Main Newspaper servlet";
    }// </editor-fold>
}
