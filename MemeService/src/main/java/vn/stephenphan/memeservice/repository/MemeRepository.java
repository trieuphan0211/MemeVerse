package vn.stephenphan.memeservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.memeservice.model.Meme;
import vn.stephenphan.memeservice.model.MemeStatus;

import java.util.List;

@Repository
public interface MemeRepository extends JpaRepository<Meme, String> {

    // Latest memes
    Page<Meme> findByStatusOrderByCreatedAtDesc(MemeStatus status, Pageable pageable);

    // User's memes
    Page<Meme> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, MemeStatus status, Pageable pageable);

    // Trending memes (by vote score)
    @Query("SELECT m FROM Meme m WHERE m.status = :status ORDER BY m.voteScore DESC, m.createdAt DESC")
    Page<Meme> findTrendingMemes(@Param("status") MemeStatus status, Pageable pageable);

    // Following feed (memes from specific users)
    @Query("SELECT m FROM Meme m WHERE m.userId IN :userIds AND m.status = :status ORDER BY m.createdAt DESC")
    Page<Meme> findFollowingMemes(@Param("userIds") List<String> userIds, @Param("status") MemeStatus status, Pageable pageable);

    // Search by caption
    @Query("SELECT m FROM Meme m WHERE m.status = :status AND " +
           "(:search IS NULL OR LOWER(m.caption) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.username) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<Meme> searchMemes(@Param("search") String search, @Param("status") MemeStatus status, Pageable pageable);

    // Find by tag
    @Query("SELECT m FROM Meme m JOIN m.tags t WHERE LOWER(t.name) = LOWER(:tagName) AND m.status = :status ORDER BY m.createdAt DESC")
    Page<Meme> findByTagName(@Param("tagName") String tagName, @Param("status") MemeStatus status, Pageable pageable);

    // Hot algorithm (Reddit Hot)
    @Query("SELECT m FROM Meme m WHERE m.status = :status ORDER BY " +
           "(LOG10(GREATEST(ABS(m.voteScore), 1)) + SIGN(m.voteScore) * 1.0 / 45000 * EXTRACT(EPOCH FROM (m.createdAt - TIMESTAMP '1970-01-01 00:00:00')) / 3600) DESC")
    Page<Meme> findHotMemes(@Param("status") MemeStatus status, Pageable pageable);

    // Top memes by time period
    @Query("SELECT m FROM Meme m WHERE m.status = :status AND m.createdAt >= :since ORDER BY m.voteScore DESC")
    Page<Meme> findTopMemes(@Param("status") MemeStatus status, @Param("since") java.time.LocalDateTime since, Pageable pageable);

    // Pending approval
    Page<Meme> findByStatus(MemeStatus status, Pageable pageable);

    // Get user's meme count
    long countByUserIdAndStatus(String userId, MemeStatus status);

    // Random memes for discovery
    @Query(value = "SELECT * FROM memes WHERE status = 'APPROVED' ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Meme> findRandomMemes(@Param("limit") int limit);
}
