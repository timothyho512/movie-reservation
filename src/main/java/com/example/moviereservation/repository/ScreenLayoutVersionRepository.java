package com.example.moviereservation.repository;

import com.example.moviereservation.entity.ScreenLayoutVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenLayoutVersionRepository extends JpaRepository<ScreenLayoutVersion, Long> {
    Optional<ScreenLayoutVersion> findFirstByScreenIdOrderByVersionNumberDesc(Long screenId);

    @Query("""
            SELECT COALESCE(MAX(slv.versionNumber), 0)
            FROM ScreenLayoutVersion slv
            WHERE slv.screen.id = :screenId
            """)
    int findMaxVersionNumberForScreen(@Param("screenId") Long screenId);
}
