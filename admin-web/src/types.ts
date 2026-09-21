export type MemberStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN' | 'EXPELLED'
export type ProductStatus = 'PENDING' | 'REGISTERED' | 'REJECTED' | 'ON_AUCTION' | 'SOLD' | 'CANCELED'
export type ReportStatus = 'PENDING' | 'ACCEPTED' | 'REFUNDED' | 'REJECTED'
export type ReportType = 'AUCTION' | 'ORDER' | 'CHATTING' | 'MEMBER'

export interface AdminMember {
  memberId: number
  email: string
  nickname: string
  name: string
  status: MemberStatus
  role: 'USER' | 'ADMIN'
  score: number
  warningCount: number
  suspendedAt?: string | null
  createdAt: string
  deletedAt?: string | null
}

export interface LoginSession {
  member: Pick<AdminMember, 'memberId' | 'email' | 'nickname' | 'status' | 'role'>
  accessToken: string
  refreshToken: string
  accessExpiresIn: number
}

export interface CursorPage<T> {
  items: T[]
  nextCursor?: string | null
  hasNext: boolean
}

export interface ApiEnvelope<T> {
  message: string
  data: T
}

export interface AdminQuestion {
  questionId: number
  memberId: number
  memberNickname: string
  title: string
  content: string
  createdAt: string
  answer?: string | null
  answeredAt?: string | null
}

export interface ModerationProduct {
  productId: number
  memberId: number
  memberNickname: string
  title: string
  status: ProductStatus
  createdAt: string
  updatedAt: string
  moderationReason?: string | null
  moderationStage?: string | null
  moderatedAt?: string | null
}

export interface ProductDetail {
  productId: number
  title: string
  description: string
  condition: string
  modelName?: string | null
  releaseYear?: number | null
  marketPrice?: number | null
  thumbnailUrl?: string | null
  status: ProductStatus
  categoryName?: string | null
  memberId: number
  nickname: string
  moderationReason?: string | null
  moderationStage?: string | null
  moderatedAt?: string | null
}

export interface FraudDetection {
  detectionId: number
  auctionId: number
  memberId: number
  memberNickname?: string | null
  bidId?: number | null
  ruleScore?: number | null
  mlScore?: number | null
  riskScore: number
  predictedLabel: number
  decisionThreshold?: number | null
  modelVersion?: string | null
  featureVersion?: string | null
  detectedAt: string
}

export interface AdminReport {
  reportId: number
  memberId: number
  memberNickname: string
  reportTargetId?: number | null
  reportTargetNickname?: string | null
  content: string
  type: ReportType
  auctionId?: number | null
  orderId?: number | null
  status: ReportStatus
  createdAt: string
  processedAt?: string | null
}

export type ViewKey = 'dashboard' | 'questions' | 'moderation' | 'fraud' | 'reports' | 'members'
