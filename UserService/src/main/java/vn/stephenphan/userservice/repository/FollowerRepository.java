package vn.stephenphan.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.userservice.model.Follower;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepository extends JpaRepository<Follower, String> {

    Optional<Follower> findByFollowerIdAndFollowingId(String followerId, String followingId);

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);

    // Get list of users that a user is following
    Page<Follower> findByFollowerId(String followerId, Pageable pageable);

    // Get list of users that are following a user
    Page<Follower> findByFollowingId(String followingId, Pageable pageable);

    @Query("SELECT f.followingId FROM Follower f WHERE f.followerId = :userId")
    List<String> findFollowingIdsByFollowerId(@Param("userId") String userId);

    @Query("SELECT f.followerId FROM Follower f WHERE f.followingId = :userId")
    List<String> findFollowerIdsByFollowingId(@Param("userId") String userId);

    long countByFollowerId(String followerId);

    long countByFollowingId(String followingId);
}
