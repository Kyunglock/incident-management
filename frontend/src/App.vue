<template>
  <div class="app-layout">
    <AppSidebar />
    <main class="app-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import AppSidebar from './components/AppSidebar.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  background-color: var(--color-bg);
  min-width: 0;
}
</style>
