<template>
  <div>
    <Breadcrumb :items="[{ label: '작업 계획서 목록', to: null }]" />

    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">작업 계획서</h2>
      <div class="flex gap-2">
        <button @click="showImport = !showImport" class="btn-secondary text-sm">
          {{ showImport ? '닫기' : '⬆ 엑셀 일괄 등록' }}
        </button>
        <button @click="showCreate = !showCreate" class="btn-primary text-sm">
          {{ showCreate ? '닫기' : '+ 새 작업 계획서' }}
        </button>
      </div>
    </div>

    <!-- 다중 시트 엑셀 일괄 등록 (접이식) -->
    <section v-if="showImport" class="card mb-6">
      <h3 class="section-title">엑셀 일괄 등록 (시트=날짜)</h3>
      <p class="text-xs text-gray-500 mb-3">
        시트별로 작업 계획서(제목 <code>2026-MM-DD</code>)와 SR 단위 반영 이력을 생성합니다.
        이미 같은 날짜가 등록되어 있으면 해당 시트는 무시합니다.
      </p>
      <div class="flex items-center gap-3">
        <input type="file" accept=".xlsx,.xls" @change="onImportFileChange" class="input py-1" />
        <label class="flex items-center gap-2 text-sm text-gray-700 whitespace-nowrap">
          <input type="checkbox" v-model="importSummarize" class="w-4 h-4" />
          작업내용 LLM 요약
        </label>
        <button @click="runImport" :disabled="!importFile || importing" class="btn-primary">
          <span v-if="importing">등록 중...</span>
          <span v-else>일괄 등록</span>
        </button>
      </div>
      <p v-if="importSummarize" class="text-xs text-gray-400 mt-1">
        시트마다 LLM 요약을 호출하므로 시트 수에 따라 시간이 걸릴 수 있습니다.
      </p>
      <div v-if="importError" class="text-red-600 text-sm mt-2">{{ importError }}</div>

      <!-- 결과 요약 -->
      <div v-if="importResult" class="mt-4 text-sm space-y-2">
        <div class="text-gray-700">
          전체 시트 {{ importResult.totalSheets }}개 ·
          생성 <span class="text-green-600 font-semibold">{{ importResult.created.length }}</span>건 ·
          기존 스킵 <span class="text-gray-500 font-semibold">{{ importResult.skippedExisting.length }}</span>건 ·
          무효/빈 시트 <span class="text-gray-400 font-semibold">{{ importResult.skippedEmptyOrInvalid.length }}</span>개
        </div>
        <div v-if="importResult.created.length" class="text-gray-600">
          <span class="font-medium">생성됨:</span>
          {{ importResult.created.map(c => `${c.title}(SR ${c.historyCount})`).join(', ') }}
        </div>
        <div v-if="importResult.skippedExisting.length" class="text-gray-500">
          <span class="font-medium">기존 스킵:</span> {{ importResult.skippedExisting.join(', ') }}
        </div>
      </div>
    </section>

    <!-- 생성 폼 (접이식) -->
    <section v-if="showCreate" class="card mb-6">
      <h3 class="section-title">새 작업 계획서 생성</h3>
      <div class="grid grid-cols-2 gap-6">
        <div class="space-y-4">
          <div>
            <label class="label">공유 Excel 파일 <span class="text-red-500">*</span></label>
            <input type="file" accept=".xlsx,.xls" @change="onFileChange" class="input py-1" />
          </div>
          <div>
            <label class="label">반영 제목</label>
            <input v-model="form.releaseTitle" type="text" class="input" placeholder="예) 2026-06-16 정기 반영" />
          </div>
        </div>

        <div class="space-y-4">
          <label class="flex items-center gap-2">
            <input type="checkbox" v-model="form.useGit" class="w-4 h-4" />
            <span class="text-sm font-medium text-gray-700">git 정보 사용 (선택)</span>
          </label>
          <div v-if="form.useGit" class="space-y-3">
            <div>
              <label class="label">시스템 (저장소)</label>
              <select v-model="form.system" class="input">
                <option value="">선택</option>
                <option v-for="s in gitSystems" :key="s" :value="s">{{ s }}</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">From commit</label>
                <input v-model="form.commitFrom" type="text" class="input font-mono text-sm" placeholder="HEAD~1" />
              </div>
              <div>
                <label class="label">To commit</label>
                <input v-model="form.commitTo" type="text" class="input font-mono text-sm" placeholder="HEAD" />
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="mt-4 flex items-center gap-3">
        <button @click="generatePlan" :disabled="!excelFile || loading" class="btn-primary">
          <span v-if="loading">생성 중...</span>
          <span v-else>📄 작업 계획서 뼈대 생성</span>
        </button>
        <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>
      </div>
    </section>

    <!-- 검색 -->
    <div class="flex justify-end mb-3">
      <div class="flex gap-2">
        <input v-model="keyword" type="text" class="input py-1 w-64" placeholder="제목 검색" @keyup.enter="search" />
        <button @click="search" class="btn-secondary text-sm">검색</button>
      </div>
    </div>

    <!-- 목록 (아코디언) -->
    <div v-if="loadingList" class="text-gray-400 text-sm py-8 text-center">불러오는 중...</div>
    <div v-else-if="plans.length === 0" class="text-gray-400 text-sm py-12 text-center border rounded">
      {{ appliedKeyword ? '검색 결과가 없습니다.' : '작업 계획서가 없습니다.' }}
    </div>
    <div v-else class="space-y-3">
      <section v-for="p in plans" :key="p.id" class="border rounded-lg bg-white overflow-hidden">
        <!-- 계획서 헤더 (클릭 시 펼침) -->
        <div class="flex items-center hover:bg-gray-50">
          <button @click="toggle(p.id)"
            class="flex-1 min-w-0 flex items-center justify-between px-5 py-4 text-left">
            <div class="flex items-center gap-3 min-w-0">
              <span class="text-gray-400 transition-transform" :class="{ 'rotate-90': expanded[p.id] }">▶</span>
              <span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono">#{{ p.id }}</span>
              <span class="font-semibold text-gray-800 whitespace-nowrap">{{ p.title }}</span>
              <span v-if="p.summary" class="text-sm text-gray-500 truncate">— {{ p.summary }}</span>
            </div>
            <span class="text-xs text-gray-400 whitespace-nowrap pl-3">{{ p.createdAt }}</span>
          </button>
          <button @click.stop="removePlan(p)" :disabled="deletingId === p.id"
            class="px-4 self-stretch text-sm text-gray-400 hover:text-red-600 disabled:opacity-40"
            title="작업 계획서 삭제">
            {{ deletingId === p.id ? '삭제 중…' : '🗑' }}
          </button>
        </div>

        <!-- 펼친 영역: 반영 이력 목록 -->
        <div v-if="expanded[p.id]" class="border-t bg-gray-50/50 px-5 py-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="text-sm font-semibold text-gray-600">반영 이력 (SR 단위)</h4>
            <div class="flex items-center gap-2">
              <router-link :to="`/release-plans/${p.id}`" class="text-xs text-blue-600 hover:underline">⚙ 사이드이펙트/취약점 분석</router-link>
              <button @click="generateWorkPlanDoc(p)" :disabled="workPlanLoading[p.id]"
                class="text-xs text-blue-600 hover:underline disabled:opacity-50">
                {{ workPlanLoading[p.id] ? '생성 중...' : '📋 작업내용 생성' }}
              </button>
              <button v-if="p.docPath" @click="downloadDoc(p.docPath)" class="text-xs text-blue-600 hover:underline">⬇ docx 다운로드</button>
            </div>
          </div>

          <div v-if="loadingHistories[p.id]" class="text-gray-400 text-sm py-4">불러오는 중...</div>
          <div v-else-if="(histories[p.id] || []).length === 0" class="text-gray-400 text-sm py-4">반영 이력이 없습니다.</div>
          <div v-else class="overflow-x-auto bg-white rounded border">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-left text-gray-400 border-b bg-gray-50">
                  <th class="py-2 px-3 font-medium">No</th>
                  <th class="py-2 px-3 font-medium w-40">SR 번호</th>
                  <th class="py-2 px-3 font-medium">서비스</th>
                  <th class="py-2 px-3 font-medium">작업내용</th>
                  <th class="py-2 px-3 font-medium">요청자</th>
                  <th class="py-2 px-3 font-medium">작업자</th>
                  <th class="py-2 px-3 font-medium w-80">Git 커밋</th>
                  <th class="py-2 px-3 font-medium text-center">사이드이펙트</th>
                  <th class="py-2 px-3 font-medium text-center">테스트케이스</th>
                  <th class="py-2 px-3 font-medium text-center">장애</th>
                  <th class="py-2 px-3 font-medium">비고</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(h, idx) in histories[p.id]" :key="h.id" class="border-b last:border-b-0 hover:bg-gray-50">
                  <td class="py-2 px-3 text-gray-400">{{ idx + 1 }}</td>
                  <td class="py-2 px-3">
                    <input v-model="h.srNumber" type="text" placeholder="SR 입력"
                      class="w-full border border-gray-200 rounded px-2 py-1 text-xs focus:border-blue-400 focus:outline-none"
                      @click.stop @keyup.enter="saveSr(h)" @blur="saveSr(h)" />
                  </td>
                  <td class="py-2 px-3 text-gray-700 cursor-pointer" @click="goHistory(h.id)">{{ h.service || '-' }}</td>
                  <td class="py-2 px-3 text-gray-700 cursor-pointer" @click="goHistory(h.id)">
                    <div class="max-w-[28rem] truncate" :title="h.workContent">{{ h.workContent || '-' }}</div>
                  </td>
                  <td class="py-2 px-3 text-gray-500 cursor-pointer" @click="goHistory(h.id)">{{ h.requester || '-' }}</td>
                  <td class="py-2 px-3 text-gray-500 cursor-pointer" @click="goHistory(h.id)">{{ h.worker || '-' }}</td>
                  <!-- Git 커밋 연동 (서비스로 시스템 매핑) -->
                  <td class="py-2 px-3" @click.stop>
                    <button v-if="serviceHasGit(h)" @click="openPicker($event, h)"
                      class="w-full flex items-center justify-between gap-1 border border-gray-200 rounded px-2 py-1 text-xs hover:border-blue-400 focus:outline-none">
                      <span v-if="commitCount(h)" class="text-gray-700 truncate">
                        {{ commitCount(h) }}개 커밋 선택됨
                      </span>
                      <span v-else class="text-gray-400">커밋 선택</span>
                      <span class="text-gray-400 flex-shrink-0">▾</span>
                    </button>
                    <span v-else class="text-gray-300 text-xs">-</span>
                  </td>
                  <!-- 사이드이펙트 검토 (git 커밋 연동 시 활성화) -->
                  <td class="py-2 px-3 text-center" @click.stop>
                    <div class="flex items-center justify-center gap-1">
                      <button @click="runRowSideEffect(h)"
                        :disabled="!commitCount(h) || sideEffectLoading[h.id]"
                        class="text-xs px-2 py-1 rounded border disabled:opacity-40 disabled:cursor-not-allowed"
                        :class="commitCount(h) ? 'text-blue-600 border-blue-200 hover:bg-blue-50' : 'text-gray-400 border-gray-200'"
                        :title="commitCount(h) ? '연동된 커밋으로 사이드이펙트 검토' : 'git 커밋을 먼저 연동하세요'">
                        {{ sideEffectLoading[h.id] ? '검토 중...' : '🔍 검토' }}
                      </button>
                      <button v-if="h.hasSideEffectReport" @click="openReportForHistory(h)"
                        class="text-xs px-2 py-1 rounded border text-gray-600 border-gray-200 hover:bg-gray-50"
                        title="저장된 검토 결과 보기">
                        📄 보기
                      </button>
                    </div>
                  </td>
                  <!-- 테스트케이스 (LLM 자동 생성) -->
                  <td class="py-2 px-3 text-center" @click.stop>
                    <div class="flex items-center justify-center gap-1">
                      <button @click="genTestCases(h)" :disabled="testCaseLoading[h.id]"
                        class="text-xs px-2 py-1 rounded border text-blue-600 border-blue-200 hover:bg-blue-50 disabled:opacity-40 disabled:cursor-not-allowed"
                        title="작업내용 기반 테스트케이스 자동 생성">
                        {{ testCaseLoading[h.id] ? '생성 중...' : '🧪 생성' }}
                      </button>
                      <button v-if="h.testCase" @click="openTestCase(h)"
                        class="text-xs px-2 py-1 rounded border text-gray-600 border-gray-200 hover:bg-gray-50"
                        title="생성된 테스트케이스 보기">
                        📄 보기
                      </button>
                    </div>
                  </td>
                  <!-- 장애 등록 여부 -->
                  <td class="py-2 px-3 text-center cursor-pointer" @click="goHistory(h.id)">
                    <span v-if="h.incidentRegistered"
                      class="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded font-medium">🚨 등록됨</span>
                    <span v-else class="text-xs text-gray-300">미등록</span>
                  </td>
                  <td class="py-2 px-3 text-gray-500 cursor-pointer" @click="goHistory(h.id)">{{ h.note || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>

    <!-- 페이징 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-1 mt-6">
      <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
      <button v-for="n in pageNumbers" :key="n" class="page-btn"
        :class="{ 'bg-blue-600 text-white border-blue-600': n - 1 === page }"
        @click="goPage(n - 1)">{{ n }}</button>
      <button class="page-btn" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">›</button>
    </div>

    <!-- Git 커밋 선택 패널 (테이블 overflow 에 잘리지 않도록 body 로 teleport) -->
    <Teleport to="body">
      <div v-if="picker.h" @click.stop
        class="fixed z-50 bg-white border border-gray-200 rounded shadow-lg max-h-80 overflow-y-auto overflow-x-hidden text-xs"
        :style="{ top: picker.top + 'px', left: picker.left + 'px', width: picker.width + 'px' }">
        <!-- 상단 고정: 작업내용 기준 추천 자동선택 -->
        <div class="sticky top-0 bg-gray-50 border-b px-3 py-2 flex items-center justify-between gap-2">
          <span class="text-gray-400 truncate">작업내용과 유사한 커밋을 위에 추천</span>
          <button @click="autoMapRecommended" :disabled="!recommendedCount"
            class="flex-shrink-0 text-xs px-2 py-0.5 rounded border text-blue-600 border-blue-200 hover:bg-blue-50 disabled:opacity-40 disabled:cursor-not-allowed">
            🎯 추천 {{ recommendedCount }}건 자동선택
          </button>
        </div>
        <div v-if="picker.h && commitsLoading[serviceToSystem(picker.h)]" class="px-3 py-3 text-gray-400">불러오는 중...</div>
        <div v-else-if="!rankedCommits.length" class="px-3 py-3 text-gray-400">커밋이 없습니다.</div>
        <label v-for="r in rankedCommits" :key="commitToken(r.c)"
          class="flex items-start gap-2 px-3 py-1.5 hover:bg-gray-50 cursor-pointer"
          :class="{ 'bg-amber-50/40': isRecItem(r) }">
          <input type="checkbox" class="mt-0.5 w-3.5 h-3.5 flex-shrink-0"
            :checked="isCommitSelected(picker.h, r.c)" @change="toggleCommit(picker.h, r.c)" />
          <span v-if="isRecItem(r)"
            class="mt-px flex-shrink-0 text-amber-700 bg-amber-100 rounded px-1">추천</span>
          <span v-if="r.c.project"
            class="mt-px flex-shrink-0 text-gray-500 bg-gray-100 rounded px-1">{{ projLabel(r.c.project) }}</span>
          <span class="text-gray-700 break-words">{{ r.c.message }}</span>
        </label>
      </div>
    </Teleport>

    <!-- 작업내용 생성 결과 모달 (한글파일 붙여넣기용) -->
    <Teleport to="body">
      <div v-if="workPlanModal.open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
        @click="workPlanModal.open = false">
        <div class="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex items-center justify-between px-5 py-3 border-b">
            <h3 class="font-semibold text-gray-800">작업내용 (한글파일에 붙여넣기)</h3>
            <button @click="workPlanModal.open = false" class="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
          </div>
          <div class="px-5 py-4 overflow-auto">
            <textarea :value="workPlanModal.content" readonly rows="18"
              class="w-full border border-gray-200 rounded p-3 text-xs font-mono whitespace-pre leading-relaxed focus:outline-none"></textarea>
          </div>
          <div class="flex justify-end gap-2 px-5 py-3 border-t">
            <button @click="copyWorkPlan" class="btn-secondary text-sm">
              {{ workPlanModal.copied ? '✓ 복사됨' : '📋 복사' }}
            </button>
            <button @click="workPlanModal.open = false" class="btn-primary text-sm">닫기</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 테스트케이스 모달 -->
    <Teleport to="body">
      <div v-if="testCaseModal.open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
        @click="testCaseModal.open = false">
        <div class="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex items-center justify-between px-5 py-3 border-b">
            <h3 class="font-semibold text-gray-800">테스트케이스</h3>
            <button @click="testCaseModal.open = false" class="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
          </div>
          <div class="px-5 py-4 overflow-auto">
            <div class="whitespace-pre-wrap break-words text-gray-700 text-sm leading-relaxed">{{ testCaseModal.content }}</div>
          </div>
          <div class="flex justify-end gap-2 px-5 py-3 border-t">
            <button @click="copyTestCase" class="btn-secondary text-sm">
              {{ testCaseModal.copied ? '✓ 복사됨' : '📋 복사' }}
            </button>
            <button @click="testCaseModal.open = false" class="btn-primary text-sm">닫기</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 사이드이펙트 검토 결과 모달 -->
    <Teleport to="body">
      <div v-if="report.open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
        @click="closeReport">
        <div class="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col" @click.stop>
          <div class="flex items-center justify-between px-5 py-3 border-b">
            <h3 class="font-semibold text-gray-800">사이드이펙트 검토 결과</h3>
            <button @click="closeReport" class="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
          </div>
          <div class="px-5 py-4 text-sm overflow-auto">
            <div v-if="report.loading" class="text-gray-400 py-10 text-center">불러오는 중...</div>
            <div v-else-if="report.error" class="text-red-600 py-10 text-center">{{ report.error }}</div>
            <template v-else-if="report.data">
              <div v-if="report.data.recommendation" class="mb-4">
                <h4 class="font-semibold text-gray-700 mb-1">권고 사항</h4>
                <p class="text-gray-600 whitespace-pre-wrap">{{ report.data.recommendation }}</p>
              </div>
              <div v-if="report.data.affected_modules && report.data.affected_modules.length" class="mb-4">
                <h4 class="font-semibold text-gray-700 mb-1">영향 모듈</h4>
                <div class="flex flex-wrap gap-1">
                  <span v-for="m in report.data.affected_modules" :key="m"
                    class="bg-gray-100 text-gray-600 rounded px-2 py-0.5 text-xs">{{ m }}</span>
                </div>
              </div>
              <div v-if="report.data.risk_items && report.data.risk_items.length">
                <h4 class="font-semibold text-gray-700 mb-1">위험 항목</h4>
                <ul class="space-y-2">
                  <li v-for="(r, i) in report.data.risk_items" :key="i" class="border rounded p-2">
                    <div class="flex items-center gap-2 mb-1">
                      <span class="text-xs px-1.5 py-0.5 rounded font-medium" :class="riskClass(r.level)">{{ r.level }}</span>
                      <span class="font-medium text-gray-700">{{ r.module }}</span>
                    </div>
                    <p class="text-gray-600 whitespace-pre-wrap">{{ r.risk }}</p>
                  </li>
                </ul>
              </div>
            </template>
            <div v-else class="whitespace-pre-wrap break-words text-gray-700 leading-relaxed">{{ report.raw }}</div>
          </div>
          <div class="flex justify-end gap-2 px-5 py-3 border-t">
            <button @click="closeReport" class="btn-primary text-sm">닫기</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  generateReleasePlan, importReleasePlans, getReleasePlans, getReleaseHistories,
  updateSrNumber, downloadDocument, deleteReleasePlan, generateWorkPlan,
  getGitCommits, getGitSystems, updateHistoryGitCommit, analyzeHistorySideEffect,
  getHistorySideEffect, generateTestCases,
} from '../services/api.js'
import Breadcrumb from '../components/Breadcrumb.vue'

const router = useRouter()

const form = reactive({ releaseTitle: '', useGit: false, system: '', commitFrom: '', commitTo: '' })
const excelFile = ref(null)
const showCreate = ref(false)

// 엑셀 일괄 등록 상태
const showImport = ref(false)
const importFile = ref(null)
const importSummarize = ref(true)
const importing = ref(false)
const importError = ref('')
const importResult = ref(null)
const plans = ref([])
const loading = ref(false)
const loadingList = ref(true)
const error = ref('')
const deletingId = ref(null)

const keyword = ref('')
const appliedKeyword = ref('')
const page = ref(0)
const size = 10
const totalPages = ref(0)

// 아코디언 상태
const expanded = reactive({})
const histories = reactive({})
const loadingHistories = reactive({})

// git 커밋 연동: SR 의 서비스로 git 시스템을 매핑(서비스명 == 설정된 시스템 키).
const gitSystems = ref([])
const commitsBySystem = reactive({})   // 시스템(=서비스) → 커밋 목록 (지연 로딩 캐시)
const commitsLoading = reactive({})    // 시스템 → 로딩 여부
const sideEffectLoading = reactive({})
// 커밋 선택 패널: 한 번에 하나만 열림. body 로 teleport 되며 화면 좌표로 위치를 잡는다.
const picker = reactive({ h: null, top: 0, left: 0, width: 0 })
let pickerTriggerEl = null   // 패널을 띄운 버튼 (스크롤 시 위치 재계산용)

const pageNumbers = computed(() => {
  const windowSize = 5
  let start = Math.max(0, page.value - Math.floor(windowSize / 2))
  let end = Math.min(totalPages.value, start + windowSize)
  start = Math.max(0, end - windowSize)
  return Array.from({ length: end - start }, (_, i) => start + i + 1)
})

const onFileChange = (e) => { excelFile.value = e.target.files[0] }

const onImportFileChange = (e) => { importFile.value = e.target.files[0] }

const runImport = async () => {
  if (!importFile.value) return
  importing.value = true
  importError.value = ''
  importResult.value = null
  try {
    const fd = new FormData()
    fd.append('excelFile', importFile.value)
    fd.append('summarize', importSummarize.value)
    const res = await importReleasePlans(fd)
    importResult.value = res.data
    page.value = 0
    await loadPlans()
  } catch (e) {
    importError.value = e.response?.data?.message || '일괄 등록 실패'
  } finally {
    importing.value = false
  }
}

const loadPlans = async () => {
  loadingList.value = true
  const res = await getReleasePlans({ keyword: appliedKeyword.value || undefined, page: page.value, size })
  plans.value = res.data.content
  totalPages.value = res.data.totalPages
  loadingList.value = false
}

const loadHistories = async (planId) => {
  loadingHistories[planId] = true
  try {
    const res = await getReleaseHistories(planId)
    histories[planId] = res.data
  } finally {
    loadingHistories[planId] = false
  }
}

const toggle = (planId) => {
  expanded[planId] = !expanded[planId]
  if (expanded[planId] && histories[planId] === undefined) {
    loadHistories(planId)
  }
}

const goHistory = (id) => router.push(`/release-histories/${id}`)

const loadGitSystems = async () => {
  try {
    const res = await getGitSystems()
    gitSystems.value = res.data || []
  } catch (e) {
    gitSystems.value = []
  }
}

// SR 의 서비스명을 설정된 git 시스템 키와 매핑 (NFC + 공백 무시로 비교, 정식 시스템 키 반환)
const normName = (s) => (s || '').normalize('NFC').replace(/\s/g, '')
const serviceToSystem = (h) => {
  const svc = normName(h && h.service)
  if (!svc) return ''
  return gitSystems.value.find(s => normName(s) === svc) || ''
}
const serviceHasGit = (h) => !!serviceToSystem(h)

// 시스템(=서비스)별 커밋을 지연 로딩 후 캐시
const loadCommitsForSystem = async (system) => {
  if (!system || commitsBySystem[system] !== undefined || commitsLoading[system]) return
  commitsLoading[system] = true
  try {
    const res = await getGitCommits({ system, count: 50 })
    commitsBySystem[system] = res.data || []
  } catch (e) {
    commitsBySystem[system] = []
  } finally {
    commitsLoading[system] = false
  }
}
const commitsForRow = (h) => commitsBySystem[serviceToSystem(h)] || []

// --- git 커밋 다중 선택 ---
// 저장 토큰: project 가 있으면 "project@hash", 없으면(로컬) "hash"
const commitToken = (c) => (c.project ? `${c.project}@${c.hash}` : c.hash)
const projLabel = (project) => (project ? project.split('/').pop() : '')

const selectedHashes = (h) => h.gitCommitHashes || []
const commitCount = (h) => selectedHashes(h).length
const isCommitSelected = (h, c) => selectedHashes(h).includes(commitToken(c))

// 커밋 메시지 앞쪽 [이름] 추출 (예: "[김현지] update ..." → 김현지)
const commitWorker = (c) => {
  const m = (c.message || '').match(/^\s*\[([^\]]+)\]/)
  return m ? m[1].trim() : ''
}
// 커밋의 [이름] 이 SR 작업자(콤마/슬래시로 여러 명 가능)와 일치하는지
const workerMatches = (h, c) => {
  const cw = commitWorker(c)
  if (!cw || !h.worker) return false
  return h.worker.split(/[,/]/).map(s => s.trim()).filter(Boolean)
    .some(w => w === cw || w.includes(cw) || cw.includes(w))
}

