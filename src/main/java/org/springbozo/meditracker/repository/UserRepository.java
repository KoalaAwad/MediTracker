package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User,Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    User findByEmailIgnoreCase(String email);

    // return all users that have the given role (e.g. "DOCTOR")
    @Query("select distinct u from User u join u.roles r where r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    // return users that have the given role and no other roles (only that role)
    @Query("select u from User u join u.roles r where r.roleName = :roleName and size(u.roles) = 1")
    List<User> findByOnlyRoleName(@Param("roleName") String roleName);
}
