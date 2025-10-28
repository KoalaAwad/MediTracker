package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User,Integer> {
    public User findByEmail(String email);

    public User readByEmail(String email);
}
