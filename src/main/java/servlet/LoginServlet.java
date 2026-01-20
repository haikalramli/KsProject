package servlet;

import dao.PhotographerDAO;
import model.Photographer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/LoginServlet", "/photographer/login"})
public class LoginServlet extends HttpServlet {
    
    private PhotographerDAO photographerDAO;
    
    @Override
    public void init() throws ServletException {
        photographerDAO = new PhotographerDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.getRequestDispatcher("/jsp/photographer/login.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String loginType = request.getParameter("loginType"); // senior or junior
        
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("error", "Please enter email and password");
            request.getRequestDispatcher("/jsp/photographer/login.jsp").forward(request, response);
            return;
        }
        
        Photographer photographer = photographerDAO.login(email.trim(), password);
        
        if (photographer != null) {
            // Validate login type matches role
            boolean canLogin = false;
            if ("senior".equals(loginType)) {
                canLogin = photographer.isSenior();
            } else if ("junior".equals(loginType)) {
                canLogin = !photographer.isSenior(); // junior or intern
            }
            
            if (!canLogin && loginType != null) {
                request.setAttribute("error", "You don't have permission for this login type");
                request.getRequestDispatcher("/jsp/photographer/login.jsp").forward(request, response);
                return;
            }
            
            HttpSession session = request.getSession();
            session.setAttribute("photographerId", photographer.getPgId());
            session.setAttribute("photographerName", photographer.getPgName());
            session.setAttribute("photographerEmail", photographer.getPgEmail());
            session.setAttribute("photographerRole", photographer.getPgRole());
            session.setAttribute("userType", "photographer");
            session.setAttribute("loginType", loginType != null ? loginType : (photographer.isSenior() ? "senior" : "junior"));
            
            response.sendRedirect(request.getContextPath() + "/calendar");
        } else {
            request.setAttribute("error", "Invalid email or password");
            request.getRequestDispatcher("/jsp/photographer/login.jsp").forward(request, response);
        }
    }
}
