package com.stage.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void userGets403OnAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(token -> token.claim("uid", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGets200OnAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_CODING_CHALLENGE"))
                                .jwt(token -> token.claim("uid", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("admin access granted"));
    }
}
