package controller;

import dao.ClientDAO;
import model.Client;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "SignInController", urlPatterns = {"/SignInController", "/client/login"})
public class SignInController extends HttpServlet {
    
    private ClientDAO clientDAO;
    
    @Override
    public void init() throws ServletException {
        clientDAO = new ClientDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/jsp/client/SignIn.jsp");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("error", "Please enter email and password");
            request.getRequestDispatcher("/jsp/client/SignIn.jsp").forward(request, response);
            return;
        }
        
        Client client = clientDAO.login(email.trim(), password);
        
        if (client != null) {
            HttpSession session = request.getSession();
            session.setAttribute("clientID", client.getClId());
            session.setAttribute("clientName", client.getClName());
            session.setAttribute("clientEmail", client.getClEmail());
            session.setAttribute("userType", "client");
            
            response.sendRedirect(request.getContextPath() + "/jsp/client/Homepage.jsp");
        } else {
            request.setAttribute("error", "Invalid email or password");
            request.getRequestDispatcher("/jsp/client/SignIn.jsp").forward(request, response);
        }
    }
}
