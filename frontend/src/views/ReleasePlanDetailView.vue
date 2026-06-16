<template>
  <div class="max-w-6xl">
    <Breadcrumb :items="breadcrumbItems" />

    <div v-if="plan">
      <div class="card mb-6 flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold text-gray-800">{{ plan.title }}</h2>
          <p class="text-xs text-gray-400 mt-1">{{ plan.createdAt }}</p>
        </div>
        <button @click="downloadDoc(plan.docPath)" class="btn-secondary text-sm">⬇ docx 다운로드</button>
      </div>

      <div class="grid grid-cols-3 gap-6">
        <section class="card col-span-1 self-start">
          <h3 class="section-title">2단계 · diff 기반 분석</h3>
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
            <div v-if="phase2Results.length" class="space-y-2 pt-2">
              <div v-for="r in phase2Results" :key="r.type" class="flex items-center justify-between bg-gray-50 rounded p-2 text-sm">
                <span class="text-gray-600">{{ r.type }}</span>
                <button @click="downloadDoc(r.docPath)" class="text-blue-600 hover:underline text-xs">⬇</button>
              </div>
            </div>
          </div>

          <div class="border-t mt-5 pt-5">
            <h3 class="section-title">반영 이력 추가</h3>
            <div class="space-y-3">
              <div>
                <label class="label">메모</label>
                <input v-model="historyForm.memo" type="text" class="input" placeholder="배포 메모 (선택)" />
              </div>
              <button @click="createHistory" :disabled="loading.history" class="btn-primary w-full">
                <span v-if="loading.history">추가 중...</span>
                <span v-else>+ 반영이력 추가</span>
              </button>
            </div>
          </div>
        </section>

        <section class="card col-span-2">
          <h3 class="section-title">반영 이력 목록</h3>
          <div v-if="histories.length === 0" class="text-gray-400 text-sm">반영 이력이 없습니다.</div>
          <table v-else class="w-full text-sm">
            <thead>
              <tr class="text-left text-gray-400 border-b">
                <th class="py-2 pr-3 font-medium">ID</th>
                <th class="py-2 pr-3 font-medium">상태</th>
                <th class="py-2 pr-3 font-medium">메모</th>
                <th class="py-2 pr-3 font-medium">생성일</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="h in histories" :key="h.id"
                class="border-b last:border-b-0 hover:bg-gray-50 cursor-pointer"
                @click="$router.push(`/release-histories/${h.id}`)">
                <td class="py-3 pr-3"><span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono">#{{ h.id }}</span></td>
                <td class="py-3 pr-3"><span class="text-xs px-2 py-0.5 rounded" :class="statusClass(h.status)">{{ h.status }}</span></td>
                <td class="py-3 pr-3 text-gray-700">{{ h.memo || '-' }}</td>
                <td class="py-3 pr-3 text-gray-400">{{ h.createdAt }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
    </div>

    <div v-if="error" class="mt-4 bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getReleasePlan, analyzeSideEffect, analyzeVuln,
  createReleaseHistory, getReleaseHistories, downloadDocument,
} from '../services/api.js'
import Breadcrumb from '../components/Breadcrumb.vue'

const route = useRoute()
const planId = route.params.id

const plan = ref(null)

const breadcrumbItems = computed(() => [
  { label: '반영 계획서 목록', to: '/' },
  { label: plan.value ? plan.value.title : `#${planId}`, to: null },
])
const histories = ref([])
const phase2Results = ref([])
const error = ref('')
const loading = reactive({ sideEffect: false, vuln: false, history: false })

const gitForm = reactive({ repoPath: '', commitFrom: '', commitTo: '' })
const historyForm = reactive({ memo: '' })

const statusClass = (status) => ({
  PENDING: 'bg-gray-100 text-gray-600',
  DEPLOYED: 'bg-green-100 text-green-700',
  ROLLED_BACK: 'bg-red-100 text-red-700',
}[status] || 'bg-gray-100 text-gray-600')

const load = async () => {
  const [planRes, historiesRes] = await Promise.all([
    getReleasePlan(planId),
    getReleaseHistories(planId),
  ])
  plan.value = planRes.data
  histories.value = historiesRes.data
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

const createHistory = async () => {
  loading.history = true
  try {
    await createReleaseHistory(planId, { memo: historyForm.memo || undefined })
    historyForm.memo = ''
    const res = await getReleaseHistories(planId)
    histories.value = res.data
  } catch (e) {
    error.value = '반영이력 추가 실패'
  } finally {
    loading.history = false
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
