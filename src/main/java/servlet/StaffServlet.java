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
import java.util.List;

@WebServlet(name = "StaffServlet", urlPatterns = {"/staff", "/StaffServlet"})
public class StaffServlet extends HttpServlet {
    
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
        
        // Check if senior
        String role = (String) session.getAttribute("photographerRole");
        if (!"senior".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/calendar");
            return;
        }
        
        displayStaffPage(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("photographerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String role = (String) session.getAttribute("photographerRole");
        if (!"senior".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/calendar");
            return;
        }
        
        String action = request.getParameter("action");
        
        switch (action != null ? action : "") {
            case "create":
                createPhotographer(request, response);
                break;
            case "update":
                updatePhotographer(request, response);
                break;
            case "delete":
                deletePhotographer(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/staff");
        }
    }
    
    private void displayStaffPage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        List<Photographer> photographers = photographerDAO.getAll();
        int seniorCount = photographerDAO.getCountByRole("senior");
        int juniorCount = photographerDAO.getCountByRole("junior");
        int internCount = photographerDAO.getCountByRole("intern");
        
        request.setAttribute("photographers", photographers);
        request.setAttribute("seniorCount", seniorCount);
        request.setAttribute("juniorCount", juniorCount);
        request.setAttribute("internCount", internCount);
        request.setAttribute("totalCount", photographers.size());
        request.setAttribute("currentPage", "staff");
        
        request.getRequestDispatcher("/jsp/photographer/staff.jsp").forward(request, response);
    }
    
    private void createPhotographer(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String role = request.getParameter("role");
        
        // Validation
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            request.setAttribute("error", "Name and email are required!");
            displayStaffPage(request, response);
            return;
        }
        
        if (password == null || password.length() < 6) {
            request.setAttribute("error", "Password must be at least 6 characters!");
            displayStaffPage(request, response);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match!");
            displayStaffPage(request, response);
            return;
        }
        
        if (photographerDAO.emailExists(email.trim())) {
            request.setAttribute("error", "Email already exists!");
            displayStaffPage(request, response);
            return;
        }
        
        HttpSession session = request.getSession();
        int currentUserId = (int) session.getAttribute("photographerId");
        
        Photographer pg = new Photographer();
        pg.setPgName(name.trim());
        pg.setPgEmail(email.trim());
        pg.setPgPh(phone != null ? phone.trim() : "");
        pg.setPgPass(password);
        pg.setPgRole(role != null ? role : "junior");
        pg.setPgStatus("active");
        pg.setPgSnr(currentUserId); // Set supervisor
        
        if (photographerDAO.create(pg)) {
            request.setAttribute("success", "Photographer registered successfully!");
        } else {
            request.setAttribute("error", "Failed to register photographer.");
        }
        
        displayStaffPage(request, response);
    }
    
    private void updatePhotographer(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int pgId = Integer.parseInt(request.getParameter("pgId"));
        Photographer pg = photographerDAO.getById(pgId);
        
        if (pg == null) {
            request.setAttribute("error", "Photographer not found.");
            displayStaffPage(request, response);
            return;
        }
        
        pg.setPgName(request.getParameter("name").trim());
        pg.setPgEmail(request.getParameter("email").trim());
        pg.setPgPh(request.getParameter("phone"));
        pg.setPgRole(request.getParameter("role"));
        pg.setPgStatus(request.getParameter("status"));
        
        if (photographerDAO.update(pg)) {
            request.setAttribute("success", "Photographer updated successfully!");
        } else {
            request.setAttribute("error", "Failed to update photographer.");
        }
        
        displayStaffPage(request, response);
    }
    
    private void deletePhotographer(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int pgId = Integer.parseInt(request.getParameter("pgId"));
        
        HttpSession session = request.getSession();
        int currentUserId = (int) session.getAttribute("photographerId");
        
        // Cannot delete yourself
        if (pgId == currentUserId) {
            request.setAttribute("error", "You cannot delete your own account!");
            displayStaffPage(request, response);
            return;
        }
        
        // Check if trying to delete a senior
        Photographer pg = photographerDAO.getById(pgId);
        if (pg != null && "senior".equalsIgnoreCase(pg.getPgRole())) {
            request.setAttribute("error", "Cannot delete senior photographers!");
            displayStaffPage(request, response);
            return;
        }
        
        if (photographerDAO.delete(pgId)) {
            request.setAttribute("success", "Staff member permanently deleted from database!");
        } else {
            request.setAttribute("error", "Failed to delete staff member.");
        }
        
        displayStaffPage(request, response);
    }
}