// --- 작업내용 ↔ 커밋 메시지 키워드 유사도 (인프라 불필요한 경량 매칭) ---
const REC_THRESHOLD = 0.12   // 이 점수 이상이면 '추천'
// 단어 + 한글 글자 bigram 집합 (한글은 띄어쓰기가 적어 bigram 으로 보완)
const simTokens = (s) => {
  const clean = (s || '').toLowerCase().replace(/[^0-9a-z가-힣]+/gi, ' ').trim()
  const set = new Set()
  for (const w of clean.split(/\s+/)) if (w.length >= 2) set.add(w)
  const nospace = clean.replace(/\s+/g, '')
  for (let i = 0; i < nospace.length - 1; i++) set.add(nospace.slice(i, i + 2))
  return set
}
const similarity = (a, b) => {
  const A = simTokens(a), B = simTokens(b)
  if (!A.size || !B.size) return 0
  let inter = 0
  for (const t of A) if (B.has(t)) inter++
  return inter / Math.sqrt(A.size * B.size)   // 코사인 유사
}
// 작업자 일치 가중치 (정렬용 종합 점수 산정). 둘 다 맞으면 위로 정렬된다.
const WORKER_WEIGHT = 0.3
// 작업자 일치 시 적용하는 완화된 내용 유사 기준 (작업자만으론 추천 안 됨)
const REC_THRESHOLD_WORKER = 0.06
// 종합 점수: 작업내용 유사도 + (작업자 일치 시 가중치) — 정렬에만 사용
const finalScore = (r) => r.score + (r.wmatch ? WORKER_WEIGHT : 0)
// 추천 여부: 반드시 내용 유사도가 있어야 함. 작업자 일치면 기준을 낮춰줌.
const isRecItem = (r) => r.score >= (r.wmatch ? REC_THRESHOLD_WORKER : REC_THRESHOLD)

