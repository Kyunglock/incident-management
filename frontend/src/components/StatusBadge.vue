<template>
  <span class="status-badge" :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true
  }
})

const statusMap = {
  OPEN: { label: '접수', class: 'badge-open' },
  IN_PROGRESS: { label: '처리중', class: 'badge-in-progress' },
  RESOLVED: { label: '해결됨', class: 'badge-resolved' },
  CLOSED: { label: '종료', class: 'badge-closed' }
}

const label = computed(() => statusMap[props.status]?.label ?? props.status)
const badgeClass = computed(() => statusMap[props.status]?.class ?? 'badge-default')
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.badge-open {
  background-color: rgba(37, 99, 235, 0.15);
  color: #2563EB;
}

.badge-in-progress {
  background-color: rgba(234, 88, 12, 0.15);
  color: #EA580C;
}

.badge-resolved {
  background-color: rgba(22, 163, 74, 0.15);
  color: #16A34A;
}

.badge-closed {
  background-color: rgba(100, 116, 139, 0.15);
  color: #64748B;
}

.badge-default {
  background-color: rgba(100, 116, 139, 0.15);
  color: #64748B;
}
</style>
