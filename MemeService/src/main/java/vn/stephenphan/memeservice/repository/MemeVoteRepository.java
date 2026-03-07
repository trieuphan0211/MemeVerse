package vn.stephenphan.memeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.memeservice.model.MemeVote;
import vn.stephenphan.memeservice.model.VoteType;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemeVoteRepository extends JpaRepository<MemeVote, String> {

    Optional<MemeVote> findByMemeIdAndUserId(String memeId, String userId);

    boolean existsByMemeIdAndUserId(String memeId, String userId);

    void deleteByMemeIdAndUserId(String memeId, String userId);

    @Query("SELECT COUNT(v) FROM MemeVote v WHERE v.memeId = :memeId AND v.voteType = :voteType")
    long countByMemeIdAndVoteType(@Param("memeId") String memeId, @Param("voteType") VoteType voteType);

    @Query("SELECT v.memeId FROM MemeVote v WHERE v.userId = :userId AND v.voteType = 'UP'")
    List<String> findUpvotedMemeIdsByUserId(@Param("userId") String userId);

    List<MemeVote> findByMemeId(String memeId);
}
