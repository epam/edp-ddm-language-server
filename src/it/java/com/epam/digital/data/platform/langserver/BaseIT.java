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

package com.epam.digital.data.platform.langserver;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.DdmLanguageServerApplication;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = DdmLanguageServerApplication.class)
public class BaseIT {

  @LocalServerPort
  private Integer port;

  @Test
  @SneakyThrows
  void hasDiagnosticMessagesTest() {
    var resourceContent = getResourceContent("request-incorrect.json");
    var webSocketClient = new StandardWebSocketClient();
    var errors = new ArrayList<>();

    var webSocketSessionListenableFuture = webSocketClient.doHandshake(
        new TextWebSocketHandler() {
          @Override
          protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            String diagnosticMessage = JsonPath.read(message.getPayload(),
                "$.params.diagnostics[0].message");
            if (diagnosticMessage == null || !diagnosticMessage
                .contains("Unexpected character: '\\' @ line 1, column 19.")) {
              errors.add("test failed");
            }
          }
        },
        new WebSocketHttpHeaders(),
        URI.create("ws://localhost:" + port + "/groovy"));

    var webSocketSession = webSocketSessionListenableFuture.get();

    sendMessage(resourceContent, webSocketSession);
    Assertions.assertThat(errors).isEmpty();
  }

  @Test
  @SneakyThrows
  void emptyDiagnosticMessagesTest() {
    var resourceContent = getResourceContent("request-correct.json");
    var webSocketClient = new StandardWebSocketClient();

    var mock = Mockito.mock(TextWebSocketHandler.class);
    var webSocketSessionListenableFuture = webSocketClient.doHandshake(
        mock,
        new WebSocketHttpHeaders(),
        URI.create("ws://localhost:" + port + "/groovy"));

    var webSocketSession = webSocketSessionListenableFuture.get();

    sendMessage(resourceContent, webSocketSession);
    Mockito.verify(mock, Mockito.never()).handleMessage(any(), any());
  }

  private static void sendMessage(String resourceContent, WebSocketSession webSocketSession)
      throws InterruptedException {
    newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
      try {
        TextMessage message = new TextMessage(resourceContent);
        webSocketSession.sendMessage(message);
      } catch (Exception e) {
      }
    }, 1, 1, TimeUnit.SECONDS);

    Thread.sleep(5000);
  }

  @SneakyThrows
  public String getResourceContent(String resourcePath)  {
    final var resource = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (Objects.isNull(resource)) {
      final var message = String.format("Resource with path %s doesn't exist", resourcePath);
      throw new IllegalArgumentException(message);
    }
    return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
  }

}
