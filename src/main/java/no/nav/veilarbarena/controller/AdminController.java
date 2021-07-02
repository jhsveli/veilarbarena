package no.nav.veilarbarena.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.job.JobRunner;
import no.nav.veilarbarena.service.KafkaRepubliseringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    public final static String PTO_ADMIN_SERVICE_USER = "srvpto-admin";

    private final AuthContextHolder authContextHolder;

    private final KafkaRepubliseringService kafkaRepubliseringService;

    @PostMapping("/republiser/endring-pa-bruker")
    public String republiserEndringPaBruker() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync("republiser-endring-pa-bruker", kafkaRepubliseringService::republiserEndringPaBrukere);
    }

    private void sjekkTilgangTilAdmin() {
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        UserRole role = authContextHolder.getRole()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!PTO_ADMIN_SERVICE_USER.equals(subject) || !role.equals(UserRole.SYSTEM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

}
