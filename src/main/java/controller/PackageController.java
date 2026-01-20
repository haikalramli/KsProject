package controller;

import dao.PackageDAO;
import model.PackageModel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * PackageController - For Client to view packages created by Photographer
 */
@WebServlet(name = "PackageController", urlPatterns = {"/packages", "/PackageController"})
public class PackageController extends HttpServlet {
    
    private PackageDAO packageDAO;
    
    @Override
    public void init() throws ServletException {
        packageDAO = new PackageDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String category = request.getParameter("category");
        List<PackageModel> packages;
        
        if (category != null && !category.isEmpty()) {
            packages = packageDAO.findByCategory(category);
            request.setAttribute("selectedCategory", category);
        } else {
            packages = packageDAO.findAll();
        }
        
        request.setAttribute("packages", packages);
        request.setAttribute("indoorCount", packageDAO.getCountByCategory("Indoor"));
        request.setAttribute("outdoorCount", packageDAO.getCountByCategory("Outdoor"));
        
        request.getRequestDispatcher("/jsp/client/Packages.jsp").forward(request, response);
    }
}
