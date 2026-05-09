package com.duoc.testsupport;

import com.duoc.backend.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class TestSecurityEndpointsController {

    @GetMapping(Constants.LOGIN_URL)
    public String loginGet() {
        return "login-get-ok";
    }

    @PostMapping(Constants.LOGIN_URL)
    public String loginPost() {
        return "login-post-ok";
    }

    @GetMapping("/pets/test")
    public String pets() {
        return "pets-ok";
    }

    @GetMapping("/private/test")
    public String privateEndpoint() {
        return "private-ok";
    }
}
