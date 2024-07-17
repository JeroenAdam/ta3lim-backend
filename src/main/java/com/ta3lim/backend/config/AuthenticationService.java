    package com.ta3lim.backend.config;

    import jakarta.servlet.http.HttpServletRequest;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.security.authentication.BadCredentialsException;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.authority.AuthorityUtils;
    import org.springframework.stereotype.Component;

    @Component
    public class AuthenticationService {

        private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";

        private static String authToken;

        @Value("${app.api-key}")
        //setter method annotated with @Value, Spring will call it during bean initialization and will set the static field
        public void setApiKey(String authToken) {
            AuthenticationService.authToken = authToken;
        }

        public static Authentication getAuthentication(HttpServletRequest request) {
            String uri = request.getRequestURI();
            String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
            if (apiKey == null || !apiKey.equals(authToken)) {
                if (!uri.contains("/images/")) {
                throw new BadCredentialsException("Invalid API Key");
                }
            }
            return new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES);
        }
    }
