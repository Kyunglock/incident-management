<template>
  <div class="max-w-3xl mx-auto">
    <h2 class="text-xl font-bold text-gray-800 mb-6">반영 계획서 자동 생성</h2>

    <!-- Phase 1: 반영 계획서 생성 -->
    <section class="card mb-6">
      <h3 class="section-title">1단계 · 반영 계획서 생성</h3>

      <div class="space-y-4">
        <div>
          <label class="label">SR 내용 <span class="text-red-500">*</span></label>
          <textarea v-model="form.srContent" rows="4" class="input" placeholder="SR 또는 요청 내용을 입력하세요"></textarea>
        </div>

        <div>
          <label class="label">반영 제목</label>
          <input v-model="form.releaseTitle" type="text" class="input" placeholder="예) 2026-06-16 정기 반영" />
        </div>

        <div>
          <label class="label">공유 Excel 파일 (선택)</label>
          <input type="file" accept=".xlsx,.xls" @change="onFileChange" class="input py-1" />
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div class="col-span-3">
            <label class="label">git 저장소 경로 (선택)</label>
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
          <div class="flex items-end">
            <button @click="loadGitInfo" :disabled="!form.repoPath" class="btn-secondary w-full">
              commit 불러오기
            </button>
          </div>
        </div>

        <div v-if="gitLoaded" class="bg-green-50 border border-green-200 rounded p-3 text-sm text-green-800">
          ✓ git 정보 로드됨 — 2단계 버튼이 활성화됩니다.
        </div>

        <button @click="generatePlan" :disabled="!form.srContent || loading.plan" class="btn-primary w-full">
          <span v-if="loading.plan">생성 중...</span>
          <span v-else>📄 반영 계획서 생성</span>
        </button>
      </div>
    </section>

    <!-- 결과 및 2단계 -->
    <section v-if="result" class="card mb-6">
      <h3 class="section-title">생성 결과</h3>
      <div class="flex items-center justify-between mb-4">
        <div>
          <p class="font-medium text-gray-800">{{ result.title }}</p>
          <p class="text-xs text-gray-500">{{ result.createdAt }}</p>
        </div>
        <button @click="downloadDoc(result.docPath)" class="btn-secondary text-sm">
          ⬇ docx 다운로드
        </button>
      </div>

      <div class="border-t pt-4">
        <h4 class="text-sm font-semibold text-gray-700 mb-3">2단계 · diff 기반 분석 <span class="text-xs text-gray-400">(git 경로 입력 시 활성화)</span></h4>
        <div class="flex gap-3">
          <button @click="runSideEffect" :disabled="!gitLoaded || loading.sideEffect" class="btn-secondary flex-1">
            <span v-if="loading.sideEffect">분석 중...</span>
            <span v-else>사이드이펙트 체크</span>
          </button>
          <button @click="runVuln" :disabled="!gitLoaded || loading.vuln" class="btn-secondary flex-1">
            <span v-if="loading.vuln">분석 중...</span>
            <span v-else>웹 취약점 체크</span>
          </button>
        </div>
        <div v-if="phase2Results.length" class="mt-3 space-y-2">
          <div v-for="r in phase2Results" :key="r.type" class="flex items-center justify-between bg-gray-50 rounded p-2 text-sm">
            <span class="text-gray-600">{{ r.type }}</span>
            <button @click="downloadDoc(r.docPath)" class="text-blue-600 hover:underline text-xs">⬇ 다운로드</button>
          </div>
        </div>
      </div>
    </section>

    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { generateReleasePlan, analyzeSideEffect, analyzeVuln, downloadDocument } from '../services/api.js'

const form = reactive({
  srContent: '',
  releaseTitle: '',
  repoPath: '',
  commitFrom: '',
  commitTo: '',
})
const excelFile = ref(null)
const gitLoaded = ref(false)
const result = ref(null)
const phase2Results = ref([])
const error = ref('')
const loading = reactive({ plan: false, sideEffect: false, vuln: false })

const onFileChange = (e) => { excelFile.value = e.target.files[0] }

const loadGitInfo = () => {
  if (!form.repoPath) return
  gitLoaded.value = true
}

const generatePlan = async () => {
  if (!form.srContent) return
  loading.plan = true
  error.value = ''
  try {
    const fd = new FormData()
    if (excelFile.value) fd.append('excelFile', excelFile.value)
    fd.append('srContent', form.srContent)
    if (form.repoPath) fd.append('repoPath', form.repoPath)
    if (form.commitFrom) fd.append('commitFrom', form.commitFrom)
    if (form.commitTo) fd.append('commitTo', form.commitTo)
    if (form.releaseTitle) fd.append('releaseTitle', form.releaseTitle)
    const res = await generateReleasePlan(fd)
    result.value = res.data
    phase2Results.value = []
  } catch (e) {
    error.value = e.response?.data?.message || '계획서 생성 실패'
  } finally {
    loading.plan = false
  }
}

const runSideEffect = async () => {
  loading.sideEffect = true
  try {
    const res = await analyzeSideEffect(result.value.id, {
      repoPath: form.repoPath,
      commitFrom: form.commitFrom,
      commitTo: form.commitTo,
    })
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
    const res = await analyzeVuln(result.value.id, {
      repoPath: form.repoPath,
      commitFrom: form.commitFrom,
      commitTo: form.commitTo,
    })
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
