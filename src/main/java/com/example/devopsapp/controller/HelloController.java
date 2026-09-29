package com.example.devopsapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private static final Logger logger =
            LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public String hello() {
        logger.debug("DEBUG: iniciando processamento do endpoint /hello");
        logger.info("INFO: endpoint /hello foi acessado");
        logger.error("ERROR: exemplo de log de erro para observabilidade");

        return "DevSecOps Spring Boot funcionando!";
    }
}