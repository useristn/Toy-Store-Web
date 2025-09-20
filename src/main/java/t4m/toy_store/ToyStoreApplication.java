package t4m.toy_store;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.repository.RoleRepository;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class ToyStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyStoreApplication.class, args);
    }

    @Bean
    public ApplicationRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            List<String> roleNames = Arrays.asList("ROLE_USER", "ROLE_VENDOR", "ROLE_SHIPPER", "ROLE_ADMIN");

            for (String rname : roleNames) {
                if (roleRepository.findByRname(rname).isEmpty()) {
                    Role role = new Role();
                    role.setRname(rname);
                    roleRepository.save(role);
                    System.out.println("Initialized role: " + rname);
                } else {
                    System.out.println("Role already exists: " + rname);
                }
            }
        };
    }
}