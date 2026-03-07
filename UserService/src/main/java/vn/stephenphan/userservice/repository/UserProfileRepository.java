package vn.stephenphan.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.userservice.model.UserProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByUsername(String username);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserProfile u WHERE u.isActive = true AND " +
           "(:search IS NULL OR :search = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<UserProfile> searchActiveUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM UserProfile u WHERE u.userId IN :userIds AND u.isActive = true")
    List<UserProfile> findByUserIdIn(@Param("userIds") List<String> userIds);
}
