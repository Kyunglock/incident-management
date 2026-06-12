<template>
  <div class="detail-page">
    <LoadingSpinner v-if="store.loading && !store.currentIncident" message="장애 정보를 불러오는 중..." />

    <template v-else-if="store.currentIncident">
      <!-- Back button -->
      <button class="btn btn-secondary back-btn" @click="$router.push('/incidents')">← 목록으로</button>

      <!-- Header section -->
      <div class="card header-card">
        <div class="incident-title-row">
          <h1 class="incident-title">{{ store.currentIncident.title }}</h1>
          <div class="badge-group">
            <StatusBadge :status="store.currentIncident.status" />
            <PriorityBadge :priority="store.currentIncident.priority" />
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">신고자</span>
            <span class="info-value">{{ store.currentIncident.reporterName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">담당자</span>
            <span class="info-value">{{ store.currentIncident.assigneeName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">등록일</span>
            <span class="info-value">{{ formatDate(store.currentIncident.createdAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">최종수정일</span>
            <span class="info-value">{{ formatDate(store.currentIncident.updatedAt) }}</span>
          </div>
        </div>

        <div v-if="store.currentIncident.symptomDescription" class="symptom-desc">
          <span class="info-label">증상 설명</span>
          <p>{{ store.currentIncident.symptomDescription }}</p>
        </div>

        <!-- Status progression -->
        <div class="status-actions">
          <button
            v-if="store.currentIncident.status === 'OPEN'"
            class="btn btn-primary"
            :disabled="statusChanging"
            @click="changeStatus('IN_PROGRESS')"
          >⚙️ 처리 시작</button>
          <button
            v-else-if="store.currentIncident.status === 'IN_PROGRESS'"
            class="btn btn-success"
            :disabled="statusChanging"
            @click="changeStatus('RESOLVED')"
          >✅ 해결 완료</button>
          <button
            v-else-if="store.currentIncident.status === 'RESOLVED'"
            class="btn btn-gray"
            :disabled="statusChanging"
            @click="changeStatus('CLOSED')"
          >🔒 종료 처리</button>
        </div>
      </div>

      <!-- Tab navigation -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >{{ tab.label }}</button>
      </div>

      <!-- Tab: Actions -->
      <div v-if="activeTab === 'actions'" class="card tab-content">
        <h2 class="section-title">조치 내역</h2>

        <div v-if="store.actions.length" class="action-list">
          <div v-for="action in store.actions" :key="action.id" class="action-item">
            <div class="action-header">
              <span class="action-type-badge">{{ actionTypeLabel(action.actionType) }}</span>
              <span class="action-meta">{{ action.performedBy || '-' }} · {{ formatDate(action.performedAt) }}</span>
            </div>
            <p class="action-desc">{{ action.description }}</p>
          </div>
        </div>
        <p v-else class="empty-text">조치 내역이 없습니다.</p>

        <div class="add-action-form">
          <h3 class="sub-section-title">조치 추가</h3>
          <form @submit.prevent="submitAction">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">조치 유형</label>
                <select v-model="actionForm.actionType" class="form-select">
                  <option value="INVESTIGATION">조사</option>
                  <option value="PATCH">패치</option>
                  <option value="ROLLBACK">롤백</option>
                  <option value="NOTIFICATION">알림</option>
                  <option value="OTHER">기타</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">수행자</label>
                <input v-model="actionForm.performedBy" type="text" class="form-input" placeholder="수행자 이름" />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">조치 내용</label>
              <textarea v-model="actionForm.description" class="form-textarea" rows="3" placeholder="조치 내용을 입력하세요" required></textarea>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="actionSubmitting">
                {{ actionSubmitting ? '추가 중...' : '조치 추가' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Tab: Deployment Plans -->
      <div v-if="activeTab === 'plans'" class="card tab-content">
        <h2 class="section-title">반영 계획서</h2>

        <div v-if="store.deploymentPlans.length" class="plan-list">
          <div v-for="plan in store.deploymentPlans" :key="plan.id" class="plan-item">
            <div class="plan-header">
              <h3 class="plan-title">{{ plan.title }}</h3>
              <span class="plan-status-badge" :class="planStatusClass(plan.status)">{{ planStatusLabel(plan.status) }}</span>
            </div>
            <p class="plan-content">{{ plan.content }}</p>
            <div class="plan-meta">
              <span>예정일: {{ formatDate(plan.plannedAt) }}</span>
            </div>
            <div v-if="plan.status === 'PENDING'" class="plan-actions">
              <button class="btn btn-success btn-sm" :disabled="planChanging" @click="approvePlan(plan.id)">승인</button>
              <button class="btn btn-danger btn-sm" :disabled="planChanging" @click="rejectPlan(plan.id)">반려</button>
            </div>
          </div>
        </div>
        <p v-else class="empty-text">반영 계획서가 없습니다.</p>

        <div class="add-plan-form">
          <h3 class="sub-section-title">새 반영 계획서 등록</h3>
          <form @submit.prevent="submitPlan">
            <div class="form-group">
              <label class="form-label">제목</label>
              <input v-model="planForm.title" type="text" class="form-input" placeholder="계획서 제목" required />
            </div>
            <div class="form-group">
              <label class="form-label">내용</label>
              <textarea v-model="planForm.content" class="form-textarea" rows="4" placeholder="반영 계획 내용을 입력하세요"></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">예정일시</label>
              <input v-model="planForm.plannedAt" type="datetime-local" class="form-input" />
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="planSubmitting">
                {{ planSubmitting ? '등록 중...' : '계획서 등록' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Tab: Documents -->
      <div v-if="activeTab === 'documents'" class="card tab-content">
        <h2 class="section-title">처리 문서</h2>

        <div class="doc-generate-bar">
          <button
            class="btn btn-primary"
            :disabled="store.generatingDocument"
            @click="generateDoc"
          >🤖 AI 문서 자동 생성</button>
        </div>

        <LoadingSpinner v-if="store.generatingDocument" message="AI가 문서를 생성 중입니다..." />

        <template v-else-if="store.documents.length">
          <div v-for="doc in store.documents" :key="doc.id" class="doc-card">
            <div class="doc-field">
              <label class="doc-field-label">증상 (Symptom)</label>
              <textarea v-model="doc.symptom" class="form-textarea" rows="3"></textarea>
            </div>
            <div class="doc-field">
              <label class="doc-field-label">근본원인 (Root Cause)</label>
              <textarea v-model="doc.rootCause" class="form-textarea" rows="3"></textarea>
            </div>
            <div class="doc-field">
              <label class="doc-field-label">조치내용 (Action Taken)</label>
              <textarea v-model="doc.actionTaken" class="form-textarea" rows="3"></textarea>
            </div>
            <div class="doc-field">
              <label class="doc-field-label">반영내역 (Deployment Summary)</label>
              <textarea v-model="doc.deploymentSummary" class="form-textarea" rows="3"></textarea>
            </div>
            <div class="doc-field">
              <label class="doc-field-label">결과 (Result)</label>
              <textarea v-model="doc.result" class="form-textarea" rows="3"></textarea>
            </div>
            <div class="form-actions">
              <button class="btn btn-primary" :disabled="docSaving" @click="saveDoc(doc)">
                {{ docSaving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </div>
        </template>

        <p v-else class="empty-text">생성된 문서가 없습니다. AI 문서 자동 생성을 눌러 문서를 생성하세요.</p>
      </div>
    </template>

    <div v-else-if="!store.loading" class="card">
      <p class="empty-text">장애 정보를 찾을 수 없습니다.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useIncidentStore } from '../stores/incidentStore.js'
import StatusBadge from '../components/StatusBadge.vue'
import PriorityBadge from '../components/PriorityBadge.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'

const route = useRoute()
const store = useIncidentStore()
const incidentId = route.params.id

const activeTab = ref('actions')
const statusChanging = ref(false)
const actionSubmitting = ref(false)
const planSubmitting = ref(false)
const planChanging = ref(false)
const docSaving = ref(false)

const tabs = [
  { key: 'actions', label: '조치 내역' },
  { key: 'plans', label: '반영 계획서' },
  { key: 'documents', label: '처리 문서' }
]

const actionForm = ref({
  actionType: 'INVESTIGATION',
  description: '',
  performedBy: ''
})

const planForm = ref({
  title: '',
  content: '',
  plannedAt: ''
})

onMounted(async () => {
  await store.fetchIncident(incidentId)
  await Promise.all([
    store.fetchActions(incidentId),
    store.fetchDeploymentPlans(incidentId),
    store.fetchDocuments(incidentId)
  ])
})

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

async function changeStatus(newStatus) {
  statusChanging.value = true
  try {
    await store.updateIncidentStatus(incidentId, newStatus)
  } finally {
    statusChanging.value = false
  }
}

async function submitAction() {
  actionSubmitting.value = true
  try {
    await store.addAction(incidentId, actionForm.value)
    actionForm.value = { actionType: 'INVESTIGATION', description: '', performedBy: '' }
  } finally {
    actionSubmitting.value = false
  }
}

async function submitPlan() {
  planSubmitting.value = true
  try {
    await store.createDeploymentPlan(incidentId, planForm.value)
    planForm.value = { title: '', content: '', plannedAt: '' }
  } finally {
    planSubmitting.value = false
  }
}

async function approvePlan(planId) {
  planChanging.value = true
  try {
    await store.updateDeploymentPlanStatus(incidentId, planId, 'APPROVED')
  } finally {
    planChanging.value = false
  }
}

async function rejectPlan(planId) {
  planChanging.value = true
  try {
    await store.updateDeploymentPlanStatus(incidentId, planId, 'REJECTED')
  } finally {
    planChanging.value = false
  }
}

async function generateDoc() {
  await store.generateDocument(incidentId)
}

async function saveDoc(doc) {
  docSaving.value = true
  try {
    await store.updateDocument(incidentId, doc.id, {
      symptom: doc.symptom,
      rootCause: doc.rootCause,
      actionTaken: doc.actionTaken,
      deploymentSummary: doc.deploymentSummary,
      result: doc.result
    })
  } finally {
    docSaving.value = false
  }
}

function actionTypeLabel(type) {
  const map = {
    INVESTIGATION: '조사',
    PATCH: '패치',
    ROLLBACK: '롤백',
    NOTIFICATION: '알림',
    OTHER: '기타'
  }
  return map[type] || type
}

function planStatusLabel(status) {
  const map = { PENDING: '대기', APPROVED: '승인', REJECTED: '반려' }
  return map[status] || status
}

function planStatusClass(status) {
  const map = { PENDING: 'plan-pending', APPROVED: 'plan-approved', REJECTED: 'plan-rejected' }
  return map[status] || ''
}
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.back-btn {
  align-self: flex-start;
  font-size: 0.875rem;
}

.card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 1.5rem;
}

.header-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.incident-title-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.incident-title {
  font-size: 1.375rem;
  font-weight: 700;
  flex: 1;
  min-width: 200px;
}

.badge-group {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
  margin-top: 0.25rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  padding: 1rem;
  background: var(--color-bg);
  border-radius: var(--border-radius-sm);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.info-value {
  font-size: 0.9375rem;
  color: var(--color-text);
}

.symptom-desc {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.symptom-desc p {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.status-actions {
  display: flex;
  gap: 0.75rem;
}

.tabs {
  display: flex;
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

.tab:hover { color: var(--color-text); }

.tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border-light);
}

.sub-section-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.75rem;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-item {
  padding: 1rem;
  background: var(--color-bg);
  border-radius: var(--border-radius-sm);
  border-left: 3px solid var(--color-primary);
}

.action-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.action-type-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.125rem 0.5rem;
  background: rgba(37, 99, 235, 0.1);
  color: var(--color-primary);
  border-radius: 9999px;
}

.action-meta {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.action-desc {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.add-action-form,
.add-plan-form {
  padding-top: 1rem;
  border-top: 1px solid var(--color-border-light);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.plan-item {
  padding: 1rem;
  background: var(--color-bg);
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--color-border-light);
}

.plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.plan-title {
  font-weight: 600;
  font-size: 0.9375rem;
}

.plan-status-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.125rem 0.625rem;
  border-radius: 9999px;
}

.plan-pending { background: rgba(234, 88, 12, 0.15); color: #EA580C; }
.plan-approved { background: rgba(22, 163, 74, 0.15); color: #16A34A; }
.plan-rejected { background: rgba(220, 38, 38, 0.15); color: #DC2626; }

.plan-content {
  font-size: 0.9rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.plan-meta {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-bottom: 0.75rem;
}

.plan-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-sm {
  padding: 0.3rem 0.75rem;
  font-size: 0.8125rem;
}

.doc-generate-bar {
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border-light);
}

.doc-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--color-bg);
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--color-border-light);
}

.doc-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.doc-field-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.empty-text {
  text-align: center;
  color: var(--color-text-muted);
  padding: 1.5rem;
  font-size: 0.9375rem;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
