import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = "com.yourcompany")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
