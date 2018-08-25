package ch.globaz.tmmas.rentesservice.domain.model.droit.index;

import ch.globaz.tmmas.rentesservice.domain.event.DossierCreeEvent;
import lombok.Getter;

@Getter
public class DossierIndex {

	private String identifiant;
	private String dateEnregistrement;
	private Long requerantId;
	private String status;
	private Long id;

	private DossierIndex(String identifiant, String dateEnregistrement, Long requerantId, String status, Long id) {
		this.identifiant = identifiant;
		this.dateEnregistrement = dateEnregistrement;
		this.requerantId = requerantId;
		this.status = status;
		this.id = id;
	}

	public DossierIndex(){}

	public static DossierIndex fromDossierCreeEvent(DossierCreeEvent event){

		return new DossierIndex(event.getIdentifiant(),event.getDateEnregistrement(),event.getRequerantId(),event
				.getStatus(),event.getId());
	}
}
