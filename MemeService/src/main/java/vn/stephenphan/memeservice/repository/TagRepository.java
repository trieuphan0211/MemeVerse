package vn.stephenphan.memeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.memeservice.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Tag> searchByName(@Param("search") String search);

    @Query("SELECT t FROM Tag t WHERE t.name IN :names")
    List<Tag> findByNameIn(@Param("names") Set<String> names);

    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC")
    List<Tag> findTopByUsageCount(org.springframework.data.domain.Pageable pageable);
}
