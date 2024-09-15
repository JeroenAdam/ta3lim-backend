package com.ta3lim.backend.repository.jpa;

import com.ta3lim.backend.domain.Links;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LinksRepository extends JpaRepository<Links, Long> {
    @Transactional
    @Modifying
    @Query("DELETE FROM Links l WHERE l.referrer.id = :referrerId")
    void deleteAllByReferrerId(@Param("referrerId") Long referrerId);
}
