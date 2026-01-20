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

@WebServlet(name = "ClientProfileController", urlPatterns = {"/ClientProfileController"})
public class ClientProfileController extends HttpServlet {
    
    private ClientDAO clientDAO;
    
    @Override
    public void init() throws ServletException {
        clientDAO = new ClientDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("clientID") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/client/SignIn.jsp");
            return;
        }
        
        int clientId = (int) session.getAttribute("clientID");
        Client client = clientDAO.getById(clientId);
        
        request.setAttribute("client", client);
        request.getRequestDispatcher("/jsp/client/EditProfile.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("clientID") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/client/SignIn.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        int clientId = (int) session.getAttribute("clientID");
        
        switch (action != null ? action : "") {
            case "updateProfile":
                updateProfile(request, response, clientId);
                break;
            case "changePassword":
                changePassword(request, response, clientId);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/ClientProfileController");
        }
    }
    
    private void updateProfile(HttpServletRequest request, HttpServletResponse response, int clientId) 
            throws ServletException, IOException {
        
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Name and email are required!");
            loadAndForward(request, response, clientId);
            return;
        }
        
        Client client = clientDAO.getById(clientId);
        client.setClName(name.trim());
        client.setClEmail(email.trim());
        client.setClPh(phone != null ? phone.trim() : "");
        client.setClAddress(address != null ? address.trim() : "");
        
        if (clientDAO.update(client)) {
            // Update session
            HttpSession session = request.getSession();
            session.setAttribute("clientName", client.getClName());
            session.setAttribute("clientEmail", client.getClEmail());
            
            request.setAttribute("success", "Profile updated successfully!");
        } else {
            request.setAttribute("error", "Failed to update profile. Please try again.");
        }
        
        loadAndForward(request, response, clientId);
    }
    
    private void changePassword(HttpServletRequest request, HttpServletResponse response, int clientId) 
            throws ServletException, IOException {
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            request.setAttribute("error", "All password fields are required!");
            loadAndForward(request, response, clientId);
            return;
        }
        
        if (newPassword.length() < 6) {
            request.setAttribute("error", "New password must be at least 6 characters!");
            loadAndForward(request, response, clientId);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "New passwords do not match!");
            loadAndForward(request, response, clientId);
            return;
        }
        
        Client client = clientDAO.getById(clientId);
        
        // Verify current password
        if (!client.getClPass().equals(currentPassword)) {
            request.setAttribute("error", "Current password is incorrect!");
            loadAndForward(request, response, clientId);
            return;
        }
        
        if (clientDAO.updatePassword(clientId, newPassword)) {
            request.setAttribute("success", "Password changed successfully! Please use your new password next time you login.");
        } else {
            request.setAttribute("error", "Failed to change password. Please try again.");
        }
        
        loadAndForward(request, response, clientId);
    }
    
    private void loadAndForward(HttpServletRequest request, HttpServletResponse response, int clientId) 
            throws ServletException, IOException {
        Client client = clientDAO.getById(clientId);
        request.setAttribute("client", client);
        request.getRequestDispatcher("/jsp/client/EditProfile.jsp").forward(request, response);
    }
}
