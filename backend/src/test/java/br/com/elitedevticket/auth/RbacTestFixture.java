package br.com.elitedevticket.auth;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class RbacTestFixture {
    private static final AtomicInteger EXECUTIONS = new AtomicInteger();

    static int executions() {
        return EXECUTIONS.get();
    }

    static void reset() {
        EXECUTIONS.set(0);
    }

    @RestController
    @RequestMapping("/test/rbac")
    static class ProtectedTestController {
        @GetMapping("/organizer")
        @PreAuthorize("hasRole('ORGANIZER')")
        Map<String, String> organizerOnly() {
            return allowed();
        }

        @GetMapping("/customer")
        @PreAuthorize("hasRole('CUSTOMER')")
        Map<String, String> customerOnly() {
            return allowed();
        }

        @GetMapping("/gate")
        @PreAuthorize("hasRole('GATE')")
        Map<String, String> gateOnly() {
            return allowed();
        }

        private Map<String, String> allowed() {
            EXECUTIONS.incrementAndGet();
            return Map.of("result", "allowed");
        }
    }
}
