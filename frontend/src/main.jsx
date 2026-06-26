import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import './style.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const ROLES = ['PROPOSER', 'EXPERT', 'SPONSOR', 'ADMIN'];
const INVESTOR_ALIAS = 'SPONSOR';
const CATEGORIES = ['TECH', 'LIFE', 'HEALTH', 'EDUCATION', 'ENVIRONMENT', 'CULTURE', 'ETC'];
const REWARDS = ['REWARD_POINT', 'FIRST_COME', 'PAYBACK'];
const TECH_STACKS = ['WEB_DEVELOPMENT', 'MOBILE_DEVELOPMENT', 'AI_ML', 'DATA_ENGINEERING', 'CLOUD_DEVOPS', 'CYBERSECURITY', 'EMBEDDED_SYSTEM', 'GAME_DEVELOPMENT', 'FINTECH', 'BIOTECH'];
const QUALIFICATIONS = ['BUSINESS_REGISTRATION', 'NATIONAL_QUALIFICATION'];
const PAYMENT_METHODS = ['CARD', 'VIRTUAL_ACCOUNT'];

const authStore = {
  get accessToken() { return localStorage.getItem('accessToken'); },
  get refreshToken() { return localStorage.getItem('refreshToken'); },
  setTokens(tokens) {
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
  },
  clear() { localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken'); },
};

const api = axios.create({ baseURL: API_BASE_URL, withCredentials: true });
api.interceptors.request.use((config) => {
  if (authStore.accessToken) config.headers.Authorization = `Bearer ${authStore.accessToken}`;
  return config;
});
api.interceptors.response.use(
  (res) => res.data?.data ?? res.data,
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.response?.data?.error || error.message || '요청 처리에 실패했습니다.';
    if (status === 401) {
      authStore.clear();
      if (window.location.pathname !== '/login') window.location.assign('/login');
    }
    return Promise.reject(new Error(message));
  },
);

const AuthContext = createContext(null);
const useAuth = () => useContext(AuthContext);

function decodeJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch {
    return null;
  }
}

function normalizeRole(raw) {
  const role = String(raw || '').replace('ROLE_', '').toUpperCase();
  return role === 'INVESTOR' ? INVESTOR_ALIAS : role;
}

