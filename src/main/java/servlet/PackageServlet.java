package servlet;

import dao.PackageDAO;
import model.PackageModel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * PackageServlet - For Photographer to manage packages
 * Packages created here will be visible to Clients
 */
@WebServlet(name = "PackageServlet", urlPatterns = {"/package", "/PackageServlet"})
public class PackageServlet extends HttpServlet {
    
    private PackageDAO packageDAO;
    
    @Override
    public void init() throws ServletException {
        packageDAO = new PackageDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("photographerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("get".equals(action)) {
            // Return single package as JSON for edit
            getPackageJson(request, response);
        } else {
            displayPackages(request, response);
        }
    }
    
    private void getPackageJson(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int pkgId = Integer.parseInt(request.getParameter("pkgId"));
        PackageModel pkg = packageDAO.getById(pkgId);
        
        if (pkg == null) {
            response.setStatus(404);
            response.getWriter().write("{\"error\":\"Package not found\"}");
            return;
        }
        
        // Build JSON manually
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        StringBuilder json = new StringBuilder("{");
        json.append("\"pkgId\":").append(pkg.getPkgId()).append(",");
        json.append("\"pkgName\":\"").append(escapeJson(pkg.getPkgName())).append("\",");
        json.append("\"pkgPrice\":").append(pkg.getPkgPrice()).append(",");
        json.append("\"pkgDuration\":").append(pkg.getPkgDuration()).append(",");
        json.append("\"pkgDesc\":\"").append(escapeJson(pkg.getPkgDesc() != null ? pkg.getPkgDesc() : "")).append("\",");
        json.append("\"pkgStatus\":\"").append(pkg.getPkgStatus() != null ? pkg.getPkgStatus() : "active").append("\",");
        json.append("\"pkgCateg\":\"").append(pkg.getPkgCateg()).append("\",");
        json.append("\"eventType\":\"").append(escapeJson(pkg.getEventType() != null ? pkg.getEventType() : "")).append("\",");
        
        if (pkg.isIndoor()) {
            json.append("\"numOfPax\":").append(pkg.getNumOfPax()).append(",");
            json.append("\"backgType\":\"").append(escapeJson(pkg.getBackgType() != null ? pkg.getBackgType() : "")).append("\"");
        } else {
            json.append("\"location\":\"").append(escapeJson(pkg.getLocation() != null ? pkg.getLocation() : "")).append("\",");
            json.append("\"distance\":").append(pkg.getDistance()).append(",");
            json.append("\"distancePricePerKm\":").append(pkg.getDistancePricePerKm());
        }
        
        json.append("}");
        
        response.getWriter().write(json.toString());
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
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
        
        try {
            switch (action != null ? action : "") {
                case "createIndoor":
                    createIndoorPackage(request, response, photographerId);
                    break;
                case "createOutdoor":
                    createOutdoorPackage(request, response, photographerId);
                    break;
                case "update":
                    updatePackage(request, response);
                    break;
                case "delete":
                    deletePackage(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/package");
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Error: " + e.getMessage());
            displayPackages(request, response);
        }
    }
    
    private void displayPackages(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String category = request.getParameter("category");
        List<PackageModel> packages;
        
        if ("Indoor".equals(category)) {
            packages = packageDAO.findByCategory("Indoor");
        } else if ("Outdoor".equals(category)) {
            packages = packageDAO.findByCategory("Outdoor");
        } else {
            packages = packageDAO.findAll();
        }
        
        request.setAttribute("packages", packages);
        request.setAttribute("selectedCategory", category);
        request.setAttribute("indoorCount", packageDAO.getCountByCategory("Indoor"));
        request.setAttribute("outdoorCount", packageDAO.getCountByCategory("Outdoor"));
        request.setAttribute("currentPage", "package");
        
        request.getRequestDispatcher("/jsp/photographer/package.jsp").forward(request, response);
    }
    
    private void createIndoorPackage(HttpServletRequest request, HttpServletResponse response, int createdBy) 
            throws ServletException, IOException {
        
        PackageModel pkg = new PackageModel();
        pkg.setPkgName(request.getParameter("pkgName"));
        pkg.setPkgPrice(Double.parseDouble(request.getParameter("pkgPrice")));
        pkg.setPkgDuration(Double.parseDouble(request.getParameter("pkgDuration")));
        pkg.setEventType(request.getParameter("eventType"));
        pkg.setPkgDesc(request.getParameter("pkgDesc"));
        pkg.setNumOfPax(Integer.parseInt(request.getParameter("numOfPax")));
        pkg.setBackgType(request.getParameter("backgType"));
        pkg.setCreatedBy(createdBy);
        
        if (packageDAO.createIndoorPackage(pkg)) {
            request.setAttribute("success", "Indoor package created successfully! Clients can now view and book it.");
        } else {
            request.setAttribute("error", "Failed to create package.");
        }
        
        displayPackages(request, response);
    }
    
    private void createOutdoorPackage(HttpServletRequest request, HttpServletResponse response, int createdBy) 
            throws ServletException, IOException {
        
        PackageModel pkg = new PackageModel();
        pkg.setPkgName(request.getParameter("pkgName"));
        pkg.setPkgPrice(Double.parseDouble(request.getParameter("pkgPrice")));
        pkg.setPkgDuration(Double.parseDouble(request.getParameter("pkgDuration")));
        pkg.setEventType(request.getParameter("eventType"));
        pkg.setPkgDesc(request.getParameter("pkgDesc"));
        pkg.setDistance(Double.parseDouble(request.getParameter("distance")));
        pkg.setDistancePricePerKm(Double.parseDouble(request.getParameter("pricePerKm")));
        pkg.setLocation(request.getParameter("location"));
        pkg.setCreatedBy(createdBy);
        
        if (packageDAO.createOutdoorPackage(pkg)) {
            request.setAttribute("success", "Outdoor package created successfully! Clients can now view and book it.");
        } else {
            request.setAttribute("error", "Failed to create package.");
        }
        
        displayPackages(request, response);
    }
    
    private void updatePackage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int pkgId = Integer.parseInt(request.getParameter("pkgId"));
        PackageModel existing = packageDAO.getById(pkgId);
        
        if (existing == null) {
            request.setAttribute("error", "Package not found.");
            displayPackages(request, response);
            return;
        }
        
        existing.setPkgName(request.getParameter("pkgName"));
        existing.setPkgPrice(Double.parseDouble(request.getParameter("pkgPrice")));
        existing.setPkgDuration(Double.parseDouble(request.getParameter("pkgDuration")));
        existing.setEventType(request.getParameter("eventType"));
        existing.setPkgDesc(request.getParameter("pkgDesc"));
        existing.setPkgStatus(request.getParameter("pkgStatus"));
        
        boolean success;
        if (existing.isIndoor()) {
            existing.setNumOfPax(Integer.parseInt(request.getParameter("numOfPax")));
            existing.setBackgType(request.getParameter("backgType"));
            success = packageDAO.updateIndoorPackage(existing);
        } else {
            existing.setDistance(Double.parseDouble(request.getParameter("distance")));
            existing.setDistancePricePerKm(Double.parseDouble(request.getParameter("pricePerKm")));
            existing.setLocation(request.getParameter("location"));
            success = packageDAO.updateOutdoorPackage(existing);
        }
        
        if (success) {
            request.setAttribute("success", "Package updated successfully!");
        } else {
            request.setAttribute("error", "Failed to update package.");
        }
        
        displayPackages(request, response);
    }
    
    private void deletePackage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int pkgId = Integer.parseInt(request.getParameter("pkgId"));
        
        if (packageDAO.delete(pkgId)) {
            request.setAttribute("success", "Package permanently deleted from database!");
        } else {
            request.setAttribute("error", "Failed to delete package.");
        }
        
        displayPackages(request, response);
    }
}
