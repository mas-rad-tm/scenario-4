package ch.globaz.tmmas.personnesservice.domain.model.index;

import ch.globaz.tmmas.personnesservice.domain.event.PersonneMoraleCreeEvent;
import ch.globaz.tmmas.personnesservice.domain.model.Adresse;
import ch.globaz.tmmas.personnesservice.domain.model.PersonneMorale;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZonedDateTime;


@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = {"username"})
public class PersonneIndex {

    @NotNull
    @Valid
    private Adresse adresse;

    @NotBlank
    private  String prenom;

    @NotBlank
    private  String nom;


    @NotNull
    private String dateNaissance;

    @NotBlank
    private  String nss;

    private PersonneIndex(@NotNull @Valid Adresse adresse, @NotBlank String prenom, @NotBlank String nom,  @NotNull
            String dateNaissance, @NotBlank String nss) {
        this.adresse = adresse;
        this.prenom = prenom;
        this.nom = nom;
        this.dateNaissance = dateNaissance;
        this.nss = nss;
    }

    public static PersonneIndex fromEvent(PersonneMoraleCreeEvent personne){
        return new PersonneIndex(personne.getAdresseActive(),
                personne.getPrenom(),
                personne.getNom(),
                personne.getDateNaissance(),
                personne.getNss());
    }

}


