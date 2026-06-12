<template>
  <teleport to="body">
    <div v-if="show" class="modal-backdrop" @click.self="$emit('close')">
      <div class="modal-card">
        <div class="modal-header">
          <h2 class="modal-title">{{ incident ? '장애 수정' : '새 장애 등록' }}</h2>
          <button class="modal-close" @click="$emit('close')">✕</button>
        </div>

        <form class="modal-body" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">제목 <span class="required">*</span></label>
            <input
              v-model="form.title"
              type="text"
              class="form-input"
              placeholder="장애 제목을 입력하세요"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">증상 설명</label>
            <textarea
              v-model="form.symptomDescription"
              class="form-textarea"
              placeholder="증상을 상세히 설명해주세요"
              rows="4"
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">우선순위</label>
              <select v-model="form.priority" class="form-select">
                <option value="LOW">낮음</option>
                <option value="MEDIUM">보통</option>
                <option value="HIGH">높음</option>
                <option value="CRITICAL">긴급</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">신고자</label>
              <input
                v-model="form.reporterName"
                type="text"
                class="form-input"
                placeholder="신고자 이름"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">담당자</label>
            <input
              v-model="form.assigneeName"
              type="text"
              class="form-input"
              placeholder="담당자 이름"
            />
          </div>

          <p v-if="error" class="form-error">{{ error }}</p>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="$emit('close')">취소</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? '저장 중...' : (incident ? '수정' : '등록') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { createIncident, updateIncident } from '../services/api.js'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  incident: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const form = ref({
  title: '',
  symptomDescription: '',
  priority: 'MEDIUM',
  reporterName: '',
  assigneeName: ''
})

const submitting = ref(false)
const error = ref('')

watch(() => props.incident, (val) => {
  if (val) {
    form.value = {
      title: val.title || '',
      symptomDescription: val.symptomDescription || '',
      priority: val.priority || 'MEDIUM',
      reporterName: val.reporterName || '',
      assigneeName: val.assigneeName || ''
    }
  } else {
    form.value = {
      title: '',
      symptomDescription: '',
      priority: 'MEDIUM',
      reporterName: '',
      assigneeName: ''
    }
  }
}, { immediate: true })

function handleKeydown(e) {
  if (e.key === 'Escape' && props.show) {
    emit('close')
  }
}

onMounted(() => document.addEventListener('keydown', handleKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))

async function handleSubmit() {
  error.value = ''
  submitting.value = true
  try {
    let result
    if (props.incident) {
      const res = await updateIncident(props.incident.id, form.value)
      result = res.data
    } else {
      const res = await createIncident(form.value)
      result = res.data
    }
    emit('saved', result)
  } catch (err) {
    error.value = err.response?.data?.message || '저장에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border-light);
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.125rem;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.25rem;
  line-height: 1;
  transition: color 0.15s;
}
.modal-close:hover { color: var(--color-text); }

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.required {
  color: var(--color-danger);
}

.form-error {
  color: var(--color-danger);
  font-size: 0.875rem;
  padding: 0.5rem 0.75rem;
  background-color: rgba(220, 38, 38, 0.08);
  border-radius: var(--border-radius-sm);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
}
</style>
