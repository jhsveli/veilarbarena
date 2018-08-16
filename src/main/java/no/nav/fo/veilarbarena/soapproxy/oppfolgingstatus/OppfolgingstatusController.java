package no.nav.fo.veilarbarena.soapproxy.oppfolgingstatus;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbarena.RestUtils;
import no.nav.fo.veilarbarena.domain.PersonId;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusUgyldigInput;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

@Component
@Path("/oppfolgingstatus")
public class OppfolgingstatusController {

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Inject
    private OppfolgingstatusService service;

    @Inject
    private AktorService aktorService;

    @GET
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fnr", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "aktorId", dataType = "string", paramType = "query")
    })
    public Oppfolgingstatus oppfolgingstatus() {
        return RestUtils.getUserIdent(requestProvider)
                .map((userid) -> userid.toAktorId(aktorService))
                .map(this::hentOppfolgingsstatus)
                .getOrElseThrow(() -> new BadRequestException("Missing userid"));
    }

    private Oppfolgingstatus hentOppfolgingsstatus(PersonId.AktorId userId) {
        return service.hentOppfoelgingsstatus(userId)
                .getOrElseThrow((error) -> Match(error).of(
                        Case($(instanceOf(HentOppfoelgingsstatusPersonIkkeFunnet.class)), (e) -> new NotFoundException("Could not find user: " + userId, e)),
                        Case($(instanceOf(HentOppfoelgingsstatusSikkerhetsbegrensning.class)), (e) -> new ForbiddenException("Cannot give you that user: " + userId, e)),
                        Case($(instanceOf(HentOppfoelgingsstatusUgyldigInput.class)), (e) -> new BadRequestException("Something went wrong when fetching: " + userId, e)),
                        Case($(), (e) -> new InternalServerErrorException("Unhandled exception", e)))
                );
    }
}
