<template>
  <div class="max-w-6xl">
    <Breadcrumb :items="breadcrumbItems" />

    <div v-if="history">
      <div class="card mb-6">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-bold text-gray-800">반영 이력 #{{ history.id }}</h2>
            <p class="text-sm text-gray-600 mt-1">{{ history.memo || '메모 없음' }}</p>
            <p class="text-xs text-gray-400 mt-1">배포일: {{ history.deployedAt }}</p>
          </div>
          <span class="text-sm px-2 py-0.5 rounded" :class="statusClass(history.status)">{{ history.status }}</span>
        </div>

        <div class="grid grid-cols-2 gap-6 mt-5 pt-5 border-t">
          <!-- 매핑된 SR -->
          <div>
            <h4 class="text-xs font-semibold text-gray-500 mb-2">매핑된 SR ({{ history.srNumbers?.length || 0 }})</h4>
            <div v-if="history.srNumbers?.length" class="flex flex-wrap gap-1.5">
              <span v-for="sr in history.srNumbers" :key="sr"
                class="text-xs bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded">{{ sr }}</span>
            </div>
            <p v-else class="text-xs text-gray-400">매핑된 SR이 없습니다.</p>
          </div>

          <!-- 매핑된 커밋 -->
          <div>
            <h4 class="text-xs font-semibold text-gray-500 mb-2">매핑된 git 커밋 ({{ history.commits?.length || 0 }})</h4>
            <div v-if="history.commits?.length" class="space-y-1 max-h-40 overflow-y-auto">
              <div v-for="c in history.commits" :key="c.hash" class="text-xs">
                <span class="font-mono text-blue-600">{{ c.hash?.slice(0, 7) }}</span>
                <span class="text-gray-700"> {{ c.message }}</span>
                <span class="text-gray-400"> · {{ c.author }}</span>
              </div>
            </div>
            <p v-else class="text-xs text-gray-400">매핑된 커밋이 없습니다.</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-3 gap-6">
        <section class="card col-span-1 self-start">
          <h3 class="section-title">3단계 · 장애 등록</h3>
          <div class="space-y-3">
            <div>
              <label class="label">장애 증상 <span class="text-red-500">*</span></label>
              <textarea v-model="incidentForm.symptom" rows="4" class="input" placeholder="장애 증상을 입력하세요"></textarea>
            </div>
            <button @click="createIncidentEntry" :disabled="!incidentForm.symptom || loading.incident" class="btn-primary w-full">
              <span v-if="loading.incident">등록 중...</span>
              <span v-else>🚨 장애 등록</span>
            </button>
          </div>
        </section>

        <section class="card col-span-2">
          <h3 class="section-title">장애 이력 목록</h3>
          <div v-if="incidents.length === 0" class="text-gray-400 text-sm">장애 이력이 없습니다.</div>
          <table v-else class="w-full text-sm">
            <thead>
              <tr class="text-left text-gray-400 border-b">
                <th class="py-2 pr-3 font-medium">ID</th>
                <th class="py-2 pr-3 font-medium">증상</th>
                <th class="py-2 pr-3 font-medium">발생 시각</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in incidents" :key="i.id"
                class="border-b last:border-b-0 hover:bg-gray-50 cursor-pointer"
                @click="$router.push(`/incidents/${i.id}`)">
                <td class="py-3 pr-3"><span class="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded font-mono">#{{ i.id }}</span></td>
                <td class="py-3 pr-3 text-gray-700">{{ i.symptom }}</td>
                <td class="py-3 pr-3 text-gray-400">{{ i.occurredAt }}</td>
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
import { getReleaseHistory, createIncident, getIncidents, getReleasePlan } from '../services/api.js'
import Breadcrumb from '../components/Breadcrumb.vue'

const route = useRoute()
const historyId = route.params.id

const history = ref(null)
const plan = ref(null)
const incidents = ref([])

const breadcrumbItems = computed(() => [
  { label: '반영 계획서 목록', to: '/' },
  { label: plan.value ? plan.value.title : (history.value ? `#${history.value.releasePlanId}` : '...'), to: history.value ? `/release-plans/${history.value.releasePlanId}` : null },
  { label: `반영 이력 #${historyId}`, to: null },
])
const error = ref('')
const loading = reactive({ incident: false })
const incidentForm = reactive({ symptom: '' })

const statusClass = (status) => ({
  PENDING: 'bg-gray-100 text-gray-600',
  DEPLOYED: 'bg-green-100 text-green-700',
  ROLLED_BACK: 'bg-red-100 text-red-700',
}[status] || 'bg-gray-100 text-gray-600')

const load = async () => {
  const [historyRes, incidentsRes] = await Promise.all([
    getReleaseHistory(historyId),
    getIncidents(historyId),
  ])
  history.value = historyRes.data
  incidents.value = incidentsRes.data
  const planRes = await getReleasePlan(history.value.releasePlanId)
  plan.value = planRes.data
}

onMounted(load)

const createIncidentEntry = async () => {
  loading.incident = true
  error.value = ''
  try {
    await createIncident(historyId, { symptom: incidentForm.symptom })
    incidentForm.symptom = ''
    const res = await getIncidents(historyId)
    incidents.value = res.data
  } catch (e) {
    error.value = '장애 등록 실패'
  } finally {
    loading.incident = false
  }
}
</script>
