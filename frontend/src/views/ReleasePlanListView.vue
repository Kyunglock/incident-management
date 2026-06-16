<template>
  <div class="max-w-3xl mx-auto">
    <h2 class="text-xl font-bold text-gray-800 mb-6">반영 계획서</h2>

    <!-- 생성 폼 -->
    <section class="card mb-6">
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

          <div v-if="form.useGit" class="grid grid-cols-3 gap-3">
            <div class="col-span-3">
              <label class="label">git 저장소 경로</label>
              <input v-model="form.repoPath" type="text" class="input" placeholder="/path/to/repo" />
            </div>
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

        <button @click="generatePlan" :disabled="!excelFile || loading" class="btn-primary w-full">
          <span v-if="loading">생성 중...</span>
          <span v-else>📄 반영 계획서 뼈대 생성</span>
        </button>
        <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
      </div>
    </section>

    <!-- 목록 -->
    <section class="card">
      <h3 class="section-title">반영 계획서 목록</h3>
      <div v-if="loadingList" class="text-gray-400 text-sm">불러오는 중...</div>
      <div v-else-if="plans.length === 0" class="text-gray-400 text-sm">반영 계획서가 없습니다.</div>
      <div v-else class="space-y-2">
        <router-link v-for="p in plans" :key="p.id" :to="`/release-plans/${p.id}`"
          class="flex items-center justify-between py-2 border-b last:border-b-0 hover:bg-gray-50 px-2 -mx-2 rounded">
          <div>
            <span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono mr-2">#{{ p.id }}</span>
            <span class="font-medium text-gray-800">{{ p.title }}</span>
          </div>
          <span class="text-xs text-gray-400">{{ p.createdAt }}</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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

const onFileChange = (e) => { excelFile.value = e.target.files[0] }

const loadPlans = async () => {
  loadingList.value = true
  const res = await getReleasePlans()
  plans.value = res.data
  loadingList.value = false
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
