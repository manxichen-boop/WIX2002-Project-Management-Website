package com.ecoswap.web;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.auth.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Login, registration and account-status pages. Signed-in users are sent to their dashboard. */
@Controller
public class AuthPageController {

    private final AuthService authService;

    public AuthPageController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AuthenticatedUser me) {
        return me != null ? redirectHome(me) : "pages/auth/login";
    }

    @GetMapping("/register")
    public String register(@AuthenticationPrincipal AuthenticatedUser me) {
        return me != null ? redirectHome(me) : "pages/auth/register";
    }

    /** state = registered (just signed up) | pending (tried to log in before approval). */
    @GetMapping("/register/pending")
    public String pending(@RequestParam(defaultValue = "registered") String state, Model model) {
        model.addAttribute("state", state.equals("pending") ? "pending" : "registered");
        return "pages/auth/pending-approval";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, Model model) {
        AuthService.VerificationResult result = authService.verifyEmail(token);
        model.addAttribute("state", "verify-" + result.name().toLowerCase());
        return "pages/auth/pending-approval";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "pages/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "pages/auth/reset-password";
    }

    private static String redirectHome(AuthenticatedUser me) {
        return me.isAdmin() ? "redirect:/admin" : "redirect:/dashboard";
    }
}
