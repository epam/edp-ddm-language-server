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

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.DdmLanguageServerApplication;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ActiveProfiles({"local", "test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = DdmLanguageServerApplication.class)
public abstract class BaseIT {

  @LocalServerPort
  protected Integer port;

  protected void sendMessage(String resourceContent, WebSocketSession webSocketSession)
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
  protected String getResourceContent(String resourcePath)  {
    final var resource = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (Objects.isNull(resource)) {
      final var message = String.format("Resource with path %s doesn't exist", resourcePath);
      throw new IllegalArgumentException(message);
    }
    return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
  }
}
