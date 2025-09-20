package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.document.UserBehaviorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserBehaviorRepository extends MongoRepository<UserBehaviorDocument, String> {
}
