<template>
  <div class="max-w-6xl">
    <router-link v-if="incident" :to="`/release-histories/${incident.releaseHistoryId}`" class="text-sm text-blue-600 hover:underline">
      ← 반영 이력으로 돌아가기
    </router-link>

    <div v-if="incident" class="mt-4">
      <div class="card mb-6">
        <h2 class="text-xl font-bold text-gray-800">장애 #{{ incident.id }}</h2>
        <p class="text-sm text-gray-700 mt-2 whitespace-pre-wrap">{{ incident.symptom }}</p>
        <p class="text-xs text-gray-400 mt-1">발생 시각: {{ incident.occurredAt }}</p>
      </div>

      <div class="grid grid-cols-3 gap-6">
        <section class="card col-span-1 self-start">
          <h3 class="section-title">4단계 · AI 장애 원인 분석</h3>
          <div class="space-y-3">
            <div>
              <label class="label">에러 로그 (선택)</label>
              <textarea v-model="analysisForm.errorLogs" rows="6" class="input font-mono text-xs" placeholder="관련 에러 로그를 붙여넣으세요"></textarea>
            </div>
            <button @click="runAnalysis" :disabled="loading.analysis" class="btn-primary w-full">
              <span v-if="loading.analysis">분석 중...</span>
              <span v-else>🔍 원인 분석 실행</span>
            </button>
          </div>
        </section>

        <section class="card col-span-2">
          <h3 class="section-title">분석 결과 이력</h3>
          <div v-if="analyses.length === 0" class="text-gray-400 text-sm">분석 결과가 없습니다.</div>
          <div v-else class="space-y-3">
            <div v-for="a in analyses" :key="a.id" class="border-b last:border-b-0 pb-3 mb-3 last:mb-0">
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs text-gray-400">{{ a.createdAt }}</span>
                <button v-if="a.docPath" @click="downloadDoc(a.docPath)" class="text-blue-600 text-xs hover:underline">⬇ 보고서</button>
              </div>
              <p class="text-sm text-gray-700 whitespace-pre-wrap">{{ a.cause }}</p>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div v-if="error" class="mt-4 bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getIncident, analyzeIncident, getIncidentAnalyses, downloadDocument } from '../services/api.js'

const route = useRoute()
const incidentId = route.params.id

const incident = ref(null)
const analyses = ref([])
const error = ref('')
const loading = reactive({ analysis: false })
const analysisForm = reactive({ errorLogs: '' })

const load = async () => {
  const [incidentRes, analysesRes] = await Promise.all([
    getIncident(incidentId),
    getIncidentAnalyses(incidentId),
  ])
  incident.value = incidentRes.data
  analyses.value = analysesRes.data
}

onMounted(load)

const runAnalysis = async () => {
  loading.analysis = true
  error.value = ''
  try {
    await analyzeIncident(incidentId, { errorLogs: analysisForm.errorLogs || undefined })
    const res = await getIncidentAnalyses(incidentId)
    analyses.value = res.data
  } catch (e) {
    error.value = '장애 분석 실패'
  } finally {
    loading.analysis = false
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
