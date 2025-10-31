package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.UserEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserEmbeddingRepository extends JpaRepository<UserEmbedding, UUID> {


}