// 현재 패널 대상 SR 기준으로 커밋 정렬 (작업자+내용 종합 점수순)
const rankedCommits = computed(() => {
  const h = picker.h
  if (!h) return []
  const wc = h.workContent || ''
  return commitsForRow(h)
    .map(c => ({ c, score: similarity(wc, c.message), wmatch: workerMatches(h, c) }))
    .sort((a, b) => {
      // 추천 항목을 항상 상단에, 그 안에서는 종합 점수순
      const ra = isRecItem(a), rb = isRecItem(b)
      if (ra !== rb) return ra ? -1 : 1
      return finalScore(b) - finalScore(a)
    })
})
const recommendedCount = computed(() => rankedCommits.value.filter(isRecItem).length)

// 추천 커밋(임계치 이상)을 현재 선택에 합쳐서 자동 연동한다.
const autoMapRecommended = async () => {
  const h = picker.h
  if (!h) return
  const recommended = rankedCommits.value
    .filter(isRecItem)
    .map(r => commitToken(r.c))
  if (!recommended.length) return
  const merged = [...new Set([...selectedHashes(h), ...recommended])]
  try {
    const res = await updateHistoryGitCommit(h.id, {
      system: serviceToSystem(h) || undefined,
      commitHashes: merged.join(','),
    })
    Object.assign(h, res.data)
  } catch (e) {
    error.value = 'git 커밋 자동 추천 실패'
  }
}

