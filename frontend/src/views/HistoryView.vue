<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-xl font-bold text-gray-800 mb-6">반영 이력 조회</h2>

    <div v-if="loading" class="text-gray-400 text-sm">불러오는 중...</div>
    <div v-else-if="history.length === 0" class="text-gray-400 text-sm">반영 이력이 없습니다.</div>

    <div v-else class="space-y-3">
      <div v-for="h in history" :key="h.id"
        class="card flex items-center justify-between">
        <div>
          <div class="flex items-center gap-2">
            <span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono">#{{ h.id }}</span>
            <p class="font-medium text-gray-800">{{ h.title }}</p>
          </div>
          <p class="text-xs text-gray-400 mt-1">{{ h.createdAt }}</p>
        </div>
        <button v-if="h.docPath" @click="downloadDoc(h.docPath)" class="btn-secondary text-sm shrink-0">
          ⬇ 반영계획서
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getReleaseHistory, downloadDocument } from '../services/api.js'

const history = ref([])
const loading = ref(true)

onMounted(async () => {
  const res = await getReleaseHistory()
  history.value = res.data
  loading.value = false
})

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
