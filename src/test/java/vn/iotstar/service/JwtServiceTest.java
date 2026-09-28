package vn.iotstar.service;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.User;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    private final String testSecret = "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b";
    private final long testExpiration = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testExpiration);

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword("password123");
    }

    @Test
    @DisplayName("Unit Test 1: Tạo token thành công và trích xuất username chính xác")
    void testGenerateAndValidateToken_Success() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtService.extractUsername(token);
        assertEquals("test@example.com", username);

        boolean isValid = jwtService.isTokenValid(token, testUser);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Unit Test 2: Xác minh token thất bại khi username không khớp")
    void testValidateToken_InvalidUser_ReturnsFalse() {
        String token = jwtService.generateToken(testUser);

        User wrongUser = new User();
        wrongUser.setEmail("wrong@example.com");

        boolean isValid = jwtService.isTokenValid(token, wrongUser);
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Unit Test 3: Xác minh token thất bại khi chữ ký bị chỉnh sửa (Tampered Signature)")
    void testValidateToken_TamperedToken_ReturnsFalse() {
        String token = jwtService.generateToken(testUser);
        String tamperedToken = token + "tampered";

        boolean isValid = jwtService.isTokenValid(tamperedToken, testUser);
        assertFalse(isValid);

        JWTClaimsSet claims = jwtService.parseAndVerifyToken(tamperedToken);
        assertNull(claims);
    }

    @Test
    @DisplayName("Unit Test 4: Xác minh token thất bại khi token đã hết hạn (Expired Token)")
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        // Set expiration to negative value (expired in past)
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -10000L);

        String expiredToken = jwtService.generateToken(testUser);
        boolean isValid = jwtService.isTokenValid(expiredToken, testUser);
        assertFalse(isValid);

        JWTClaimsSet claims = jwtService.parseAndVerifyToken(expiredToken);
        assertNull(claims);
    }

    @Test
    @DisplayName("Unit Test 5: Trích xuất username từ token không hợp lệ trả về null")
    void testExtractUsername_InvalidToken_ReturnsNull() {
        String username = jwtService.extractUsername("invalid.token.string");
        assertNull(username);
    }
}
