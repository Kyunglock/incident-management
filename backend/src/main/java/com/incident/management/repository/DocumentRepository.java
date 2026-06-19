package com.incident.management.repository;

import com.incident.management.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByRefId(Long refId);

    /** 특정 참조(refType+refId)의 특정 타입 문서 중 최신 1건 */
    Document findFirstByRefTypeAndRefIdAndTypeOrderByCreatedAtDesc(
            String refType, Long refId, String type);

    /** 여러 참조의 특정 타입 문서 (존재 여부 일괄 조회용) */
    List<Document> findByRefTypeAndRefIdInAndType(
            String refType, Collection<Long> refIds, String type);
}
