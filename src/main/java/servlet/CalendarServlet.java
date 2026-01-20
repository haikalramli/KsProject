package servlet;

import dao.BookingDAO;
import dao.PhotographerDAO;
import model.BookingModel;
import model.Photographer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
 
/**
 * CalendarServlet - Shows client bookings to photographer
 * Client bookings appear here in the Activity Calendar
 */
@WebServlet(name = "CalendarServlet", urlPatterns = {"/calendar", "/CalendarServlet"})
public class CalendarServlet extends HttpServlet {
    
    private BookingDAO bookingDAO;
    private PhotographerDAO photographerDAO;
    
    @Override
    public void init() throws ServletException {
        bookingDAO = new BookingDAO();
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
        
        String action = request.getParameter("action");
        
        if ("getBookings".equals(action)) {
            // AJAX request for calendar data
            getCalendarData(request, response);
        } else if ("getBookingsByDate".equals(action)) {
            // Get bookings for specific date
            getBookingsByDate(request, response);
        } else {
            // Display calendar page
            displayCalendar(request, response);
        }
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
        
        switch (action != null ? action : "") {
            case "updateStatus":
                updateBookingStatus(request, response);
                break;
            case "assignPhotographer":
                assignPhotographer(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/calendar");
        }
    }
    
    /**
     * Display calendar page with all bookings
     */
    private void displayCalendar(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Get all bookings for display
        List<BookingModel> allBookings = bookingDAO.getAllBookings();
        request.setAttribute("bookings", allBookings);
        
        // Get photographers for assignment dropdown
        List<Photographer> photographers = photographerDAO.getAll();
        request.setAttribute("photographers", photographers);
        
        // Get booking counts
        request.setAttribute("pendingCount", bookingDAO.getCountByStatus("Pending"));
        request.setAttribute("confirmedCount", bookingDAO.getCountByStatus("Confirmed"));
        request.setAttribute("completedCount", bookingDAO.getCountByStatus("Completed"));
        
        request.setAttribute("currentPage", "calendar");
        request.getRequestDispatcher("/jsp/photographer/calendar.jsp").forward(request, response);
    }
    
    /**
     * Get calendar data for AJAX (returns JSON)
     */
    private void getCalendarData(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int year = Integer.parseInt(request.getParameter("year"));
        int month = Integer.parseInt(request.getParameter("month"));

        List<Map<String, Object>> bookingData = bookingDAO.getBookingsByMonth(year, month);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder();
        json.append("[");

        for (int i = 0; i < bookingData.size(); i++) {
            Map<String, Object> data = bookingData.get(i);

            json.append("{");
            json.append("\"date\":\"").append(data.get("date")).append("\",");
            json.append("\"count\":").append(data.get("count")).append(",");
            json.append("\"statuses\":\"").append(data.get("statuses")).append("\"");
            json.append("}");

            if (i < bookingData.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");

        PrintWriter out = response.getWriter();
        out.print(json.toString());
        out.flush();
    }

    
    /**
     * Get bookings for specific date (returns JSON)
     */
    private void getBookingsByDate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String date = request.getParameter("date");
        List<BookingModel> bookings = bookingDAO.getBookingsByDate(date);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder();
        json.append("[");

        for (int i = 0; i < bookings.size(); i++) {
            BookingModel b = bookings.get(i);

            json.append("{");
            json.append("\"bookingId\":").append(b.getBookingId()).append(",");
            json.append("\"clientName\":\"").append(b.getClientName()).append("\",");
            json.append("\"clientPhone\":\"").append(b.getClientPhone()).append("\",");
            json.append("\"packageName\":\"").append(b.getPackageName()).append("\",");
            json.append("\"packageCateg\":\"").append(b.getPackageCateg()).append("\",");
            json.append("\"bookTime\":\"").append(b.getBookTime()).append("\",");
            json.append("\"bookPax\":").append(b.getBookPax()).append(",");
            json.append("\"totalPrice\":").append(b.getTotalPrice()).append(",");
            json.append("\"status\":\"").append(b.getBookStatus()).append("\",");
            json.append("\"location\":\"").append(b.getBookLocation()).append("\",");
            json.append("\"photographerName\":\"").append(b.getPhotographerName()).append("\"");
            json.append("}");

            if (i < bookings.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");

        PrintWriter out = response.getWriter();
        out.print(json.toString());
        out.flush();
    }

    
    /**
     * Update booking status
     */
    private void updateBookingStatus(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        String status = request.getParameter("status");
        
        boolean success = bookingDAO.updateStatus(bookingId, status);
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print("{\"success\":" + success + "}");
        out.flush();
    }
    
    /**
     * Assign photographer to booking
     */
    private void assignPhotographer(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        int bookingId = Integer.parseInt(request.getParameter("bookingId"));
        int photographerId = Integer.parseInt(request.getParameter("photographerId"));
        
        boolean success = bookingDAO.assignPhotographer(bookingId, photographerId);
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print("{\"success\":" + success + "}");
        out.flush();
    }
}
