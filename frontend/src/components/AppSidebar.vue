<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="sidebar-logo">⚡</div>
      <h1 class="sidebar-title">장애 관리 시스템</h1>
    </div>

    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item) }"
        exact
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer">
      <span class="version-text">v1.0.0</span>
    </div>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/', icon: '📊', label: '대시보드' },
  { path: '/incidents', icon: '🚨', label: '장애 목록' }
]

function isActive(item) {
  if (item.path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(item.path)
}
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  height: 100vh;
  background-color: #1E293B;
  color: white;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-logo {
  font-size: 1.5rem;
  line-height: 1;
}

.sidebar-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #F1F5F9;
  line-height: 1.3;
  word-break: keep-all;
}

.sidebar-nav {
  flex: 1;
  padding: 1rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--border-radius-sm);
  color: #94A3B8;
  transition: background-color 0.15s ease, color 0.15s ease;
  text-decoration: none;
  font-weight: 500;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.06);
  color: #E2E8F0;
}

.nav-item.active {
  background-color: rgba(37, 99, 235, 0.25);
  color: #93C5FD;
}

.nav-item.active .nav-icon {
  filter: none;
}

.nav-icon {
  font-size: 1.125rem;
  line-height: 1;
  width: 1.25rem;
  text-align: center;
  flex-shrink: 0;
}

.nav-label {
  font-size: 0.9375rem;
}

.sidebar-footer {
  padding: 1rem 1.25rem;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}

.version-text {
  font-size: 0.75rem;
  color: #475569;
}
</style>
