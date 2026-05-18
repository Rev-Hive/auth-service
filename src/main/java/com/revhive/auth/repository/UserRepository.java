package com.revhive.auth.repository;


import com.revhive.auth.enums.Role;
import com.revhive.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);


    boolean existsByEmail(String email);

    boolean existsByUsername(String username);


    long countByCreatedAtAfter(LocalDateTime date);

    long countByLastActive(LocalDateTime date);

    @Query("""
    SELECT FUNCTION('DATE', u.createdAt), COUNT(u)
    FROM User u
    GROUP BY FUNCTION('DATE', u.createdAt)
    ORDER BY FUNCTION('DATE', u.createdAt)
""")
    List<Object[]> getUserGrowth();


    @Query("""
    SELECT COUNT(u)
    FROM User u
    WHERE u.lastActive >= CURRENT_DATE
""")
    long getDailyActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastActive >= :date")
    long getMonthlyActiveUsers(@Param("date") LocalDateTime date);


//    List<User> findbyNameOrEmail(String email,String name);

//    List <User> findByStatus(User.s)



    Optional<User> findByResetToken(String token);

    long countByStatus(String status);

    long countByRole(Role role);

    List <User> findTop10ByUsernameContainingIgnoreCase(String query);



}
