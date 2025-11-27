package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.UserVectorStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVectorRepository extends JpaRepository<UserVectorStore, Integer> {


}
