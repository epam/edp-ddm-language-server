/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.langserver;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

class XmlLangServerIT extends BaseIT {

  @Test
  @SneakyThrows
  void hasDiagnosticMessagesTest() {
    var initializeRequest = getResourceContent("xml/xml-initialize-request.json");
    var didOpenRequest = getResourceContent("xml/xml-did-open-request.json");
    var resourceContent = getResourceContent("xml/xml-request-incorrect.json");

    var webSocketClient = new StandardWebSocketClient();
    List<String> errors = new ArrayList<>();

    var webSocketSessionListenableFuture = webSocketClient.doHandshake(
        new TextWebSocketHandler() {
          @Override
          protected void handleTextMessage(@NonNull WebSocketSession session,
              @NonNull TextMessage message) {
            if (message.getPayload().contains("Content is not allowed in prolog.")) {
              errors.add("has error");
            }
          }
        },
        new WebSocketHttpHeaders(),
        URI.create("ws://localhost:" + port + "/xml"));

    var webSocketSession = webSocketSessionListenableFuture.get();

    sendMessage(initializeRequest, webSocketSession);
    sendMessage(didOpenRequest, webSocketSession);
    sendMessage(resourceContent, webSocketSession);

    Assertions.assertThat(errors).isNotEmpty();
  }

  @Test
  @SneakyThrows
  void emptyDiagnosticMessagesTest() {
    var initializeRequest = getResourceContent("xml/xml-initialize-request.json");
    var resourceContent = getResourceContent("xml/xml-request-correct.json");
    var didOpenRequest = getResourceContent("xml/xml-did-open-request.json");
    var webSocketClient = new StandardWebSocketClient();
    var errors = new ArrayList<>();

    var webSocketSessionListenableFuture = webSocketClient.doHandshake(
        new TextWebSocketHandler() {
          @Override
          protected void handleTextMessage(@NonNull WebSocketSession session,
              @NonNull TextMessage message) {
            Map<String, List<String>> params = JsonPath.read(message.getPayload(),
                "$.params");
            List<String> diagnostics = params.get("diagnostics");
            if (Objects.nonNull(diagnostics)) {
              errors.addAll(diagnostics);
            }
          }
        },
        new WebSocketHttpHeaders(),
        URI.create("ws://localhost:" + port + "/xml"));

    var webSocketSession = webSocketSessionListenableFuture.get();
    sendMessage(initializeRequest, webSocketSession);
    sendMessage(didOpenRequest, webSocketSession);
    sendMessage(resourceContent, webSocketSession);

    Assertions.assertThat(errors).isEmpty();
  }
}
