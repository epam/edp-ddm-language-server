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

package com.epam.digital.data.platform.langserver.ddmlanguageserver;

import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.handler.GroovyWebSocketHandler;
import java.util.Map;
import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(SpringExtension.class)
class DdmLanguageServerApplicationTests {

  @Mock
  WebSocketSession session;
  @Mock
  RemoteEndpoint remoteEndpoint;
  @Mock
  Map<WebSocketSession, RemoteEndpoint> servers;
  @Mock
  MessageJsonHandler messageJsonHandler;
  @InjectMocks
  GroovyWebSocketHandler groovyWebSocketHandler;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(groovyWebSocketHandler, "servers", servers);
    ReflectionTestUtils.setField(groovyWebSocketHandler, "messageJsonHandler", messageJsonHandler);
  }

  @Test
  void afterConnectionEstablishedTest() {
    groovyWebSocketHandler.afterConnectionEstablished(session);
    Mockito.verify(servers).put(eq(session), Mockito.any(RemoteEndpoint.class));
  }

  @Test
  void afterConnectionClosedTest() {
    groovyWebSocketHandler.afterConnectionClosed(session, CloseStatus.BAD_DATA);
    Mockito.verify(servers).remove(session);
  }

  @Test
  void handleTextMessageTest() {
    var textMessage = new TextMessage(RandomString.make());
    Mockito.when(servers.get(session)).thenReturn(remoteEndpoint);
    Mockito.when(messageJsonHandler.parseMessage(textMessage.getPayload()))
        .thenReturn(new RequestMessage());
    groovyWebSocketHandler.handleTextMessage(session, textMessage);

    Mockito.verify(remoteEndpoint)
        .consume(messageJsonHandler.parseMessage(textMessage.getPayload()));
  }

  @Test
  void handleTransportErrorDoNotThrowAnyExceptions() {
    Assertions.assertThatCode(
            () -> groovyWebSocketHandler.handleTransportError(session, new Exception()))
        .doesNotThrowAnyException();
  }

  @Test
  void subProtocolsTest() {
    var subProtocols = groovyWebSocketHandler.getSubProtocols();
    Assertions.assertThat(subProtocols).isNotNull();
    Assertions.assertThat(subProtocols).isNotEmpty();
    Assertions.assertThat(subProtocols.get(0))
        .isEqualTo("com.epam.digital.data.platform.langserver");
  }

}
