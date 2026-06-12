import { defineStore } from 'pinia'
import { getDashboardStats, getIncidents } from '../services/api.js'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    stats: {
      openCount: 0,
      inProgressCount: 0,
      resolvedCount: 0,
      closedCount: 0,
      totalCount: 0
    },
    recentIncidents: [],
    loading: false,
    error: null
  }),

  actions: {
    async fetchDashboardData() {
      this.loading = true
      this.error = null
      try {
        const [statsRes, incidentsRes] = await Promise.all([
          getDashboardStats(),
          getIncidents({ size: 5, sort: 'createdAt,desc' })
        ])

        this.stats = {
          openCount: statsRes.data?.openCount ?? 0,
          inProgressCount: statsRes.data?.inProgressCount ?? 0,
          resolvedCount: statsRes.data?.resolvedCount ?? 0,
          closedCount: statsRes.data?.closedCount ?? 0,
          totalCount: statsRes.data?.totalCount ?? 0
        }

        const incData = incidentsRes.data
        if (incData && incData.content !== undefined) {
          this.recentIncidents = incData.content
        } else if (Array.isArray(incData)) {
          this.recentIncidents = incData.slice(0, 5)
        } else {
          this.recentIncidents = []
        }
      } catch (err) {
        this.error = err.response?.data?.message || '대시보드 데이터를 불러오는데 실패했습니다.'
        console.error('Dashboard fetch error:', err)
      } finally {
        this.loading = false
      }
    }
  }
})
