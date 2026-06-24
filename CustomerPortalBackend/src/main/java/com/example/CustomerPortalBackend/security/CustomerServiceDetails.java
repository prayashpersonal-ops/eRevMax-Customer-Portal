package com.example.CustomerPortalBackend.security;

import com.example.CustomerPortalBackend.repository.AdminRepository;
import com.example.CustomerPortalBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerServiceDetails implements UserDetailsService {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (userRepository.findByEmail(username).isPresent()) {
            return userRepository.findByEmail(username).get();
        }
        if (adminRepository.findByEmail(username).isPresent()) {
            return adminRepository.findByEmail(username).get();
        }
        throw new UsernameNotFoundException("User not found with email: " + username);
    }
}
