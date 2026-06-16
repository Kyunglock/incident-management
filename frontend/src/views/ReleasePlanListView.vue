<template>
  <div class="max-w-6xl">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">반영 계획서</h2>

    <div class="grid grid-cols-3 gap-6">
      <!-- 생성 폼 -->
      <section class="card col-span-1 self-start">
        <h3 class="section-title">새 반영 계획서 생성</h3>
        <div class="space-y-4">
          <div>
            <label class="label">공유 Excel 파일 <span class="text-red-500">*</span></label>
            <input type="file" accept=".xlsx,.xls" @change="onFileChange" class="input py-1" />
          </div>

          <div>
            <label class="label">반영 제목</label>
            <input v-model="form.releaseTitle" type="text" class="input" placeholder="예) 2026-06-16 정기 반영" />
          </div>

          <div class="border-t pt-4">
            <label class="flex items-center gap-2 mb-3">
              <input type="checkbox" v-model="form.useGit" class="w-4 h-4" />
              <span class="text-sm font-medium text-gray-700">git 정보 사용 (선택)</span>
            </label>

            <div v-if="form.useGit" class="space-y-3">
              <div>
                <label class="label">git 저장소 경로</label>
                <input v-model="form.repoPath" type="text" class="input" placeholder="/path/to/repo" />
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

          <button @click="generatePlan" :disabled="!excelFile || loading" class="btn-primary w-full">
            <span v-if="loading">생성 중...</span>
            <span v-else>📄 반영 계획서 뼈대 생성</span>
          </button>
          <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
        </div>
      </section>

      <!-- 목록 -->
      <section class="card col-span-2">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-gray-700">반영 계획서 목록</h3>
          <div class="flex gap-2">
            <input v-model="keyword" type="text" class="input py-1 w-48" placeholder="제목 검색"
              @keyup.enter="search" />
            <button @click="search" class="btn-secondary text-sm">검색</button>
          </div>
        </div>

        <div v-if="loadingList" class="text-gray-400 text-sm">불러오는 중...</div>
        <div v-else-if="plans.length === 0" class="text-gray-400 text-sm py-8 text-center">
          {{ appliedKeyword ? '검색 결과가 없습니다.' : '반영 계획서가 없습니다.' }}
        </div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="text-left text-gray-400 border-b">
              <th class="py-2 pr-3 font-medium">ID</th>
              <th class="py-2 pr-3 font-medium">제목</th>
              <th class="py-2 pr-3 font-medium">생성일</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in plans" :key="p.id"
              class="border-b last:border-b-0 hover:bg-gray-50 cursor-pointer"
              @click="$router.push(`/release-plans/${p.id}`)">
              <td class="py-3 pr-3"><span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono">#{{ p.id }}</span></td>
              <td class="py-3 pr-3 font-medium text-gray-800">{{ p.title }}</td>
              <td class="py-3 pr-3 text-gray-400">{{ p.createdAt }}</td>
            </tr>
          </tbody>
        </table>

        <!-- 페이징 -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-1 mt-5">
          <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
          <button v-for="n in pageNumbers" :key="n" class="page-btn"
            :class="{ 'bg-blue-600 text-white border-blue-600': n - 1 === page }"
            @click="goPage(n - 1)">{{ n }}</button>
          <button class="page-btn" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">›</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { generateReleasePlan, getReleasePlans } from '../services/api.js'

const router = useRouter()

const form = reactive({
  releaseTitle: '',
  useGit: false,
  repoPath: '',
  commitFrom: '',
  commitTo: '',
})
const excelFile = ref(null)
const plans = ref([])
const loading = ref(false)
const loadingList = ref(true)
const error = ref('')

const keyword = ref('')
const appliedKeyword = ref('')
const page = ref(0)
const size = 10
const totalPages = ref(0)

const pageNumbers = computed(() => {
  const windowSize = 5
  let start = Math.max(0, page.value - Math.floor(windowSize / 2))
  let end = Math.min(totalPages.value, start + windowSize)
  start = Math.max(0, end - windowSize)
  return Array.from({ length: end - start }, (_, i) => start + i + 1)
})

const onFileChange = (e) => { excelFile.value = e.target.files[0] }

const loadPlans = async () => {
  loadingList.value = true
  const res = await getReleasePlans({ keyword: appliedKeyword.value || undefined, page: page.value, size })
  plans.value = res.data.content
  totalPages.value = res.data.totalPages
  loadingList.value = false
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

onMounted(loadPlans)

const generatePlan = async () => {
  if (!excelFile.value) return
  loading.value = true
  error.value = ''
  try {
    const fd = new FormData()
    fd.append('excelFile', excelFile.value)
    fd.append('useGit', form.useGit)
    if (form.useGit) {
      if (form.repoPath) fd.append('repoPath', form.repoPath)
      if (form.commitFrom) fd.append('commitFrom', form.commitFrom)
      if (form.commitTo) fd.append('commitTo', form.commitTo)
    }
    if (form.releaseTitle) fd.append('releaseTitle', form.releaseTitle)
    const res = await generateReleasePlan(fd)
    router.push(`/release-plans/${res.data.id}`)
  } catch (e) {
    error.value = e.response?.data?.message || '계획서 생성 실패'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-btn {
  @apply min-w-8 h-8 px-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors;
}
</style>
