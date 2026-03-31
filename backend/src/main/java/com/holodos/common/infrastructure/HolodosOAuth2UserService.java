package com.holodos.common.infrastructure;

import com.holodos.common.domain.User;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class HolodosOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    HolodosOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
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
