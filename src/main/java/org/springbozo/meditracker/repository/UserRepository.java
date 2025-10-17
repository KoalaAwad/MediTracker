package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User,Integer> {
}
