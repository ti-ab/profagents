
package com.user.repository;

import com.user.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, Long> {
    List<Progress> findByUserId(Long userId);
    List<Progress> findByUserIdAndBookId(Long userId, Long bookId);

    @Query("select coalesce(sum(p.sectionTime),0) from Progress p where p.userId=:userId and (:bookId is null or p.bookId=:bookId)")
    Long sumTime(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
