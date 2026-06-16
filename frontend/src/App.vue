<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="bg-white border-b border-gray-200 px-6 py-3 flex items-center gap-6">
      <span class="font-bold text-gray-800 text-lg">반영계획서 자동 생성</span>
      <router-link to="/" class="nav-link">홈</router-link>
      <router-link to="/release" class="nav-link">반영 계획서</router-link>
      <router-link to="/incident" class="nav-link">장애 분석</router-link>
      <router-link to="/history" class="nav-link">이력 조회</router-link>
    </nav>
    <main class="p-6">
      <router-view />
    </main>
  </div>
</template>

<style>
.nav-link {
  @apply text-gray-600 hover:text-blue-600 text-sm font-medium transition-colors;
}
.nav-link.router-link-active {
  @apply text-blue-600 font-semibold;
}
</style>
