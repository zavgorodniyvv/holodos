package com.holodos.common.infrastructure;

import com.holodos.common.domain.OperationLog;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    /** Rows of [eventType, count] for operations within the range. */
    @Query("select o.eventType, count(o) from OperationLog o where o.createdAt between :from and :to group by o.eventType")
    List<Object[]> countByEventTypeBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