// 트리거 버튼 기준으로 패널 위치/너비를 (재)계산한다.
// 너비는 넓게 잡되 화면 밖으로 넘쳐 가로 스크롤이 생기지 않도록 화면폭에 맞춰 조정한다.
const PANEL_WIDTH = 560
const repositionPicker = () => {
  if (!picker.h || !pickerTriggerEl) return
  const rect = pickerTriggerEl.getBoundingClientRect()
  const vw = document.documentElement.clientWidth   // 스크롤바 제외 가시 폭
  const width = Math.min(PANEL_WIDTH, vw - 16)
  let left = rect.left
  if (left + width > vw - 8) left = vw - width - 8   // 오른쪽으로 넘치면 왼쪽으로 당김
  if (left < 8) left = 8
  picker.top = rect.bottom + 4
  picker.left = left
  picker.width = width
}

// 버튼 위치 아래에 패널을 띄운다. 같은 행을 다시 누르면 닫힘.
const openPicker = (e, h) => {
  if (picker.h && picker.h.id === h.id) {
    picker.h = null
    pickerTriggerEl = null
    return
  }
  pickerTriggerEl = e.currentTarget
  picker.h = h
  loadCommitsForSystem(serviceToSystem(h))   // 서비스에 매핑된 시스템의 커밋 로딩
  repositionPicker()
}

