package servlet;

import dao.BookingDAO;
import dao.PaymentDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = {"/payment", "/PaymentServlet"})
public class PaymentServlet extends HttpServlet {
    
    private PaymentDAO paymentDAO;
    private BookingDAO bookingDAO;
    
    @Override
    public void init() throws ServletException {
        paymentDAO = new PaymentDAO();
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
        
        displayPaymentPage(request, response);
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
            case "verify":
                verifyPayment(request, response, photographerId);
                break;
            case "reject":
                rejectPayment(request, response, photographerId);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/payment");
        }
    }
    
    private void displayPaymentPage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Get pending payments (submitted, waiting for verification)
        List<Map<String, Object>> pendingPayments = paymentDAO.getPaymentsByStatusList("submitted");
        
        // Get completed payments (fully verified)
        List<Map<String, Object>> completedPayments = paymentDAO.getPaymentsByStatusList("verified");
        
        // Get confirm history (all payments with verifiedDate not null)
        List<Map<String, Object>> confirmHistory = paymentDAO.getConfirmHistory();
        
        request.setAttribute("pendingPayments", pendingPayments);
        request.setAttribute("completedPayments", completedPayments);
        request.setAttribute("confirmHistory", confirmHistory);
        request.setAttribute("currentPage", "payment");
        
        request.getRequestDispatcher("/jsp/photographer/payment.jsp").forward(request, response);
    }
    
    private void verifyPayment(HttpServletRequest request, HttpServletResponse response, int photographerId) 
            throws ServletException, IOException {
        
        int payId = Integer.parseInt(request.getParameter("payId"));
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        String verifyType = request.getParameter("verifyType");
        double verifyAmount = Double.parseDouble(request.getParameter("verifyAmount"));
        
        boolean success;
        
        if ("deposit".equals(verifyType)) {
            // Verify deposit - adds to paid amount, decreases remaining
            success = paymentDAO.verifyDepositPayment(payId, verifyAmount, photographerId);
            if (success) {
                request.setAttribute("success", "Deposit payment verified! RM " + String.format("%.2f", verifyAmount) + " has been added to paid amount.");
            }
        } else if ("full".equals(verifyType)) {
            // Verify full payment - marks as fully verified
            success = paymentDAO.verifyFullPayment(payId, verifyAmount, photographerId);
            if (success) {
                // Update booking status to Confirmed
                bookingDAO.updateStatus(bookingId, "Confirmed");
                request.setAttribute("success", "Full payment verified! Client can now view their photos.");
            }
        } else {
            request.setAttribute("error", "Please select payment type to verify.");
            displayPaymentPage(request, response);
            return;
        }
        
        if (!success) {
            request.setAttribute("error", "Failed to verify payment.");
        }
        
        displayPaymentPage(request, response);
    }
    
    private void rejectPayment(HttpServletRequest request, HttpServletResponse response, int photographerId) 
            throws ServletException, IOException {
        
        int payId = Integer.parseInt(request.getParameter("payId"));
        
        if (paymentDAO.rejectPayment(payId, photographerId)) {
            request.setAttribute("success", "Payment rejected. Client will be notified.");
        } else {
            request.setAttribute("error", "Failed to reject payment.");
        }
        
        displayPaymentPage(request, response);
    }
}
