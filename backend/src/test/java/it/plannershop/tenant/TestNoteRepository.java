package it.plannershop.tenant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestNoteRepository extends JpaRepository<TestNote, Long> {

    @Query("select n from TestNote n where n.content like :content")
    List<TestNote> findByContentLike(String content);

    @Query(value = "select count(*) from test_note where id = :id", nativeQuery = true)
    long countPhysicalRows(Long id);
}
