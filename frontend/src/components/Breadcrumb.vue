<template>
  <nav class="flex items-center gap-1.5 text-sm mb-4 flex-wrap">
    <template v-for="(item, idx) in items" :key="idx">
      <router-link v-if="item.to && idx !== items.length - 1" :to="item.to" class="text-blue-600 hover:underline">
        {{ item.label }}
      </router-link>
      <span v-else class="text-gray-700 font-medium">{{ item.label }}</span>
      <span v-if="idx !== items.length - 1" class="text-gray-400">›</span>
    </template>
  </nav>
</template>

<script setup>
defineProps({
  items: { type: Array, required: true }, // [{ label, to }]
})
</script>
