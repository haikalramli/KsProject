package controller;

import dao.ClientDAO;
import model.Client;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "SignUpController", urlPatterns = {"/SignUpController", "/client/register"})
public class SignUpController extends HttpServlet {
    
    private ClientDAO clientDAO;
    
    @Override
    public void init() throws ServletException {
        clientDAO = new ClientDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/jsp/client/SignUp.jsp");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        
        // Validation
        if (name == null || name.trim().isEmpty()) {
            request.setAttribute("error", "Name is required");
            request.getRequestDispatcher("/jsp/client/SignUp.jsp").forward(request, response);
            return;
        }
        
        if (email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Email is required");
            request.getRequestDispatcher("/jsp/client/SignUp.jsp").forward(request, response);
            return;
        }
        
        if (password == null || password.length() < 6) {
            request.setAttribute("error", "Password must be at least 6 characters");
            request.getRequestDispatcher("/jsp/client/SignUp.jsp").forward(request, response);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match");
            request.getRequestDispatcher("/jsp/client/SignUp.jsp").forward(request, response);
            return;
        }
        
        // Check if email exists
        if (clientDAO.emailExists(email.trim())) {
            request.setAttribute("error", "Email already registered");
            request.getRequestDispatcher("/jsp/client/SignUp.jsp").forward(request, response);
            return;
        }
        
        // Create client
        Client client = new Client();
        client.setClName(name.trim());
        client.setClEmail(email.trim());
        client.setClPh(phone != null ? phone.trim() : "");
        client.setClAddress(address != null ? address.trim() : "");
        client.setClPass(password);
        
        if (clientDAO.register(client)) {
            request.setAttribute("success", "Registration successful! Please login.");
            request.getRequestDispatcher("/jsp/client/SignIn.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Registration failed. Please try again.");
            request.getRequestDispatcher("/jsp/client/SignUp.jsp").forward(request, response);
        }
    }
}
