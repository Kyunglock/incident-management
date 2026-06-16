-- ============================================================
-- 테스트 시드 데이터 (idempotent: 재기동해도 중복 삽입 안 됨)
-- 계층: release_plan(1) - release_history(N) - incident(N) - incident_analysis(N)
-- ============================================================

-- 반영 계획서
INSERT INTO release_plan (id, title, created_at, excel_path, doc_path, raw_input, llm_output)
SELECT 1, '2026-06-10 정기 반영 (회원/결제)', NOW() - INTERVAL '6 day',
       'release_2026-06-10.xlsx', 'generated-docs/release-plan-20260610.docx',
       '{"excel": "release_2026-06-10.xlsx"}'::jsonb,
       '{"title":"2026-06-10 정기 반영","purpose":"회원 가입 정책 변경 및 결제 모듈 점검","scope":"회원 서비스, 결제 서비스","changes":[{"item":"회원 가입 약관 동의 추가","description":"필수 약관 항목 2건 추가"},{"item":"결제 타임아웃 조정","description":"PG 연동 타임아웃 5초→10초"}],"rollback_plan":"이전 빌드 재배포","risk":"중간 - 결제 영향 가능"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM release_plan WHERE id = 1);

INSERT INTO release_plan (id, title, created_at, excel_path, doc_path, raw_input, llm_output)
SELECT 2, '2026-06-12 긴급 핫픽스 (로그인)', NOW() - INTERVAL '4 day',
       'hotfix_login.xlsx', 'generated-docs/release-plan-20260612.docx',
       '{"excel": "hotfix_login.xlsx"}'::jsonb,
       '{"title":"로그인 긴급 핫픽스","purpose":"간헐적 로그인 500 오류 수정","scope":"인증 서비스","changes":[{"item":"세션 처리 NPE 수정","description":"null 세션 분기 추가"}],"rollback_plan":"핫픽스 커밋 revert","risk":"낮음"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM release_plan WHERE id = 2);

INSERT INTO release_plan (id, title, created_at, excel_path, doc_path, raw_input, llm_output)
SELECT 3, '2026-06-15 배치/통계 반영', NOW() - INTERVAL '1 day',
       'batch_stats.xlsx', 'generated-docs/release-plan-20260615.docx',
       '{"excel": "batch_stats.xlsx"}'::jsonb,
       '{"title":"배치/통계 반영","purpose":"일배치 성능 개선 및 통계 지표 추가","scope":"배치 서버, 통계 API","changes":[{"item":"집계 쿼리 인덱스 추가","description":"created_at 복합 인덱스"}],"rollback_plan":"인덱스 드롭","risk":"낮음"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM release_plan WHERE id = 3);

-- 반영 이력 (SR 1건 = 1행. service 등 상세는 레드마인 연동으로 채워지는 값)
INSERT INTO release_history (id, release_plan_id, sr_number, service, work_content, requester, worker, test_url_verify, test_url_prod, test_detail, frontend_changed, backend_changed, note, final_confirmed, created_at)
SELECT 1, 1, 'SR-2026-0001', '회원', '회원 가입 약관 동의 추가', '장윤옥', '박영우',
       'https://dev.example.net/signup', 'https://www.example.net/signup',
       '1. 약관 동의 체크박스 2건 노출 확인', true, true, NULL, true, NOW() - INTERVAL '6 day'
WHERE NOT EXISTS (SELECT 1 FROM release_history WHERE id = 1);

INSERT INTO release_history (id, release_plan_id, sr_number, service, work_content, requester, worker, test_url_verify, test_url_prod, test_detail, frontend_changed, backend_changed, note, final_confirmed, created_at)
SELECT 2, 1, 'SR-2026-0002', '결제', '결제 타임아웃 조정 (5초 -> 10초)', '장윤옥', '박영우',
       'https://dev.example.net/payment', 'https://www.example.net/payment',
       '1. PG 응답 지연 상황에서 결제 성공 처리 확인', false, true, '롤백 이력 있음', true, NOW() - INTERVAL '5 day'
WHERE NOT EXISTS (SELECT 1 FROM release_history WHERE id = 2);

INSERT INTO release_history (id, release_plan_id, sr_number, service, work_content, requester, worker, test_url_verify, test_url_prod, test_detail, frontend_changed, backend_changed, note, final_confirmed, created_at)
SELECT 3, 2, 'SR-2026-0003', '인증', '세션 처리 NPE 수정 (null 세션 분기 추가)', '장윤옥', '박영우',
       'https://dev.example.net/login', 'https://www.example.net/login',
       '1. 만료 토큰으로 로그인 시 500 오류 없는지 확인', false, true, '긴급 핫픽스', true, NOW() - INTERVAL '4 day'
WHERE NOT EXISTS (SELECT 1 FROM release_history WHERE id = 3);

