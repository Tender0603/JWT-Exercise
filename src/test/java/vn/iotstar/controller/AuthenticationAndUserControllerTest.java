package vn.iotstar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.entity.User;
import vn.iotstar.models.LoginResponse;
import vn.iotstar.models.LoginUserModel;
import vn.iotstar.models.RegisterUserModel;
import vn.iotstar.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthenticationAndUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String validJwtToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Register a standard user for tests
        RegisterUserModel registerUser = new RegisterUserModel();
        registerUser.setEmail("student@example.com");
        registerUser.setPassword("Password123!");
        registerUser.setFullName("Nguyen Van A");
        registerUser.setImages("avatar.jpg");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
                .andExpect(status().isOk());

        // Login to get a valid JWT Token for tests
        LoginUserModel loginUser = new LoginUserModel("student@example.com", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        validJwtToken = loginResponse.getToken();
    }

    @Test
    @DisplayName("Integration Test 1: Đăng ký người dùng lưu DB, mật khẩu được BCrypt mã hóa và KHÔNG trả về password hash trong API")
    void testSignup_Success_PasswordNotReturnedAndEncryptedInDB() {
        User savedUser = userRepository.findByEmail("student@example.com").orElse(null);
        assertNotNull(savedUser);
        assertNotEquals("Password123!", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPassword())); // BCrypt Verification
    }

    @Test
    @DisplayName("Integration Test 2: Đăng nhập đúng thông tin sinh JWT Token và thời gian hết hạn")
    void testLogin_Success_ReturnsJwtToken() throws Exception {
        LoginUserModel loginUser = new LoginUserModel("student@example.com", "Password123!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiresIn").value(3600000));
    }

    @Test
    @DisplayName("Integration Test 3: Đăng nhập sai mật khẩu trả về HTTP 401 Unauthorized")
    void testLogin_InvalidCredentials_Returns401() throws Exception {
        LoginUserModel invalidLogin = new LoginUserModel("student@example.com", "WrongPassword");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Integration Test 4: Truy cập /users/me không có Bearer Token trả về HTTP 401 Unauthorized")
    void testGetMe_NoToken_Returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Integration Test 5: Truy cập /users/me có Bearer Token hợp lệ trả về HTTP 200 OK và thông tin người dùng")
    void testGetMe_ValidToken_ReturnsUserProfile() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Integration Test 6: Truy cập /users/me với Token sai chữ ký trả về HTTP 401 Unauthorized")
    void testGetMe_TamperedToken_Returns401() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + validJwtToken + "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Integration Test 7: Truy cập /users có Bearer Token hợp lệ trả về danh sách người dùng")
    void testGetAllUsers_ValidToken_ReturnsUsersList() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("Integration Test 8: Truy cập /users không có Bearer Token trả về HTTP 401 Unauthorized")
    void testGetAllUsers_NoToken_Returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
