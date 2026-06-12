<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">대시보드</h1>
      <button class="btn btn-primary" @click="showModal = true">+ 새 장애 등록</button>
    </div>

    <LoadingSpinner v-if="dashStore.loading" message="데이터를 불러오는 중..." />

    <template v-else>
      <!-- Stat Cards -->
      <div class="stat-grid">
        <div class="stat-card stat-blue">
          <div class="stat-icon">📋</div>
          <div class="stat-body">
            <div class="stat-number">{{ dashStore.stats.openCount }}</div>
            <div class="stat-label">전체 접수</div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-icon">⚙️</div>
          <div class="stat-body">
            <div class="stat-number">{{ dashStore.stats.inProgressCount }}</div>
            <div class="stat-label">처리중</div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-icon">✅</div>
          <div class="stat-body">
            <div class="stat-number">{{ dashStore.stats.resolvedCount }}</div>
            <div class="stat-label">해결됨</div>
          </div>
        </div>
        <div class="stat-card stat-gray">
          <div class="stat-icon">🔒</div>
          <div class="stat-body">
            <div class="stat-number">{{ dashStore.stats.closedCount }}</div>
            <div class="stat-label">종료</div>
          </div>
        </div>
      </div>

      <!-- Recent Incidents -->
      <div class="card">
        <h2 class="section-title">최근 장애 현황</h2>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>제목</th>
                <th>상태</th>
                <th>우선순위</th>
                <th>담당자</th>
                <th>등록일</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="incident in dashStore.recentIncidents"
                :key="incident.id"
                class="clickable"
                @click="$router.push(`/incidents/${incident.id}`)"
              >
                <td class="id-cell">#{{ incident.id }}</td>
                <td class="title-cell">{{ incident.title }}</td>
                <td><StatusBadge :status="incident.status" /></td>
                <td><PriorityBadge :priority="incident.priority" /></td>
                <td>{{ incident.assigneeName || '-' }}</td>
                <td>{{ formatDate(incident.createdAt) }}</td>
              </tr>
              <tr v-if="!dashStore.recentIncidents.length">
                <td colspan="6" class="empty-row">등록된 장애가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <IncidentModal
      :show="showModal"
      :incident="null"
      @close="showModal = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '../stores/dashboardStore.js'
import StatusBadge from '../components/StatusBadge.vue'
import PriorityBadge from '../components/PriorityBadge.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import IncidentModal from '../components/IncidentModal.vue'

const router = useRouter()
const dashStore = useDashboardStore()
const showModal = ref(false)

onMounted(() => dashStore.fetchDashboardData())

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function onSaved(incident) {
  showModal.value = false
  dashStore.fetchDashboardData()
  if (incident?.id) router.push(`/incidents/${incident.id}`)
}
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  border-left: 4px solid transparent;
}

.stat-blue { border-left-color: #2563EB; }
.stat-orange { border-left-color: #EA580C; }
.stat-green { border-left-color: #16A34A; }
.stat-gray { border-left-color: #64748B; }

.stat-icon {
  font-size: 2rem;
  line-height: 1;
}

.stat-number {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-text);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin-top: 0.25rem;
}

.card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border-light);
}

.id-cell {
  font-weight: 600;
  color: var(--color-primary);
  font-size: 0.875rem;
}

.title-cell {
  font-weight: 500;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-row {
  text-align: center;
  color: var(--color-text-muted);
  padding: 2rem;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
