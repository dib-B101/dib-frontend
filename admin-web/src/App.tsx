import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react'
import { api, ApiError, getSession, mediaUrl } from './api'
import type {
  AdminMember,
  AdminQuestion,
  AdminReport,
  FraudDetection,
  LoginSession,
  MemberStatus,
  ModerationProduct,
  ProductDetail,
  ProductStatus,
  ReportStatus,
  ReportType,
  ViewKey,
} from './types'

const navigation: Array<{ key: ViewKey; label: string; icon: string; description: string }> = [
  { key: 'dashboard', label: '운영 현황', icon: '⌂', description: '처리 대기 항목 요약' },
  { key: 'questions', label: '문의 처리', icon: 'Q', description: '고객 문의와 답변' },
  { key: 'moderation', label: 'AI 상품 검수', icon: 'AI', description: '보류·차단 판정 확인' },
  { key: 'fraud', label: '이상 입찰', icon: '!', description: '위험 점수와 탐지 로그' },
  { key: 'reports', label: '신고 관리', icon: '⚑', description: '신고 판정과 제재' },
  { key: 'members', label: '회원 관리', icon: '◎', description: '회원 검색과 정지' },
]

const statusLabels: Record<string, string> = {
  ACTIVE: '활성',
  SUSPENDED: '정지',
  WITHDRAWN: '탈퇴',
  EXPELLED: '제명',
  PENDING: '대기',
  REGISTERED: '승인',
  REJECTED: '반려',
  ACCEPTED: '인정',
  REFUNDED: '환불',
  ON_AUCTION: '경매 중',
  SOLD: '판매 완료',
  CANCELED: '취소',
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : '요청 처리 중 오류가 발생했습니다.'
}

function formatDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatNumber(value?: number | null, digits = 0): string {
  if (value === undefined || value === null) return '—'
  return value.toLocaleString('ko-KR', { maximumFractionDigits: digits })
}

function StatusBadge({ value }: { value: string }) {
  return <span className={`status-badge status-${value.toLowerCase()}`}>{statusLabels[value] || value}</span>
}

function PageHeader({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="page-description">{description}</p>
      </div>
      {action}
    </div>
  )
}

function LoadingRows() {
  return (
    <div className="loading-stack" aria-label="불러오는 중">
      {[1, 2, 3].map((item) => <div className="loading-line" key={item} />)}
    </div>
  )
}

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="empty-state">
      <span className="empty-mark">✓</span>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  )
}

function ErrorBanner({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      {onRetry && <button className="button button-ghost" onClick={onRetry}>다시 시도</button>}
    </div>
  )
}

function Drawer({ title, eyebrow, onClose, children }: { title: string; eyebrow: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="drawer-backdrop" onMouseDown={onClose}>
      <aside className="drawer" onMouseDown={(event) => event.stopPropagation()}>
        <div className="drawer-header">
          <div><p className="eyebrow">{eyebrow}</p><h2>{title}</h2></div>
          <button className="icon-button" onClick={onClose} aria-label="닫기">×</button>
        </div>
        <div className="drawer-content">{children}</div>
      </aside>
    </div>
  )
}

function LoginPage({ onLogin }: { onLogin: (session: LoginSession) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      onLogin(await api.login(email.trim(), password))
    } catch (caught) {
      setError(messageOf(caught))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-layout">
      <section className="login-brand-panel">
        <div className="brand-lockup brand-lockup-light"><span className="brand-symbol">D</span><span>DIB</span></div>
        <div className="login-copy">
          <p className="eyebrow light">OPERATIONS CONSOLE</p>
          <h1>신뢰할 수 있는 거래를<br />운영하는 한 곳.</h1>
          <p>문의부터 AI 검수, 이상 입찰과 신고까지<br />실시간 운영 흐름을 놓치지 마세요.</p>
        </div>
        <div className="system-pulse"><i /> Backend API 연결 준비 완료</div>
      </section>
      <section className="login-form-panel">
        <form className="login-card" onSubmit={submit}>
          <div className="mobile-brand brand-lockup"><span className="brand-symbol">D</span><span>DIB</span></div>
          <p className="eyebrow">ADMIN ACCESS</p>
          <h2>관리자 로그인</h2>
          <p className="muted">허가된 관리자 계정으로 로그인해주세요.</p>
          {error && <ErrorBanner message={error} />}
          <label className="field">
            <span>이메일</span>
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="admin@example.com" autoComplete="username" required />
          </label>
          <label className="field">
            <span>비밀번호</span>
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="비밀번호 입력" autoComplete="current-password" required />
          </label>
          <button className="button button-primary button-wide" disabled={loading}>{loading ? '확인 중…' : '관리자 콘솔 입장'}</button>
          {api.baseUrl.includes('localhost') && <p className="local-hint">로컬 테스트: admin@example.com / Test1234!</p>}
          <p className="security-note">관리자 작업은 감사 로그와 함께 기록됩니다.</p>
        </form>
      </section>
    </main>
  )
}

