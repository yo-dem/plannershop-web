package it.plannershop.tenant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotaProvaRepository extends JpaRepository<NotaProva, Long> {

    @Query("select n from NotaProva n where n.testo like :testo")
    List<NotaProva> cercaPerTesto(String testo);

    @Query(value = "select count(*) from nota_prova where id = :id", nativeQuery = true)
    long contaRigheFisiche(Long id);
}
