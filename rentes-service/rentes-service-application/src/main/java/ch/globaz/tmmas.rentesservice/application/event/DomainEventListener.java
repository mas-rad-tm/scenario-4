package ch.globaz.tmmas.rentesservice.application.event;



import ch.globaz.tmmas.rentesservice.domain.event.DomainEvent;
import ch.globaz.tmmas.rentesservice.domain.event.DossierCreeEvent;
import ch.globaz.tmmas.rentesservice.domain.model.droit.index.DossierIndex;
import ch.globaz.tmmas.rentesservice.infrastructure.elasticsearch.ElasticSearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class DomainEventListener {

    @Autowired
    ElasticSearchClient esclient;

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventListener.class);


    @EventListener
    void onDomainEvent(DomainEvent event) throws JsonProcessingException {

        LOGGER.info("onDomainEvent {}",event);

    }

    @EventListener
    void onDossierCreeEvent(DossierCreeEvent event) throws IOException {

        LOGGER.info("onDossierCreeEvent {}",event);

        esclient.index(DossierIndex.fromDossierCreeEvent(event));

    }

}
