package com.project.back_end;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class SecurityContextLoadsTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithSecurityBeans() {
        assertNotNull(context.getBean(AuthenticationManager.class));
        assertNotNull(context.getBean(BCryptPasswordEncoder.class));
    }
}
