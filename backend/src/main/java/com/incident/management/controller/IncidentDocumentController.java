package com.incident.management.controller;

import com.incident.management.dto.request.UpdateIncidentDocumentRequest;
import com.incident.management.dto.response.ApiResponse;
import com.incident.management.dto.response.IncidentDocumentResponse;
import com.incident.management.service.AiService;
import com.incident.management.service.IncidentDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}")
@RequiredArgsConstructor
public class IncidentDocumentController {

    private final IncidentDocumentService documentService;
    private final AiService aiService;

    @GetMapping("/documents")
    public ApiResponse<List<IncidentDocumentResponse>> getDocuments(@PathVariable Long incidentId) {
        return ApiResponse.ok(documentService.getDocuments(incidentId));
    }

    @PostMapping("/generate-document")
    public ApiResponse<IncidentDocumentResponse> generateDocument(@PathVariable Long incidentId) {
        return ApiResponse.ok(aiService.generateDocument(incidentId), "AI 문서가 생성되었습니다.");
    }

    @PutMapping("/documents/{docId}")
    public ApiResponse<IncidentDocumentResponse> updateDocument(@PathVariable Long incidentId,
                                                                 @PathVariable Long docId,
                                                                 @Valid @RequestBody UpdateIncidentDocumentRequest req) {
        return ApiResponse.ok(documentService.updateDocument(incidentId, docId, req));
    }
}
