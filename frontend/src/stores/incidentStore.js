import { defineStore } from 'pinia'
import {
  getIncidents,
  getIncident,
  createIncident,
  updateIncident,
  updateIncidentStatus,
  getIncidentActions,
  addIncidentAction,
  getDeploymentPlans,
  createDeploymentPlan,
  updateDeploymentPlanStatus,
  generateDocument,
  getDocuments,
  updateDocument
} from '../services/api.js'

export const useIncidentStore = defineStore('incident', {
  state: () => ({
    incidents: [],
    currentIncident: null,
    total: 0,
    page: 0,
    pageSize: 10,
    statusFilter: '',
    loading: false,
    error: null,
    actions: [],
    deploymentPlans: [],
    documents: [],
    generatingDocument: false
  }),

  getters: {
    totalPages: (state) => Math.ceil(state.total / state.pageSize)
  },

  actions: {
    async fetchIncidents(params = {}) {
      this.loading = true
      this.error = null
      try {
        const queryParams = {
          page: params.page ?? this.page,
          size: params.size ?? this.pageSize,
          ...(params.status ? { status: params.status } : {})
        }
        const response = await getIncidents(queryParams)
        const data = response.data
        // Handle both paginated and plain array responses
        if (data && data.content !== undefined) {
          this.incidents = data.content
          this.total = data.totalElements ?? data.content.length
        } else if (Array.isArray(data)) {
          this.incidents = data
          this.total = data.length
        } else {
          this.incidents = []
          this.total = 0
        }
      } catch (err) {
        this.error = err.response?.data?.message || '목록을 불러오는데 실패했습니다.'
        this.incidents = []
      } finally {
        this.loading = false
      }
    },

    async fetchIncident(id) {
      this.loading = true
      this.error = null
      try {
        const response = await getIncident(id)
        this.currentIncident = response.data
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '장애 정보를 불러오는데 실패했습니다.'
        this.currentIncident = null
        throw err
      } finally {
        this.loading = false
      }
    },

    async createIncident(data) {
      this.loading = true
      this.error = null
      try {
        const response = await createIncident(data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '장애 등록에 실패했습니다.'
        throw err
      } finally {
        this.loading = false
      }
    },

    async updateIncident(id, data) {
      this.loading = true
      this.error = null
      try {
        const response = await updateIncident(id, data)
        this.currentIncident = response.data
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '장애 수정에 실패했습니다.'
        throw err
      } finally {
        this.loading = false
      }
    },

    async updateIncidentStatus(id, status) {
      this.error = null
      try {
        const response = await updateIncidentStatus(id, status)
        this.currentIncident = response.data
        // Update in list too
        const idx = this.incidents.findIndex(i => i.id === id)
        if (idx !== -1) this.incidents[idx] = response.data
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '상태 변경에 실패했습니다.'
        throw err
      }
    },

    async fetchActions(id) {
      this.error = null
      try {
        const response = await getIncidentActions(id)
        this.actions = Array.isArray(response.data) ? response.data :
          (response.data?.content ?? [])
        return this.actions
      } catch (err) {
        this.error = err.response?.data?.message || '조치 내역을 불러오는데 실패했습니다.'
        this.actions = []
        throw err
      }
    },

    async addAction(id, data) {
      this.error = null
      try {
        const response = await addIncidentAction(id, data)
        this.actions.unshift(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '조치 추가에 실패했습니다.'
        throw err
      }
    },

    async fetchDeploymentPlans(id) {
      this.error = null
      try {
        const response = await getDeploymentPlans(id)
        this.deploymentPlans = Array.isArray(response.data) ? response.data :
          (response.data?.content ?? [])
        return this.deploymentPlans
      } catch (err) {
        this.error = err.response?.data?.message || '반영 계획서를 불러오는데 실패했습니다.'
        this.deploymentPlans = []
        throw err
      }
    },

    async createDeploymentPlan(id, data) {
      this.error = null
      try {
        const response = await createDeploymentPlan(id, data)
        this.deploymentPlans.unshift(response.data)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '반영 계획서 등록에 실패했습니다.'
        throw err
      }
    },

    async updateDeploymentPlanStatus(id, planId, status) {
      this.error = null
      try {
        const response = await updateDeploymentPlanStatus(id, planId, status)
        const idx = this.deploymentPlans.findIndex(p => p.id === planId)
        if (idx !== -1) this.deploymentPlans[idx] = response.data
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '상태 변경에 실패했습니다.'
        throw err
      }
    },

    async generateDocument(id) {
      this.generatingDocument = true
      this.error = null
      try {
        const response = await generateDocument(id)
        // After generation, refresh documents
        await this.fetchDocuments(id)
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '문서 생성에 실패했습니다.'
        throw err
      } finally {
        this.generatingDocument = false
      }
    },

    async fetchDocuments(id) {
      this.error = null
      try {
        const response = await getDocuments(id)
        this.documents = Array.isArray(response.data) ? response.data :
          (response.data?.content ?? [])
        return this.documents
      } catch (err) {
        this.error = err.response?.data?.message || '문서를 불러오는데 실패했습니다.'
        this.documents = []
        throw err
      }
    },

    async updateDocument(id, docId, data) {
      this.error = null
      try {
        const response = await updateDocument(id, docId, data)
        const idx = this.documents.findIndex(d => d.id === docId)
        if (idx !== -1) this.documents[idx] = response.data
        return response.data
      } catch (err) {
        this.error = err.response?.data?.message || '문서 저장에 실패했습니다.'
        throw err
      }
    }
  }
})
