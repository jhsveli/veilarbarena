package no.nav.veilarbarena.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbarena.domain.User;
import no.nav.veilarbarena.repository.KafkaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.toJson;
import static org.apache.commons.lang3.ClassUtils.getName;

@Slf4j
@Component
public class KafkaProducer {

    public enum Topic {
        ENDRING_PA_OPPFOLGINGSBRUKER
    }

    private final KafkaTopics kafkaTopics;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaRepository kafkaRepository;

    @Autowired
    public KafkaProducer(KafkaTopics kafkaTopics, KafkaTemplate<String, String> kafkaTemplate, KafkaRepository kafkaRepository) {
        this.kafkaTopics = kafkaTopics;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaRepository = kafkaRepository;
    }

    public void sendEndringPaOppfolgingsbruker(User user) {
        send(Topic.ENDRING_PA_OPPFOLGINGSBRUKER, user.getAktoerid().get(), toJson(user));
    }

    public void sendTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        kafkaTemplate.send(kafkaTopicToStr(feiletKafkaMelding.getTopic()), feiletKafkaMelding.getKey(), feiletKafkaMelding.getJsonPayload())
                .addCallback(
                        sendResult -> onSuccessTidligereFeilet(feiletKafkaMelding),
                        throwable -> onErrorTidligereFeilet(feiletKafkaMelding, throwable)
                );
    }

    private void send(Topic topic, String key, String jsonPayload) {
        String topic = kafkaTopicToStr(kafkaTopic);
        kafkaTemplate.send(topic, key, jsonPayload)
                .addCallback(
                        sendResult -> onSuccess(topic, key),
                        throwable -> onError(topic, key, throwable)
                );
    }

    private String kafkaTopicToStr(Topic topic) {
        switch (topic) {
            case ENDRING_PA_OPPFOLGINGSBRUKER:
                return kafkaTopics.getEndringPaOppfolgingBruker();
            default:
                throw new IllegalArgumentException("Unknown topic " + getName(topic));
        }
    }

    private void onSuccess(String topic, String key) {
        log.info(format("Publiserte melding på topic %s med key %s", topic, key));
    }

    private void onError(String topic, String key, Throwable throwable) {
        log.error(format("Kunne ikke publisere melding på topic %s med key %s \nERROR: %s", topic, key, throwable));
    }

    private void onSuccessTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        String topic =  kafkaTopicToStr(feiletKafkaMelding.getTopic());
        String key = feiletKafkaMelding.getKey();

        log.info(format("Publiserte tidligere feilet melding på topic %s med key %s", topic, key));
        kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMelding.getId());
    }

    private void onErrorTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding, Throwable throwable) {
        KafkaProducerTopic kafkaTopic = feiletKafkaMelding.getTopic();
        String topic = kafkaTopicToStr(kafkaTopic);
        String key = feiletKafkaMelding.getKey();
        String jsonPayload = feiletKafkaMelding.getJsonPayload();

        log.error(format("Kunne ikke publisere tidligere feilet melding på topic %s med key %s \nERROR: %s", topic, key, throwable));
        kafkaRepository.lagreFeiletKafkaMelding(kafkaTopic, key, jsonPayload);
    }

}
