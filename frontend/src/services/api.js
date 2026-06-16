import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export const generateReleasePlan = (formData) =>
  api.post('/release/plan', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export const getReleaseHistory = () => api.get('/release/history')

export const analyzeSideEffect = (id, params) =>
  api.post(`/release/${id}/side-effect`, null, { params })

export const analyzeVuln = (id, params) =>
  api.post(`/release/${id}/vuln-check`, null, { params })

export const analyzeIncident = (params) =>
  api.post('/incident/analyze', null, { params })

export const getIncidents = () => api.get('/incident')

export const downloadDocument = (filename) =>
  api.get(`/document/${filename}/download`, { responseType: 'blob' })
