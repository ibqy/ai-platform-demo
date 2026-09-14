package com.xb.platform.gateway;

import com.xb.platform.gateway.AiGatewayService.AiGatewayRequest;
import com.xb.platform.gateway.AiGatewayService.AiGatewayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiGatewayController {

    @Autowired
    private AiGatewayService gatewayService;

    @PostMapping("/chat")
    public ResponseEntity<AiGatewayResponse> chat(@RequestBody AiGatewayRequest request) {
        AiGatewayResponse response = gatewayService.process(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}