/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.security;

/**
 *
 * @author achissimo
 */
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import framework.data.DataException;
import framework.data.dao.PollDataLayer;
import java.io.IOException;
import java.util.Calendar;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.RandomStringUtils;

public class SecurityLayer {

    //--------- SESSION SECURITY ------------    
     //questa funzione esegue una serie di controlli di sicurezza
    //sulla sessione corrente. Se la sessione non Ã¨ valida, la cancella
    //e ritorna null, altrimenti la aggiorna e la restituisce
    
    //this method executed a set of standard chacks on the current session.
    //If the session exists and is valid, it is rerutned, otherwise
    //the session is invalidated and the method returns null
    public static HttpSession checkSession(HttpServletRequest r) {
        boolean check = true;

        HttpSession s = r.getSession(false);
        //per prima cosa vediamo se la sessione Ã¨ attiva
        //first, let's see is the sessione is active
        if (s == null) {
            return null;
        }

        //check sulla validitÃ Â  della sessione
        //second, check is the session contains valid data
        if (s.getAttribute("userid") == null) {
            check = false;
            //check sull'ip del client
            //check if the client ip chaged
        } else if ((s.getAttribute("ip") == null) || !((String) s.getAttribute("ip")).equals(r.getRemoteHost())) {
            check = false;
            //check sulle date
            //check if the session is timed out
        } else {
            //inizio sessione
            //session start timestamp
            Calendar begin = (Calendar) s.getAttribute("inizio-sessione");
            //ultima azione
            //last action timestamp
            Calendar last = (Calendar) s.getAttribute("ultima-azione");
            //data/ora correnti
            //current timestamp
            Calendar now = Calendar.getInstance();
            if (begin == null) {
                check = false;
            } else {
                //secondi trascorsi dall'inizio della sessione
                //seconds from the session start
                long secondsfrombegin = (now.getTimeInMillis() - begin.getTimeInMillis()) / 1000;
                //dopo tre ore la sessione scade
                //after three hours the session is invalidated
                if (secondsfrombegin > 3 * 60 * 60) {
                    check = false;
                } else if (last != null) {
                    //secondi trascorsi dall'ultima azione
                    //seconds from the last valid action
                    long secondsfromlast = (now.getTimeInMillis() - last.getTimeInMillis()) / 1000;
                    //dopo trenta minuti dall'ultima operazione la sessione Ã¨ invalidata
                    //after 30 minutes since the last action the session is invalidated                    
                    if (secondsfromlast > 30 * 60) {
                        check = false;
                    }
                }
            }
        }
        if (!check) {
            s.invalidate();
            return null;
        } else {
            //reimpostiamo la data/ora dell'ultima azione
            //if che checks are ok, update the last action timestamp
            s.setAttribute("ultima-azione", Calendar.getInstance());
            return s;
        }
    }

    public static HttpSession createSession(HttpServletRequest request, String username) {
        HttpSession s = request.getSession(true);
        s.setAttribute("username", username);
        s.setAttribute("ip", request.getRemoteHost());
        s.setAttribute("inizio-sessione", Calendar.getInstance());
        String token = tokenGenerator(s);
      // Cookie cookie = new Cookie("token", token);
        s.setAttribute("token", token);
        try {
            ((PollDataLayer)request.getAttribute("datalayer")).getResponsibleUserDAO().setToken(username, token);
            s.setAttribute("userid",((PollDataLayer)request.getAttribute("datalayer")).getResponsibleUserDAO().getResponsibleUser(token).getKey());

        } catch (DataException ex) {
           // ServletContext sc = get
        }
        return s;
    } 
    
     public static HttpSession createSession(HttpServletRequest request) throws DataException {
        HttpSession s = request.getSession(true);
        
        s.setAttribute("ip", request.getRemoteHost());
        s.setAttribute("inizio-sessione", Calendar.getInstance());
        String token = tokenGenerator(s);
      // Cookie cookie = new Cookie("token", token);
        s.setAttribute("token", token);
       ((PollDataLayer)request.getAttribute("datalayer")).getPartecipantDAO().openPartecipant(token);
              
        return s;
    }

    public static HttpSession createSession(HttpServletRequest request, String username, int poll_id) {
        HttpSession s = request.getSession(true);
        s.setAttribute("username", username);
        s.setAttribute("ip", request.getRemoteHost());
        s.setAttribute("inizio-sessione", Calendar.getInstance());
        String token = tokenGenerator(s);
        // Cookie cookie = new Cookie("token", token);
        s.setAttribute("token", token);
        try {
            ((PollDataLayer)request.getAttribute("datalayer")).getPartecipantDAO().setToken(username, token);
            s.setAttribute("part_id",((PollDataLayer)request.getAttribute("datalayer")).getPartecipantDAO().getUserByApiKey(token).getKey());
            s.setAttribute("which_poll", poll_id);
        } catch (DataException ex) {
            // ServletContext sc = get
        }
        return s;
    }
    //SOLO PER CAPIRE SE FUNZIONA TUTTO
    public static String retrieveSession(HttpServletRequest request) {
        HttpSession s = checkSession(request);
        String session_string = (String) (s.getAttribute("username") + " " + s.getAttribute("ip") + " " + s.getAttribute("inizio-sessione") + " " + s.getAttribute("token"));
        return session_string;
    }
    
