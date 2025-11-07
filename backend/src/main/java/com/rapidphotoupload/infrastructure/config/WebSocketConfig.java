  package com.rapidphotoupload.infrastructure.config;

  import org.springframework.context.annotation.Configuration;
  import org.springframework.messaging.simp.config.MessageBrokerRegistry;
  import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
  import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
  import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

  /**
   * WebSocket configuration for real-time upload progress updates.
   * Enables clients to receive live progress notifications during uploads.
   */
  @Configuration
  @EnableWebSocketMessageBroker
  public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

      @Override
      public void configureMessageBroker(MessageBrokerRegistry config) {
          // Enable simple broker for sending messages to clients
          config.enableSimpleBroker("/topic", "/queue");

          // Prefix for messages from clients
          config.setApplicationDestinationPrefixes("/app");

          // Prefix for user-specific messages
          config.setUserDestinationPrefix("/user");
      }

      @Override
      public void registerStompEndpoints(StompEndpointRegistry registry) {
          // Register WebSocket endpoint with SockJS fallback
          registry.addEndpoint("/ws")
              .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:19006")
              .withSockJS();
      }
  }