<template>
  <span class="priority-badge" :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  priority: {
    type: String,
    required: true
  }
})

const priorityMap = {
  LOW: { label: '낮음', class: 'badge-low' },
  MEDIUM: { label: '보통', class: 'badge-medium' },
  HIGH: { label: '높음', class: 'badge-high' },
  CRITICAL: { label: '긴급', class: 'badge-critical' }
}

const label = computed(() => priorityMap[props.priority]?.label ?? props.priority)
const badgeClass = computed(() => priorityMap[props.priority]?.class ?? 'badge-default')
</script>

<style scoped>
.priority-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.badge-low {
  background-color: rgba(100, 116, 139, 0.15);
  color: #64748B;
}

.badge-medium {
  background-color: rgba(37, 99, 235, 0.15);
  color: #2563EB;
}

.badge-high {
  background-color: rgba(234, 88, 12, 0.15);
  color: #EA580C;
}

.badge-critical {
  background-color: rgba(220, 38, 38, 0.15);
  color: #DC2626;
}

.badge-default {
  background-color: rgba(100, 116, 139, 0.15);
  color: #64748B;
}
</style>
