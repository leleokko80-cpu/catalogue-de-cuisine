package com.recipecatalog.backend.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChefChatController {

    private final ChefChatService chatService;

    public ChefChatController(ChefChatService chatService) {
        this.chatService = chatService;
    }

    public record ChatTurn(String role, String text) {}

    public record ChatRequest(
            String message,
            Long recipeId,
            Double budgetTnd,
            List<ChatTurn> history
    ) {}

    @PostMapping
    public ChefChatService.ChatResponse chat(@RequestBody ChatRequest payload,
                                             HttpServletRequest request,
                                             Authentication authentication) {
        String clientIp = request.getRemoteAddr();
        return chatService.respond(
                payload.message(),
                payload.recipeId(),
                payload.budgetTnd(),
                payload.history(),
                clientIp,
                authentication
        );
    }
}
