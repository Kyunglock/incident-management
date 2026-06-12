<template>
  <div class="create-page">
    <div class="page-header">
      <button class="btn btn-secondary back-btn" @click="$router.back()">← 뒤로</button>
      <h1 class="page-title">새 장애 등록</h1>
    </div>

    <div class="card form-card">
      <form @submit.prevent="handleSubmit">
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
            rows="5"
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

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" @click="$router.back()">취소</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? '등록 중...' : '장애 등록' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createIncident } from '../services/api.js'

const router = useRouter()

const form = ref({
  title: '',
  symptomDescription: '',
  priority: 'MEDIUM',
  reporterName: '',
  assigneeName: ''
})

const submitting = ref(false)
const error = ref('')

async function handleSubmit() {
  error.value = ''
  submitting.value = true
  try {
    const res = await createIncident(form.value)
    const newId = res.data?.id
    if (newId) {
      router.push(`/incidents/${newId}`)
    } else {
      router.push('/incidents')
    }
  } catch (err) {
    error.value = err.response?.data?.message || '장애 등록에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-page {
  max-width: 720px;
}

.back-btn {
  font-size: 0.875rem;
}

.form-card {
  background: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-card form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
}
</style>
