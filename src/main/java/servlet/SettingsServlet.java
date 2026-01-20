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

@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings", "/SettingsServlet"})
public class SettingsServlet extends HttpServlet {
    
    private PhotographerDAO photographerDAO;
    
    @Override
    public void init() throws ServletException {
        photographerDAO = new PhotographerDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("photographerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        int photographerId = (int) session.getAttribute("photographerId");
        Photographer photographer = photographerDAO.getById(photographerId);
        
        request.setAttribute("photographer", photographer);
        request.setAttribute("currentPage", "settings");
        
        request.getRequestDispatcher("/jsp/photographer/settings.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("photographerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        int photographerId = (int) session.getAttribute("photographerId");
        
        switch (action != null ? action : "") {
            case "updateProfile":
                updateProfile(request, response, photographerId);
                break;
            case "changePassword":
                changePassword(request, response, photographerId);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/settings");
        }
    }
    
    private void updateProfile(HttpServletRequest request, HttpServletResponse response, int photographerId) 
            throws ServletException, IOException {
        
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Name and email are required!");
            doGet(request, response);
            return;
        }
        
        Photographer photographer = photographerDAO.getById(photographerId);
        photographer.setPgName(name.trim());
        photographer.setPgEmail(email.trim());
        photographer.setPgPh(phone != null ? phone.trim() : "");
        
        if (photographerDAO.update(photographer)) {
            // Update session
            HttpSession session = request.getSession();
            session.setAttribute("photographerName", photographer.getPgName());
            session.setAttribute("photographerEmail", photographer.getPgEmail());
            
            request.setAttribute("success", "Profile updated successfully!");
        } else {
            request.setAttribute("error", "Failed to update profile.");
        }
        
        doGet(request, response);
    }
    
    private void changePassword(HttpServletRequest request, HttpServletResponse response, int photographerId) 
            throws ServletException, IOException {
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            request.setAttribute("error", "All password fields are required!");
            doGet(request, response);
            return;
        }
        
        if (newPassword.length() < 6) {
            request.setAttribute("error", "New password must be at least 6 characters!");
            doGet(request, response);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "New passwords do not match!");
            doGet(request, response);
            return;
        }
        
        Photographer photographer = photographerDAO.getById(photographerId);
        
        // Verify current password
        if (!photographer.getPgPass().equals(currentPassword)) {
            request.setAttribute("error", "Current password is incorrect!");
            doGet(request, response);
            return;
        }
        
        if (photographerDAO.updatePassword(photographerId, newPassword)) {
            request.setAttribute("success", "Password changed successfully!");
        } else {
            request.setAttribute("error", "Failed to change password.");
        }
        
        doGet(request, response);
    }
}