INSERT INTO release_history (id, release_plan_id, sr_number, service, work_content, requester, worker, test_url_verify, test_url_prod, test_detail, frontend_changed, backend_changed, note, final_confirmed, created_at)
SELECT 4, 3, 'SR-2026-0004', '배치/통계', '집계 쿼리 인덱스 추가 (created_at 복합 인덱스)', '장윤옥', '박영우',
       'https://dev.example.net/admin/stats', 'https://www.example.net/admin/stats',
       '1. 통계 화면 응답 속도 개선 확인', false, true, '배포 대기 중', false, NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (SELECT 1 FROM release_history WHERE id = 4);

-- 장애 이력
INSERT INTO incident (id, release_history_id, occurred_at, symptom, created_at)
SELECT 1, 2, NOW() - INTERVAL '5 day' + INTERVAL '2 hour',
       '결제 요청 시 타임아웃 다수 발생, 사용자 결제 실패 신고 접수', NOW() - INTERVAL '5 day' + INTERVAL '2 hour'
WHERE NOT EXISTS (SELECT 1 FROM incident WHERE id = 1);

INSERT INTO incident (id, release_history_id, occurred_at, symptom, created_at)
SELECT 2, 1, NOW() - INTERVAL '6 day' + INTERVAL '5 hour',
       '회원 가입 화면에서 약관 동의 체크박스가 일부 노출되지 않음', NOW() - INTERVAL '6 day' + INTERVAL '5 hour'
WHERE NOT EXISTS (SELECT 1 FROM incident WHERE id = 2);

INSERT INTO incident (id, release_history_id, occurred_at, symptom, created_at)
SELECT 3, 3, NOW() - INTERVAL '3 day',
       '핫픽스 이후에도 특정 SSO 사용자 로그인 간헐 실패', NOW() - INTERVAL '3 day'
WHERE NOT EXISTS (SELECT 1 FROM incident WHERE id = 3);

-- 장애 분석
INSERT INTO incident_analysis (id, incident_id, error_logs, cause, doc_path, created_at)
SELECT 1, 1, 'java.net.SocketTimeoutException: Read timed out at PgClient.pay()',
       '{"root_cause":"PG사 응답 지연 + 타임아웃 5초로 과소 설정","contributing_factors":["피크 시간 트래픽 급증","커넥션 풀 부족"],"timeline":"14:00 지연 시작, 14:20 실패율 급증","resolution":"타임아웃 10초 상향, 커넥션 풀 증설","prevention":"PG 응답시간 모니터링 알림 추가"}',
       'generated-docs/incident-report-0001.docx', NOW() - INTERVAL '5 day' + INTERVAL '3 hour'
WHERE NOT EXISTS (SELECT 1 FROM incident_analysis WHERE id = 1);

INSERT INTO incident_analysis (id, incident_id, error_logs, cause, doc_path, created_at)
SELECT 2, 1, NULL,
       '{"root_cause":"재분석: 롤백 후 잔여 캐시로 일부 결제 큐 적체","contributing_factors":["캐시 무효화 누락"],"timeline":"롤백 직후 10분간 잔여 영향","resolution":"결제 큐 수동 플러시","prevention":"롤백 시 캐시 무효화 절차 표준화"}',
       'generated-docs/incident-report-0002.docx', NOW() - INTERVAL '5 day' + INTERVAL '4 hour'
WHERE NOT EXISTS (SELECT 1 FROM incident_analysis WHERE id = 2);

INSERT INTO incident_analysis (id, incident_id, error_logs, cause, doc_path, created_at)
SELECT 3, 3, 'NullPointerException at SsoSessionHandler.resolve()',
       '{"root_cause":"SSO 토큰 만료 시 세션 분기 미처리","contributing_factors":["만료 토큰 테스트 케이스 부재"],"timeline":"불규칙 발생","resolution":"만료 토큰 예외 처리 추가","prevention":"인증 회귀 테스트 보강"}',
       'generated-docs/incident-report-0003.docx', NOW() - INTERVAL '3 day' + INTERVAL '1 hour'
WHERE NOT EXISTS (SELECT 1 FROM incident_analysis WHERE id = 3);

-- 시퀀스 보정 (수동 ID 삽입 후 다음 자동 증가값 맞춤)
SELECT setval(pg_get_serial_sequence('release_plan', 'id'), (SELECT COALESCE(MAX(id), 1) FROM release_plan));
SELECT setval(pg_get_serial_sequence('release_history', 'id'), (SELECT COALESCE(MAX(id), 1) FROM release_history));
SELECT setval(pg_get_serial_sequence('incident', 'id'), (SELECT COALESCE(MAX(id), 1) FROM incident));
SELECT setval(pg_get_serial_sequence('incident_analysis', 'id'), (SELECT COALESCE(MAX(id), 1) FROM incident_analysis));
