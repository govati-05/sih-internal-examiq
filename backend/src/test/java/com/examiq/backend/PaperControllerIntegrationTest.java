package com.examiq.backend;

import com.examiq.backend.entity.Role;
import com.examiq.backend.entity.Subject;
import com.examiq.backend.entity.University;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.NotificationRepository;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.RoleRepository;
import com.examiq.backend.repository.SubjectRepository;
import com.examiq.backend.repository.UniversityRepository;
import com.examiq.backend.repository.UploadRepository;
import com.examiq.backend.repository.UserRepository;
import com.examiq.backend.repository.VerificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaperControllerIntegrationTest {

    private static final AtomicReference<Double> subjectMatchScore = new AtomicReference<>(0.9);
    private static HttpServer aiServer;

    @DynamicPropertySource
    static void registerAiProperties(DynamicPropertyRegistry registry) throws IOException {
        int port = findFreePort();
        aiServer = HttpServer.create(new InetSocketAddress(port), 0);
        aiServer.createContext("/ai/subject-check", exchange -> {
            String body = String.format("{\"data\":{\"match_score\":%.2f}}", subjectMatchScore.get());
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        aiServer.start();
        registry.add("app.ai.service-url", () -> "http://localhost:" + port);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private VerificationLogRepository verificationLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        subjectMatchScore.set(0.9);
        // Delete relations in FK order to avoid integrity violations in H2.
        verificationLogRepository.deleteAll();
        uploadRepository.deleteAll();
        paperRepository.deleteAll();
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        universityRepository.deleteAll();
        subjectRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setName("FACULTY");
        roleRepository.save(role);

        University university = new University();
        university.setName("NIT Trichy");
        universityRepository.save(university);

        Subject subject = new Subject();
        subject.setName("Database Management Systems");
        subject.setCanonicalName("Database Management Systems");
        subjectRepository.save(subject);

        User user = new User();
        user.setUsername("faculty1");
        user.setEmail("faculty1@example.com");
        user.setPassword("encoded");
        user.setFullName("Faculty One");
        user.setRole(role);
        user.setUniversity(university);
        userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void uploadPaper_shouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "DBMS Final Exam")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "2024")
                .param("examType", "Final")
                .param("author", "Faculty One")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void uploadPaper_shouldStoreWebAccessibleFileUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "DBMS Final Exam")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "2024")
                .param("examType", "Final")
                .param("author", "Faculty One")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileUrl").value(org.hamcrest.Matchers.startsWith("/files/")));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void uploadPaper_shouldRejectAssignmentLikeUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "Assignment 2: DBMS Lab Report Submit by 5pm")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "2024")
                .param("examType", "MID")
                .param("author", "Faculty One")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void uploadPaper_shouldRejectSevereExamTypeMismatch() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "End Semester Examination DBMS")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "2024")
                .param("examType", "MID")
                .param("author", "Faculty One")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void uploadPaper_shouldRejectIrrelevantSubjectPaper() throws Exception {
        subjectMatchScore.set(0.2);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "Operating Systems Final Exam")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "2024")
                .param("examType", "Final")
                .param("author", "Faculty One")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void uploadPaper_shouldRejectClearlyDifferentSubjectEvenIfAiLooksHigh() throws Exception {
        subjectMatchScore.set(0.95);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/papers/upload")
                .file(file)
                .param("title", "Operating Systems Final Exam")
                .param("subject", "DBMS")
                .param("university", "NIT Trichy")
                .param("year", "2024")
                .param("examType", "Final")
                .param("author", "Faculty One")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void searchPapers_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/papers/search")
                .param("q", "DBMS"))
                .andExpect(status().isOk());
    }
}
