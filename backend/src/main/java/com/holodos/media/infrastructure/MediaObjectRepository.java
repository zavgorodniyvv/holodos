package com.holodos.media.infrastructure;

import com.holodos.media.domain.MediaObject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaObjectRepository extends JpaRepository<MediaObject, Long> {

    Optional<MediaObject> findByObjectKey(String objectKey);

    void deleteByObjectKey(String objectKey);
}
