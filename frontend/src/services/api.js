import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

// 반영계획서
export const generateReleasePlan = (formData) =>
  api.post('/release-plans', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

// 다중 시트 엑셀 업로드 → 시트(날짜)별 반영 계획서 일괄 생성
export const importReleasePlans = (formData) =>
  api.post('/release-plans/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export const getReleasePlans = (params) => api.get('/release-plans', { params })

export const getReleasePlan = (id) => api.get(`/release-plans/${id}`)

export const deleteReleasePlan = (id) => api.delete(`/release-plans/${id}`)

export const analyzeSideEffect = (id, params) =>
  api.post(`/release-plans/${id}/side-effect`, null, { params })

export const analyzeVuln = (id, params) =>
  api.post(`/release-plans/${id}/vuln-check`, null, { params })

// git 커밋 (저장소는 백엔드에서 system 키로 분기)
export const getGitCommits = (params) => api.get('/git/commits', { params })

export const getGitSystems = () => api.get('/git/systems')

// 반영이력 (반영계획서 하위)
export const createReleaseHistory = (planId, body) =>
  api.post(`/release-plans/${planId}/histories`, body)

export const getReleaseHistories = (planId) =>
  api.get(`/release-plans/${planId}/histories`)

export const getReleaseHistory = (id) => api.get(`/release-histories/${id}`)

export const updateFinalConfirmed = (id, finalConfirmed) =>
  api.patch(`/release-histories/${id}/final-confirm`, null, { params: { finalConfirmed } })

export const updateSrNumber = (id, srNumber) =>
  api.patch(`/release-histories/${id}/sr-number`, null, { params: { srNumber } })

// SR 에 git 커밋 연동 (params.commitHashes: 콤마 구분 다중 해시, 비면 연동 해제)
export const updateHistoryGitCommit = (id, params) =>
  api.patch(`/release-histories/${id}/git-commit`, null, { params })

// 연동된 git 커밋 기준 사이드이펙트 검토
export const analyzeHistorySideEffect = (id) =>
  api.post(`/release-histories/${id}/side-effect`)

// 장애이력 (반영이력 하위)
export const createIncident = (historyId, params) =>
  api.post(`/release-histories/${historyId}/incidents`, null, { params })

export const getIncidents = (historyId) =>
  api.get(`/release-histories/${historyId}/incidents`)

export const getIncident = (id) => api.get(`/incidents/${id}`)

// 장애분석 (장애이력 하위)
export const analyzeIncident = (incidentId, params) =>
  api.post(`/incidents/${incidentId}/analyses`, null, { params })

export const getIncidentAnalyses = (incidentId) =>
  api.get(`/incidents/${incidentId}/analyses`)

// 문서 다운로드
export const downloadDocument = (filename) =>
  api.get(`/document/${filename}/download`, { responseType: 'blob' })
