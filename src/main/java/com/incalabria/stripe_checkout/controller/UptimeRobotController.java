package com.incalabria.stripe_checkout.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uptime-robot")
public class UptimeRobotController {

    @GetMapping
    public ResponseEntity<String> isAlive() {
        return ResponseEntity.ok().build();
    }

}
