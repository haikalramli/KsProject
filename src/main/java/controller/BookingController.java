package controller;

import dao.BookingDAO;
import dao.PackageDAO;
import dao.PaymentDAO;
import dao.PhotoDAO;
import model.BookingModel;
import model.PackageModel;
import model.PaymentModel;
import model.Photo;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * BookingController - Handles client bookings
 * Bookings created here will appear in Photographer's Activity Calendar
 */
@WebServlet(name = "BookingController", urlPatterns = {"/BookingController", "/booking/*"})
public class BookingController extends HttpServlet {
    
    private BookingDAO bookingDAO;
    private PackageDAO packageDAO;
    private PaymentDAO paymentDAO;
    private PhotoDAO photoDAO;
    
    @Override
    public void init() throws ServletException {
        bookingDAO = new BookingDAO();
        packageDAO = new PackageDAO();
        paymentDAO = new PaymentDAO();
        photoDAO = new PhotoDAO();
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
        if (action == null) action = "list";
        
        switch (action) {
            case "indoor":
                showIndoorPackages(request, response);
                break;
            case "outdoor":
                showOutdoorPackages(request, response);
                break;
            case "details":
                showBookingDetails(request, response);
                break;
            case "edit": 
                showEditBooking(request, response);
                break;   
            case "photos":
                showPhotos(request, response);
                break;
            case "getBookedTimes":
                getBookedTimes(request, response);
                break;
            case "list":
            default:
                showBookingList(request, response);
                break;
        }
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
        
        switch (action != null ? action : "") {
            case "create":
                createBooking(request, response);
                break;
            case "cancel":
                cancelBooking(request, response);
                break;
            case "update":
                updateBooking(request, response);
                break; 
            default:
                response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
        }
    }
    
    /**
     * Show indoor packages for booking
     */
    private void showIndoorPackages(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        List<PackageModel> packages = packageDAO.findByCategory("Indoor");
        request.setAttribute("packages", packages);
        request.getRequestDispatcher("/jsp/client/BookingIndoor.jsp").forward(request, response);
    }
    
    /**
     * Show outdoor packages for booking
     */
    private void showOutdoorPackages(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        List<PackageModel> packages = packageDAO.findByCategory("Outdoor");
        request.setAttribute("packages", packages);
        request.getRequestDispatcher("/jsp/client/BookingOutdoor.jsp").forward(request, response);
    }
    
    /**
     * Show client's booking list with payment info
     */
    private void showBookingList(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        int clientId = (int) session.getAttribute("clientID");
        
        List<BookingModel> bookings = bookingDAO.getBookingsByClientId(clientId);
        
        // Get payment info for each booking
        java.util.Map<Integer, PaymentModel> paymentMap = new java.util.HashMap<>();
        for (BookingModel booking : bookings) {
            PaymentModel payment = paymentDAO.getByBookingId(booking.getBookingId());
            if (payment != null) {
                paymentMap.put(booking.getBookingId(), payment);
            }
        }
        
        // Check for session messages
        String success = (String) session.getAttribute("success");
        String error = (String) session.getAttribute("error");
        if (success != null) {
            request.setAttribute("success", success);
            session.removeAttribute("success");
        }
        if (error != null) {
            request.setAttribute("error", error);
            session.removeAttribute("error");
        }
        
        request.setAttribute("bookings", bookings);
        request.setAttribute("paymentMap", paymentMap);
        request.getRequestDispatcher("/jsp/client/BookingList.jsp").forward(request, response);
    }
    
    /**
     * Show booking details
     */
    private void showBookingDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        int bookingId = Integer.parseInt(request.getParameter("id"));
        BookingModel booking = bookingDAO.getById(bookingId);
        
        if (booking != null) {
            PaymentModel payment = paymentDAO.getByBookingId(bookingId);
            request.setAttribute("booking", booking);
            request.setAttribute("payment", payment);
        }
        
