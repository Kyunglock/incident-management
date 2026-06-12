import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
apiClient.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

// Incidents
export const getIncidents = (params) =>
  apiClient.get('/incidents', { params })

export const getIncident = (id) =>
  apiClient.get(`/incidents/${id}`)

export const createIncident = (data) =>
  apiClient.post('/incidents', data)

export const updateIncident = (id, data) =>
  apiClient.put(`/incidents/${id}`, data)

export const updateIncidentStatus = (id, status) =>
  apiClient.patch(`/incidents/${id}/status`, { status })

// Actions
export const getIncidentActions = (id) =>
  apiClient.get(`/incidents/${id}/actions`)

export const addIncidentAction = (id, data) =>
  apiClient.post(`/incidents/${id}/actions`, data)

// Deployment Plans
export const getDeploymentPlans = (id) =>
  apiClient.get(`/incidents/${id}/deployment-plans`)

export const createDeploymentPlan = (id, data) =>
  apiClient.post(`/incidents/${id}/deployment-plans`, data)

export const updateDeploymentPlanStatus = (id, planId, status) =>
  apiClient.patch(`/incidents/${id}/deployment-plans/${planId}/status`, { status })

// Documents
export const generateDocument = (id) =>
  apiClient.post(`/incidents/${id}/generate-document`)

export const getDocuments = (id) =>
  apiClient.get(`/incidents/${id}/documents`)

export const updateDocument = (id, docId, data) =>
  apiClient.put(`/incidents/${id}/documents/${docId}`, data)

// Dashboard
export const getDashboardStats = () =>
  apiClient.get('/dashboard/stats')

export default apiClient
