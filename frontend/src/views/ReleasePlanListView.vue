<template>
  <div>
    <Breadcrumb :items="[{ label: '반영 계획서 목록', to: null }]" />

    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">반영 계획서</h2>
      <div class="flex gap-2">
        <button @click="showImport = !showImport" class="btn-secondary text-sm">
          {{ showImport ? '닫기' : '⬆ 엑셀 일괄 등록' }}
        </button>
        <button @click="showCreate = !showCreate" class="btn-primary text-sm">
          {{ showCreate ? '닫기' : '+ 새 반영 계획서' }}
        </button>
      </div>
    </div>

    <!-- 다중 시트 엑셀 일괄 등록 (접이식) -->
    <section v-if="showImport" class="card mb-6">
      <h3 class="section-title">엑셀 일괄 등록 (시트=날짜)</h3>
      <p class="text-xs text-gray-500 mb-3">
        시트별로 반영 계획서(제목 <code>2026-MM-DD</code>)와 SR 단위 반영 이력을 생성합니다.
        이미 같은 날짜가 등록되어 있으면 해당 시트는 무시합니다.
      </p>
      <div class="flex items-center gap-3">
        <input type="file" accept=".xlsx,.xls" @change="onImportFileChange" class="input py-1" />
        <label class="flex items-center gap-2 text-sm text-gray-700 whitespace-nowrap">
          <input type="checkbox" v-model="importSummarize" class="w-4 h-4" />
          작업내용 LLM 요약
        </label>
        <button @click="runImport" :disabled="!importFile || importing" class="btn-primary">
          <span v-if="importing">등록 중...</span>
          <span v-else>일괄 등록</span>
        </button>
      </div>
      <p v-if="importSummarize" class="text-xs text-gray-400 mt-1">
        시트마다 LLM 요약을 호출하므로 시트 수에 따라 시간이 걸릴 수 있습니다.
      </p>
      <div v-if="importError" class="text-red-600 text-sm mt-2">{{ importError }}</div>

      <!-- 결과 요약 -->
      <div v-if="importResult" class="mt-4 text-sm space-y-2">
        <div class="text-gray-700">
          전체 시트 {{ importResult.totalSheets }}개 ·
          생성 <span class="text-green-600 font-semibold">{{ importResult.created.length }}</span>건 ·
          기존 스킵 <span class="text-gray-500 font-semibold">{{ importResult.skippedExisting.length }}</span>건 ·
          무효/빈 시트 <span class="text-gray-400 font-semibold">{{ importResult.skippedEmptyOrInvalid.length }}</span>개
        </div>
        <div v-if="importResult.created.length" class="text-gray-600">
          <span class="font-medium">생성됨:</span>
          {{ importResult.created.map(c => `${c.title}(SR ${c.historyCount})`).join(', ') }}
        </div>
        <div v-if="importResult.skippedExisting.length" class="text-gray-500">
          <span class="font-medium">기존 스킵:</span> {{ importResult.skippedExisting.join(', ') }}
        </div>
      </div>
    </section>

    <!-- 생성 폼 (접이식) -->
    <section v-if="showCreate" class="card mb-6">
      <h3 class="section-title">새 반영 계획서 생성</h3>
      <div class="grid grid-cols-2 gap-6">
        <div class="space-y-4">
          <div>
            <label class="label">공유 Excel 파일 <span class="text-red-500">*</span></label>
            <input type="file" accept=".xlsx,.xls" @change="onFileChange" class="input py-1" />
          </div>
          <div>
            <label class="label">반영 제목</label>
            <input v-model="form.releaseTitle" type="text" class="input" placeholder="예) 2026-06-16 정기 반영" />
          </div>
        </div>

        <div class="space-y-4">
          <label class="flex items-center gap-2">
            <input type="checkbox" v-model="form.useGit" class="w-4 h-4" />
            <span class="text-sm font-medium text-gray-700">git 정보 사용 (선택)</span>
          </label>
          <div v-if="form.useGit" class="space-y-3">
            <div>
              <label class="label">시스템 (저장소)</label>
              <select v-model="form.system" class="input">
                <option value="">선택</option>
                <option v-for="s in gitSystems" :key="s" :value="s">{{ s }}</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">From commit</label>
                <input v-model="form.commitFrom" type="text" class="input font-mono text-sm" placeholder="HEAD~1" />
              </div>
              <div>
                <label class="label">To commit</label>
                <input v-model="form.commitTo" type="text" class="input font-mono text-sm" placeholder="HEAD" />
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="mt-4 flex items-center gap-3">
        <button @click="generatePlan" :disabled="!excelFile || loading" class="btn-primary">
          <span v-if="loading">생성 중...</span>
          <span v-else>📄 반영 계획서 뼈대 생성</span>
        </button>
        <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>
      </div>
    </section>

    <!-- 검색 -->
    <div class="flex justify-end mb-3">
      <div class="flex gap-2">
        <input v-model="keyword" type="text" class="input py-1 w-64" placeholder="제목 검색" @keyup.enter="search" />
        <button @click="search" class="btn-secondary text-sm">검색</button>
      </div>
    </div>

    <!-- 목록 (아코디언) -->
    <div v-if="loadingList" class="text-gray-400 text-sm py-8 text-center">불러오는 중...</div>
    <div v-else-if="plans.length === 0" class="text-gray-400 text-sm py-12 text-center border rounded">
      {{ appliedKeyword ? '검색 결과가 없습니다.' : '반영 계획서가 없습니다.' }}
    </div>
    <div v-else class="space-y-3">
      <section v-for="p in plans" :key="p.id" class="border rounded-lg bg-white overflow-hidden">
        <!-- 계획서 헤더 (클릭 시 펼침) -->
        <div class="flex items-center hover:bg-gray-50">
          <button @click="toggle(p.id)"
            class="flex-1 min-w-0 flex items-center justify-between px-5 py-4 text-left">
            <div class="flex items-center gap-3 min-w-0">
              <span class="text-gray-400 transition-transform" :class="{ 'rotate-90': expanded[p.id] }">▶</span>
              <span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono">#{{ p.id }}</span>
              <span class="font-semibold text-gray-800 whitespace-nowrap">{{ p.title }}</span>
              <span v-if="p.summary" class="text-sm text-gray-500 truncate">— {{ p.summary }}</span>
            </div>
            <span class="text-xs text-gray-400 whitespace-nowrap pl-3">{{ p.createdAt }}</span>
          </button>
          <button @click.stop="removePlan(p)" :disabled="deletingId === p.id"
            class="px-4 self-stretch text-sm text-gray-400 hover:text-red-600 disabled:opacity-40"
            title="반영 계획서 삭제">
            {{ deletingId === p.id ? '삭제 중…' : '🗑' }}
          </button>
        </div>

        <!-- 펼친 영역: 반영 이력 목록 -->
        <div v-if="expanded[p.id]" class="border-t bg-gray-50/50 px-5 py-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="text-sm font-semibold text-gray-600">반영 이력 (SR 단위)</h4>
            <div class="flex items-center gap-2">
              <label v-if="gitSystems.length" class="flex items-center gap-1 text-xs text-gray-500">
                커밋 시스템
                <select v-model="selectedSystem" @change="loadGitCommits"
                  class="border border-gray-200 rounded px-1.5 py-0.5 text-xs focus:border-blue-400 focus:outline-none">
                  <option v-for="s in gitSystems" :key="s" :value="s">{{ s }}</option>
                </select>
              </label>
              <router-link :to="`/release-plans/${p.id}`" class="text-xs text-blue-600 hover:underline">⚙ 사이드이펙트/취약점 분석</router-link>
              <button v-if="p.docPath" @click="downloadDoc(p.docPath)" class="text-xs text-blue-600 hover:underline">⬇ docx 다운로드</button>
            </div>
          </div>

          <div v-if="loadingHistories[p.id]" class="text-gray-400 text-sm py-4">불러오는 중...</div>
          <div v-else-if="(histories[p.id] || []).length === 0" class="text-gray-400 text-sm py-4">반영 이력이 없습니다.</div>
          <div v-else class="overflow-x-auto bg-white rounded border">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-left text-gray-400 border-b bg-gray-50">
                  <th class="py-2 px-3 font-medium">No</th>
                  <th class="py-2 px-3 font-medium w-40">SR 번호</th>
                  <th class="py-2 px-3 font-medium">서비스</th>
                  <th class="py-2 px-3 font-medium">작업내용</th>
                  <th class="py-2 px-3 font-medium">요청자</th>
                  <th class="py-2 px-3 font-medium">작업자</th>
                  <th class="py-2 px-3 font-medium w-52">Git 커밋</th>
                  <th class="py-2 px-3 font-medium text-center">사이드이펙트</th>
                  <th class="py-2 px-3 font-medium text-center">장애</th>
                  <th class="py-2 px-3 font-medium">비고</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(h, idx) in histories[p.id]" :key="h.id" class="border-b last:border-b-0 hover:bg-gray-50">
                  <td class="py-2 px-3 text-gray-400">{{ idx + 1 }}</td>
                  <td class="py-2 px-3">
                    <input v-model="h.srNumber" type="text" placeholder="SR 입력"
                      class="w-full border border-gray-200 rounded px-2 py-1 text-xs focus:border-blue-400 focus:outline-none"
                      @click.stop @keyup.enter="saveSr(h)" @blur="saveSr(h)" />
                  </td>
                  <td class="py-2 px-3 text-gray-700 cursor-pointer" @click="goHistory(h.id)">{{ h.service || '-' }}</td>
                  <td class="py-2 px-3 text-gray-700 cursor-pointer" @click="goHistory(h.id)">{{ h.workContent || '-' }}</td>
                  <td class="py-2 px-3 text-gray-500 cursor-pointer" @click="goHistory(h.id)">{{ h.requester || '-' }}</td>
                  <td class="py-2 px-3 text-gray-500 cursor-pointer" @click="goHistory(h.id)">{{ h.worker || '-' }}</td>
                  <!-- Git 커밋 연동 (다중 선택) -->
                  <td class="py-2 px-3 relative" @click.stop>
                    <button @click="toggleCommitPicker(h.id)"
                      class="w-full flex items-center justify-between gap-1 border border-gray-200 rounded px-2 py-1 text-xs hover:border-blue-400 focus:outline-none">
                      <span v-if="commitCount(h)" class="text-gray-700 truncate">
                        {{ commitCount(h) }}개 커밋 ({{ shortHashes(h) }})
                      </span>
                      <span v-else class="text-gray-400">{{ gitCommits.length ? '커밋 선택' : '커밋 없음' }}</span>
                      <span class="text-gray-400">▾</span>
                    </button>
                    <div v-if="commitPickerOpen[h.id]"
                      class="absolute z-20 left-3 right-3 mt-1 bg-white border border-gray-200 rounded shadow-lg max-h-64 overflow-auto">
                      <div v-if="!gitCommits.length" class="px-3 py-3 text-xs text-gray-400">커밋이 없습니다.</div>
                      <label v-for="c in gitCommits" :key="commitToken(c)"
                        class="flex items-start gap-2 px-3 py-1.5 text-xs hover:bg-gray-50 cursor-pointer">
                        <input type="checkbox" class="mt-0.5 w-3.5 h-3.5 flex-shrink-0"
                          :checked="isCommitSelected(h, c)" @change="toggleCommit(h, c)" />
                        <span class="min-w-0">
                          <span v-if="c.project" class="text-gray-400">[{{ projLabel(c.project) }}] </span>
                          <span class="font-mono text-blue-600">{{ c.hash.slice(0, 7) }}</span>
                          <span class="text-gray-600"> · {{ c.message }}</span>
                        </span>
                      </label>
                    </div>
                  </td>
                  <!-- 사이드이펙트 검토 (git 커밋 연동 시 활성화) -->
                  <td class="py-2 px-3 text-center" @click.stop>
                    <button @click="runRowSideEffect(h)"
                      :disabled="!commitCount(h) || sideEffectLoading[h.id]"
                      class="text-xs px-2 py-1 rounded border disabled:opacity-40 disabled:cursor-not-allowed"
                      :class="commitCount(h) ? 'text-blue-600 border-blue-200 hover:bg-blue-50' : 'text-gray-400 border-gray-200'"
                      :title="commitCount(h) ? '연동된 커밋으로 사이드이펙트 검토' : 'git 커밋을 먼저 연동하세요'">
                      {{ sideEffectLoading[h.id] ? '검토 중...' : '🔍 검토' }}
                    </button>
                  </td>
                  <!-- 장애 등록 여부 -->
                  <td class="py-2 px-3 text-center cursor-pointer" @click="goHistory(h.id)">
                    <span v-if="h.incidentRegistered"
                      class="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded font-medium">🚨 등록됨</span>
                    <span v-else class="text-xs text-gray-300">미등록</span>
                  </td>
                  <td class="py-2 px-3 text-gray-500 cursor-pointer" @click="goHistory(h.id)">{{ h.note || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>

    <!-- 페이징 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-1 mt-6">
      <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
      <button v-for="n in pageNumbers" :key="n" class="page-btn"
        :class="{ 'bg-blue-600 text-white border-blue-600': n - 1 === page }"
        @click="goPage(n - 1)">{{ n }}</button>
      <button class="page-btn" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">›</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  generateReleasePlan, importReleasePlans, getReleasePlans, getReleaseHistories,
  updateSrNumber, downloadDocument, deleteReleasePlan,
  getGitCommits, getGitSystems, updateHistoryGitCommit, analyzeHistorySideEffect,
} from '../services/api.js'
import Breadcrumb from '../components/Breadcrumb.vue'

const router = useRouter()

const form = reactive({ releaseTitle: '', useGit: false, system: '', commitFrom: '', commitTo: '' })
const excelFile = ref(null)
const showCreate = ref(false)

// 엑셀 일괄 등록 상태
const showImport = ref(false)
const importFile = ref(null)
const importSummarize = ref(true)
const importing = ref(false)
const importError = ref('')
const importResult = ref(null)
const plans = ref([])
const loading = ref(false)
const loadingList = ref(true)
const error = ref('')
const deletingId = ref(null)

const keyword = ref('')
const appliedKeyword = ref('')
const page = ref(0)
const size = 10
const totalPages = ref(0)

// 아코디언 상태
const expanded = reactive({})
const histories = reactive({})
const loadingHistories = reactive({})

// git 커밋 연동
const gitSystems = ref([])
const selectedSystem = ref('')
const gitCommits = ref([])
const sideEffectLoading = reactive({})
const commitPickerOpen = reactive({})

const pageNumbers = computed(() => {
  const windowSize = 5
  let start = Math.max(0, page.value - Math.floor(windowSize / 2))
  let end = Math.min(totalPages.value, start + windowSize)
  start = Math.max(0, end - windowSize)
  return Array.from({ length: end - start }, (_, i) => start + i + 1)
})

const onFileChange = (e) => { excelFile.value = e.target.files[0] }

const onImportFileChange = (e) => { importFile.value = e.target.files[0] }

const runImport = async () => {
  if (!importFile.value) return
  importing.value = true
  importError.value = ''
  importResult.value = null
  try {
    const fd = new FormData()
    fd.append('excelFile', importFile.value)
    fd.append('summarize', importSummarize.value)
    const res = await importReleasePlans(fd)
    importResult.value = res.data
    page.value = 0
    await loadPlans()
  } catch (e) {
    importError.value = e.response?.data?.message || '일괄 등록 실패'
  } finally {
    importing.value = false
  }
}

const loadPlans = async () => {
  loadingList.value = true
  const res = await getReleasePlans({ keyword: appliedKeyword.value || undefined, page: page.value, size })
  plans.value = res.data.content
  totalPages.value = res.data.totalPages
  loadingList.value = false
}

const loadHistories = async (planId) => {
  loadingHistories[planId] = true
  try {
    const res = await getReleaseHistories(planId)
    histories[planId] = res.data
  } finally {
    loadingHistories[planId] = false
  }
}

const toggle = (planId) => {
  expanded[planId] = !expanded[planId]
  if (expanded[planId] && histories[planId] === undefined) {
    loadHistories(planId)
  }
}

const goHistory = (id) => router.push(`/release-histories/${id}`)

const loadGitSystems = async () => {
  try {
    const res = await getGitSystems()
    gitSystems.value = res.data || []
    if (!selectedSystem.value && gitSystems.value.length) {
      selectedSystem.value = gitSystems.value[0]
    }
  } catch (e) {
    gitSystems.value = []
  }
}

const loadGitCommits = async () => {
  try {
    const res = await getGitCommits({ system: selectedSystem.value || undefined, count: 50 })
    gitCommits.value = res.data
  } catch (e) {
    gitCommits.value = []
  }
}

// --- git 커밋 다중 선택 ---
// 저장 토큰: project 가 있으면 "project@hash", 없으면(로컬) "hash"
const commitToken = (c) => (c.project ? `${c.project}@${c.hash}` : c.hash)
const tokenHash = (token) => { const i = token.lastIndexOf('@'); return i < 0 ? token : token.slice(i + 1) }
const tokenProject = (token) => { const i = token.lastIndexOf('@'); return i < 0 ? '' : token.slice(0, i) }
const projLabel = (project) => (project ? project.split('/').pop() : '')

const selectedHashes = (h) => h.gitCommitHashes || []
const commitCount = (h) => selectedHashes(h).length
const isCommitSelected = (h, c) => selectedHashes(h).includes(commitToken(c))
const shortHashes = (h) => selectedHashes(h).map(t => {
  const p = tokenProject(t)
  const short = tokenHash(t).slice(0, 7)
  return p ? `[${projLabel(p)}] ${short}` : short
}).join(', ')

const toggleCommitPicker = (id) => { commitPickerOpen[id] = !commitPickerOpen[id] }

// 커밋 체크/해제 후 연동 저장 (선택 토큰 전체를 콤마로 보냄)
const toggleCommit = async (h, c) => {
  const token = commitToken(c)
  const current = selectedHashes(h)
  const next = current.includes(token)
    ? current.filter(x => x !== token)
    : [...current, token]
  try {
    const res = await updateHistoryGitCommit(h.id, {
      system: selectedSystem.value || undefined,
      commitHashes: next.join(','),
    })
    Object.assign(h, res.data)
  } catch (e) {
    error.value = 'git 커밋 연동 실패'
  }
}

// 연동된 커밋 기준 사이드이펙트 검토 → 보고서 다운로드
const runRowSideEffect = async (h) => {
  if (!commitCount(h)) return
  sideEffectLoading[h.id] = true
  error.value = ''
  try {
    const res = await analyzeHistorySideEffect(h.id)
    if (res.data?.docPath) await downloadDoc(res.data.docPath)
  } catch (e) {
    error.value = e.response?.data?.message || '사이드이펙트 검토 실패'
  } finally {
    sideEffectLoading[h.id] = false
  }
}

const removePlan = async (p) => {
  if (!confirm(`'${p.title}' 반영 계획서와 하위 반영 이력/장애를 모두 삭제할까요?`)) return
  deletingId.value = p.id
  try {
    await deleteReleasePlan(p.id)
    delete expanded[p.id]
    delete histories[p.id]
    // 마지막 항목 삭제로 빈 페이지가 되면 이전 페이지로
    if (plans.value.length === 1 && page.value > 0) page.value -= 1
    await loadPlans()
  } catch (e) {
    error.value = e.response?.data?.message || '삭제 실패'
  } finally {
    deletingId.value = null
  }
}

const saveSr = async (h) => {
  try {
    const res = await updateSrNumber(h.id, h.srNumber || '')
    Object.assign(h, res.data)
  } catch (e) {
    error.value = 'SR 번호 저장 실패'
  }
}

const search = () => {
  appliedKeyword.value = keyword.value.trim()
  page.value = 0
  loadPlans()
}

const goPage = (n) => {
  if (n < 0 || n >= totalPages.value) return
  page.value = n
  loadPlans()
}

// 셀 바깥 클릭 시 열린 커밋 선택창을 모두 닫는다 (셀에는 @click.stop 적용됨)
const closeAllCommitPickers = () => {
  Object.keys(commitPickerOpen).forEach(k => { commitPickerOpen[k] = false })
}

onMounted(async () => {
  loadPlans()
  await loadGitSystems()
  loadGitCommits()
  document.addEventListener('click', closeAllCommitPickers)
})

onUnmounted(() => {
  document.removeEventListener('click', closeAllCommitPickers)
})

const generatePlan = async () => {
  if (!excelFile.value) return
  loading.value = true
  error.value = ''
  try {
    const fd = new FormData()
    fd.append('excelFile', excelFile.value)
    fd.append('useGit', form.useGit)
    if (form.useGit) {
      if (form.system) fd.append('system', form.system)
      if (form.commitFrom) fd.append('commitFrom', form.commitFrom)
      if (form.commitTo) fd.append('commitTo', form.commitTo)
    }
    if (form.releaseTitle) fd.append('releaseTitle', form.releaseTitle)
    const res = await generateReleasePlan(fd)
    showCreate.value = false
    excelFile.value = null
    form.releaseTitle = ''
    await loadPlans()
    // 새로 만든 계획서 자동 펼침
    expanded[res.data.id] = true
    loadHistories(res.data.id)
  } catch (e) {
    error.value = e.response?.data?.message || '계획서 생성 실패'
  } finally {
    loading.value = false
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

<style scoped>
.page-btn {
  @apply min-w-8 h-8 px-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors;
}
</style>
