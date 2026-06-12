package com.incident.management.controller;

import com.incident.management.dto.request.UpdateIncidentDocumentRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.IncidentDocumentResponse;
import com.incident.management.service.IncidentDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentDocumentController {

    private final IncidentDocumentService incidentDocumentService;

    @PostMapping("/{id}/generate-document")
    public ResponseEntity<ApiResponse<IncidentDocumentResponse>> generateDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                incidentDocumentService.generateDocument(id), "문서가 생성되었습니다."));
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<IncidentDocumentResponse>>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(incidentDocumentService.getDocuments(id)));
    }

    @PutMapping("/{id}/documents/{docId}")
    public ResponseEntity<ApiResponse<IncidentDocumentResponse>> updateDocument(
            @PathVariable Long id,
            @PathVariable Long docId,
            @RequestBody UpdateIncidentDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                incidentDocumentService.updateDocument(id, docId, request), "문서가 수정되었습니다."));
    }
}
