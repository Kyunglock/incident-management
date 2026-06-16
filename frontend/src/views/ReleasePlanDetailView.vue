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
            <h3 class="section-title">반영 이력 추가 (SR 1건)</h3>
            <div class="space-y-3">
              <div>
                <label class="label">SR 번호 <span class="text-red-500">*</span></label>
                <input v-model="historyForm.srNumber" type="text" class="input" placeholder="예) SR-2026-0001"
                  @keyup.enter="createHistory" />
                <p class="text-xs text-gray-400 mt-1">서비스·작업내용 등 상세 정보는 SR 번호로 레드마인에서 가져옵니다.</p>
              </div>

              <!-- git 커밋 매핑 (체크박스) -->
              <div>
                <label class="label">git 커밋 매핑</label>
                <div class="flex gap-2">
                  <select v-if="systems.length" v-model="commitSystem" class="input py-1 text-sm">
                    <option value="">기본 저장소</option>
                    <option v-for="s in systems" :key="s" :value="s">{{ s }}</option>
                  </select>
                  <button @click="loadCommits" :disabled="loading.commits" class="btn-secondary text-sm whitespace-nowrap">
                    <span v-if="loading.commits">불러오는 중...</span>
                    <span v-else>커밋 불러오기</span>
                  </button>
                </div>
                <div v-if="commitList.length" class="mt-2 border rounded max-h-56 overflow-y-auto divide-y">
                  <label v-for="c in commitList" :key="c.hash"
                    class="flex items-start gap-2 p-2 text-xs hover:bg-gray-50 cursor-pointer">
                    <input type="checkbox" :value="c.hash" v-model="selectedCommitHashes" class="mt-0.5" />
                    <span class="flex-1">
                      <span class="font-mono text-blue-600">{{ c.hash.slice(0, 7) }}</span>
                      <span class="text-gray-700"> {{ c.message }}</span>
                      <span class="block text-gray-400">{{ c.author }} · {{ c.date }}</span>
                    </span>
                  </label>
                </div>
                <p v-else-if="commitsLoaded" class="text-xs text-gray-400 mt-2">커밋이 없습니다.</p>
                <p v-if="selectedCommitHashes.length" class="text-xs text-gray-500 mt-1">
                  {{ selectedCommitHashes.length }}개 커밋 선택됨
                </p>
              </div>

              <button @click="createHistory" :disabled="!historyForm.srNumber || loading.history" class="btn-primary w-full">
                <span v-if="loading.history">추가 중...</span>
                <span v-else>+ 반영이력 추가</span>
              </button>
            </div>
          </div>
        </section>

        <section class="card col-span-2 self-start">
          <h3 class="section-title">반영 이력 목록 (SR 단위)</h3>
          <div v-if="histories.length === 0" class="text-gray-400 text-sm">반영 이력이 없습니다.</div>
          <div v-else class="overflow-x-auto">
            <table class="w-full text-sm min-w-[860px]">
              <thead>
                <tr class="text-left text-gray-400 border-b">
                  <th class="py-2 pr-3 font-medium">No</th>
                  <th class="py-2 pr-3 font-medium">SR</th>
                  <th class="py-2 pr-3 font-medium">서비스</th>
                  <th class="py-2 pr-3 font-medium">작업내용</th>
                  <th class="py-2 pr-3 font-medium">요청자</th>
                  <th class="py-2 pr-3 font-medium">작업자</th>
                  <th class="py-2 pr-3 font-medium text-center">FE</th>
                  <th class="py-2 pr-3 font-medium text-center">BE</th>
                  <th class="py-2 pr-3 font-medium">비고</th>
                  <th class="py-2 pr-3 font-medium text-center bg-red-50">최종확인</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(h, idx) in histories" :key="h.id"
                  class="border-b last:border-b-0 hover:bg-gray-50 cursor-pointer"
                  @click="$router.push(`/release-histories/${h.id}`)">
                  <td class="py-3 pr-3 text-gray-400">{{ idx + 1 }}</td>
                  <td class="py-3 pr-3"><span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono">{{ h.srNumber }}</span></td>
                  <td class="py-3 pr-3 text-gray-700">{{ h.service || '-' }}</td>
                  <td class="py-3 pr-3 text-gray-700">{{ h.workContent || '-' }}</td>
                  <td class="py-3 pr-3 text-gray-500">{{ h.requester || '-' }}</td>
                  <td class="py-3 pr-3 text-gray-500">{{ h.worker || '-' }}</td>
                  <td class="py-3 pr-3 text-center">{{ h.frontendChanged ? 'O' : '' }}</td>
                  <td class="py-3 pr-3 text-center">{{ h.backendChanged ? 'O' : '' }}</td>
                  <td class="py-3 pr-3 text-gray-500">{{ h.note || '-' }}</td>
                  <td class="py-3 pr-3 text-center bg-red-50" :class="h.finalConfirmed ? 'text-green-600' : 'text-gray-300'">
                    {{ h.finalConfirmed ? 'O' : '-' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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
  getGitCommits, getGitSystems,
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
const loading = reactive({ sideEffect: false, vuln: false, history: false, commits: false })

const gitForm = reactive({ repoPath: '', commitFrom: '', commitTo: '' })
const historyForm = reactive({ srNumber: '' })

// git 커밋
const systems = ref([])
const commitSystem = ref('')
const commitList = ref([])
const selectedCommitHashes = ref([])
const commitsLoaded = ref(false)

const loadCommits = async () => {
  loading.commits = true
  try {
    const res = await getGitCommits({ system: commitSystem.value || undefined })
    commitList.value = res.data
    commitsLoaded.value = true
  } catch (e) {
    error.value = '커밋 목록을 불러오지 못했습니다.'
  } finally {
    loading.commits = false
  }
}

const load = async () => {
  const [planRes, historiesRes] = await Promise.all([
    getReleasePlan(planId),
    getReleaseHistories(planId),
  ])
  plan.value = planRes.data
  histories.value = historiesRes.data
  try {
    const sysRes = await getGitSystems()
    systems.value = sysRes.data
  } catch (e) { /* 시스템 목록 없으면 기본 저장소만 사용 */ }
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
    const selectedCommits = commitList.value.filter(c => selectedCommitHashes.value.includes(c.hash))
    await createReleaseHistory(planId, {
      srNumber: historyForm.srNumber,
      commits: selectedCommits,
    })
    historyForm.srNumber = ''
    selectedCommitHashes.value = []
    commitList.value = []
    commitsLoaded.value = false
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
