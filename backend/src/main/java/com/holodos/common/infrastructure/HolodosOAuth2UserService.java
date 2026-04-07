package com.holodos.common.infrastructure;

import com.holodos.common.domain.User;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
class HolodosOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final Set<String> allowedEmails;

    HolodosOAuth2UserService(UserRepository userRepository, Environment env) {
        this.userRepository = userRepository;
        List<String> emails = Binder.get(env)
                .bind("holodos.security.allowed-emails", List.class)
                .orElse(List.of());
        this.allowedEmails = Set.copyOf(emails);
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();

        if (!allowedEmails.contains(email)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"),
                    "Access denied: " + email + " is not allowed");
        }

        String subject = oidcUser.getSubject();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        userRepository.findByGoogleSubject(subject)
                .ifPresentOrElse(
                        user -> user.update(email, name, picture),
                        () -> userRepository.save(new User(subject, email, name, picture))
                );

        return oidcUser;
    }
}
