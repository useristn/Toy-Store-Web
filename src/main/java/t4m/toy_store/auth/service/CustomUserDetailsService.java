package t4m.toy_store.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import t4m.toy_store.auth.repository.UserRepository;

import java.util.concurrent.TimeUnit;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final Cache<String, UserDetails> userCache;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.userCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1000).build();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);
        try {
            return userCache.get(email, key -> userRepository.findByEmail(key).orElseThrow(() -> {
                logger.warn("User not found with email: {}", key);
                return new UsernameNotFoundException("User not found with email: " + key);
            }));
        } catch (Exception e) {
            logger.error("Error loading user: {}", e.getMessage());
            throw new UsernameNotFoundException("Error loading user", e);
        }
    }
}