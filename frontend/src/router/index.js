import { createRouter, createWebHistory } from 'vue-router'
import ReleasePlanListView from '../views/ReleasePlanListView.vue'
import ReleasePlanDetailView from '../views/ReleasePlanDetailView.vue'
import ReleaseHistoryDetailView from '../views/ReleaseHistoryDetailView.vue'
import IncidentListView from '../views/IncidentListView.vue'
import IncidentDetailView from '../views/IncidentDetailView.vue'

const routes = [
  { path: '/', name: 'ReleasePlanList', component: ReleasePlanListView },
  { path: '/release-plans/:id', name: 'ReleasePlanDetail', component: ReleasePlanDetailView, props: true },
  { path: '/release-histories/:id', name: 'ReleaseHistoryDetail', component: ReleaseHistoryDetailView, props: true },
  { path: '/incidents', name: 'IncidentList', component: IncidentListView },
  { path: '/incidents/:id', name: 'IncidentDetail', component: IncidentDetailView, props: true },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
