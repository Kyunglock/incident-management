<template>
  <div class="max-w-4xl">
    <Breadcrumb :items="breadcrumbItems" />

    <div v-if="plan">
      <div class="card mb-6 flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold text-gray-800">{{ plan.title }}</h2>
          <p class="text-xs text-gray-400 mt-1">{{ plan.createdAt }}</p>
        </div>
        <button @click="downloadDoc(plan.docPath)" class="btn-secondary text-sm">⬇ docx 다운로드</button>
      </div>

      <section class="card">
        <h3 class="section-title">사이드이펙트 / 취약점 분석 (git diff 기반)</h3>
        <div class="grid grid-cols-2 gap-6">
          <div class="space-y-3">
            <div>
              <label class="label">git 저장소 경로</label>
              <input v-model="gitForm.repoPath" type="text" class="input" placeholder="/path/to/repo" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">From commit</label>
                <input v-model="gitForm.commitFrom" type="text" class="input font-mono text-sm" placeholder="HEAD~1" />
              </div>
              <div>
                <label class="label">To commit</label>
                <input v-model="gitForm.commitTo" type="text" class="input font-mono text-sm" placeholder="HEAD" />
              </div>
            </div>
            <div class="flex gap-3">
              <button @click="runSideEffect" :disabled="!gitForm.repoPath || loading.sideEffect" class="btn-secondary flex-1">
                <span v-if="loading.sideEffect">분석 중...</span>
                <span v-else>사이드이펙트</span>
              </button>
              <button @click="runVuln" :disabled="!gitForm.repoPath || loading.vuln" class="btn-secondary flex-1">
                <span v-if="loading.vuln">분석 중...</span>
                <span v-else>취약점 체크</span>
              </button>
            </div>
          </div>

          <div>
            <h4 class="text-sm font-semibold text-gray-600 mb-2">분석 결과</h4>
            <div v-if="phase2Results.length" class="space-y-2">
              <div v-for="r in phase2Results" :key="r.type" class="flex items-center justify-between bg-gray-50 rounded p-2 text-sm">
                <span class="text-gray-600">{{ r.type }}</span>
                <button @click="downloadDoc(r.docPath)" class="text-blue-600 hover:underline text-xs">⬇ 다운로드</button>
              </div>
            </div>
            <p v-else class="text-gray-400 text-sm">아직 분석 결과가 없습니다.</p>
          </div>
        </div>
      </section>
    </div>

    <div v-if="error" class="mt-4 bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getReleasePlan, analyzeSideEffect, analyzeVuln, downloadDocument } from '../services/api.js'
import Breadcrumb from '../components/Breadcrumb.vue'

const route = useRoute()
const planId = route.params.id

const plan = ref(null)
const phase2Results = ref([])
const error = ref('')
const loading = reactive({ sideEffect: false, vuln: false })
const gitForm = reactive({ repoPath: '', commitFrom: '', commitTo: '' })

const breadcrumbItems = computed(() => [
  { label: '반영 계획서 목록', to: '/' },
  { label: plan.value ? plan.value.title : `#${planId}`, to: null },
])

const load = async () => {
  const res = await getReleasePlan(planId)
  plan.value = res.data
}

onMounted(load)

const runSideEffect = async () => {
  loading.sideEffect = true
  try {
    const res = await analyzeSideEffect(planId, gitForm)
    phase2Results.value.push({ type: '사이드이펙트 보고서', docPath: res.data.docPath })
  } catch (e) {
    error.value = '사이드이펙트 분석 실패'
  } finally {
    loading.sideEffect = false
  }
}

const runVuln = async () => {
  loading.vuln = true
  try {
    const res = await analyzeVuln(planId, gitForm)
    phase2Results.value.push({ type: '취약점 보고서', docPath: res.data.docPath })
  } catch (e) {
    error.value = '취약점 체크 실패'
  } finally {
    loading.vuln = false
  }
}

const downloadDoc = async (docPath) => {
  if (!docPath) return
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
