import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ReleasePlanView from '../views/ReleasePlanView.vue'
import IncidentView from '../views/IncidentView.vue'
import HistoryView from '../views/HistoryView.vue'

const routes = [
  { path: '/', name: 'Home', component: HomeView },
  { path: '/release', name: 'ReleasePlan', component: ReleasePlanView },
  { path: '/incident', name: 'Incident', component: IncidentView },
  { path: '/history', name: 'History', component: HistoryView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