interface DashboardCounts {
  questions: string
  moderation: number
  reports: string
  fraud: number
}

function Dashboard({ onNavigate }: { onNavigate: (key: ViewKey) => void }) {
  const [counts, setCounts] = useState<DashboardCounts | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [questions, moderation, reports, fraud] = await Promise.all([
        api.questions(false),
        api.moderation('PENDING'),
        api.reports({ status: 'PENDING' }),
        api.fraudDetections({ riskScoreGte: '0.7' }),
      ])
      setCounts({
        questions: `${questions.data.items.length}${questions.data.hasNext ? '+' : ''}`,
        moderation: moderation.data.length,
        reports: `${reports.data.items.length}${reports.data.hasNext ? '+' : ''}`,
        fraud: fraud.data.length,
      })
    } catch (caught) {
      setError(messageOf(caught))
    }
  }, [])

  useEffect(() => { void load() }, [load])

  const cards = [
    { key: 'questions' as ViewKey, label: '답변 대기 문의', value: counts?.questions, tone: 'blue', meta: '고객 응답 필요' },
    { key: 'moderation' as ViewKey, label: 'AI 검수 보류', value: counts?.moderation, tone: 'amber', meta: '관리자 최종 판정' },
    { key: 'reports' as ViewKey, label: '처리 대기 신고', value: counts?.reports, tone: 'red', meta: '내용 확인 필요' },
    { key: 'fraud' as ViewKey, label: '고위험 입찰', value: counts?.fraud, tone: 'purple', meta: '위험 점수 0.7 이상' },
  ]

  return (
    <>
      <PageHeader eyebrow="TODAY'S OPERATIONS" title="운영 현황" description="우선 확인해야 할 운영 큐를 한눈에 확인합니다." action={<button className="button button-secondary" onClick={load}>새로고침</button>} />
      {error && <ErrorBanner message={error} onRetry={load} />}
      <div className="metric-grid">
        {cards.map((card) => (
          <button className={`metric-card metric-${card.tone}`} key={card.key} onClick={() => onNavigate(card.key)}>
            <span className="metric-label">{card.label}</span>
            <strong>{card.value ?? '—'}</strong>
            <span className="metric-meta">{card.meta}<b>→</b></span>
          </button>
        ))}
      </div>
      <section className="panel operational-guide">
        <div>
          <p className="eyebrow">REVIEW ORDER</p>
          <h2>권장 처리 순서</h2>
        </div>
        <ol className="guide-list">
          <li><span>01</span><div><strong>고위험 신고와 이상 입찰 확인</strong><p>거래 피해 확산 가능성이 있는 회원을 먼저 확인합니다.</p></div></li>
          <li><span>02</span><div><strong>AI 검수 보류 상품 판정</strong><p>자동 판정 근거를 확인하고 승인 또는 반려합니다.</p></div></li>
          <li><span>03</span><div><strong>고객 문의 응답</strong><p>접수 순서와 영향도를 고려해 답변을 등록합니다.</p></div></li>
        </ol>
      </section>
    </>
  )
}

