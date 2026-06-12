import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import IncidentListView from '../views/IncidentListView.vue'
import CreateIncidentView from '../views/CreateIncidentView.vue'
import IncidentDetailView from '../views/IncidentDetailView.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: DashboardView
  },
  {
    path: '/incidents',
    name: 'IncidentList',
    component: IncidentListView
  },
  {
    path: '/incidents/new',
    name: 'CreateIncident',
    component: CreateIncidentView
  },
  {
    path: '/incidents/:id',
    name: 'IncidentDetail',
    component: IncidentDetailView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
