import { http } from './http'

export interface HealthStatus {
  service: string
  status: string
  mysql: string
  redis: string
}

export function getHealth() {
  return http.get<HealthStatus>('/api/health')
}