    public static boolean isValid (HttpServletRequest request) {
        HttpSession s = checkSession(request);
        return s!=null;
    }
    
    private static String tokenGenerator(HttpSession s) {
        String toEncode = RandomStringUtils.randomAlphanumeric(40);
        return toEncode;
    }

    public static void disposeSession(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) {
            s.invalidate();
        }
    }

    //--------- DATA SECURITY ------------
    //questa funzione aggiunge un backslash davanti a
    //tutti i caratteri "pericolosi", usati per eseguire
    //SQL injection attraverso i parametri delle form
    
    //this function adds backslashes in front of
    //all the "malicious" charcaters, usually exploited
    //to perform SQL injection through form parameters
    public static String addSlashes(String s) {
        return s.replaceAll("(['\"\\\\])", "\\\\$1");
    }

    //questa funzione rimuove gli slash aggiunti da addSlashes
    //this function removes the slashes added by addSlashes
    public static String stripSlashes(String s) {
        return s.replaceAll("\\\\(['\"\\\\])", "$1");
    }

    public static int checkNumeric(String s) throws NumberFormatException {
        //convertiamo la stringa in numero, ma assicuriamoci prima che sia valida
        //convert the string to a number, ensuring its validity
        if (s != null) {
            //se la conversione fallisce, viene generata un'eccezione
            //if the conversion fails, an exception is raised
            return Integer.parseInt(s);
        } else {
            throw new NumberFormatException("String argument is null");
        }
    }

    //--------- CONNECTION SECURITY ------------
    //questa funzione verifica se il protocollo HTTPS Ã¨ attivo
    //checks if the HTTPS protocol is in use
    public static boolean checkHttps(HttpServletRequest r) {
        return r.isSecure();
        //metodo "fatto a mano" che funziona solo se il server trasmette gli header corretti
        //the following is an "handmade" alternative, which works only if the server sends correct headers
        //String httpsheader = r.getHeader("HTTPS");
        //return (httpsheader != null && httpsheader.toLowerCase().equals("on"));
    }

    //questa funzione ridirige il browser sullo stesso indirizzo
    //attuale, ma con protocollo https
    //this function redirects the browser on the current address, but
    //with https protocol
    public static void redirectToHttps(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        //estraiamo le parti della request url
        String server = request.getServerName();
        //int port = request.getServerPort();
        String context = request.getContextPath();
        String path = request.getServletPath();
        String info = request.getPathInfo();
        String query = request.getQueryString();

        //ricostruiamo la url cambiando il protocollo e la porta COME SPECIFICATO NELLA CONFIGURAZIONE DI TOMCAT
        //rebuild the url changing port and protocol AS SPECIFIED IN THE SERVER CONFIGURATION
        String newUrl = "https://" + server + ":8443" +  context + path + (info != null ? info : "") + (query != null ? "?" + query : "");
        try {
            //ridirigiamo il client
            //redirect
            response.sendRedirect(newUrl);
        } catch (IOException ex) {
            try {
                //in caso di problemi tentiamo prima di inviare un errore HTTP standard
                //in case of problems, first try to send a standard HTTP error message
                response.sendError(response.SC_INTERNAL_SERVER_ERROR, "Cannot redirect to HTTPS, blocking request");
            } catch (IOException ex1) {
                //altrimenti generiamo un'eccezione
                //otherwise, raise an exception
                throw new ServletException("Cannot redirect to https!");
            }
        }
    }

    public static boolean sendEmail(final String fromEmail, final String password, String toEmail, String subject, String body) throws ServletException {
        try
        {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
            props.put("mail.smtp.port", "587"); //TLS Port
            props.put("mail.smtp.auth", "true"); //enable authentication
            props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS

            //create Authenticator object to pass in Session.getInstance argument
            Authenticator auth = new Authenticator() {
                //override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            };
            Session session = Session.getInstance(props, auth);

            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress(fromEmail, "NoReply-pollweb"));

            msg.setReplyTo(InternetAddress.parse(fromEmail, false));

            msg.setSubject(subject, "UTF-8");

            msg.setText(body, "UTF-8");

            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            Transport.send(msg);
            return true;
        }
        catch (Exception e) {
            throw new ServletException("Cannot send email"+e);
        }
    }


}
