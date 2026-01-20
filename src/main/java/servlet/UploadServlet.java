package servlet;

import dao.BookingDAO;
import dao.PhotoDAO;
import model.BookingModel;
import model.Photo;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "UploadServlet", urlPatterns = {"/upload", "/UploadServlet"})
public class UploadServlet extends HttpServlet {
    
    private PhotoDAO photoDAO;
    private BookingDAO bookingDAO;
    
    @Override
    public void init() throws ServletException {
        photoDAO = new PhotoDAO();
        bookingDAO = new BookingDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("photographerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        // Get bookings with verified payment but no photo uploaded (Pending Upload)
        List<java.util.Map<String, Object>> pendingUpload = bookingDAO.getBookingsWithVerifiedPaymentNoPhoto();
        
        // Get bookings with verified payment AND photo uploaded (Completed Upload)
        List<java.util.Map<String, Object>> completedUpload = bookingDAO.getBookingsWithVerifiedPaymentAndPhoto();
        
        // Get all uploaded photos (Upload History)
        List<Photo> uploadHistory = photoDAO.getAll();
        
        request.setAttribute("pendingUpload", pendingUpload);
        request.setAttribute("completedUpload", completedUpload);
        request.setAttribute("uploadHistory", uploadHistory);
        request.setAttribute("currentPage", "upload");
        
        request.getRequestDispatcher("/jsp/photographer/upload.jsp").forward(request, response);
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
            case "upload":
                uploadPhoto(request, response, photographerId);
                break;
            case "updateNotes":
                updateNotes(request, response);
                break;
            case "delete":
                deletePhoto(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/upload");
        }
    }
    
    private void uploadPhoto(HttpServletRequest request, HttpServletResponse response, int photographerId) 
            throws ServletException, IOException {
        
        String folderName = request.getParameter("folderName");
        String folderLink = request.getParameter("folderLink");
        String notesForClient = request.getParameter("notesForClient");
        String bookingIdStr = request.getParameter("bookingId");
        
        if (folderName == null || folderName.trim().isEmpty() || 
            folderLink == null || folderLink.trim().isEmpty()) {
            request.setAttribute("error", "Folder name and link are required!");
            doGet(request, response);
            return;
        }
        
        Photo photo = new Photo();
        photo.setFolderName(folderName.trim());
        photo.setFolderLink(folderLink.trim());
        photo.setNotesForClient(notesForClient != null ? notesForClient.trim() : "");
        photo.setUploadedBy(photographerId);
        
        int folderId = photoDAO.create(photo);
        
        if (folderId > 0) {
            // If booking selected, link photo to booking
            if (bookingIdStr != null && !bookingIdStr.isEmpty()) {
                int bookingId = Integer.parseInt(bookingIdStr);
                bookingDAO.linkPhotoToBooking(bookingId, folderId);
            }
            request.setAttribute("success", "Photo folder uploaded successfully! Client will see the link after payment is verified.");
        } else {
            request.setAttribute("error", "Failed to upload photo folder.");
        }
        
        doGet(request, response);
    }
    
    private void updateNotes(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int folderId = Integer.parseInt(request.getParameter("folderId"));
        String notes = request.getParameter("notesForClient");
        
        if (photoDAO.updateNotes(folderId, notes)) {
            request.setAttribute("success", "Notes updated successfully!");
        } else {
            request.setAttribute("error", "Failed to update notes.");
        }
        
        doGet(request, response);
    }
    
    private void deletePhoto(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int folderId = Integer.parseInt(request.getParameter("folderId"));
        
        if (photoDAO.delete(folderId)) {
            request.setAttribute("success", "Photo folder deleted successfully!");
        } else {
            request.setAttribute("error", "Failed to delete photo folder.");
        }
        
        doGet(request, response);
    }
}
