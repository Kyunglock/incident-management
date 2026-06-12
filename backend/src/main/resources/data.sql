INSERT IGNORE INTO incidents (id, title, description, status, priority, reporter_name, assignee_name, created_at, updated_at, resolved_at) VALUES
(1, '웹서버 응답 지연 장애', 'nginx 웹서버에서 응답 지연 발생. 평균 응답 시간 5초 이상으로 사용자 불만 접수됨.', 'RESOLVED', 'HIGH', '김철수', '이영희', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
(2, '데이터베이스 연결 오류', 'MySQL 데이터베이스 연결 풀 소진으로 인한 서비스 중단 발생.', 'CLOSED', 'CRITICAL', '박민준', '최지은', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY),
(3, 'API 서버 메모리 누수', 'Java 힙 메모리 사용량이 지속적으로 증가하여 OOM 오류 발생 위험.', 'IN_PROGRESS', 'HIGH', '정수현', '김태호', NOW() - INTERVAL 2 DAY, NOW(), NULL),
(4, '배치 작업 실패', '야간 배치 작업이 반복적으로 실패하여 데이터 동기화 오류 발생.', 'OPEN', 'MEDIUM', '이지원', NULL, NOW() - INTERVAL 1 DAY, NOW(), NULL),
(5, 'SSL 인증서 만료 경고', 'production 환경의 SSL 인증서가 7일 후 만료 예정.', 'OPEN', 'LOW', '홍길동', NULL, NOW(), NOW(), NULL);

INSERT IGNORE INTO incident_actions (id, incident_id, action_description, action_type, performed_by, performed_at, created_at) VALUES
(1, 1, 'nginx worker_connections 및 keepalive_timeout 설정 최적화', 'FIX', '이영희', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),
(2, 1, '변경 사항 운영 환경 반영 및 모니터링', 'DEPLOY', '이영희', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
(3, 1, '응답 시간 정상화 확인 (평균 200ms 이하)', 'VERIFY', '이영희', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
(4, 2, 'DB 연결 풀 크기 50 → 200으로 증가 및 타임아웃 설정 조정', 'FIX', '최지은', NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 9 DAY),
(5, 3, 'Heap dump 분석을 통한 메모리 누수 위치 파악 (캐시 관련 코드 의심)', 'ANALYSIS', '김태호', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);
