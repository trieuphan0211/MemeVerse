package vn.stephenphan.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.userservice.model.UserStats;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, String> {

    Optional<UserStats> findByUserId(String userId);

    @Query("SELECT us FROM UserStats us ORDER BY us.gameScore DESC")
    List<UserStats> findTopByGameScore(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT us FROM UserStats us WHERE us.userId IN :userIds")
    List<UserStats> findByUserIdIn(@Param("userIds") List<String> userIds);
}
