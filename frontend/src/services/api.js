import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

// 반영계획서
export const generateReleasePlan = (formData) =>
  api.post('/release-plans', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export const getReleasePlans = (params) => api.get('/release-plans', { params })

export const getReleasePlan = (id) => api.get(`/release-plans/${id}`)

export const analyzeSideEffect = (id, params) =>
  api.post(`/release-plans/${id}/side-effect`, null, { params })

export const analyzeVuln = (id, params) =>
  api.post(`/release-plans/${id}/vuln-check`, null, { params })

// 반영이력 (반영계획서 하위)
export const createReleaseHistory = (planId, params) =>
  api.post(`/release-plans/${planId}/histories`, null, { params })

export const getReleaseHistories = (planId) =>
  api.get(`/release-plans/${planId}/histories`)

export const getReleaseHistory = (id) => api.get(`/release-histories/${id}`)

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
