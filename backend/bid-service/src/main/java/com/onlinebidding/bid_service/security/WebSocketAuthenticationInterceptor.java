package com.onlinebidding.bid_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private static final String SESSION_PRINCIPAL_ATTRIBUTE =
            WebSocketAuthenticationInterceptor.class.getName() + ".principal";

    private final String jwtSecret;
    private final ConcurrentMap<String, Principal> authenticatedSessions = new ConcurrentHashMap<>();

    public WebSocketAuthenticationInterceptor(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Principal principal = authenticate(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));
            accessor.setUser(principal);
            rememberSession(accessor, principal);
            return authenticatedMessage(message, accessor);
        }

        String sessionId = accessor.getSessionId();
        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            if (sessionId != null) {
                authenticatedSessions.remove(sessionId);
            }
            return message;
        }

        Principal principal = findAuthenticatedPrincipal(accessor);
        if (principal == null) {
            throw new MessagingException("WebSocket session is not authenticated");
        }

        // The principal set on CONNECT is not guaranteed to be copied onto
        // every subsequent inbound STOMP frame. Restore it from the session
        // so SUBSCRIBE, SEND, and other frames remain authenticated.
        accessor.setUser(principal);
        return authenticatedMessage(message, accessor);
    }

    private void rememberSession(StompHeaderAccessor accessor, Principal principal) {
        if (accessor.getSessionId() != null) {
            authenticatedSessions.put(accessor.getSessionId(), principal);
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put(SESSION_PRINCIPAL_ATTRIBUTE, principal);
        }
    }

    private Principal findAuthenticatedPrincipal(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Principal principal = sessionId == null ? null : authenticatedSessions.get(sessionId);
        if (principal != null) {
            return principal;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        Object sessionPrincipal = sessionAttributes.get(SESSION_PRINCIPAL_ATTRIBUTE);
        return sessionPrincipal instanceof Principal ? (Principal) sessionPrincipal : null;
    }

    private Message<?> authenticatedMessage(Message<?> message, StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        authenticatedSessions.remove(event.getSessionId());
    }

    private WebSocketUserPrincipal authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new MessagingException("Missing or invalid Authorization Header");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authorizationHeader.substring(7))
                    .getPayload();

            Date expiration = claims.getExpiration();
            Long userId = extractUserId(claims);
            String role = claims.get("role", String.class);
            String email = claims.getSubject();

            if (expiration == null || expiration.before(new Date()) || userId == null || role == null || email == null) {
                throw new MessagingException("Invalid or expired JWT token");
            }

            return new WebSocketUserPrincipal(userId, role, email);
        } catch (MessagingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MessagingException("Invalid or expired JWT token", exception);
        }
    }

    private Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        return userId instanceof Number ? ((Number) userId).longValue() : null;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
