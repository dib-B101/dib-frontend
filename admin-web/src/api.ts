import type {
  AdminMember,
  AdminQuestion,
  AdminReport,
  ApiEnvelope,
  CursorPage,
  FraudDetection,
  LoginSession,
  MemberStatus,
  ModerationProduct,
  ProductDetail,
  ProductStatus,
  ReportStatus,
  ReportType,
} from './types'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const SESSION_KEY = 'dib.admin.session'
const DEVICE_KEY = 'dib.admin.device-id'

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message)
  }
}

// 관리자 웹은 배포 ALB 의 http 주소(비보안 컨텍스트)로 열린다. crypto.randomUUID 는 HTTPS 와 localhost 에서만
// 제공돼서, 그대로 부르면 로그인할 때 "crypto.randomUUID is not a function" 으로 멈춘다.
// crypto.getRandomValues 는 http 에서도 동작하므로 없을 때는 이것으로 UUID v4 를 만든다
function randomUuid(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40 // 버전 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80 // RFC 4122 변형
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

function deviceId(): string {
  const stored = localStorage.getItem(DEVICE_KEY)
  if (stored) return stored
  const next = `admin-web-${randomUuid()}`
  localStorage.setItem(DEVICE_KEY, next)
  return next
}

export function getSession(): LoginSession | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as LoginSession) : null
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function mediaUrl(value?: string | null): string {
  if (!value) return ''
  try {
    return new URL(value, `${API_BASE_URL}/`).toString()
  } catch {
    return value
  }
}

function saveSession(session: LoginSession | null): void {
  if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  else sessionStorage.removeItem(SESSION_KEY)
  window.dispatchEvent(new Event('dib-admin-session'))
}

async function errorFrom(response: Response): Promise<ApiError> {
  let body: Record<string, unknown> | undefined
  try {
    body = (await response.json()) as Record<string, unknown>
  } catch {
    body = undefined
  }
  const message = String(body?.message || body?.error || `요청 처리에 실패했습니다. (${response.status})`)
  return new ApiError(message, response.status, body?.code ? String(body.code) : undefined)
}

async function refresh(): Promise<boolean> {
  const session = getSession()
  if (!session?.refreshToken) return false
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/token/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: session.refreshToken, deviceId: deviceId() }),
  })
  if (!response.ok) {
    saveSession(null)
    return false
  }
  const tokens = (await response.json()) as Pick<LoginSession, 'accessToken' | 'refreshToken' | 'accessExpiresIn'>
  saveSession({ ...session, ...tokens })
  return true
}

async function request<T>(path: string, init: RequestInit = {}, canRetry = true): Promise<T> {
  const session = getSession()
  const headers = new Headers(init.headers)
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`)
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  } catch {
    throw new ApiError('백엔드에 연결할 수 없습니다. 서버 주소와 실행 상태를 확인해주세요.', 0)
  }
  if (response.status === 401 && canRetry && (await refresh())) return request<T>(path, init, false)
  if (!response.ok) throw await errorFrom(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

function query(params: Record<string, string | number | boolean | null | undefined>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const value = search.toString()
  return value ? `?${value}` : ''
}

export const api = {
  baseUrl: API_BASE_URL,

  async login(email: string, password: string): Promise<LoginSession> {
    const response = await request<LoginSession>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password, deviceId: deviceId() }),
    })
    if (response.member.role !== 'ADMIN') throw new ApiError('관리자 계정만 접근할 수 있습니다.', 403)
    saveSession(response)
    return response
  },

  async logout(): Promise<void> {
    try {
      await request<void>('/api/v1/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ deviceId: deviceId() }),
      })
    } finally {
      saveSession(null)
    }
  },

  questions(answered?: boolean, cursor?: string) {
    return request<ApiEnvelope<CursorPage<AdminQuestion>>>(
      `/api/v1/admin/questions${query({ answered, cursor, size: 20 })}`,
    )
  },

  answerQuestion(questionId: number, answer: string) {
    return request<ApiEnvelope<{ questionId: number; answer: string; answeredAt: string }>>(
      `/api/v1/admin/questions/${questionId}/answer`,
      { method: 'PATCH', body: JSON.stringify({ answer }) },
    )
  },

  moderation(status?: ProductStatus) {
    return request<ApiEnvelope<ModerationProduct[]>>(
      `/api/v1/admin/products/moderation${query({ status })}`,
    )
  },

  product(productId: number) {
    return request<ApiEnvelope<ProductDetail>>(`/api/v1/products/${productId}`)
  },

  moderateProduct(productId: number, status: 'REGISTERED' | 'REJECTED', reason?: string) {
    return request<ApiEnvelope<unknown>>(`/api/v1/admin/products/${productId}/moderation`, {
      method: 'PATCH',
      body: JSON.stringify({ status, reason: reason || null }),
    })
  },

  fraudDetections(filters: { auctionId?: string; memberId?: string; riskScoreGte?: string }) {
    return request<ApiEnvelope<FraudDetection[]>>(
      `/api/v1/admin/fraud-detections${query(filters)}`,
    )
  },

  reports(filters: { type?: ReportType; status?: ReportStatus; cursor?: string }) {
    return request<ApiEnvelope<CursorPage<AdminReport>>>(
      `/api/v1/admin/reports${query({ ...filters, size: 20 })}`,
    )
  },

  processReport(
    reportId: number,
    payload: {
      status: ReportStatus
      sanction?: boolean
      sanctionStatus?: MemberStatus
      warningCount?: number
      refundReason?: string
      refundAmount?: number
      adminNote?: string
    },
  ) {
    return request<ApiEnvelope<unknown>>(`/api/v1/admin/reports/${reportId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
  },

  members(filters: { q?: string; status?: MemberStatus; warningCount?: string; cursor?: string }) {
    return request<ApiEnvelope<CursorPage<AdminMember>>>(
      `/api/v1/admin/members${query({ ...filters, size: 20 })}`,
    )
  },

  sanctionMember(memberId: number, status: 'SUSPENDED' | 'EXPELLED', reason: string, warningCount?: number) {
    return request<ApiEnvelope<unknown>>(`/api/v1/admin/members/${memberId}/sanction`, {
      method: 'PATCH',
      body: JSON.stringify({ status, reason, warningCount }),
    })
  },

  releaseMember(memberId: number) {
    return request<ApiEnvelope<unknown>>(`/api/v1/admin/members/${memberId}/sanction`, {
      method: 'DELETE',
    })
  },
}
