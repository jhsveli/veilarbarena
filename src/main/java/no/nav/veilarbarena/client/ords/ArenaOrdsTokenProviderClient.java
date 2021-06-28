package no.nav.veilarbarena.client.ords;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import okhttp3.*;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.common.utils.AuthUtils.basicCredentials;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;

@Slf4j
public class ArenaOrdsTokenProviderClient {

    public static final String ARENA_ORDS_CLIENT_ID_PROPERTY = "ARENA_ORDS_CLIENT_ID";
    public static final String ARENA_ORDS_CLIENT_SECRET_PROPERTY = "ARENA_ORDS_CLIENT_SECRET";

    private static final int MINIMUM_TIME_TO_EXPIRE_BEFORE_REFRESH = 60;

    private final OkHttpClient client;

    private final String arenaOrdsUrl;

    public ArenaOrdsTokenProviderClient(String arenaOrdsUrl) {
        this(arenaOrdsUrl, RestClient.baseClient());
    }

    public ArenaOrdsTokenProviderClient(String arenaOrdsUrl, OkHttpClient client) {
        this.arenaOrdsUrl = arenaOrdsUrl;
        this.client = client;
    }

    private TokenCache tokenCache = null;

    public String getToken() {
        if (tokenIsSoonExpired()) {
            refreshToken();
        }
        return tokenCache.getOrdsToken().getAccessToken();
    }

    @SneakyThrows
    private void refreshToken() {
        String basicAuth = basicCredentials(
                getRequiredProperty(ARENA_ORDS_CLIENT_ID_PROPERTY),
                getRequiredProperty(ARENA_ORDS_CLIENT_SECRET_PROPERTY));

        Request request = new Request.Builder()
                .url(joinPaths(arenaOrdsUrl, "arena/api/oauth/token"))
                .header(CACHE_CONTROL, "no-cache")
                .header(AUTHORIZATION, basicAuth)
                .post(RequestBody.create(MediaType.get("application/x-www-form-urlencoded"), "grant_type=client_credentials"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            OrdsToken ordsToken = RestUtils.parseJsonResponseOrThrow(response, OrdsToken.class);
            this.tokenCache = new TokenCache(ordsToken);
        }
    }

    private boolean tokenIsSoonExpired() {
        return tokenCache == null || timeToRefresh().isBefore(LocalDateTime.now());
    }

    private LocalDateTime timeToRefresh() {
        return tokenCache.getTime().plus(
                tokenCache.getOrdsToken().getExpiresIn() - MINIMUM_TIME_TO_EXPIRE_BEFORE_REFRESH, SECONDS);
    }

    @Getter
    private static class TokenCache {
        private final OrdsToken ordsToken;
        private final LocalDateTime time;

        TokenCache(OrdsToken ordsToken) {
            this.ordsToken = ordsToken;
            this.time = LocalDateTime.now();
        }
    }

    @Value
    public static class OrdsToken {
        @JsonAlias("access_token")
        String accessToken;

        @JsonAlias("token_type")
        String tokenType;

        @JsonAlias("expires_in")
        int expiresIn;
    }
}
