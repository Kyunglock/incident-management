<template>
  <div class="max-w-3xl mx-auto">
    <h2 class="text-xl font-bold text-gray-800 mb-6">3단계 · 장애 원인 추론</h2>

    <section class="card mb-6">
      <h3 class="section-title">장애 정보 입력</h3>
      <div class="space-y-4">
        <div>
          <label class="label">장애 증상 <span class="text-red-500">*</span></label>
          <textarea v-model="form.symptom" rows="3" class="input" placeholder="장애 증상을 입력하세요. 예) 오후 2시부터 API 응답 지연"></textarea>
        </div>
        <div>
          <label class="label">에러 로그 (선택)</label>
          <textarea v-model="form.errorLogs" rows="5" class="input font-mono text-xs" placeholder="관련 에러 로그를 붙여넣으세요"></textarea>
        </div>
        <div>
          <label class="label">연관 반영 이력 ID (선택)</label>
          <input v-model="form.releaseHistoryId" type="number" class="input" placeholder="반영 이력 ID (이력 조회에서 확인)" />
        </div>
        <button @click="analyze" :disabled="!form.symptom || loading" class="btn-primary w-full">
          <span v-if="loading">분석 중...</span>
          <span v-else>🚨 장애 원인 분석</span>
        </button>
      </div>
    </section>

    <section v-if="result" class="card mb-6">
      <h3 class="section-title">분석 결과</h3>
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-gray-500">발생 시각: {{ result.occurredAt }}</p>
          <p class="mt-2 text-gray-800 text-sm whitespace-pre-wrap">{{ result.cause }}</p>
        </div>
        <button v-if="result.docPath" @click="downloadDoc(result.docPath)" class="btn-secondary ml-4 shrink-0">
          ⬇ 보고서
        </button>
      </div>
    </section>

    <section class="card">
      <h3 class="section-title">장애 이력</h3>
      <div v-if="incidents.length === 0" class="text-gray-400 text-sm">장애 이력이 없습니다.</div>
      <div v-else class="space-y-2">
        <div v-for="i in incidents" :key="i.id"
          class="flex items-start justify-between py-2 border-b last:border-b-0">
          <div>
            <p class="text-sm font-medium text-gray-700">{{ i.symptom }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ i.occurredAt }}</p>
          </div>
          <button v-if="i.docPath" @click="downloadDoc(i.docPath)" class="text-blue-600 text-xs hover:underline ml-3 shrink-0">⬇</button>
        </div>
      </div>
    </section>

    <div v-if="error" class="mt-4 bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { analyzeIncident, getIncidents, downloadDocument } from '../services/api.js'

const form = reactive({ symptom: '', errorLogs: '', releaseHistoryId: null })
const result = ref(null)
const incidents = ref([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  const res = await getIncidents()
  incidents.value = res.data
})

const analyze = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await analyzeIncident({
      symptom: form.symptom,
      errorLogs: form.errorLogs || undefined,
      releaseHistoryId: form.releaseHistoryId || undefined,
    })
    result.value = res.data
    incidents.value.unshift(res.data)
  } catch (e) {
    error.value = '장애 분석 실패'
  } finally {
    loading.value = false
  }
}

const downloadDoc = async (docPath) => {
  const filename = docPath.split('/').pop()
  const res = await downloadDocument(filename)
  const url = URL.createObjectURL(new Blob([res.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
</script>
