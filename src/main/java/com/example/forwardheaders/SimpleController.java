package com.example.forwardheaders;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Controller
public class SimpleController {

  @GetMapping("/index")
  public ResponseEntity<String> index() {
    log.traceEntry("index");

    log.traceExit("index");
    return ResponseEntity.ok("Welcome to index");
  }

  @SneakyThrows
  @GetMapping("/redirect-to-index")
  public void redirect(HttpServletResponse response, @RequestHeader Map<String, String> headers) {
    var allHeaders =
        headers.entrySet().stream()
            .map(e -> "['" + e.getKey() + "': '" + e.getValue() + "']")
            .collect(Collectors.joining("\n"));

    log.traceEntry("redirect, headers: \n{}", allHeaders);

    response.sendRedirect("index");

    log.info("Location header: '{}'", response.getHeader("location"));
    log.traceExit("redirect");
  }
}
