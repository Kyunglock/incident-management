INSERT INTO incidents (title, description, status, priority, reporter_name, assignee_name, created_at, updated_at) VALUES
('서버 응답 지연 장애', '오후 2시부터 API 서버 응답 시간이 5초 이상 지연되는 현상이 발생하고 있습니다.', 'IN_PROGRESS', 'HIGH', '김철수', '이영희', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = title;

INSERT INTO incidents (title, description, status, priority, reporter_name, assignee_name, created_at, updated_at) VALUES
('데이터베이스 커넥션 풀 고갈', 'DB 커넥션 풀이 고갈되어 신규 요청이 처리되지 않는 문제가 발생했습니다.', 'RESOLVED', 'CRITICAL', '박민수', '최지원', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE title = title;

INSERT INTO incidents (title, description, status, priority, reporter_name, assignee_name, created_at, updated_at) VALUES
('로그인 서비스 간헐적 오류', '특정 사용자들이 로그인 시 500 에러를 경험하고 있습니다.', 'OPEN', 'MEDIUM', '정수진', NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR))
ON DUPLICATE KEY UPDATE title = title;

INSERT INTO incidents (title, description, status, priority, reporter_name, assignee_name, created_at, updated_at) VALUES
('CDN 캐시 무효화 실패', '이미지 CDN 캐시가 정상적으로 무효화되지 않아 구 버전 이미지가 노출됩니다.', 'CLOSED', 'LOW', '한지훈', '강미래', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE title = title;

INSERT INTO incidents (title, description, status, priority, reporter_name, assignee_name, created_at, updated_at) VALUES
('결제 모듈 오류', '결제 처리 중 타임아웃이 발생하여 사용자 결제가 실패하는 현상이 발생했습니다.', 'OPEN', 'CRITICAL', '오현석', '윤서영', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE))
ON DUPLICATE KEY UPDATE title = title;
