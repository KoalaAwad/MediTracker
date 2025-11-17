package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User,Integer> {
    User findByEmail(String email);
    User readByEmail(String email);
    User findByEmailIgnoreCase(String email);
}
