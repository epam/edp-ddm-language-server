package com.epam.digital.data.platform.langserver.ddmlanguageserver.config;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.handler.GroovyWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(webSocketHandler(), "/groovy").setAllowedOriginPatterns("*");
  }

  @Bean
  public WebSocketHandler webSocketHandler() {
    return new GroovyWebSocketHandler();
  }
}