// 커밋 체크/해제 후 연동 저장 (선택 토큰 전체를 콤마로 보냄)
const toggleCommit = async (h, c) => {
  const token = commitToken(c)
  const current = selectedHashes(h)
  const next = current.includes(token)
    ? current.filter(x => x !== token)
    : [...current, token]
  try {
    const res = await updateHistoryGitCommit(h.id, {
      system: serviceToSystem(h) || undefined,
      commitHashes: next.join(','),
    })
    Object.assign(h, res.data)
  } catch (e) {
    error.value = 'git 커밋 연동 실패'
  }
}

// --- 사이드이펙트 검토 결과 모달 ---
const report = reactive({ open: false, loading: false, data: null, raw: '', error: '' })

// LLM 추론 텍스트를 모달로 보여준다. (혹시 JSON 형식이면 구조화해서 표시)
const showReport = (content) => {
  report.error = ''
  report.raw = content || ''
  try {
    const parsed = content ? JSON.parse(content) : null
    report.data = (parsed && typeof parsed === 'object') ? parsed : null
  } catch (e) {
    report.data = null   // 일반 텍스트면 raw 로 표시
  }
  report.open = true
}
const closeReport = () => { report.open = false }

// 위험 레벨별 배지 색상
const riskClass = (level) => {
  const l = (level || '').toUpperCase()
  if (l === 'HIGH') return 'bg-red-100 text-red-700'
  if (l === 'MEDIUM') return 'bg-amber-100 text-amber-700'
  if (l === 'LOW') return 'bg-green-100 text-green-700'
  return 'bg-gray-100 text-gray-600'
}

