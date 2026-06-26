package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.notification.NotificationsPageDto;
import com.evoting.evotingsystem.service.NotificationService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String notifications(
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        NotificationsPageDto pageData = notificationService.getNotificationsPage(userId, type, page, 10);

        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("pageData", pageData);
        model.addAttribute("activeMenu", "notifications");
        return "voter/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        notificationService.markAllAsRead(userId);
        redirectAttributes.addFlashAttribute("toastMessage", "All notifications marked as read");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:/voter/notifications";
    }

    @PostMapping("/notifications/{notificationId}/read")
    public String markAsRead(
            @PathVariable Long notificationId,
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        notificationService.markAsRead(userId, notificationId);
        return "redirect:/voter/notifications?type=" + type + "&page=" + page;
    }

    @PostMapping("/notifications/enable")
    public String enableNotifications(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        notificationService.enableNotifications(userId);
        redirectAttributes.addFlashAttribute("toastMessage", "Notification preferences updated");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:/voter/notifications";
    }
}
