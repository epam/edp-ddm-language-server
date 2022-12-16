package com.epam.digital.data.platform.langserver;

import com.epam.digital.data.platform.langserver.ddmlanguageserver.DdmLanguageServerApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.WebSocketHandler;

@ActiveProfiles({"local", "test"})
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = DdmLanguageServerApplication.class)
public class BaseIT {

  @Autowired
  WebSocketHandler webSocketHandler;

  @Test
  void testSuccess() {

  }

}