// 연동된 커밋 기준 사이드이펙트 검토 → 결과 모달 표시
const runRowSideEffect = async (h) => {
  if (!commitCount(h)) return
  sideEffectLoading[h.id] = true
  error.value = ''
  try {
    const res = await analyzeHistorySideEffect(h.id)
    h.hasSideEffectReport = true   // 검토 결과 생성됨 → 보기 버튼 활성화
    showReport(res.data?.content)
  } catch (e) {
    error.value = e.response?.data?.message || '사이드이펙트 검토 실패'
  } finally {
    sideEffectLoading[h.id] = false
  }
}

// --- 테스트케이스 자동 생성 ---
const testCaseLoading = reactive({})
const testCaseModal = reactive({ open: false, content: '', copied: false })
const genTestCases = async (h) => {
  testCaseLoading[h.id] = true
  error.value = ''
  try {
    const res = await generateTestCases(h.id)
    Object.assign(h, res.data)
    testCaseModal.content = h.testCase || ''
    testCaseModal.copied = false
    testCaseModal.open = true
  } catch (e) {
    error.value = e.response?.data?.message || '테스트케이스 생성 실패'
  } finally {
    testCaseLoading[h.id] = false
  }
}
const openTestCase = (h) => {
  testCaseModal.content = h.testCase || ''
  testCaseModal.copied = false
  testCaseModal.open = true
}
const copyTestCase = async () => {
  try {
    await navigator.clipboard.writeText(testCaseModal.content)
    testCaseModal.copied = true
    setTimeout(() => { testCaseModal.copied = false }, 1500)
  } catch (e) { /* 무시 */ }
}

