package com.epam.digital.data.platform.langserver.ddmlanguageserver.handler;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.exception.WebSocketConnectionException;
import com.epam.digital.data.platform.langserver.ddmlanguageserver.exception.WebSocketException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.prominic.groovyls.GroovyLanguageServer;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class GroovyWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

  private static final Logger logger = LoggerFactory.getLogger(GroovyWebSocketHandler.class);

  private final Map<WebSocketSession, RemoteEndpoint> servers = new ConcurrentHashMap<>();

  private final MessageJsonHandler messageJsonHandler = new MessageJsonHandler(
      ServiceEndpoints.getSupportedMethods(GroovyLanguageServer.class));


  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    logger.info("Server connection opened");
    GroovyLanguageServer groovyLanguageServer = new GroovyLanguageServer();
    RemoteEndpoint remoteEndpoint = new RemoteEndpoint(message -> {
      String json = messageJsonHandler.serialize(message);
      logger.info("Supposed output: {}", json);
      try {
        if (session.isOpen()) {
          session.sendMessage(new TextMessage(json));
        } else {
          logger.error("Session is closed");
          throw new WebSocketConnectionException("Session is closed");
        }
      } catch (IOException e) {
        logger.error("Exception occurs during sending message");
        throw new WebSocketException("Exception occurs during sending message", e);
      }

    }, ServiceEndpoints.toEndpoint(groovyLanguageServer));
    LanguageClient languageClient = ServiceEndpoints.toServiceObject(remoteEndpoint,
        LanguageClient.class);
    groovyLanguageServer.connect(languageClient);

    servers.put(session, remoteEndpoint);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    logger.info("Server connection closed: {}", status);
    servers.remove(session);
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    String request = message.getPayload();
    logger.info("input: {}", request);
    servers.get(session).consume(messageJsonHandler.parseMessage(request));
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    logger.error("Server transport error: {}", exception.getMessage());
  }

  @Override
  public List<String> getSubProtocols() {
    return Collections.singletonList("com.epam.digital.data.platform.langserver");
  }
}
