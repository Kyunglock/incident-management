package com.incident.management.controller;

import com.incident.management.dto.request.UpdateIncidentDocumentRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.IncidentDocumentResponse;
import com.incident.management.entity.IncidentDocument;
import com.incident.management.service.AiService;
import com.incident.management.service.IncidentDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}")
@RequiredArgsConstructor
public class IncidentDocumentController {

    private final AiService aiService;
    private final IncidentDocumentService incidentDocumentService;

    @PostMapping("/generate-document")
    public ResponseEntity<ApiResponse<IncidentDocumentResponse>> generateDocument(@PathVariable Long incidentId) {
        IncidentDocument document = aiService.generateDocument(incidentId);
        return ResponseEntity.ok(ApiResponse.ok(IncidentDocumentResponse.from(document), "Document generated successfully"));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<IncidentDocumentResponse>>> getDocuments(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.ok(incidentDocumentService.getDocuments(incidentId)));
    }

    @PutMapping("/documents/{docId}")
    public ResponseEntity<ApiResponse<IncidentDocumentResponse>> updateDocument(
            @PathVariable Long incidentId,
            @PathVariable Long docId,
            @Valid @RequestBody UpdateIncidentDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(incidentDocumentService.updateDocument(incidentId, docId, request)));
    }
}