function extractRole(payload, me) {
  return normalizeRole(me?.role || payload?.role || payload?.roles?.[0] || payload?.authorities?.[0]?.authority || payload?.auth);
}

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const refreshUser = async () => {
    if (!authStore.accessToken) { setUser(null); return null; }
    const payload = decodeJwt(authStore.accessToken);
    try {
      const me = await api.get('/users/me');
      const next = { ...me, role: extractRole(payload, me), tokenPayload: payload };
      setUser(next);
      return next;
    } catch {
      const fallback = { role: extractRole(payload), tokenPayload: payload };
      setUser(fallback.role ? fallback : null);
      return fallback;
    }
  };
  useEffect(() => { refreshUser().finally(() => setReady(true)); }, []);
  const value = useMemo(() => ({ user, ready, refreshUser, setUser }), [user, ready]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function Field({ label, children, hint }) { return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>; }
function TextInput(props) { return <input {...props} />; }
function TextArea(props) { return <textarea rows="4" {...props} />; }
function ErrorBox({ message }) { return message ? <div className="error">{message}</div> : null; }
function Empty({ children = '표시할 데이터가 없습니다.' }) { return <div className="empty">{children}</div>; }
function Card({ children, className = '' }) { return <section className={`card ${className}`}>{children}</section>; }
function Button({ children, variant = '', ...props }) { return <button className={`btn ${variant}`} {...props}>{children}</button>; }

function AppShell() {
  const { user } = useAuth();
  const nav = user?.role === 'EXPERT'
    ? [['/expert', '전문가 홈'], ['/matches', '매칭 검토']]
    : user?.role === 'ADMIN'
      ? [['/admin', '관리자'], ['/ideas', '아이디어']]
      : user?.role === 'SPONSOR'
        ? [['/ideas', '투자 둘러보기'], ['/mypage', '마이페이지']]
        : [['/ideas', '아이디어'], ['/ideas/new', '아이디어 등록'], ['/drafts', '임시저장'], ['/mypage', '마이페이지']];
  return <>
    <header className="topbar"><Link to="/" className="brand">IdeaBridge</Link><nav>{nav.map(([to, label]) => <Link key={to} to={to}>{label}</Link>)}{user ? <Logout /> : <><Link to="/login">로그인</Link><Link to="/signup">회원가입</Link></>}</nav></header>
    <main><Routes>
      <Route path="/" element={<Home />} /><Route path="/login" element={<Login />} /><Route path="/signup" element={<Signup />} />
      <Route path="/ideas" element={<Ideas />} /><Route path="/ideas/new" element={<Protected roles={['PROPOSER']}><IdeaForm /></Protected>} />
      <Route path="/ideas/:ideaId" element={<Protected><IdeaDetail /></Protected>} /><Route path="/drafts" element={<Protected roles={['PROPOSER']}><Drafts /></Protected>} />
      <Route path="/mypage" element={<Protected><MyPage /></Protected>} /><Route path="/expert" element={<Protected roles={['EXPERT']}><ExpertHome /></Protected>} />
      <Route path="/matches" element={<Protected roles={['EXPERT']}><Matches /></Protected>} /><Route path="/admin" element={<Protected roles={['ADMIN']}><AdminPage /></Protected>} />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes></main>
  </>;
}
function Protected({ roles, children }) { const { user, ready } = useAuth(); if (!ready) return <main className="loading">확인 중...</main>; if (!user) return <Navigate to="/login" replace />; if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />; return children; }
function Logout() { const navigate = useNavigate(); return <button className="linkButton" onClick={async()=>{try{await api.post('/auth/logout');}catch{} authStore.clear(); navigate('/login');}}>로그아웃</button>; }
function Home() { const { user } = useAuth(); return <div className="hero"><p className="eyebrow">검증 기반 아이디어 펀딩 플랫폼</p><h1>아이디어 제안부터 전문가 검토, 펀딩, 정산까지 한 곳에서 진행하세요.</h1><p>역할에 맞는 워크스페이스로 실제 서비스 플로우를 이어갈 수 있습니다.</p><div className="actions"><Link className="btn primary" to={user ? '/ideas' : '/signup'}>{user ? '프로젝트 보기' : '시작하기'}</Link><Link className="btn" to="/ideas">공개 프로젝트 둘러보기</Link></div></div>; }

function Login() { const { refreshUser } = useAuth(); const navigate = useNavigate(); const [form, setForm] = useState({ email:'', password:'' }); const [error, setError] = useState('');
  async function submit(e) { e.preventDefault(); setError(''); try { const tokens = await api.post('/auth/login', form); authStore.setTokens(tokens); const user = await refreshUser(); navigate(user?.role === 'EXPERT' ? '/expert' : user?.role === 'ADMIN' ? '/admin' : '/ideas'); } catch (err) { setError(err.message); } }
  return <AuthCard title="로그인"><form onSubmit={submit}><ErrorBox message={error}/><Field label="이메일"><TextInput type="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} required /></Field><Field label="비밀번호"><TextInput type="password" value={form.password} onChange={e=>setForm({...form,password:e.target.value})} required /></Field><Button variant="primary">로그인</Button></form></AuthCard>; }
function Signup() { const { refreshUser } = useAuth(); const navigate = useNavigate(); const [form, setForm] = useState({ email:'', password:'', name:'', nickname:'', age:19, role:'PROPOSER' }); const [otp, setOtp] = useState(''); const [msg,setMsg]=useState(''); const [error,setError]=useState('');
  const change = (k,v)=>setForm({...form,[k]:v});
  async function sendOtp(){setError('');try{await api.post('/auth/email-verify/send',{email:form.email});setMsg('인증번호를 전송했습니다.');}catch(e){setError(e.message)}}
  async function confirmOtp(){setError('');try{await api.post('/auth/email-verify/confirm',{email:form.email,otp});setMsg('이메일 인증이 완료되었습니다.');}catch(e){setError(e.message)}}
  async function submit(e){e.preventDefault();setError('');try{const tokens=await api.post('/auth/signup',{...form,age:Number(form.age)});authStore.setTokens(tokens);await refreshUser();navigate('/ideas');}catch(e){setError(e.message)}}
  return <AuthCard title="회원가입"><form onSubmit={submit}><ErrorBox message={error}/>{msg&&<div className="success">{msg}</div>}<Field label="이메일"><div className="inline"><TextInput type="email" value={form.email} onChange={e=>change('email',e.target.value)} required/><Button type="button" onClick={sendOtp}>인증번호 받기</Button></div></Field><Field label="인증번호"><div className="inline"><TextInput value={otp} onChange={e=>setOtp(e.target.value)}/><Button type="button" onClick={confirmOtp}>인증 확인</Button></div></Field><Field label="비밀번호"><TextInput type="password" value={form.password} onChange={e=>change('password',e.target.value)} required/></Field><Field label="이름"><TextInput value={form.name} onChange={e=>change('name',e.target.value)} required/></Field><Field label="닉네임"><TextInput value={form.nickname} onChange={e=>change('nickname',e.target.value)} required/></Field><Field label="나이"><TextInput type="number" value={form.age} onChange={e=>change('age',e.target.value)} min="19" required/></Field><Field label="역할"><select value={form.role} onChange={e=>change('role',e.target.value)}>{ROLES.map(r=><option key={r} value={r}>{r === 'SPONSOR' ? 'INVESTOR / SPONSOR' : r}</option>)}</select></Field><Button variant="primary">가입 완료</Button></form></AuthCard>; }
function AuthCard({ title, children }) { return <Card className="auth"><h1>{title}</h1>{children}</Card>; }

function Ideas() { const [items,setItems]=useState([]),[error,setError]=useState(''),[search,setSearch]=useState(''),[category,setCategory]=useState(''),[closing,setClosing]=useState(false),[sort,setSort]=useState('latest');
  async function load(){setError('');try{const params={sort,closingSoon:closing}; if(category) params.category=category; const data=search?await api.get('/ideas/search',{params:{keyword:search,sort}}):await api.get('/ideas',{params}); setItems(data.content||data||[]);}catch(e){setError(e.message)}} useEffect(()=>{load()},[]);
  return <div><PageTitle title="아이디어 목록" action={<Link className="btn primary" to="/ideas/new">새 아이디어 등록</Link>}/><Card><div className="filters"><input placeholder="프로젝트명 검색" value={search} onChange={e=>setSearch(e.target.value)}/><select value={category} onChange={e=>setCategory(e.target.value)}><option value="">전체 카테고리</option>{CATEGORIES.map(x=><option key={x}>{x}</option>)}</select><select value={sort} onChange={e=>setSort(e.target.value)}><option value="latest">최신순</option><option value="amount">모금액순</option><option value="closing">마감임박순</option></select><label className="check"><input type="checkbox" checked={closing} onChange={e=>setClosing(e.target.checked)}/> 마감임박</label><Button onClick={load}>검색</Button></div><ErrorBox message={error}/></Card><div className="grid">{items.map(i=><IdeaCard key={i.ideaId} idea={i}/>)}</div>{!items.length&&!error&&<Empty/>}</div>; }
function IdeaCard({ idea }) { const pct = idea.goalAmount ? Math.min(100, Math.round((idea.currentAmount || 0) / idea.goalAmount * 100)) : 0; return <Link className="ideaCard" to={`/ideas/${idea.ideaId}`}><span>{idea.category}</span><h3>{idea.title}</h3><p>{idea.oneLineIntro}</p><div className="progress"><i style={{width:`${pct}%`}}/></div><b>{pct}% 달성</b><small>{idea.status}</small></Link>; }
function PageTitle({ title, action }) { return <div className="pageTitle"><h1>{title}</h1>{action}</div>; }

const emptyIdea = { title:'', category:'TECH', oneLineIntro:'', problemDefinition:'', solution:'', goal:'', targetCustomer:'', competitor:'', teamIntro:'', goalAmount:1000000, fundingStartAt:'', fundingEndAt:'', rewardType:'REWARD_POINT', milestones:[1,2,3].map(step=>({step,goal:'',expectedResult:'',expectedDate:''})) };
function IdeaForm({ draft }) { const navigate=useNavigate(); const [form,setForm]=useState(draft||emptyIdea); const [error,setError]=useState(''); const set=(k,v)=>setForm({...form,[k]:v}); const ms=(idx,k,v)=>setForm({...form,milestones:form.milestones.map((m,i)=>i===idx?{...m,[k]:v}:m)});
  async function submit(e){e.preventDefault();setError('');try{const body={...form,goalAmount:Number(form.goalAmount),fundingStartAt:toLocalDateTime(form.fundingStartAt),fundingEndAt:toLocalDateTime(form.fundingEndAt)}; const res=await api.post('/ideas',body); navigate(`/ideas/${res.ideaId}`);}catch(e){setError(e.message)}}
  async function saveDraft(){setError('');try{await api.post('/ideas/drafts',{...form, milestones:undefined, goalAmount: form.goalAmount?Number(form.goalAmount):null, fundingStartAt: toLocalDateTime(form.fundingStartAt), fundingEndAt: toLocalDateTime(form.fundingEndAt)});navigate('/drafts');}catch(e){setError(e.message)}}
  return <Card><h1>아이디어 등록</h1><form onSubmit={submit} className="wideForm"><ErrorBox message={error}/><IdeaFields form={form} set={set}/><h2>3단계 마일스톤</h2>{form.milestones.map((m,i)=><div className="milestone" key={m.step}><b>{m.step}단계</b><Field label="목표"><TextArea value={m.goal} onChange={e=>ms(i,'goal',e.target.value)} required/></Field><Field label="예상 결과"><TextArea value={m.expectedResult} onChange={e=>ms(i,'expectedResult',e.target.value)} required/></Field><Field label="예상 완료일"><TextInput type="date" value={m.expectedDate} onChange={e=>ms(i,'expectedDate',e.target.value)} required/></Field></div>)}<div className="actions"><Button type="button" onClick={saveDraft}>임시저장</Button><Button variant="primary">등록 제출</Button></div></form></Card>; }
function IdeaFields({form,set}) { return <><Field label="제목"><TextInput value={form.title} onChange={e=>set('title',e.target.value)} required/></Field><Field label="카테고리"><select value={form.category} onChange={e=>set('category',e.target.value)}>{CATEGORIES.map(x=><option key={x}>{x}</option>)}</select></Field><Field label="한 줄 소개"><TextInput value={form.oneLineIntro} onChange={e=>set('oneLineIntro',e.target.value)} required/></Field>{['problemDefinition','solution','goal','targetCustomer','competitor','teamIntro'].map(k=><Field key={k} label={({problemDefinition:'문제 정의',solution:'해결 방안',goal:'사업 목표',targetCustomer:'목표 고객',competitor:'경쟁사',teamIntro:'팀 소개'})[k]}><TextArea value={form[k]} onChange={e=>set(k,e.target.value)} required/></Field>)}<Field label="목표 금액"><TextInput type="number" min="1000000" value={form.goalAmount} onChange={e=>set('goalAmount',e.target.value)} required/></Field><Field label="펀딩 시작"><TextInput type="datetime-local" value={form.fundingStartAt} onChange={e=>set('fundingStartAt',e.target.value)} required/></Field><Field label="펀딩 종료"><TextInput type="datetime-local" value={form.fundingEndAt} onChange={e=>set('fundingEndAt',e.target.value)} required/></Field><Field label="보상 방식"><select value={form.rewardType} onChange={e=>set('rewardType',e.target.value)}>{REWARDS.map(x=><option key={x}>{x}</option>)}</select></Field></>; }
function toLocalDateTime(v){ return v ? (v.length===16 ? `${v}:00` : v) : null; }

function IdeaDetail(){ const {ideaId}=useParams(); const {user}=useAuth(); const [idea,setIdea]=useState(null),[fundings,setFundings]=useState([]),[error,setError]=useState(''); useEffect(()=>{api.get(`/ideas/${ideaId}`).then(setIdea).catch(e=>setError(e.message)); api.get(`/fundings/ideas/${ideaId}`).then(d=>setFundings(d.content||[])).catch(()=>{});},[ideaId]); if(error) return <ErrorBox message={error}/>; if(!idea) return <div className="loading">불러오는 중...</div>; return <div><PageTitle title={idea.title}/><Card><p className="lead">{idea.oneLineIntro}</p><Meta data={idea}/><div className="detail"><h3>문제 정의</h3><p>{idea.problemDefinition}</p><h3>해결 방안</h3><p>{idea.solution}</p><h3>목표</h3><p>{idea.goal}</p><h3>목표 고객</h3><p>{idea.targetCustomer}</p><h3>경쟁 환경</h3><p>{idea.competitor}</p><h3>팀 소개</h3><p>{idea.teamIntro}</p></div></Card><div className="columns"><FundingPanel ideaId={ideaId}/><ReportPanel ideaId={ideaId}/>{user?.role==='PROPOSER'&&<ProposerOps ideaId={ideaId}/>}</div><Card><h2>후원 현황</h2>{fundings.length?fundings.map(f=><p key={f.fundingId}>{f.amount?.toLocaleString()}원 · {f.status}</p>):<Empty/>}</Card></div> }
function Meta({data}){return <div className="meta">{['category','status','badge','rewardType'].map(k=><span key={k}>{data[k]}</span>)}<span>목표 {data.goalAmount?.toLocaleString()}원</span><span>현재 {data.currentAmount?.toLocaleString()}원</span></div>}
function FundingPanel({ideaId}){ const [amount,setAmount]=useState(10000),[sponsorId,setSponsorId]=useState(''),[funding,setFunding]=useState(null),[method,setMethod]=useState('CARD'),[payment,setPayment]=useState(null),[error,setError]=useState(''); async function fund(){try{setError('');setFunding(await api.post(`/fundings/ideas/${ideaId}`,{sponsorId:Number(sponsorId),amount:Number(amount)}));}catch(e){setError(e.message)}} async function pay(){try{setPayment(await api.post('/payments',{fundingId:funding.fundingId,amount:Number(amount),method}));}catch(e){setError(e.message)}} return <Card><h2>후원하기</h2><ErrorBox message={error}/><Field label="후원자 번호"><TextInput type="number" value={sponsorId} onChange={e=>setSponsorId(e.target.value)}/></Field><Field label="금액"><TextInput type="number" value={amount} onChange={e=>setAmount(e.target.value)}/></Field><Button onClick={fund}>후원 신청</Button>{funding&&<><Field label="결제 수단"><select value={method} onChange={e=>setMethod(e.target.value)}>{PAYMENT_METHODS.map(x=><option key={x}>{x}</option>)}</select></Field><Button variant="primary" onClick={pay}>결제 준비</Button></>}{payment&&<div className="success">결제가 생성되었습니다. 주문번호 {payment.orderId}</div>}</Card> }
function ReportPanel({ideaId}){const[reason,setReason]=useState(''),[msg,setMsg]=useState(''),[error,setError]=useState('');return <Card><h2>신고</h2><ErrorBox message={error}/><TextArea placeholder="신고 사유" value={reason} onChange={e=>setReason(e.target.value)}/><Button onClick={async()=>{try{setMsg((await api.post(`/ideas/${ideaId}/reports`,{reason})).message)}catch(e){setError(e.message)}}}>신고 접수</Button>{msg&&<div className="success">{msg}</div>}</Card>}
function ProposerOps({ideaId}){return <Card><h2>프로젝트 운영</h2><Link className="btn" to={`/mypage?ideaId=${ideaId}`}>자금 사용·정산 관리</Link></Card>}

function Drafts(){const[items,setItems]=useState([]),[error,setError]=useState(''); useEffect(()=>{api.get('/ideas/drafts').then(setItems).catch(e=>setError(e.message))},[]); return <div><PageTitle title="임시저장"/><ErrorBox message={error}/><div className="grid">{items.map(d=><Card key={d.draftId}><h3>{d.title||'제목 없음'}</h3><p>{d.oneLineIntro}</p><Button onClick={async()=>{await api.delete(`/ideas/drafts/${d.draftId}`);setItems(items.filter(x=>x.draftId!==d.draftId));}}>삭제</Button></Card>)}</div>{!items.length&&!error&&<Empty/>}</div>}
function MyPage(){const[user,setUser]=useState(null),[error,setError]=useState(''),[params]=useSearchParams(); useEffect(()=>{api.get('/users/me').then(setUser).catch(e=>setError(e.message))},[]); return <div><PageTitle title="마이페이지"/><ErrorBox message={error}/>{user&&<ProfileForm user={user} onSaved={setUser}/>}<BusinessBox/>{params.get('ideaId')&&<Operations ideaId={params.get('ideaId')}/>}<Notifications/></div>}
function ProfileForm({user,onSaved}){const[form,setForm]=useState({nickname:user.nickname||'',intro:user.intro||'',portfolioUrl:user.portfolioUrl||''}),[error,setError]=useState(''); return <Card><h2>프로필</h2><ErrorBox message={error}/><Field label="닉네임"><TextInput value={form.nickname} onChange={e=>setForm({...form,nickname:e.target.value})}/></Field><Field label="소개"><TextArea value={form.intro} onChange={e=>setForm({...form,intro:e.target.value})}/></Field><Field label="포트폴리오"><TextInput value={form.portfolioUrl} onChange={e=>setForm({...form,portfolioUrl:e.target.value})}/></Field><Button onClick={async()=>{try{onSaved(await api.patch('/users/me',form))}catch(e){setError(e.message)}}}>저장</Button></Card>}
function BusinessBox(){const[form,setForm]=useState({businessNumber:'',representativeName:'',openDate:''}),[biz,setBiz]=useState(null),[error,setError]=useState(''); return <Card><h2>사업자 인증</h2><ErrorBox message={error}/><Field label="사업자등록번호"><TextInput value={form.businessNumber} onChange={e=>setForm({...form,businessNumber:e.target.value})}/></Field><Field label="대표자명"><TextInput value={form.representativeName} onChange={e=>setForm({...form,representativeName:e.target.value})}/></Field><Field label="개업일자"><TextInput placeholder="YYYYMMDD" value={form.openDate} onChange={e=>setForm({...form,openDate:e.target.value})}/></Field><Button onClick={async()=>{try{setBiz(await api.post('/users/me/business',form))}catch(e){setError(e.message)}}}>인증 등록</Button>{biz&&<div className="success">인증 상태: {String(biz.verified)}</div>}</Card>}
function Operations({ideaId}){const[usage,setUsage]=useState({itemName:'',amount:'',usedAt:''}),[settlements,setSettlements]=useState([]),[error,setError]=useState('');return <Card><h2>자금 사용·정산</h2><ErrorBox message={error}/><Field label="항목"><TextInput value={usage.itemName} onChange={e=>setUsage({...usage,itemName:e.target.value})}/></Field><Field label="금액"><TextInput type="number" value={usage.amount} onChange={e=>setUsage({...usage,amount:e.target.value})}/></Field><Field label="사용일"><TextInput type="date" value={usage.usedAt} onChange={e=>setUsage({...usage,usedAt:e.target.value})}/></Field><Button onClick={async()=>{try{await api.post(`/fund-usages/${ideaId}`,{...usage,amount:Number(usage.amount)})}catch(e){setError(e.message)}}}>사용 내역 등록</Button><Button onClick={async()=>{try{setSettlements(await api.get(`/settlements/ideas/${ideaId}`))}catch(e){setError(e.message)}}}>정산 조회</Button>{settlements.map(s=><p key={s.settlementId}>{s.type} · {s.payoutAmount?.toLocaleString()}원 · {s.status}</p>)}</Card>}
function Notifications(){const[items,setItems]=useState([]);useEffect(()=>{api.get('/notifications').then(d=>setItems(d.content||[])).catch(()=>{})},[]);return <Card><h2>알림</h2>{items.map(n=><p key={n.id}><b>{n.title}</b> {n.message}</p>)}{!items.length&&<Empty/>}</Card>}
function ExpertHome(){return <div><PageTitle title="전문가 워크스페이스"/><ExpertProfile/><ExpertVerify/></div>}
function ExpertProfile(){const[form,setForm]=useState({techStack:'WEB_DEVELOPMENT',portfolioUrl:'',career:''}),[res,setRes]=useState(null),[error,setError]=useState('');return <Card><h2>전문가 프로필</h2><ErrorBox message={error}/><Field label="기술 분야"><select value={form.techStack} onChange={e=>setForm({...form,techStack:e.target.value})}>{TECH_STACKS.map(x=><option key={x}>{x}</option>)}</select></Field><Field label="포트폴리오"><TextInput value={form.portfolioUrl} onChange={e=>setForm({...form,portfolioUrl:e.target.value})}/></Field><Field label="경력"><TextArea value={form.career} onChange={e=>setForm({...form,career:e.target.value})}/></Field><Button onClick={async()=>{try{setRes(await api.post('/experts/profile',form))}catch(e){setError(e.message)}}}>프로필 등록</Button>{res&&<div className="success">프로필 번호 {res.expertProfileId}</div>}</Card>}
function ExpertVerify(){const[form,setForm]=useState({qualificationType:'BUSINESS_REGISTRATION',qualificationNumber:'',startDate:'',representativeName:'',fileUrl:''}),[res,setRes]=useState(null),[error,setError]=useState('');return <Card><h2>자격 검증</h2><ErrorBox message={error}/><Field label="자격 유형"><select value={form.qualificationType} onChange={e=>setForm({...form,qualificationType:e.target.value})}>{QUALIFICATIONS.map(x=><option key={x}>{x}</option>)}</select></Field>{Object.keys(form).filter(k=>k!=='qualificationType').map(k=><Field key={k} label={k}><TextInput value={form[k]} onChange={e=>setForm({...form,[k]:e.target.value})}/></Field>)}<Button onClick={async()=>{try{setRes(await api.post('/experts/verify',form))}catch(e){setError(e.message)}}}>검증 요청</Button>{res&&<div className="success">검증 상태 {res.status}</div>}</Card>}
function Matches(){const[items,setItems]=useState([]),[error,setError]=useState('');useEffect(()=>{api.get('/matches').then(setItems).catch(e=>setError(e.message))},[]);return <div><PageTitle title="매칭 요청"/><ErrorBox message={error}/>{items.map(m=><MatchCard key={m.matchId} match={m}/>) }{!items.length&&!error&&<Empty/>}</div>}
function MatchCard({match}){const[status,setStatus]=useState(match.status),[rejectReason,setRejectReason]=useState(''),[review,setReview]=useState({feasibility:'POSSIBLE',expectedPeriod:'',techStack:'',riskFactor:'',opinion:''}),[error,setError]=useState('');return <Card><h3>아이디어 #{match.ideaId}</h3><p>상태 {status}</p><ErrorBox message={error}/><div className="actions"><Button onClick={async()=>{try{setStatus((await api.patch(`/matches/${match.matchId}`,{status:'ACCEPTED'})).status)}catch(e){setError(e.message)}}}>수락</Button><TextInput placeholder="거절 사유" value={rejectReason} onChange={e=>setRejectReason(e.target.value)}/><Button onClick={async()=>{try{setStatus((await api.patch(`/matches/${match.matchId}`,{status:'REJECTED',rejectReason})).status)}catch(e){setError(e.message)}}}>거절</Button></div><h4>검토서 작성</h4><Field label="구현 가능성"><select value={review.feasibility} onChange={e=>setReview({...review,feasibility:e.target.value})}><option>POSSIBLE</option><option>IMPOSSIBLE</option></select></Field>{['expectedPeriod','techStack','riskFactor','opinion'].map(k=><Field key={k} label={k}><TextArea value={review[k]} onChange={e=>setReview({...review,[k]:e.target.value})}/></Field>)}<Button variant="primary" onClick={async()=>{try{await api.post(`/matches/${match.matchId}/review`,review)}catch(e){setError(e.message)}}}>검토서 제출</Button></Card>}
function AdminPage(){const[id,setId]=useState(''),[pre,setPre]=useState(''),[error,setError]=useState('');return <div><PageTitle title="관리자 페이지"/><Card><h2>마일스톤 보고서 처리</h2><ErrorBox message={error}/><Field label="마일스톤 번호"><TextInput type="number" value={id} onChange={e=>setId(e.target.value)}/></Field><Button onClick={async()=>{try{await api.post(`/milestones/${id}/reports/approve`)}catch(e){setError(e.message)}}}>승인</Button><Button onClick={async()=>{try{await api.post(`/milestones/${id}/reports/reject`)}catch(e){setError(e.message)}}}>반려</Button></Card><Card><h2>선정산 지급 처리</h2><Field label="선정산 번호"><TextInput type="number" value={pre} onChange={e=>setPre(e.target.value)}/></Field><Button onClick={async()=>{try{await api.patch(`/pre-settlements/${pre}/complete`)}catch(e){setError(e.message)}}}>지급 완료</Button><Button onClick={async()=>{try{await api.patch(`/pre-settlements/${pre}/fail`)}catch(e){setError(e.message)}}}>지급 실패</Button></Card></div>}

createRoot(document.getElementById('root')).render(<BrowserRouter><AuthProvider><AppShell/></AuthProvider></BrowserRouter>);
