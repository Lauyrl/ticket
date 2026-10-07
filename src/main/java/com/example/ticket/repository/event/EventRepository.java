package com.example.ticket.repository.event;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.ticket.entity.event.EventEntity;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    
    @Query(value = """
        SELECT *
        FROM events e
        WHERE (CAST(:search AS TEXT) IS NULL OR e.name ILIKE '%' || :search || '%')
        AND (CAST(:city AS TEXT) IS NULL OR e.city ILIKE '%' || :city || '%')
        AND (CAST(:startsBefore AS TIMESTAMPTZ) IS NULL OR e.starts_at <= :startsBefore)
        AND (CAST(:startsAfter AS TIMESTAMPTZ) IS NULL OR e.starts_at >= :startsAfter)
        AND (CAST(:endsBefore AS TIMESTAMPTZ) IS NULL OR e.ends_at <= :endsBefore)
        AND (CAST(:endsAfter AS TIMESTAMPTZ) IS NULL OR e.ends_at >= :endsAfter)
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM events e
        WHERE (CAST(:search AS TEXT) IS NULL OR e.name ILIKE '%' || :search || '%')
        AND (CAST(:city AS TEXT) IS NULL OR e.city ILIKE '%' || :city || '%')
        AND (CAST(:startsBefore AS TIMESTAMPTZ) IS NULL OR e.starts_at <= :startsBefore)
        AND (CAST(:startsAfter AS TIMESTAMPTZ) IS NULL OR e.starts_at >= :startsAfter)
        AND (CAST(:endsBefore AS TIMESTAMPTZ) IS NULL OR e.ends_at <= :endsBefore)
        AND (CAST(:endsAfter AS TIMESTAMPTZ) IS NULL OR e.ends_at >= :endsAfter)
        """,
        nativeQuery = true
    )
    public Page<EventEntity> findEvents(
        String search, String city, Instant startsAfter, Instant startsBefore, Instant endsAfter, Instant endsBefore, Pageable pageable
    );

    @Query(value = """
        SELECT *
        FROM events e
        WHERE (:search IS NULL OR e.name % :search)
        ORDER BY 
            similarity(e.name, :search) DESC,
            CASE 
                IF e.description IS NULL THEN NULL 
                ELSE similarity(e.description, :search) 
            END DESC
        LIMIT 5
    """, nativeQuery = true)
    public List<EventEntity> findSuggestedEvents(String search);
}
