/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.langserver.ddmlanguageserver.handler;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.exception.WebSocketConnectionException;
import com.epam.digital.data.platform.langserver.ddmlanguageserver.exception.WebSocketException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.prominic.groovyls.GroovyLanguageServer;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class GroovyWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

  private final Map<WebSocketSession, RemoteEndpoint> servers = new ConcurrentHashMap<>();

  private final MessageJsonHandler messageJsonHandler = new MessageJsonHandler(
      ServiceEndpoints.getSupportedMethods(GroovyLanguageServer.class));

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    log.info("Server connection opened");
    var groovyLanguageServer = new GroovyLanguageServer();
    var remoteEndpoint = new RemoteEndpoint(message -> {
      var json = messageJsonHandler.serialize(message);
      log.trace("Output message: {}", json);
      try {
        if (session.isOpen()) {
          session.sendMessage(new TextMessage(json));
        } else {
          log.error("Session is closed");
          throw new WebSocketConnectionException("Session is closed");
        }
      } catch (IOException e) {
        log.error("Exception occurs during sending message");
        throw new WebSocketException("Exception occurs during sending message", e);
      }

    }, ServiceEndpoints.toEndpoint(groovyLanguageServer));
    var languageClient = ServiceEndpoints.toServiceObject(remoteEndpoint, LanguageClient.class);
    groovyLanguageServer.connect(languageClient);

    servers.put(session, remoteEndpoint);
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session,
      @NonNull CloseStatus status) {
    log.info("Server connection closed: {}", status);
    servers.remove(session);
  }

  @Override
  public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
    var request = message.getPayload();
    log.trace("Input message: {}", request);
    servers.get(session).consume(messageJsonHandler.parseMessage(request));
  }

  @Override
  public void handleTransportError(@NonNull WebSocketSession session, Throwable exception) {
    log.error("Server transport error: {}", exception.getMessage());
  }

  @Override
  @NonNull
  public List<String> getSubProtocols() {
    return List.of("com.epam.digital.data.platform.langserver");
  }
}