// 저장된 검토 결과 다시 보기
const openReportForHistory = async (h) => {
  report.loading = true
  report.open = true
  report.data = null
  report.raw = ''
  report.error = ''
  try {
    const res = await getHistorySideEffect(h.id)
    if (res.data?.exists) {
      showReport(res.data.content)
    } else {
      report.error = '저장된 검토 결과가 없습니다.'
    }
  } catch (e) {
    report.error = '검토 결과를 불러오지 못했습니다.'
  } finally {
    report.loading = false
  }
}

// 반영이력 기반 작업내용 텍스트 생성 → 모달 표시(한글파일 붙여넣기용)
const workPlanLoading = reactive({})
const workPlanModal = reactive({ open: false, content: '', copied: false })
const generateWorkPlanDoc = async (p) => {
  workPlanLoading[p.id] = true
  error.value = ''
  try {
    const res = await generateWorkPlan(p.id)
    workPlanModal.content = res.data?.content || ''
    workPlanModal.copied = false
    workPlanModal.open = true
  } catch (e) {
    error.value = e.response?.data?.message || '작업내용 생성 실패'
  } finally {
    workPlanLoading[p.id] = false
  }
}
const copyWorkPlan = async () => {
  try {
    await navigator.clipboard.writeText(workPlanModal.content)
    workPlanModal.copied = true
    setTimeout(() => { workPlanModal.copied = false }, 1500)
  } catch (e) {
    error.value = '클립보드 복사 실패 (직접 선택해 복사하세요)'
  }
}

