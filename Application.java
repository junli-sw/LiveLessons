@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = "com.yourcompany")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
