package com.holodos.common.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SameOriginFilterTest {
    private final SameOriginFilter filter = new SameOriginFilter(Set.of("https://trusted.example.com"));

    private MockHttpServletResponse run(String method, String uri, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setScheme("http");
        request.setServerName("192.168.1.100");
        request.setServerPort(8080);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (chain.getRequest() == null) {
            assertEquals(403, response.getStatus());
            return null;
        }
        return response;
    }

    @Test
    void sameOriginWriteIsAllowed() throws Exception {
        assertNotNull(run("POST", "/api/stock-entries/1/discard", "http://192.168.1.100:8080"));
    }

    @Test
    void otherPortOnSameHostIsRejected() throws Exception {
        assertNull(run("POST", "/api/stock-entries/1/discard", "http://192.168.1.100:3000"));
    }

    @Test
    void foreignSiteIsRejectedButConfiguredOriginIsTrusted() throws Exception {
        assertNull(run("DELETE", "/api/products/1", "https://evil.example.com"));
        assertNotNull(run("DELETE", "/api/products/1", "https://trusted.example.com"));
    }

    @Test
    void readsAndRequestsWithoutOriginPass() throws Exception {
        assertNotNull(run("GET", "/api/products", "https://evil.example.com"));
        assertNotNull(run("POST", "/api/stock-entries/1/discard", null));
    }
}
