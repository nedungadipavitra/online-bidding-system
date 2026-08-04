package com.onlinebidding.bid_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WebSocketAuthenticationInterceptorTest {

    private static final String JWT_SECRET = "Z3V4aFJqSmVOUlVJbEZ2QzN3d0h0RkhzRWhWaVppcFVKaFVQMEVYeDliQjN0NQ==";

    private WebSocketAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthenticationInterceptor(JWT_SECRET);
    }

    @Test
    void authenticatesConnectFrameAndSetsPrincipal() {
        Message<?> message = connectMessage("Bearer " + createToken());

        Message<?> authenticated = interceptor.preSend(message, mock(MessageChannel.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(authenticated);
        assertThat(accessor.getUser()).isInstanceOf(WebSocketUserPrincipal.class);
        WebSocketUserPrincipal principal = (WebSocketUserPrincipal) accessor.getUser();
        assertThat(principal.getUserId()).isEqualTo(42L);
        assertThat(principal.getRole()).isEqualTo("BUYER");
    }

    @Test
    void rejectsConnectFrameWithoutAuthorization() {
        Message<?> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Missing or invalid Authorization Header");
    }

    @Test
    void rejectsMessagesWithoutAnAuthenticatedSession() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("missing-session");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessagingException.class)
                .hasMessage("WebSocket session is not authenticated");
    }

    @Test
    void restoresPrincipalForSubsequentFramesInTheSameSession() {
        Message<?> connect = connectMessage("session-1", "Bearer " + createToken());

        interceptor.preSend(connect, mock(MessageChannel.class));

        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setSessionId("session-1");
        Message<?> subscribe = MessageBuilder.createMessage(new byte[0], subscribeAccessor.getMessageHeaders());

        Message<?> authenticated = interceptor.preSend(subscribe, mock(MessageChannel.class));
        StompHeaderAccessor authenticatedAccessor = StompHeaderAccessor.wrap(authenticated);

        assertThat(authenticatedAccessor.getUser()).isInstanceOf(WebSocketUserPrincipal.class);
    }

    private Message<?> connectMessage(String authorization) {
        return connectMessage(null, authorization);
    }

    private Message<?> connectMessage(String sessionId, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(sessionId);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private String createToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("buyer@example.com")
                .claim("userId", 42L)
                .claim("role", "BUYER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(10))))
                .signWith(key)
                .compact();
    }
}