const removePlan = async (p) => {
  if (!confirm(`'${p.title}' 작업 계획서와 하위 반영 이력/장애를 모두 삭제할까요?`)) return
  deletingId.value = p.id
  try {
    await deleteReleasePlan(p.id)
    delete expanded[p.id]
    delete histories[p.id]
    // 마지막 항목 삭제로 빈 페이지가 되면 이전 페이지로
    if (plans.value.length === 1 && page.value > 0) page.value -= 1
    await loadPlans()
  } catch (e) {
    error.value = e.response?.data?.message || '삭제 실패'
  } finally {
    deletingId.value = null
  }
}

const saveSr = async (h) => {
  try {
    const res = await updateSrNumber(h.id, h.srNumber || '')
    Object.assign(h, res.data)
  } catch (e) {
    error.value = 'SR 번호 저장 실패'
  }
}

const search = () => {
  appliedKeyword.value = keyword.value.trim()
  page.value = 0
  loadPlans()
}

const goPage = (n) => {
  if (n < 0 || n >= totalPages.value) return
  page.value = n
  loadPlans()
}

// 바깥 클릭 시 커밋 선택 패널을 닫는다 (셀/패널에는 @click.stop 적용됨)
const closeAllCommitPickers = () => { picker.h = null; pickerTriggerEl = null }

onMounted(async () => {
  loadPlans()
  await loadGitSystems()
  document.addEventListener('click', closeAllCommitPickers)
  // teleport 패널은 고정 위치라, 스크롤/리사이즈 시 버튼을 따라 위치를 재계산한다.
  window.addEventListener('scroll', repositionPicker, true)
  window.addEventListener('resize', repositionPicker)
})

onUnmounted(() => {
  document.removeEventListener('click', closeAllCommitPickers)
  window.removeEventListener('scroll', repositionPicker, true)
  window.removeEventListener('resize', repositionPicker)
})

const generatePlan = async () => {
  if (!excelFile.value) return
  loading.value = true
  error.value = ''
  try {
    const fd = new FormData()
    fd.append('excelFile', excelFile.value)
    fd.append('useGit', form.useGit)
    if (form.useGit) {
      if (form.system) fd.append('system', form.system)
      if (form.commitFrom) fd.append('commitFrom', form.commitFrom)
      if (form.commitTo) fd.append('commitTo', form.commitTo)
    }
    if (form.releaseTitle) fd.append('releaseTitle', form.releaseTitle)
    const res = await generateReleasePlan(fd)
    showCreate.value = false
    excelFile.value = null
    form.releaseTitle = ''
    await loadPlans()
    // 새로 만든 계획서 자동 펼침
    expanded[res.data.id] = true
    loadHistories(res.data.id)
  } catch (e) {
    error.value = e.response?.data?.message || '계획서 생성 실패'
  } finally {
    loading.value = false
  }
}

const downloadDoc = async (docPath) => {
  if (!docPath) return
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

<style scoped>
.page-btn {
  @apply min-w-8 h-8 px-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors;
}
</style>
