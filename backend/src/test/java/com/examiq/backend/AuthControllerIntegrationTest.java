package com.examiq.backend;

import com.examiq.backend.dto.RegisterRequest;
import com.examiq.backend.entity.Role;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.RoleRepository;
import com.examiq.backend.repository.UserRepository;
import com.examiq.backend.security.JwtService;
import com.examiq.backend.service.StudentDashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentDashboardService studentDashboardService;

    @Autowired
    private JwtService jwtService;

    @Test
    void registerUser_shouldReturnOk() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student1");
        request.setEmail("student1@example.com");
        request.setPassword("Password123");
        request.setFullName("Student One");
        request.setRole("STUDENT");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfile_shouldPersistUniversityAndReturnUpdatedProfile() {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        String username = "profile-update-user-" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hashed-password");
        user.setFullName("Old Name");
        user.setRole(studentRole);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        Map<String, Object> updated = studentDashboardService.updateProfile(user,
                Map.of(
                        "fullName", "Updated Name",
                        "email", username + "@updated.com",
                        "university", "NIT Trichy"));

        assertThat(updated.get("fullName")).isEqualTo("Updated Name");
        assertThat(updated.get("email")).isEqualTo(username + "@updated.com");
        assertThat(updated.get("university")).isEqualTo("NIT Trichy");
    }

    @Test
    void loginWithBadCredentials_shouldReturnUnauthorized() throws Exception {
        String username = "bad-login-user-" + System.currentTimeMillis();
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("$2a$10$z7h0Zl2nQ5fP8CwK1pJt0u5bl8N0f9Wf0oW2u7i5bY3wiW0T5XBm");
        user.setFullName("Bad Login User");
        user.setRole(studentRole);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadPaper_shouldAcceptEmptyYear() throws Exception {
        Role facultyRole = roleRepository.findByName("FACULTY")
                .orElseThrow(() -> new IllegalStateException("FACULTY role not found"));

        String username = "faculty-upload-" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hashed-password");
        user.setFullName("Faculty Upload");
        user.setRole(facultyRole);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        String token = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                username,
                "hashed-password",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "dummy pdf content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "Test Paper")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "")
                .param("examType", "Final")
                .param("author", "Faculty Upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
