import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbarena.utils.MigrationUtils;
import no.nav.fo.veilarbarena.config.ApplicationConfig;
import no.nav.fo.veilarbarena.config.DbConfig;
import no.nav.fo.veilarbarena.utils.VaultUtils;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbarena.config.ApplicationConfig.*;
import static no.nav.fo.veilarbarena.config.DbConfig.VEILARBARENADB_PASSWORD;
import static no.nav.fo.veilarbarena.config.DbConfig.VEILARBARENADB_USERNAME;
import static no.nav.fo.veilarbarena.utils.VaultUtils.getCredentials;
import static no.nav.fo.veilarbarena.utils.VaultUtils.getDefaultSecretPath;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {
    public static void main(String... args) {
        VaultUtils.Credentials serviceUser = getCredentials(getDefaultSecretPath("service_user"));
        System.setProperty(SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(SYSTEMUSER_PASSWORD, serviceUser.password);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);
        System.setProperty(StsSecurityConstants.STS_URL_KEY, getRequiredProperty(SECURITYTOKENSERVICE_URL));
        System.setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_ENDPOINTURL));
        System.setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(REDIRECT_URL_PROPERTY));
        System.setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRequiredProperty(ABAC_PDP_ENDPOINT_URL));

        VaultUtils.Credentials oracleCreds = getCredentials(getDefaultSecretPath("oracle_creds"));
        System.setProperty(VEILARBARENADB_USERNAME, oracleCreds.username);
        System.setProperty(VEILARBARENADB_PASSWORD, oracleCreds.password);

        MigrationUtils.createTables(DbConfig.getDataSource());

        ApiApp.runApp(ApplicationConfig.class, args);
    }
}
