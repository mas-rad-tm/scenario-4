package ch.globaz.tmmas.rentesservice.domain.event;

import ch.globaz.tmmas.rentesservice.domain.common.GlobalParams;

import java.time.format.DateTimeFormatter;

public interface DomainEvent {

    DateTimeFormatter formatter
            = DateTimeFormatter.ofPattern(GlobalParams.DATE_FORMATTER_PATTER.value);
}
