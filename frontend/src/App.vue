<template>
  <div class="min-h-screen bg-gray-100 flex">
    <aside class="w-56 bg-gray-900 text-gray-200 flex flex-col shrink-0">
      <div class="px-5 py-5 border-b border-gray-800">
        <router-link to="/" class="font-bold text-white text-base leading-tight block">
          작업계획서<br />자동 생성 시스템
        </router-link>
      </div>
      <nav class="flex-1 px-3 py-4 space-y-1">
        <router-link to="/" class="side-link">📄 작업 계획서</router-link>
        <router-link to="/incidents" class="side-link">🚨 장애 관리</router-link>
      </nav>
      <div class="px-5 py-4 text-xs text-gray-500 border-t border-gray-800">v0.1</div>
    </aside>

    <div class="flex-1 min-w-0">
      <header class="bg-white border-b border-gray-200 px-8 py-4">
        <h1 class="text-sm text-gray-400">운영 반영 · 장애 대응 자동화</h1>
      </header>
      <main class="p-8">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style>
.side-link {
  @apply block px-3 py-2 rounded-lg text-sm text-gray-300 hover:bg-gray-800 hover:text-white transition-colors;
}
.side-link.router-link-exact-active {
  @apply bg-gray-800 text-white font-medium;
}
</style>