function QuestionsPage({ notify }: { notify: (message: string) => void }) {
  const [filter, setFilter] = useState<'all' | 'open' | 'done'>('open')
  const [items, setItems] = useState<AdminQuestion[]>([])
  const [selected, setSelected] = useState<AdminQuestion | null>(null)
  const [answer, setAnswer] = useState('')
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (nextCursor?: string, append = false) => {
    setLoading(true)
    setError('')
    try {
      const answered = filter === 'all' ? undefined : filter === 'done'
      const response = await api.questions(answered, nextCursor)
      setItems((previous) => append ? [...previous, ...response.data.items] : response.data.items)
      setCursor(response.data.nextCursor || null)
      setHasNext(response.data.hasNext)
    } catch (caught) {
      setError(messageOf(caught))
    } finally {
      setLoading(false)
    }
  }, [filter])

  useEffect(() => { void load() }, [load])

  function open(question: AdminQuestion) {
    setSelected(question)
    setAnswer(question.answer || '')
  }

  async function submitAnswer(event: FormEvent) {
    event.preventDefault()
    if (!selected || !answer.trim()) return
    setSaving(true)
    try {
      await api.answerQuestion(selected.questionId, answer.trim())
      notify('문의 답변을 등록했습니다.')
      setSelected(null)
      await load()
    } catch (caught) {
      setError(messageOf(caught))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <PageHeader eyebrow="CUSTOMER CARE" title="문의 처리" description="고객 문의 내용을 확인하고 답변을 등록합니다." action={<button className="button button-secondary" onClick={() => load()}>새로고침</button>} />
      <div className="segmented">
        {([['open', '답변 대기'], ['done', '답변 완료'], ['all', '전체']] as const).map(([value, label]) => <button key={value} className={filter === value ? 'active' : ''} onClick={() => setFilter(value)}>{label}</button>)}
      </div>
      {error && <ErrorBanner message={error} onRetry={() => load()} />}
      <section className="panel table-panel">
        {loading && !items.length ? <LoadingRows /> : !items.length ? <EmptyState title="처리할 문의가 없습니다" description="새 문의가 접수되면 이곳에 표시됩니다." /> : (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead><tr><th>ID</th><th>문의자</th><th>제목</th><th>접수 일시</th><th>상태</th><th /></tr></thead>
              <tbody>{items.map((item) => (
                <tr key={item.questionId} onClick={() => open(item)}>
                  <td className="mono">Q-{item.questionId}</td><td>{item.memberNickname}<small>#{item.memberId}</small></td><td><strong>{item.title}</strong><small className="truncate">{item.content}</small></td><td>{formatDate(item.createdAt)}</td><td>{item.answeredAt ? <StatusBadge value="ACCEPTED" /> : <StatusBadge value="PENDING" />}</td><td><button className="table-action">보기</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        {hasNext && <div className="load-more"><button className="button button-secondary" disabled={loading} onClick={() => cursor && load(cursor, true)}>다음 20개</button></div>}
      </section>
      {selected && <Drawer eyebrow={`QUESTION Q-${selected.questionId}`} title={selected.title} onClose={() => setSelected(null)}>
        <div className="detail-meta"><span>{selected.memberNickname} · 회원 #{selected.memberId}</span><span>{formatDate(selected.createdAt)}</span></div>
        <div className="content-box"><p>{selected.content}</p></div>
        <form className="drawer-form" onSubmit={submitAnswer}>
          <label className="field"><span>관리자 답변</span><textarea rows={8} value={answer} onChange={(event) => setAnswer(event.target.value)} placeholder="고객에게 전달할 답변을 입력해주세요." required /></label>
          <button className="button button-primary button-wide" disabled={saving || !answer.trim()}>{saving ? '등록 중…' : selected.answeredAt ? '답변 수정' : '답변 등록'}</button>
        </form>
      </Drawer>}
    </>
  )
}

function ModerationPage({ notify }: { notify: (message: string) => void }) {
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'REJECTED'>('PENDING')
  const [items, setItems] = useState<ModerationProduct[]>([])
  const [selected, setSelected] = useState<ModerationProduct | null>(null)
  const [detail, setDetail] = useState<ProductDetail | null>(null)
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setItems((await api.moderation(filter === 'ALL' ? undefined : filter)).data) }
    catch (caught) { setError(messageOf(caught)) }
    finally { setLoading(false) }
  }, [filter])

  useEffect(() => { void load() }, [load])

  async function open(item: ModerationProduct) {
    setSelected(item); setDetail(null); setReason(item.moderationReason || '')
    try { setDetail((await api.product(item.productId)).data) }
    catch { /* 목록 정보만으로도 판정 가능 */ }
  }

  async function decide(status: 'REGISTERED' | 'REJECTED') {
    if (!selected) return
    if (status === 'REJECTED' && !reason.trim()) { setError('반려 사유를 입력해주세요.'); return }
    setSaving(true); setError('')
    try {
      await api.moderateProduct(selected.productId, status, reason.trim())
      notify(status === 'REGISTERED' ? '상품을 승인했습니다.' : '상품을 반려했습니다.')
      setSelected(null); await load()
    } catch (caught) { setError(messageOf(caught)) }
    finally { setSaving(false) }
  }

  return (
    <>
      <PageHeader eyebrow="AI MODERATION" title="AI 상품 검수" description="자동 판정 근거를 확인하고 상품의 최종 노출 여부를 결정합니다." action={<button className="button button-secondary" onClick={load}>새로고침</button>} />
      <div className="segmented">
        {([['PENDING', '보류'], ['REJECTED', '반려'], ['ALL', '전체']] as const).map(([value, label]) => <button key={value} className={filter === value ? 'active' : ''} onClick={() => setFilter(value)}>{label}</button>)}
      </div>
      {error && <ErrorBanner message={error} onRetry={load} />}
      <section className="panel table-panel">
        {loading ? <LoadingRows /> : !items.length ? <EmptyState title="검수 대기 상품이 없습니다" description="AI가 보류하거나 차단한 상품이 이곳에 표시됩니다." /> : <div className="data-table-wrap"><table className="data-table"><thead><tr><th>상품</th><th>판매자</th><th>AI 단계</th><th>판정 사유</th><th>상태</th><th /></tr></thead><tbody>{items.map((item) => <tr key={item.productId} onClick={() => open(item)}><td><strong>{item.title}</strong><small>상품 #{item.productId} · {formatDate(item.createdAt)}</small></td><td>{item.memberNickname}<small>#{item.memberId}</small></td><td><span className="stage-chip">{item.moderationStage || '미판정'}</span></td><td className="reason-cell">{item.moderationReason || '판정 사유 없음'}</td><td><StatusBadge value={item.status} /></td><td><button className="table-action">검토</button></td></tr>)}</tbody></table></div>}
      </section>
      {selected && <Drawer eyebrow={`PRODUCT #${selected.productId}`} title={selected.title} onClose={() => setSelected(null)}>
        <div className="product-preview">
          {detail?.thumbnailUrl ? <img src={mediaUrl(detail.thumbnailUrl)} alt="상품" /> : <div className="image-placeholder">DIB</div>}
          <div><StatusBadge value={selected.status} /><p>{detail?.categoryName || '카테고리 정보 없음'}</p><strong>{detail?.marketPrice ? `${formatNumber(detail.marketPrice)}원` : '시세 미입력'}</strong></div>
        </div>
        {detail?.description && <div className="content-box"><h3>상품 설명</h3><p>{detail.description}</p></div>}
        <div className="ai-verdict">
          <div><span>검수 단계</span><strong>{selected.moderationStage || '미판정'}</strong></div>
          <div><span>검수 시각</span><strong>{formatDate(selected.moderatedAt)}</strong></div>
          <p>{selected.moderationReason || 'AI가 별도 판정 사유를 남기지 않았습니다.'}</p>
        </div>
        <label className="field"><span>관리자 판정 사유</span><textarea rows={5} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="반려 시 판매자에게 보일 사유를 입력해주세요." /></label>
        <div className="action-grid"><button className="button button-danger" disabled={saving} onClick={() => decide('REJECTED')}>반려</button><button className="button button-primary" disabled={saving} onClick={() => decide('REGISTERED')}>승인</button></div>
      </Drawer>}
    </>
  )
}

function FraudPage({ onSanction }: { onSanction: (memberId: number, nickname?: string) => void }) {
  const [items, setItems] = useState<FraudDetection[]>([])
  const [risk, setRisk] = useState('0.5')
  const [auctionId, setAuctionId] = useState('')
  const [memberId, setMemberId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setItems((await api.fraudDetections({ auctionId, memberId, riskScoreGte: risk })).data) }
    catch (caught) { setError(messageOf(caught)) }
    finally { setLoading(false) }
  }, [auctionId, memberId, risk])
  useEffect(() => { void load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <PageHeader eyebrow="FRAUD SIGNALS" title="이상 입찰 로그" description="규칙·모델 위험 점수를 비교하고 의심 회원을 즉시 조치합니다." />
      <form className="filter-bar" onSubmit={(event) => { event.preventDefault(); void load() }}>
        <label><span>최소 위험 점수</span><select value={risk} onChange={(event) => setRisk(event.target.value)}><option value="0">전체</option><option value="0.5">0.5 이상</option><option value="0.7">0.7 이상</option><option value="0.9">0.9 이상</option></select></label>
        <label><span>경매 ID</span><input inputMode="numeric" value={auctionId} onChange={(event) => setAuctionId(event.target.value)} placeholder="전체" /></label>
        <label><span>회원 ID</span><input inputMode="numeric" value={memberId} onChange={(event) => setMemberId(event.target.value)} placeholder="전체" /></label>
        <button className="button button-primary">조회</button>
      </form>
      {error && <ErrorBanner message={error} onRetry={load} />}
      <section className="panel table-panel">
        {loading ? <LoadingRows /> : !items.length ? <EmptyState title="조건에 맞는 탐지가 없습니다" description="조회 조건을 낮추거나 다른 경매를 확인해보세요." /> : <div className="data-table-wrap"><table className="data-table"><thead><tr><th>탐지</th><th>대상 회원</th><th>경매/입찰</th><th>규칙 점수</th><th>모델 점수</th><th>종합 위험</th><th /></tr></thead><tbody>{items.map((item) => <tr key={item.detectionId}><td><span className="mono">F-{item.detectionId}</span><small>{formatDate(item.detectedAt)}</small></td><td><strong>{item.memberNickname || `회원 #${item.memberId}`}</strong><small>#{item.memberId}</small></td><td>경매 #{item.auctionId}<small>{item.bidId ? `입찰 #${item.bidId}` : '대표 입찰 없음'}</small></td><td>{formatNumber(item.ruleScore, 3)}</td><td>{formatNumber(item.mlScore, 3)}</td><td><div className="risk-meter"><i style={{ width: `${Math.min(100, Math.max(0, item.riskScore * 100))}%` }} /><strong>{formatNumber(item.riskScore, 3)}</strong></div></td><td><button className="button button-small button-danger-ghost" onClick={() => onSanction(item.memberId, item.memberNickname || undefined)}>회원 정지</button></td></tr>)}</tbody></table></div>}
      </section>
    </>
  )
}

function ReportsPage({ notify, onSanction }: { notify: (message: string) => void; onSanction: (memberId: number, nickname?: string) => void }) {
  const [type, setType] = useState<ReportType | ''>('')
  const [status, setStatus] = useState<ReportStatus | ''>('PENDING')
  const [items, setItems] = useState<AdminReport[]>([])
  const [selected, setSelected] = useState<AdminReport | null>(null)
  const [adminNote, setAdminNote] = useState('')
  const [sanction, setSanction] = useState(false)
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (nextCursor?: string, append = false) => {
    setLoading(true); setError('')
    try {
      const response = await api.reports({ type: type || undefined, status: status || undefined, cursor: nextCursor })
      setItems((previous) => append ? [...previous, ...response.data.items] : response.data.items)
      setCursor(response.data.nextCursor || null); setHasNext(response.data.hasNext)
    } catch (caught) { setError(messageOf(caught)) }
    finally { setLoading(false) }
  }, [status, type])
  useEffect(() => { void load() }, [load])

  function open(item: AdminReport) { setSelected(item); setAdminNote(''); setSanction(false) }
  async function process(nextStatus: ReportStatus) {
    if (!selected) return
    setSaving(true); setError('')
    try {
      await api.processReport(selected.reportId, { status: nextStatus, sanction: nextStatus !== 'REJECTED' && sanction, sanctionStatus: 'SUSPENDED', adminNote: adminNote.trim() || undefined })
      notify('신고 처리 결과를 저장했습니다.'); setSelected(null); await load()
    } catch (caught) { setError(messageOf(caught)) }
    finally { setSaving(false) }
  }

  return (
    <>
      <PageHeader eyebrow="TRUST & SAFETY" title="신고 관리" description="신고 내용을 판정하고 필요하면 피신고 회원을 함께 제재합니다." action={<button className="button button-secondary" onClick={() => load()}>새로고침</button>} />
      <div className="filter-bar compact"><label><span>유형</span><select value={type} onChange={(event) => setType(event.target.value as ReportType | '')}><option value="">전체</option><option value="AUCTION">경매</option><option value="ORDER">거래</option><option value="CHATTING">채팅</option><option value="MEMBER">회원</option></select></label><label><span>상태</span><select value={status} onChange={(event) => setStatus(event.target.value as ReportStatus | '')}><option value="">전체</option><option value="PENDING">대기</option><option value="ACCEPTED">인정</option><option value="REFUNDED">환불</option><option value="REJECTED">기각</option></select></label></div>
      {error && <ErrorBanner message={error} onRetry={() => load()} />}
      <section className="panel table-panel">
        {loading && !items.length ? <LoadingRows /> : !items.length ? <EmptyState title="처리할 신고가 없습니다" description="새 신고가 접수되면 이곳에 표시됩니다." /> : <div className="data-table-wrap"><table className="data-table"><thead><tr><th>신고</th><th>신고자</th><th>피신고자</th><th>내용</th><th>상태</th><th /></tr></thead><tbody>{items.map((item) => <tr key={item.reportId} onClick={() => open(item)}><td><span className="mono">R-{item.reportId}</span><small>{item.type} · {formatDate(item.createdAt)}</small></td><td>{item.memberNickname}<small>#{item.memberId}</small></td><td>{item.reportTargetNickname || '대상 없음'}<small>{item.reportTargetId ? `#${item.reportTargetId}` : '—'}</small></td><td className="reason-cell">{item.content}</td><td><StatusBadge value={item.status} /></td><td><button className="table-action">처리</button></td></tr>)}</tbody></table></div>}
        {hasNext && <div className="load-more"><button className="button button-secondary" onClick={() => cursor && load(cursor, true)}>다음 20개</button></div>}
      </section>
      {selected && <Drawer eyebrow={`${selected.type} REPORT R-${selected.reportId}`} title="신고 상세" onClose={() => setSelected(null)}>
        <div className="party-grid"><div><span>신고자</span><strong>{selected.memberNickname}</strong><small>회원 #{selected.memberId}</small></div><div><span>피신고자</span><strong>{selected.reportTargetNickname || '대상 없음'}</strong><small>{selected.reportTargetId ? `회원 #${selected.reportTargetId}` : '연결된 회원 없음'}</small></div></div>
        <div className="content-box"><h3>신고 내용</h3><p>{selected.content}</p></div>
        <label className="field"><span>처리 메모</span><textarea rows={4} value={adminNote} onChange={(event) => setAdminNote(event.target.value)} placeholder="처리 근거 또는 사용자 안내 내용을 입력하세요." /></label>
        {selected.reportTargetId && <label className="check-field"><input type="checkbox" checked={sanction} onChange={(event) => setSanction(event.target.checked)} /><span><strong>신고 인정과 함께 회원 정지</strong><small>피신고 회원을 SUSPENDED 상태로 변경합니다.</small></span></label>}
        <div className={`action-grid ${selected.orderId ? 'three' : 'two'}`}><button className="button button-secondary" disabled={saving} onClick={() => process('REJECTED')}>기각</button>{selected.orderId && <button className="button button-danger" disabled={saving} onClick={() => process('REFUNDED')}>환불 처리</button>}<button className="button button-primary" disabled={saving} onClick={() => process('ACCEPTED')}>신고 인정</button></div>
        {selected.reportTargetId && <button className="text-action" onClick={() => onSanction(selected.reportTargetId!, selected.reportTargetNickname || undefined)}>회원 제재 화면에서 별도 처리 →</button>}
      </Drawer>}
    </>
  )
}

function MembersPage({ notify, focusMember, clearFocus }: { notify: (message: string) => void; focusMember: { id: number; nickname?: string } | null; clearFocus: () => void }) {
  const [items, setItems] = useState<AdminMember[]>([])
  const [q, setQ] = useState(focusMember ? String(focusMember.id) : '')
  const [status, setStatus] = useState<MemberStatus | ''>('')
  const [warningCount, setWarningCount] = useState('')
  const [selected, setSelected] = useState<AdminMember | null>(null)
  const [reason, setReason] = useState('')
  const [sanctionStatus, setSanctionStatus] = useState<'SUSPENDED' | 'EXPELLED'>('SUSPENDED')
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (nextCursor?: string, append = false, search = q) => {
    setLoading(true); setError('')
    try {
      const response = await api.members({ q: search, status: status || undefined, warningCount, cursor: nextCursor })
      setItems((previous) => append ? [...previous, ...response.data.items] : response.data.items)
      setCursor(response.data.nextCursor || null); setHasNext(response.data.hasNext)
    } catch (caught) { setError(messageOf(caught)) }
    finally { setLoading(false) }
  }, [q, status, warningCount])

  useEffect(() => {
    if (focusMember) { const search = String(focusMember.id); setQ(search); void load(undefined, false, search); clearFocus() }
    else void load()
  }, [focusMember]) // eslint-disable-line react-hooks/exhaustive-deps

  async function sanction() {
    if (!selected || !reason.trim()) { setError('제재 사유를 입력해주세요.'); return }
    setSaving(true); setError('')
    try {
      await api.sanctionMember(selected.memberId, sanctionStatus, reason.trim(), (selected.warningCount || 0) + 1)
      notify(sanctionStatus === 'SUSPENDED' ? '회원을 정지했습니다.' : '회원을 제명했습니다.')
      setSelected(null); await load()
    } catch (caught) { setError(messageOf(caught)) }
    finally { setSaving(false) }
  }

  async function release(item: AdminMember) {
    if (!window.confirm(`${item.nickname} 회원의 정지를 해제할까요?`)) return
    try { await api.releaseMember(item.memberId); notify('회원 정지를 해제했습니다.'); await load() }
    catch (caught) { setError(messageOf(caught)) }
  }

  return (
    <>
      <PageHeader eyebrow="MEMBER CONTROL" title="회원 관리" description="회원 상태와 경고 이력을 확인하고 정지 또는 해제합니다." />
      <form className="filter-bar" onSubmit={(event) => { event.preventDefault(); void load() }}><label className="grow"><span>회원 검색</span><input value={q} onChange={(event) => setQ(event.target.value)} placeholder="이메일, 닉네임, 이름 또는 회원 ID" /></label><label><span>상태</span><select value={status} onChange={(event) => setStatus(event.target.value as MemberStatus | '')}><option value="">전체</option><option value="ACTIVE">활성</option><option value="SUSPENDED">정지</option><option value="WITHDRAWN">탈퇴</option><option value="EXPELLED">제명</option></select></label><label><span>최소 경고</span><input className="short-input" inputMode="numeric" value={warningCount} onChange={(event) => setWarningCount(event.target.value)} placeholder="0" /></label><button className="button button-primary">검색</button></form>
      {error && <ErrorBanner message={error} onRetry={() => load()} />}
      <section className="panel table-panel">
        {loading && !items.length ? <LoadingRows /> : !items.length ? <EmptyState title="검색 결과가 없습니다" description="검색어나 회원 상태 조건을 변경해보세요." /> : <div className="data-table-wrap"><table className="data-table"><thead><tr><th>회원</th><th>이메일</th><th>신뢰 점수</th><th>경고</th><th>상태</th><th /></tr></thead><tbody>{items.map((item) => <tr key={item.memberId}><td><strong>{item.nickname}</strong><small>{item.name} · #{item.memberId}</small></td><td>{item.email}<small>{item.role}</small></td><td>{formatNumber(item.score, 1)}</td><td>{item.warningCount || 0}회</td><td><StatusBadge value={item.status} /></td><td>{item.role === 'ADMIN' ? <span className="muted">관리자</span> : item.status === 'SUSPENDED' ? <button className="button button-small button-secondary" onClick={() => release(item)}>정지 해제</button> : <button className="button button-small button-danger-ghost" onClick={() => { setSelected(item); setReason('') }}>제재</button>}</td></tr>)}</tbody></table></div>}
        {hasNext && <div className="load-more"><button className="button button-secondary" onClick={() => cursor && load(cursor, true)}>다음 20개</button></div>}
      </section>
      {selected && <Drawer eyebrow={`MEMBER #${selected.memberId}`} title={`${selected.nickname} 회원 제재`} onClose={() => setSelected(null)}>
        <div className="member-summary"><StatusBadge value={selected.status} /><h3>{selected.email}</h3><p>현재 경고 {selected.warningCount || 0}회 · 신뢰 점수 {formatNumber(selected.score, 1)}</p></div>
        <label className="field"><span>제재 수준</span><select value={sanctionStatus} onChange={(event) => setSanctionStatus(event.target.value as 'SUSPENDED' | 'EXPELLED')}><option value="SUSPENDED">계정 정지</option><option value="EXPELLED">영구 제명</option></select></label>
        <label className="field"><span>제재 사유</span><textarea rows={6} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="내부 기록과 회원 안내에 사용할 사유를 입력하세요." required /></label>
        <button className="button button-danger button-wide" disabled={saving || !reason.trim()} onClick={sanction}>{saving ? '처리 중…' : sanctionStatus === 'SUSPENDED' ? '회원 정지 적용' : '회원 영구 제명'}</button>
      </Drawer>}
    </>
  )
}

function AdminShell({ session, onLogout }: { session: LoginSession; onLogout: () => void }) {
  const [view, setView] = useState<ViewKey>('dashboard')
  const [menuOpen, setMenuOpen] = useState(false)
  const [toast, setToast] = useState('')
  const [focusMember, setFocusMember] = useState<{ id: number; nickname?: string } | null>(null)
  const current = useMemo(() => navigation.find((item) => item.key === view)!, [view])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 3200)
    return () => window.clearTimeout(timer)
  }, [toast])

  function navigate(key: ViewKey) { setView(key); setMenuOpen(false) }
  function sanction(memberId: number, nickname?: string) { setFocusMember({ id: memberId, nickname }); navigate('members') }

  return (
    <div className="admin-layout">
      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <div className="brand-lockup"><span className="brand-symbol">D</span><span>DIB <small>ADMIN</small></span></div>
        <nav>{navigation.map((item) => <button key={item.key} className={view === item.key ? 'active' : ''} onClick={() => navigate(item.key)}><span className="nav-icon">{item.icon}</span><span><strong>{item.label}</strong><small>{item.description}</small></span></button>)}</nav>
        <div className="sidebar-footer"><div className="admin-avatar">{session.member.nickname.slice(0, 1)}</div><div><strong>{session.member.nickname}</strong><small>{session.member.email}</small></div><button className="icon-button" onClick={onLogout} title="로그아웃">↪</button></div>
      </aside>
      {menuOpen && <button className="sidebar-scrim" aria-label="메뉴 닫기" onClick={() => setMenuOpen(false)} />}
      <main className="main-area">
        <header className="topbar"><button className="menu-button" onClick={() => setMenuOpen(true)}>☰</button><div><strong>{current.label}</strong><span className="breadcrumb">DIB 운영센터 / {current.label}</span></div><div className="live-status"><i /> API 연결됨</div></header>
        <div className="page-content">
          {view === 'dashboard' && <Dashboard onNavigate={navigate} />}
          {view === 'questions' && <QuestionsPage notify={setToast} />}
          {view === 'moderation' && <ModerationPage notify={setToast} />}
          {view === 'fraud' && <FraudPage onSanction={sanction} />}
          {view === 'reports' && <ReportsPage notify={setToast} onSanction={sanction} />}
          {view === 'members' && <MembersPage notify={setToast} focusMember={focusMember} clearFocus={() => setFocusMember(null)} />}
        </div>
      </main>
      {toast && <div className="toast"><span>✓</span>{toast}</div>}
    </div>
  )
}

export default function App() {
  const [session, setSession] = useState<LoginSession | null>(() => getSession())

  useEffect(() => {
    const sync = () => setSession(getSession())
    window.addEventListener('dib-admin-session', sync)
    return () => window.removeEventListener('dib-admin-session', sync)
  }, [])

  async function logout() {
    try { await api.logout() }
    catch (error) {
      if (!(error instanceof ApiError && error.status === 401)) console.error(error)
    } finally { setSession(null) }
  }

  return session ? <AdminShell session={session} onLogout={logout} /> : <LoginPage onLogin={setSession} />
}