        request.getRequestDispatcher("/jsp/client/BookingDetails.jsp").forward(request, response);
    }
    
    /**
     * Create new booking (will appear in Photographer's Calendar)
     */
    private void createBooking(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        int clientId = (int) session.getAttribute("clientID");
        
        try {
            int pkgId = Integer.parseInt(request.getParameter("pkgId"));
            String bookDate = request.getParameter("bookDate");
            String bookTime = request.getParameter("bookTime"); // Format: HH:mm
            int bookPax = Integer.parseInt(request.getParameter("bookPax"));
            String location = request.getParameter("location");
            String notes = request.getParameter("notes");
            
            // Get package details for price and duration
            PackageModel pkg = packageDAO.getById(pkgId);
            double totalPrice = pkg.getPkgPrice();
            double durationHours = pkg.getPkgDuration();
            
            // For outdoor, add distance price
            if (pkg.isOutdoor() && request.getParameter("distance") != null) {
                double distance = Double.parseDouble(request.getParameter("distance"));
                totalPrice += (distance * pkg.getDistancePricePerKm());
            }
            
            // Parse time and create timestamps
            java.sql.Timestamp startTime = null;
            java.sql.Timestamp endTime = null;
            
            if (bookTime != null && !bookTime.isEmpty()) {
                try {
                    // Convert 12-hour format (09:00 AM) to 24-hour format
                    int hour = 0;
                    int minute = 0;
                    
                    String timeTrimmed = bookTime.trim().toUpperCase();
                    boolean isPM = timeTrimmed.contains("PM");
                    boolean isAM = timeTrimmed.contains("AM");
                    
                    // Remove AM/PM
                    String timeOnly = timeTrimmed.replace("AM", "").replace("PM", "").trim();
                    String[] timeParts = timeOnly.split(":");
                    
                    hour = Integer.parseInt(timeParts[0].trim());
                    minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1].trim()) : 0;
                    
                    // Convert to 24-hour format
                    if (isPM && hour != 12) {
                        hour += 12;
                    } else if (isAM && hour == 12) {
                        hour = 0;
                    }
                    
                    // Create start timestamp
                    String startTimeStr = bookDate + " " + String.format("%02d:%02d:00", hour, minute);
                    startTime = java.sql.Timestamp.valueOf(startTimeStr);
                    
                    // Check if time slot is already booked
                    if (bookingDAO.isTimeSlotBooked(bookDate, startTime)) {
                        request.setAttribute("error", "This time slot is already booked. Please choose a different time.");
                        response.sendRedirect(request.getContextPath() + "/BookingController?action=" + 
                            (pkg.isIndoor() ? "indoor" : "outdoor") + "&error=timeslot");
                        return;
                    }
                    
                    // Calculate end time based on package duration
                    long durationMillis = (long)(durationHours * 60 * 60 * 1000);
                    endTime = new java.sql.Timestamp(startTime.getTime() + durationMillis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            BookingModel booking = new BookingModel();
            booking.setClId(clientId);
            booking.setPkgId(pkgId);
            booking.setBookDate(bookDate);
            booking.setBookStartTime(startTime);
            booking.setBookEndTime(endTime);
            booking.setBookPax(bookPax);
            booking.setBookLocation(location != null ? location : (pkg.isIndoor() ? "KS Studio" : pkg.getLocation()));
            booking.setTotalPrice(totalPrice);
            booking.setBookStatus("Pending");
            booking.setBookNotes(notes);
            
            if (bookingDAO.saveBooking(booking)) {
                // Create initial payment record
                BookingModel lastBooking = bookingDAO.getLastBookingByClient(clientId);
                if (lastBooking != null) {
                    double depositAmount = totalPrice * 0.3; // 30% deposit
                    paymentDAO.createInitialPayment(lastBooking.getBookingId(), depositAmount, totalPrice - depositAmount);
                }
                
                request.setAttribute("success", "Booking created successfully! Please proceed to payment.");
                response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            } else {
                request.setAttribute("error", "Failed to create booking. Please try again.");
                response.sendRedirect(request.getContextPath() + "/BookingController?action=" + 
                    (pkg.isIndoor() ? "indoor" : "outdoor"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
        }
    }
    
    /**
     * Cancel booking
     */
    private void cancelBooking(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        
        if (bookingDAO.updateStatus(bookingId, "Cancelled")) {
            request.setAttribute("success", "Booking cancelled successfully.");
        } else {
            request.setAttribute("error", "Failed to cancel booking.");
        }
        
        response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
    }
    
    /**
     * Get booked times for a specific date (AJAX endpoint)
     */
    private void getBookedTimes(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String date = request.getParameter("date");
        java.util.List<String> bookedTimes = bookingDAO.getBookedTimeSlots(date);
        
        // Return as JSON array
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < bookedTimes.size(); i++) {
            json.append("\"").append(bookedTimes.get(i)).append("\"");
            if (i < bookedTimes.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        
        response.getWriter().write(json.toString());
    }
    
    /**
     * Show photos for a booking (only if payment is verified)
     */
    private void showPhotos(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        int clientId = (int) request.getSession().getAttribute("clientID");
        
        BookingModel booking = bookingDAO.getById(bookingId);
        
        // Verify booking belongs to this client
        if (booking == null || booking.getClId() != clientId) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        // Check if payment is verified
        boolean paymentVerified = paymentDAO.isPaymentVerified(bookingId);
        
        request.setAttribute("booking", booking);
        request.setAttribute("paymentVerified", paymentVerified);
        
        if (paymentVerified) {
            // Get photo if available - check by folderId in booking
            Photo photo = null;
            if (booking.getFolderId() != null && booking.getFolderId() > 0) {
                photo = photoDAO.getById(booking.getFolderId());
            }
            request.setAttribute("photo", photo);
        }
        
        request.getRequestDispatcher("/jsp/client/MyPhotos.jsp").forward(request, response);
    }
    
    /**
     * Show edit booking form
     */
    private void showEditBooking(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        int clientId = (int) request.getSession().getAttribute("clientID");
        
        BookingModel booking = bookingDAO.getById(bookingId);
        
        // Verify booking belongs to this client
        if (booking == null || booking.getClId() != clientId) {
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        // Only allow edit for Pending or Waiting Approval bookings
        String status = booking.getBookStatus();
        if (!"Pending".equalsIgnoreCase(status) && !"Waiting Approval".equalsIgnoreCase(status)) {
            request.getSession().setAttribute("error", "Only pending bookings can be edited.");
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        request.setAttribute("booking", booking);
        request.getRequestDispatcher("/jsp/client/EditBooking.jsp").forward(request, response);
    }

    /**
     * Update booking date and time
     */
    private void updateBooking(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        int clientId = (int) request.getSession().getAttribute("clientID");
        
        BookingModel booking = bookingDAO.getById(bookingId);
        
        // Verify ownership and status (allow Pending or Waiting Approval)
        String status = booking != null ? booking.getBookStatus() : null;
        boolean canEdit = "Pending".equalsIgnoreCase(status) || "Waiting Approval".equalsIgnoreCase(status);
        
        if (booking == null || booking.getClId() != clientId || !canEdit) {
            request.getSession().setAttribute("error", "Cannot edit this booking.");
            response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            return;
        }
        
        try {
            String newDate = request.getParameter("bookDate");
            String newTime = request.getParameter("bookTime");
            
            // Parse time and create timestamps
            java.sql.Timestamp newStartTime = null;
            java.sql.Timestamp newEndTime = null;
            
            if (newTime != null && !newTime.isEmpty()) {
                String timeTrimmed = newTime.trim().toUpperCase();
                boolean isPM = timeTrimmed.contains("PM");
                String timeOnly = timeTrimmed.replace("AM", "").replace("PM", "").trim();
                String[] timeParts = timeOnly.split(":");
                
                int hour = Integer.parseInt(timeParts[0].trim());
                int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1].trim()) : 0;
                
                if (isPM && hour != 12) hour += 12;
                else if (!isPM && hour == 12) hour = 0;
                
                String startTimeStr = newDate + " " + String.format("%02d:%02d:00", hour, minute);
                newStartTime = java.sql.Timestamp.valueOf(startTimeStr);
                
                // Check if new time slot is available (exclude current booking)
                if (bookingDAO.isTimeSlotBookedExcept(newDate, newStartTime, bookingId)) {
                    request.setAttribute("error", "This time slot is already booked. Please choose another time.");
                    request.setAttribute("booking", booking);
                    request.getRequestDispatcher("/jsp/client/EditBooking.jsp").forward(request, response);
                    return;
                }
                
                // Calculate end time
                long durationMillis = (long)(booking.getPackageDuration() * 60 * 60 * 1000);
                newEndTime = new java.sql.Timestamp(newStartTime.getTime() + durationMillis);
            }
            
            // Update booking
            if (bookingDAO.updateDateTime(bookingId, newDate, newStartTime, newEndTime)) {
                request.getSession().setAttribute("success", "Booking rescheduled successfully! Your new date and time have been confirmed.");
                response.sendRedirect(request.getContextPath() + "/BookingController?action=list");
            } else {
                request.setAttribute("error", "Failed to update booking. Please try again.");
                request.setAttribute("booking", booking);
                request.getRequestDispatcher("/jsp/client/EditBooking.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "An error occurred: " + e.getMessage());
            request.setAttribute("booking", booking);
            request.getRequestDispatcher("/jsp/client/EditBooking.jsp").forward(request, response);
        }
    }
}
