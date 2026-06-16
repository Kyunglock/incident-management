<template>
  <div class="max-w-2xl mx-auto mt-10 text-center">
    <h1 class="text-3xl font-bold text-gray-800 mb-3">반영계획서 자동 생성 시스템</h1>
    <p class="text-gray-500 mb-8 text-sm">SR · Excel · Git commit을 입력으로 반영계획서를 자동 생성합니다.</p>

    <div class="grid grid-cols-2 gap-4 mt-6">
      <router-link to="/release"
        class="card-btn bg-blue-600 text-white hover:bg-blue-700">
        <div class="text-2xl mb-2">📄</div>
        <div class="font-semibold">반영 계획서 생성</div>
        <div class="text-xs opacity-80 mt-1">SR + Excel + Git → docx</div>
      </router-link>
      <router-link to="/incident"
        class="card-btn bg-red-600 text-white hover:bg-red-700">
        <div class="text-2xl mb-2">🚨</div>
        <div class="font-semibold">장애 분석</div>
        <div class="text-xs opacity-80 mt-1">증상 + 로그 → 원인 추론</div>
      </router-link>
      <router-link to="/history"
        class="card-btn bg-gray-700 text-white hover:bg-gray-800">
        <div class="text-2xl mb-2">📋</div>
        <div class="font-semibold">반영 이력</div>
        <div class="text-xs opacity-80 mt-1">과거 반영 목록 조회</div>
      </router-link>
      <div class="card-btn bg-gray-200 text-gray-400 cursor-not-allowed">
        <div class="text-2xl mb-2">🔍</div>
        <div class="font-semibold">유사 사례 검색</div>
        <div class="text-xs mt-1">이력 축적 후 활성화 (RAG)</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-btn {
  @apply flex flex-col items-center justify-center p-6 rounded-xl transition-colors;
}
</style>
