package vn.stephenphan.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.userservice.model.Block;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, String> {

    Optional<Block> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    void deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);

    // Get list of users that a user has blocked
    List<Block> findByBlockerId(String blockerId);

    // Get list of users that have blocked a user
    List<Block> findByBlockedId(String blockedId);

    @Query("SELECT b.blockedId FROM Block b WHERE b.blockerId = :userId")
    List<String> findBlockedIdsByBlockerId(@Param("userId") String userId);

    @Query("SELECT b.blockerId FROM Block b WHERE b.blockedId = :userId")
    List<String> findBlockerIdsByBlockedId(@Param("userId") String userId);

    // Check if either user has blocked the other
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE (b.blockerId = :userId1 AND b.blockedId = :userId2) OR " +
           "(b.blockerId = :userId2 AND b.blockedId = :userId1)")
    boolean existsBlockBetweenUsers(@Param("userId1") String userId1, @Param("userId2") String userId2);
}
