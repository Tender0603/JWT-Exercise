package vn.iotstar;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;

@SpringBootApplication
public class JwtSpringBoot3Application {

    public static void main(String[] args) {
        SpringApplication.run(JwtSpringBoot3Application.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User user = new User();
                user.setEmail("admin@gmail.com");
                user.setPassword(passwordEncoder.encode("123456"));
                user.setFullName("Admin");
                user.setImages("default.png");
                userRepository.save(user);
                System.out.println("====== DANG TAO TAI KHOAN THANH CONG ======");
            }
        };
    }
}