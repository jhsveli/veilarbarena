package no.nav.fo.veilarbarena.config;

import no.nav.fo.veilarbarena.selftest.KafkaHelsesjekk;
import no.nav.fo.veilarbarena.service.OppfolgingsbrukerEndringRepository;
import no.nav.fo.veilarbarena.service.OppfolgingsbrukerEndringTemplate;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.HashMap;
import java.util.Map;

import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
@Import({KafkaHelsesjekk.class})
public class KafkaConfig {

    private static final String KAFKA_BROKERS = getRequiredProperty("KAFKA_BROKERS_URL");
    private static final String USERNAME = getRequiredProperty(SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(SYSTEMUSER_PASSWORD);

    @Bean
    public static Map<String, Object> producerConfigs() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 10);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 200); // batch opp i ett halvt sekund eller 16_384 * 4 byte før man sender
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384 * 4); // 4 ganger default
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbarena-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");

        return props;
    }

    @Bean
    public static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);
        return template;
    }

    @Bean
    public OppfolgingsbrukerEndringRepository oppfolgingsbrukerEndringRepository() {
        return new OppfolgingsbrukerEndringRepository();
    }

    @Bean
    public OppfolgingsbrukerEndringTemplate oppfolgingsbrukerEndringTemplate() {
        return new OppfolgingsbrukerEndringTemplate(kafkaTemplate(), oppfolgingsbrukerEndringRepository(), "aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireEnvironmentName());
    }
}
