package com.incident.management.common;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildReleasePlanPrompt(String commitMessages, String excelSummary) {
        return String.format("""
                당신은 IT 운영 담당자를 돕는 AI 어시스턴트입니다.
                아래 정보를 바탕으로 반영 계획서 뼈대(초안)를 JSON 형식으로 작성해 주세요.
                정보가 부족한 항목은 빈 문자열이나 빈 배열로 두고, 있는 정보만으로 구조를 채워주세요.

                [Git Commit 메시지]
                %s

                [Excel 작업 항목 요약]
                %s

                반드시 아래 JSON 구조로만 응답하세요:
                {
                  "title": "반영 계획서 제목",
                  "purpose": "반영 목적",
                  "scope": "반영 범위",
                  "changes": [{"item": "변경 항목", "description": "설명"}],
                  "rollback_plan": "롤백 방안",
                  "risk": "위험도 및 영향"
                }
                """, commitMessages, excelSummary);
    }

    /**
     * 한 날짜(시트)에 포함된 SR 작업내용들을 한 줄로 요약하는 프롬프트.
     * JSON 없이 순수 텍스트 한 줄만 받기 위해 형식을 단순화한다.
     */
    public String buildWorkSummaryPrompt(String workItems) {
        return String.format("""
                당신은 IT 운영 담당자를 돕는 AI 어시스턴트입니다.
                아래는 같은 날짜에 반영된 작업(SR) 목록입니다.
                전체를 대표하는 한국어 한 줄 요약을 작성해 주세요.

                규칙:
                - 80자 이내, 한 문장
                - 마크다운/따옴표/줄바꿈 없이 평문으로만
                - 핵심 작업 위주로 간결하게 (예: "통합검색 필터 개선 및 통계 데이터 반영 외 2건")

                [작업 목록]
                %s
                """, workItems);
    }

    public String buildSideEffectPrompt(String gitDiff) {
        return String.format("""
                아래 git diff를 분석하여 사이드이펙트를 JSON으로 보고해 주세요.

                [Git Diff]
                %s

                반드시 아래 JSON 구조로만 응답하세요:
                {
                  "affected_modules": ["영향 모듈 목록"],
                  "risk_items": [{"module": "모듈명", "risk": "위험 내용", "level": "HIGH|MEDIUM|LOW"}],
                  "recommendation": "권고 사항"
                }
                """, gitDiff);
    }

    public String buildVulnCheckPrompt(String gitDiff) {
        return String.format("""
                아래 git diff를 분석하여 웹 보안 취약점을 JSON으로 보고해 주세요.
                OWASP Top 10 기준으로 점검해 주세요.

                [Git Diff]
                %s

                반드시 아래 JSON 구조로만 응답하세요:
                {
                  "vulnerabilities": [{"type": "취약점 유형", "location": "위치", "severity": "HIGH|MEDIUM|LOW", "description": "설명"}],
                  "overall_risk": "HIGH|MEDIUM|LOW",
                  "recommendation": "권고 사항"
                }
                """, gitDiff);
    }

    public String buildIncidentAnalysisPrompt(String symptom, String errorLogs, String releaseHistory) {
        return String.format("""
                아래 장애 증상, 에러 로그, 최근 반영 이력을 바탕으로 원인을 분석하고 JSON으로 보고해 주세요.

                [장애 증상]
                %s

                [에러 로그]
                %s

                [최근 반영 이력]
                %s

                반드시 아래 JSON 구조로만 응답하세요:
                {
                  "root_cause": "근본 원인",
                  "contributing_factors": ["기여 요인 목록"],
                  "timeline": "장애 타임라인",
                  "resolution": "조치 방안",
                  "prevention": "재발 방지 방안"
                }
                """, symptom, errorLogs, releaseHistory);
    }
}
