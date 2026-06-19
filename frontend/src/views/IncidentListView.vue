<template>
  <div>
    <Breadcrumb :items="[{ label: '장애 관리', to: null }]" />

    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">장애 관리</h2>
      <button @click="showCreate = !showCreate" class="btn-primary text-sm">
        {{ showCreate ? '닫기' : '+ 장애 등록' }}
      </button>
    </div>

    <!-- 등록 폼 (접이식) -->
    <section v-if="showCreate" class="card mb-6">
      <h3 class="section-title">장애 등록</h3>
      <div class="grid grid-cols-2 gap-6">
        <div class="space-y-3">
          <div>
            <label class="label">대상 SR (반영 이력) <span class="text-red-500">*</span></label>
            <select v-model="form.releaseHistoryId" class="input">
              <option value="">SR 선택</option>
              <option v-for="h in histories" :key="h.id" :value="h.id">
                {{ h.srNumber || '(SR 미입력)' }} · {{ h.service || '-' }}
              </option>
            </select>
          </div>
          <div>
            <label class="label">발생 시각 (선택)</label>
            <input v-model="form.occurredAt" type="datetime-local" class="input" />
          </div>
        </div>
        <div>
          <label class="label">장애 증상 <span class="text-red-500">*</span></label>
          <textarea v-model="form.symptom" rows="5" class="input" placeholder="장애 증상을 입력하세요"></textarea>
        </div>
      </div>
      <div class="mt-4 flex items-center gap-3">
        <button @click="register" :disabled="!form.releaseHistoryId || !form.symptom || saving" class="btn-primary">
          <span v-if="saving">등록 중...</span>
          <span v-else>🚨 장애 등록</span>
        </button>
        <span v-if="error" class="text-red-600 text-sm">{{ error }}</span>
      </div>
    </section>

    <!-- 장애 목록 -->
    <div v-if="loading" class="text-gray-400 text-sm py-8 text-center">불러오는 중...</div>
    <div v-else-if="incidents.length === 0" class="text-gray-400 text-sm py-12 text-center border rounded">
      등록된 장애가 없습니다.
    </div>
    <div v-else class="overflow-x-auto bg-white rounded border">
      <table class="w-full text-sm">
        <thead>
          <tr class="text-left text-gray-400 border-b bg-gray-50">
            <th class="py-2 px-3 font-medium w-16">ID</th>
            <th class="py-2 px-3 font-medium w-40">SR 번호</th>
            <th class="py-2 px-3 font-medium w-40">서비스</th>
            <th class="py-2 px-3 font-medium">증상</th>
            <th class="py-2 px-3 font-medium w-44">발생 시각</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="i in incidents" :key="i.id"
            class="border-b last:border-b-0 hover:bg-gray-50 cursor-pointer"
            @click="goDetail(i.id)">
            <td class="py-2.5 px-3">
              <span class="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded font-mono">#{{ i.id }}</span>
            </td>
            <td class="py-2.5 px-3 font-mono text-gray-600">{{ i.srNumber || '-' }}</td>
            <td class="py-2.5 px-3 text-gray-700">{{ i.service || '-' }}</td>
            <td class="py-2.5 px-3 text-gray-700 truncate max-w-md">{{ i.symptom }}</td>
            <td class="py-2.5 px-3 text-gray-400">{{ i.occurredAt }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllIncidents, getReleaseHistorySummaries, createIncident } from '../services/api.js'
import Breadcrumb from '../components/Breadcrumb.vue'

const router = useRouter()

const incidents = ref([])
const histories = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const showCreate = ref(false)
const form = reactive({ releaseHistoryId: '', symptom: '', occurredAt: '' })

const loadIncidents = async () => {
  loading.value = true
  try {
    const res = await getAllIncidents()
    incidents.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadHistories = async () => {
  try {
    const res = await getReleaseHistorySummaries()
    histories.value = res.data || []
  } catch (e) {
    histories.value = []
  }
}

const register = async () => {
  if (!form.releaseHistoryId || !form.symptom) return
  saving.value = true
  error.value = ''
  try {
    const params = { symptom: form.symptom }
    if (form.occurredAt) params.occurredAt = form.occurredAt
    await createIncident(form.releaseHistoryId, params)
    form.symptom = ''
    form.occurredAt = ''
    form.releaseHistoryId = ''
    showCreate.value = false
    await loadIncidents()
  } catch (e) {
    error.value = e.response?.data?.message || '장애 등록 실패'
  } finally {
    saving.value = false
  }
}

const goDetail = (id) => router.push(`/incidents/${id}`)

onMounted(() => {
  loadIncidents()
  loadHistories()
})
</script>
