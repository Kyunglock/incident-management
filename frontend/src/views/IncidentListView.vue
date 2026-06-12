<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">장애 목록</h1>
      <button class="btn btn-primary" @click="showModal = true">+ 새 장애 등록</button>
    </div>

    <!-- Status filter tabs -->
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab"
        :class="{ active: activeFilter === tab.value }"
        @click="changeFilter(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="card">
      <LoadingSpinner v-if="store.loading" message="목록을 불러오는 중..." />
      <template v-else>
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
                v-for="incident in store.incidents"
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
              <tr v-if="!store.incidents.length">
                <td colspan="6" class="empty-row">장애 내역이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="store.total > 0" class="pagination">
          <span class="total-count">총 {{ store.total }}건</span>
          <div class="page-controls">
            <button
              class="btn btn-secondary page-btn"
              :disabled="currentPage === 0"
              @click="changePage(currentPage - 1)"
            >← 이전</button>

            <button
              v-for="p in visiblePages"
              :key="p"
              class="btn page-btn"
              :class="p === currentPage ? 'btn-primary' : 'btn-secondary'"
              @click="changePage(p)"
            >{{ p + 1 }}</button>

            <button
              class="btn btn-secondary page-btn"
              :disabled="currentPage >= totalPages - 1"
              @click="changePage(currentPage + 1)"
            >다음 →</button>
          </div>
        </div>
      </template>
    </div>

    <IncidentModal
      :show="showModal"
      :incident="null"
      @close="showModal = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useIncidentStore } from '../stores/incidentStore.js'
import StatusBadge from '../components/StatusBadge.vue'
import PriorityBadge from '../components/PriorityBadge.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import IncidentModal from '../components/IncidentModal.vue'

const router = useRouter()
const store = useIncidentStore()
const showModal = ref(false)
const activeFilter = ref('')
const currentPage = ref(0)

const tabs = [
  { label: '전체', value: '' },
  { label: '접수', value: 'OPEN' },
  { label: '처리중', value: 'IN_PROGRESS' },
  { label: '해결됨', value: 'RESOLVED' },
  { label: '종료', value: 'CLOSED' }
]

const totalPages = computed(() => Math.ceil(store.total / store.pageSize))

const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  const pages = []
  const start = Math.max(0, cur - 2)
  const end = Math.min(total - 1, cur + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function loadData() {
  store.fetchIncidents({
    page: currentPage.value,
    size: store.pageSize,
    ...(activeFilter.value ? { status: activeFilter.value } : {})
  })
}

function changeFilter(val) {
  activeFilter.value = val
  currentPage.value = 0
}

function changePage(p) {
  if (p < 0 || p >= totalPages.value) return
  currentPage.value = p
}

watch([activeFilter, currentPage], loadData)
onMounted(loadData)

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function onSaved(incident) {
  showModal.value = false
  loadData()
  if (incident?.id) router.push(`/incidents/${incident.id}`)
}
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 1rem;
  border-bottom: 2px solid var(--color-border-light);
}

.tab {
  background: none;
  border: none;
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.15s, border-color 0.15s;
}

.tab:hover {
  color: var(--color-text);
}

.tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.id-cell {
  font-weight: 600;
  color: var(--color-primary);
  font-size: 0.875rem;
}

.title-cell {
  font-weight: 500;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-row {
  text-align: center;
  color: var(--color-text-muted);
  padding: 2.5rem;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border-light);
}

.total-count {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.page-controls {
  display: flex;
  gap: 0.375rem;
  align-items: center;
}

.page-btn {
  min-width: 2.25rem;
  padding: 0.375rem 0.625rem;
  font-size: 0.875rem;
  justify-content: center;
}
</style>
