package controller;

import dao.BookingDAO;
import dao.PaymentDAO;
import model.BookingModel;
import model.PaymentModel;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@WebServlet(name = "ClientPaymentController", urlPatterns = {"/ClientPaymentController"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,    // 1 MB
    maxFileSize = 1024 * 1024 * 10,      // 10 MB
    maxRequestSize = 1024 * 1024 * 15    // 15 MB
)
public class ClientPaymentController extends HttpServlet {
    
    private PaymentDAO paymentDAO;
    private BookingDAO bookingDAO;
    private static final String UPLOAD_DIR = "receipts";
    
    @Override
    public void init() throws ServletException {
        paymentDAO = new PaymentDAO();
        bookingDAO = new BookingDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("clientID") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/client/SignIn.jsp");
            return;
        }
        
        String action = request.getParameter("action");
        
        if ("invoice".equals(action)) {
            showInvoice(request, response);
        } else {
            showPaymentPage(request, response);
        }
    }
    
    private void showPaymentPage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String bookingIdStr = request.getParameter("bookingId");
        if (bookingIdStr == null || bookingIdStr.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        int bookingId = Integer.parseInt(bookingIdStr);
        int clientId = (int) request.getSession().getAttribute("clientID");
        
        BookingModel booking = bookingDAO.getById(bookingId);
        
        // Verify booking belongs to this client
        if (booking == null || booking.getClId() != clientId) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        PaymentModel payment = paymentDAO.getByBookingId(bookingId);
        
        // Create payment record if not exists
        if (payment == null) {
            paymentDAO.createPayment(bookingId, booking.getTotalPrice());
            payment = paymentDAO.getByBookingId(bookingId);
        }
        
        request.setAttribute("booking", booking);
        request.setAttribute("payment", payment);
        
        request.getRequestDispatcher("/jsp/client/Payment.jsp").forward(request, response);
    }
    
    private void showInvoice(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String bookingIdStr = request.getParameter("bookingId");
        if (bookingIdStr == null || bookingIdStr.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        int bookingId = Integer.parseInt(bookingIdStr);
        int clientId = (int) request.getSession().getAttribute("clientID");
        
        BookingModel booking = bookingDAO.getById(bookingId);
        
        // Verify booking belongs to this client
        if (booking == null || booking.getClId() != clientId) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        PaymentModel payment = paymentDAO.getByBookingId(bookingId);
        
        request.setAttribute("booking", booking);
        request.setAttribute("payment", payment);
        
        request.getRequestDispatcher("/jsp/client/Invoice.jsp").forward(request, response);
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
        
        if ("submit".equals(action)) {
            submitPayment(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
        }
    }
    
    private void submitPayment(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        String reference = request.getParameter("reference");
        String amountStr = request.getParameter("amount");
        String paymentType = request.getParameter("paymentType"); // 'deposit' or 'full'
        
        int clientId = (int) request.getSession().getAttribute("clientID");
        
        // Verify booking belongs to this client
        BookingModel booking = bookingDAO.getById(bookingId);
        if (booking == null || booking.getClId() != clientId) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        // Validate amount - must be greater than 0
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                request.setAttribute("error", "Payment amount must be greater than RM 0.00. Please enter a valid amount.");
                request.setAttribute("booking", booking);
                request.setAttribute("payment", paymentDAO.getByBookingId(bookingId));
                request.getRequestDispatcher("/jsp/client/Payment.jsp").forward(request, response);
                return;
            }
        } catch (NumberFormatException e) {
            request.setAttribute("error", "Invalid payment amount. Please enter a valid number.");
            request.setAttribute("booking", booking);
            request.setAttribute("payment", paymentDAO.getByBookingId(bookingId));
            request.getRequestDispatcher("/jsp/client/Payment.jsp").forward(request, response);
            return;
        }
        
        // Validate reference number
        if (reference == null || reference.trim().isEmpty()) {
            request.setAttribute("error", "Payment reference number is required.");
            request.setAttribute("booking", booking);
            request.setAttribute("payment", paymentDAO.getByBookingId(bookingId));
            request.getRequestDispatcher("/jsp/client/Payment.jsp").forward(request, response);
            return;
        }
        
        // Handle file upload
        String receiptPath = null;
        Part filePart = request.getPart("receiptFile");
        
        if (filePart != null && filePart.getSize() > 0) {
            String fileName = getSubmittedFileName(filePart);
            if (fileName != null && !fileName.isEmpty()) {
                // Validate file type
                String contentType = filePart.getContentType();
                if (!isValidImageType(contentType)) {
                    request.setAttribute("error", "Invalid file type. Please upload PNG, JPG, JPEG, or GIF image.");
                    request.setAttribute("booking", booking);
                    request.setAttribute("payment", paymentDAO.getByBookingId(bookingId));
                    request.getRequestDispatcher("/jsp/client/Payment.jsp").forward(request, response);
                    return;
                }
                
                // Create upload directory
                String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                
                // Generate unique filename
                String extension = fileName.substring(fileName.lastIndexOf("."));
                String uniqueFileName = "receipt_" + bookingId + "_" + paymentType + "_" + 
                                       UUID.randomUUID().toString().substring(0, 8) + extension;
                
                // Save file
                Path filePath = Paths.get(uploadPath, uniqueFileName);
                try (InputStream input = filePart.getInputStream()) {
                    Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                receiptPath = UPLOAD_DIR + "/" + uniqueFileName;
            }
        }
        
        boolean success;
        
        if ("deposit".equals(paymentType)) {
            // Submit deposit payment - updates DEPOPREF column
            success = paymentDAO.submitDepositPayment(bookingId, reference, amount, receiptPath);
            if (success) {
                request.setAttribute("success", "Deposit payment submitted successfully! Please wait for admin verification.");
            }
        } else {
            // Submit full/remaining payment - updates FULLPREF column
            success = paymentDAO.submitFullPayment(bookingId, reference, amount, receiptPath);
            if (success) {
                request.setAttribute("success", "Full payment submitted successfully! Please wait for admin verification.");
            }
        }
        
        if (!success) {
            request.setAttribute("error", "Failed to submit payment. Please try again.");
        }
        
        // Reload page
        request.setAttribute("booking", booking);
        request.setAttribute("payment", paymentDAO.getByBookingId(bookingId));
        request.getRequestDispatcher("/jsp/client/Payment.jsp").forward(request, response);
    }
    
    private String getSubmittedFileName(Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/png") ||
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg") ||
            contentType.equals("image/gif")
        );
    }
}
