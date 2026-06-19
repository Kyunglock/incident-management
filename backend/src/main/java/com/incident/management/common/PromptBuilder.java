package com.incident.management.common;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    /** 작업내용 + 코드 변경(git diff)을 바탕으로 검증용 테스트케이스를 한국어로 작성. */
    public String buildTestCasePrompt(String workContent, String gitDiff) {
        String diffSection = (gitDiff == null || gitDiff.isBlank()) ? "(연동된 git 커밋 없음)" : gitDiff;
        return String.format("""
                아래 작업내용과 코드 변경(git diff)을 함께 보고, 검증용 테스트케이스를 한국어로 간결하게 작성해 주세요.
                각 케이스는 한 줄로, 번호를 붙여 "확인할 동작 / 기대 결과" 형태로 적어주세요.
                코드 변경에서 드러나는 구체적 동작(분기, 예외 처리 등)도 케이스로 반영하고,
                정상 케이스와 함께 주요 예외/경계 케이스도 포함하세요. 서론 없이 목록만 출력하세요.

                [작업내용]
                %s

                [코드 변경(git diff)]
                %s
                """, workContent, diffSection);
    }

    public String buildReleasePlanPrompt(String commitMessages, String excelSummary) {
        return String.format("""
                당신은 IT 운영 담당자를 돕는 AI 어시스턴트입니다.
                아래 정보를 바탕으로 작업 계획서 뼈대(초안)를 JSON 형식으로 작성해 주세요.
                정보가 부족한 항목은 빈 문자열이나 빈 배열로 두고, 있는 정보만으로 구조를 채워주세요.

                [Git Commit 메시지]
                %s

                [Excel 작업 항목 요약]
                %s

                반드시 아래 JSON 구조로만 응답하세요:
                {
                  "title": "작업 계획서 제목",
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
                아래 git diff(코드 변경분)를 분석해서, 이 변경으로 발생할 수 있는
                사이드이펙트(부작용/영향)를 한국어로 '간결하게' 요약해 주세요.

                장황한 설명/서론 없이 핵심만 다음 형식으로:
                - 핵심 영향·위험: 3~5개 불릿 (각 한 줄, 중요도 [높음/보통/낮음] 표시)
                - 결론: 1~2문장 (배포해도 되는지/주의점)

                [Git Diff]
                %s
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
