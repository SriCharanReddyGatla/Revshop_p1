package com.rev.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rev.entity.Product;
import com.rev.entity.Seller;
import com.rev.service.EmailService;
import com.rev.service.ProductService;
import com.rev.service.SellerService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/seller")
public class SellerController {
	
    @Autowired
    private SellerService sellerService;

    @GetMapping("/")
    @ResponseBody
    private String sellerWelcome() {
		return "hello seller side";
	}

    @GetMapping("/logout")
    private String logout(HttpSession session) {
    	session.invalidate();
		return "redirect:/seller/login";
	}

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("seller", new Seller());
        return "seller_register"; 
    }

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public String registerSeller(@ModelAttribute Seller seller,RedirectAttributes redirectAttributes) {
    	System.out.println(seller.getUsername()+" "+seller.getEmail());
        sellerService.saveSeller(seller);
     // Send registration confirmation email
        emailService.sendRegistrationEmail(seller.getEmail(), seller.getUsername(), "seller");

        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! A confirmation email has been sent to your registered email address.");

        return "redirect:/seller/login";  
    }

    // Endpoint to show login form
    @GetMapping("/login")
    public String showLoginForm(Model model) {
    	System.out.println("in seller login");
        return "seller_login";
    }

    // Endpoint for seller login
    @PostMapping("/login")
    public String login(@ModelAttribute Seller seller, Model model, HttpSession session) {
        Optional<Seller> existingSeller = Optional.ofNullable(sellerService.findByUsername(seller.getUsername()));

        if (existingSeller.isPresent() && existingSeller.get().getPassword().equals(seller.getPassword())) {
            // Successful login, store the username in session
            session.setAttribute("sellerName", seller.getUsername());
            return "redirect:/seller/dashboard";  // Redirect to seller dashboard
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "seller_login";  // Return to login page with error
        }
    }
    
    @SuppressWarnings("unused")
	@GetMapping("/dashboard")
    public String showSellerDashboard(Model model,HttpSession session){
    	String sellerName = (String) session.getAttribute("sellerName");
    	model.addAttribute("sellerName",sellerName);
    	System.out.println(sellerName);
    	if(sellerName==null) 
        	return "redirect:/seller/login";
    	else
    		return "seller_dashboard";  // Name of your JSP file for the dashboard

    }

    // Endpoint to show seller profile
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        // Get the sellerName (or username) from the session
        String sellerName = (String) session.getAttribute("sellerName");
        if (sellerName == null) {
            // If the session attribute is missing, redirect to login
            return "redirect:/seller/login";
        }

        Optional<Seller> sellerOpt = Optional.ofNullable(sellerService.findByUsername(sellerName));
        if (sellerOpt.isPresent()) {
            model.addAttribute("seller", sellerOpt.get());
            return "seller_profile";
        }
        return "redirect:/seller/login";
    }


    @PostMapping("/updateProfile")
    public String updateSellerProfile(@ModelAttribute Seller seller, Model model, HttpSession session) {
        // Retrieve the username from session
        String sellerName = (String) session.getAttribute("sellerName");
        if (sellerName == null) {
            return "redirect:/seller/login";  // Redirect to login if session is missing
        }

        Optional<Seller> existingSellerOpt =Optional.of(sellerService.findByUsername(sellerName));
        if (existingSellerOpt.isPresent()) {
            Seller existingSeller = existingSellerOpt.get();
            existingSeller.setEmail(seller.getEmail());
            existingSeller.setPhoneNumber(seller.getPhoneNumber());
            existingSeller.setAddress(seller.getAddress());
            // Update password only if a new one is provided
            if (seller.getPassword() != null && !seller.getPassword().isEmpty()) {
                existingSeller.setPassword(seller.getPassword());
            }
            sellerService.saveSeller(existingSeller); // Save updated seller
            model.addAttribute("success", "Profile updated successfully!");
        } else {
            model.addAttribute("error", "Seller not found!");
        }
        
        return "redirect:/seller/dashboard";  // Redirect back to profile after updating
    }
    
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/add-product")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product()); // Create a new Product object
        return "seller_add_product"; // Name of the JSP file to render
    }
    @GetMapping("/my-products")
    public String showMyProducts(Model model, @SessionAttribute String sellerName) {
        Optional<Seller> seller = Optional.ofNullable(sellerService.findByUsername(sellerName));
        if (seller.isPresent()) {
            List<Product> products = productService.getProductsBySellerId(seller.get().getId());
            model.addAttribute("products", products);
            return "seller_my_product"; // Ensure this view exists
        } else {
            return "redirect:/seller/login"; // Redirect to login if seller not found
        }
    }

    @PostMapping("/add-product")
    public String addProduct(@ModelAttribute Product product, 
                             @SessionAttribute String sellerName, 
                             RedirectAttributes redirectAttributes) {
        Optional<Seller> seller = Optional.ofNullable(sellerService.findByUsername(sellerName));
        if (seller.isPresent()) {
            product.setSellerId(seller.get().getId()); // Set the seller ID to the product
            productService.addProduct(product); // Call your service to save the product

            // Add a success message
            redirectAttributes.addFlashAttribute("message", "Product added successfully!");

            return "redirect:/seller/dashboard"; // Redirect to the seller's products page
        } else {
            return "redirect:/seller/login"; // Redirect to login if the seller is not found
        }
        
    }
    
    
    @GetMapping("/delete-product/{productId}")
    public String deleteProduct(@PathVariable Long productId, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(productId); // Call service to delete the product
            redirectAttributes.addFlashAttribute("message", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
            e.printStackTrace(); // Print stack trace to console for debugging
        }
        return "redirect:/seller/dashboard"; // Redirect to the seller's products page
    }

    // Endpoint to show seller's products

    @GetMapping("/edit-product/{productId}")
    public String showUpdateProductForm(@PathVariable Long productId, Model model) {
        Product product = productService.getProductById(productId);
        model.addAttribute("product", product); // Add product to the model
        return "seller_edit_product"; // Name of the JSP file to render the edit form
    }
    
    @PostMapping("/edit-product/{productId}")
    public String updateProduct(
        @PathVariable Long productId,
        @ModelAttribute("product") Product updatedProduct,
        RedirectAttributes redirectAttributes) {
        
    	
        try {
            productService.updateProduct(productId, updatedProduct); // Call service to update the product
            redirectAttributes.addFlashAttribute("message", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }
        return "redirect:/seller/dashboard"; // Redirect back to the products list
    }

    
    
    private Map<String, String> otpStorage = new HashMap<>();

    
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        System.out.println("Sending OTP to: " + email); // Logging email
        try {
            // Generate a 6-digit OTP
            String otp = String.format("%06d", new Random().nextInt(1000000)); // Ensure OTP is 6 digits
            otpStorage.put(email, otp); // Store the OTP associated with the email
            
            // Log the OTP and email for verification
            System.out.println("Generated OTP: " + otp + " for Email: " + email);

            // Send OTP to user's email
            emailService.sendOtpEmail(email, otp); 

            // Return a structured JSON response
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP sent successfully to " + email));
        } catch (Exception e) {
            System.err.println("Error sending OTP email: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            // Return a structured error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("success", false, "message", "Failed to send OTP. Please try again."));
        }
    }


    // Verify the OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam("email") String email, 
                                                          @RequestParam("otp") String otp) {
        // Retrieve stored OTP from the storage
        String storedOtp = otpStorage.get(email);
        System.out.println("Verifying OTP for email: " + email); // Log email being verified
        System.out.println("Stored OTP: " + storedOtp + ", Input OTP: " + otp); // Log stored and input OTP for debugging

        Map<String, Object> response = new HashMap<>();

        // Check if the OTP is correct
        if (storedOtp != null && storedOtp.equals(otp)) {
            // OTP is correct
            response.put("success", true);
            otpStorage.remove(email); // Remove OTP after verification
            System.out.println("OTP verification successful for email: " + email); // Log success
        } else {
            // OTP is incorrect
            response.put("success", false);
            System.out.println("OTP verification failed for email: " + email); // Log failure
        }

        // Return the response as JSON
        return ResponseEntity.ok(response);
    }











}
