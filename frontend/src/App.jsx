import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

const NAV = [
  { id: 'overview', label: 'Overview', icon: '◫' },
  { id: 'rooms', label: 'Rooms', icon: '▦' },
  { id: 'teachers', label: 'Teachers', icon: '♙' },
  { id: 'timetable', label: 'Timetable', icon: '▤' },
  { id: 'messages', label: 'Messages', icon: '⌁' },
]

function authHeader(credentials) {
  return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`
}

async function api(path, credentials, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      Authorization: authHeader(credentials),
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })
  if (!response.ok) {
    const detail = await response.text()
    let message = detail || `Request failed (${response.status})`
    try {
      const parsed = JSON.parse(detail)
      message = parsed.detail || parsed.message || message
    } catch { /* backend may return plain text */ }
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

function localDate(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatTime(value) {
  if (!value) return '—'
  const [hour, minute] = value.split(':').map(Number)
  return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(2000, 0, 1, hour, minute))
}

function prettyDate(value) {
  return new Intl.DateTimeFormat(undefined, { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date(`${value}T12:00:00`))
}

function App() {
  const [credentials, setCredentials] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem('dt-credentials') || 'null') } catch { return null }
  })
  const [page, setPage] = useState('overview')
  const [rooms, setRooms] = useState([])
  const [teachers, setTeachers] = useState([])
  const [todaySchedule, setTodaySchedule] = useState(null)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)

  const refresh = useCallback(async () => {
    if (!credentials) return
    setLoading(true)
    setLoadError('')
    const [roomResult, teacherResult, scheduleResult] = await Promise.allSettled([
      api('/api/rooms/current', credentials),
      api('/api/teachers', credentials),
      api(`/api/timetable/on-date?date=${localDate()}`, credentials),
    ])
    if (roomResult.status === 'fulfilled') setRooms(roomResult.value)
    if (teacherResult.status === 'fulfilled') setTeachers(teacherResult.value)
    if (scheduleResult.status === 'fulfilled') setTodaySchedule(scheduleResult.value)
    const rejected = [roomResult, teacherResult, scheduleResult].find((result) => result.status === 'rejected')
    if (rejected) setLoadError(rejected.reason.message)
    else setLastUpdated(new Date())
    setLoading(false)
  }, [credentials])

  useEffect(() => {
    const timer = window.setTimeout(refresh, 0)
    return () => window.clearTimeout(timer)
  }, [refresh])

  function signOut() {
    sessionStorage.removeItem('dt-credentials')
    setCredentials(null)
    setRooms([])
    setTeachers([])
    setTodaySchedule(null)
  }

  if (!credentials) return <Login onLogin={(value) => {
    sessionStorage.setItem('dt-credentials', JSON.stringify(value))
    setCredentials(value)
  }} />

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#overview" onClick={() => setPage('overview')}>
          <span className="brand-mark">d<span>t</span></span>
          <span className="brand-copy"><strong>campus twin</strong><small>BUILDING, IN SYNC</small></span>
        </a>
        <div className="sidebar-label">WORKSPACE</div>
        <nav className="main-nav" aria-label="Main navigation">
          {NAV.map((item) => (
            <button key={item.id} className={`nav-item ${page === item.id ? 'active' : ''}`} onClick={() => setPage(item.id)}>
              <span className="nav-icon">{item.icon}</span><span>{item.label}</span>
              {item.id === 'messages' && <span className="nav-live-dot" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="building-card"><span className="building-light" /><div><strong>DT Building</strong><small>Live campus data</small></div></div>
          <button className="user-card" onClick={signOut} title="Sign out">
            <span className="avatar">{credentials.username.slice(0, 1).toUpperCase()}</span>
            <span className="user-meta"><strong>{credentials.username}</strong><small>Connected account</small></span>
            <span className="sign-out">↗</span>
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Digital Twin</span><i>/</i><strong>{NAV.find((item) => item.id === page)?.label}</strong></div>
          <div className="topbar-actions">
            <span className="connection"><i />{loadError ? "API needs attention" : lastUpdated ? "API connected" : "Connecting"}</span>
            <button className="icon-button refresh-button" onClick={refresh} disabled={loading} aria-label="Refresh data">{loading ? '…' : '↻'}</button>
            <span className="top-avatar">{credentials.username.slice(0, 1).toUpperCase()}</span>
          </div>
        </header>

        <div className="page-content">
          {loadError && <div className="error-banner"><span>!</span><div><strong>Some building data could not load</strong><small>{loadError}</small></div><button onClick={refresh}>Retry</button></div>}
          {page === 'overview' && <Overview rooms={rooms} teachers={teachers} schedule={todaySchedule} lastUpdated={lastUpdated} loading={loading} onNavigate={setPage} />}
          {page === 'rooms' && <RoomsPage rooms={rooms} loading={loading} />}
          {page === 'teachers' && <TeachersPage teachers={teachers} credentials={credentials} />}
          {page === 'timetable' && <TimetablePage credentials={credentials} />}
          {page === 'messages' && <MessagesPage credentials={credentials} />}
        </div>
      </main>

      <nav className="mobile-nav" aria-label="Mobile navigation">
        {NAV.map((item) => <button key={item.id} className={page === item.id ? 'active' : ''} onClick={() => setPage(item.id)}><span>{item.icon}</span><small>{item.label}</small></button>)}
      </nav>
    </div>
  )
}

function Login({ onLogin }) {
  const [username, setUsername] = useState('dev')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    const credentials = { username, password }
    try {
      await api('/api/rooms/current', credentials)
      onLogin(credentials)
    } catch (problem) {
      setError(problem.message.includes('401') ? 'That username or password was not accepted.' : problem.message)
    } finally { setBusy(false) }
  }

  return <main className="login-page">
    <div className="login-panel">
      <a className="brand login-brand" href="#"><span className="brand-mark">d<span>t</span></span><span className="brand-copy"><strong>campus twin</strong><small>BUILDING, IN SYNC</small></span></a>
      <div className="login-art"><span className="art-sun"/><span className="art-building"><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/></span><span className="art-ground"/></div>
      <div className="login-copy"><span className="eyebrow">YOUR CAMPUS, CONNECTED</span><h1>Find your place<br/>on campus.</h1><p>Rooms, classes, teachers, and campus conversations in one place.</p></div>
      <form className="login-form" onSubmit={submit}>
        <label>Username<input autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} required /></label>
        <label>Password<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Enter your backend password" required /></label>
        {error && <div className="form-error">{error}</div>}
        <button className="primary-button" disabled={busy}>{busy ? 'Connecting…' : 'Connect to campus'} <span>→</span></button>
        <p className="login-hint">Uses the current backend development account.</p>
      </form>
    </div>
    <footer className="login-footer">A clearer way to move through your day.</footer>
  </main>
}

function Overview({ rooms, teachers, schedule, lastUpdated, loading, onNavigate }) {
  const classes = schedule?.classes || []
  const scheduled = classes.filter((item) => item.status === 'SCHEDULED')
  const occupied = rooms.filter((room) => room.status === 'OCCUPIED').length
  const upcoming = scheduled.slice(0, 5)
  return <>
    <div className="page-heading heading-row">
      <div><span className="eyebrow">{prettyDate(localDate())}</span><h1>Good day. <span className="heading-serif">Here’s your campus.</span></h1><p>One clear view of what’s happening across the building.</p></div>
      <div className="updated-label"><span className="live-pulse"/> LIVE <span className="updated-time">{lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}` : 'Syncing'}</span></div>
    </div>
    <section className="stats-grid">
      <StatCard label="Rooms in building" value={rooms.length} detail="Across all floors" icon="▦" tone="blue" />
      <StatCard label="Free by timetable" value={rooms.length - occupied} detail="No class scheduled now" icon="◌" tone="mint" />
      <StatCard label="Classes today" value={scheduled.length} detail={`${classes.length - scheduled.length} cancelled`} icon="▤" tone="peach" />
      <StatCard label="Teachers" value={teachers.length} detail="In the directory" icon="♙" tone="lilac" />
    </section>
    <div className="overview-grid">
      <section className="surface room-surface">
        <div className="section-title"><div><span className="eyebrow">RIGHT NOW</span><h2>Room availability</h2></div><button className="text-link" onClick={() => onNavigate('rooms')}>All rooms <span>→</span></button></div>
        <div className="room-legend"><span><i className="legend-dot green"/>No class scheduled</span><span><i className="legend-dot navy"/>In use</span></div>
        {rooms.length === 0 ? <EmptyState loading={loading} title="No rooms yet" text="Add the building’s rooms to see their status here."/> : <div className="room-list">{rooms.slice(0, 6).map((room) => <RoomRow key={room.roomNumber} room={room}/>)}</div>}
      </section>
      <section className="surface schedule-surface">
        <div className="section-title"><div><span className="eyebrow">TODAY · {schedule?.activeWeek || '—'}</span><h2>Class schedule</h2></div><button className="text-link" onClick={() => onNavigate('timetable')}>Full timetable <span>→</span></button></div>
        {upcoming.length === 0 ? <EmptyState loading={loading} title="A little breathing room" text="No scheduled classes for today."/> : <div className="timeline-list">{upcoming.map((item) => <ScheduleRow key={`${item.timetableEntryId}-${item.startTime}`} item={item}/>)}</div>}
      </section>
    </div>
    <section className="bottom-callout"><span className="callout-symbol">✳</span><div><strong>Looking for a teacher?</strong><p>Check their current class and next scheduled room.</p></div><button className="secondary-button" onClick={() => onNavigate('teachers')}>Find a teacher <span>→</span></button></section>
    <div className="data-note">Room availability is calculated from the timetable and confirmed bookings.</div>
  </>
}

function StatCard({ label, value, detail, icon, tone }) {
  return <article className="stat-card"><div className={`stat-icon ${tone}`}>{icon}</div><div className="stat-label">{label}</div><div className="stat-value">{value}</div><div className="stat-detail">{detail}</div></article>
}

function RoomRow({ room }) {
  const busy = room.status === 'OCCUPIED'
  return <div className="room-row"><div className={`room-symbol ${busy ? 'busy' : 'free'}`}>▦</div><div className="room-main"><strong>{room.roomNumber}</strong><span>Floor {room.floor} <i>·</i> {room.capacity} seats</span></div><div className={`room-state ${busy ? 'busy' : ''}`}><i/>{busy ? `In use · ${room.currentClass?.subjectCode || 'Class'}` : 'No class scheduled'}</div></div>
}

function ScheduleRow({ item }) {
  const cancelled = item.status === 'CANCELLED'
  return <div className={`timeline-row ${cancelled ? 'cancelled' : ''}`}><div className="timeline-time"><strong>{formatTime(item.startTime)}</strong><span>{formatTime(item.endTime)}</span></div><div className="timeline-track"><i/></div><div className="timeline-info"><strong>{item.subjectCode} <span>{item.subjectName}</span></strong><small>{item.sectionCode} <i>·</i> {item.roomNumber}</small></div><span className={`class-status ${cancelled ? 'is-cancelled' : ''}`}>{cancelled ? 'Cancelled' : 'Scheduled'}</span></div>
}

function EmptyState({ loading, title, text }) {
  return <div className="empty-state"><span>{loading ? '◌' : '✳'}</span><strong>{loading ? 'Loading campus data…' : title}</strong><p>{loading ? 'This should only take a moment.' : text}</p></div>
}

function RoomsPage({ rooms, loading }) {
  const [query, setQuery] = useState('')
  const filtered = rooms.filter((room) => room.roomNumber.toLowerCase().includes(query.toLowerCase()))
  return <>
    <div className="page-heading"><span className="eyebrow">THE BUILDING</span><h1>Rooms <span className="heading-serif">at a glance.</span></h1><p>See what’s in use and what has no class scheduled right now.</p></div>
    <div className="toolbar"><label className="search-box"><span>⌕</span><input placeholder="Find a room by number" value={query} onChange={(event) => setQuery(event.target.value)}/></label><span className="result-count">{filtered.length} rooms</span></div>
    {filtered.length ? <div className="rooms-grid">{filtered.map((room) => <article className="room-card" key={room.roomNumber}><div className="room-card-top"><span className={`room-card-icon ${room.status === 'OCCUPIED' ? 'busy' : ''}`}>▦</span><span className={`availability-tag ${room.status === 'OCCUPIED' ? 'occupied' : ''}`}><i/>{room.status === 'OCCUPIED' ? 'In use' : 'No class scheduled'}</span></div><h2>{room.roomNumber}</h2><p>{room.roomType} <span>·</span> floor {room.floor}</p><div className="room-card-bottom"><span>◉ &nbsp;{room.capacity} seats</span>{room.currentClass && <span>{room.currentClass.subjectCode}</span>}</div></article>)}</div> : <EmptyState loading={loading} title="No matching rooms" text="Try a different room number."/>}
    <div className="data-note">“No class scheduled” is based on timetable data; it does not confirm physical occupancy.</div>
  </>
}

function TeachersPage({ teachers, credentials }) {
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(null)
  const [status, setStatus] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const filtered = teachers.filter((teacher) => `${teacher.fullName} ${teacher.staffCode}`.toLowerCase().includes(query.toLowerCase()))
  async function checkTeacher(teacher) {
    setSelected(teacher)
    setBusy(true)
    setError('')
    setStatus(null)
    try { setStatus(await api(`/api/teachers/${encodeURIComponent(teacher.staffCode)}/current-location`, credentials)) }
    catch (problem) { setError(problem.message) }
    finally { setBusy(false) }
  }
  return <>
    <div className="page-heading"><span className="eyebrow">PEOPLE IN THE BUILDING</span><h1>Find your <span className="heading-serif">teacher.</span></h1><p>Check their scheduled class and see where to find them next.</p></div>
    <div className="teacher-layout">
      <section className="surface teacher-directory"><label className="search-box"><span>⌕</span><input placeholder="Search a name or staff code" value={query} onChange={(event) => setQuery(event.target.value)}/></label><div className="directory-label">TEACHER DIRECTORY <span>{filtered.length}</span></div>
        <div className="teacher-list">{filtered.map((teacher, index) => <button key={teacher.staffCode} className={`teacher-option ${selected?.staffCode === teacher.staffCode ? 'selected' : ''}`} onClick={() => checkTeacher(teacher)}><span className={`teacher-avatar av-${index % 5}`}>{teacher.fullName.split(' ').filter(Boolean).slice(-1)[0]?.[0]}</span><span className="teacher-option-copy"><strong>{teacher.fullName}</strong><small>{teacher.staffCode}</small></span><span className="teacher-chevron">›</span></button>)}{teachers.length === 0 && <EmptyState title="No teachers added" text="The teacher directory is empty."/>}</div>
      </section>
      <section className="surface teacher-detail">{!selected ? <div className="teacher-empty"><span className="large-avatar">♙</span><strong>Select a teacher</strong><p>Choose someone from the directory to check their schedule.</p></div> : <><div className="teacher-profile"><span className="large-avatar">{selected.fullName.split(' ').filter(Boolean).slice(-1)[0]?.[0]}</span><div><span className="eyebrow">TEACHER</span><h2>{selected.fullName}</h2><p>{selected.staffCode}</p></div></div>{busy ? <EmptyState loading title="Checking schedule" text=""/> : error ? <div className="form-error">{error}</div> : status && <><div className={`location-card ${status.status === 'IN_CLASS' ? 'in-class' : ''}`}><span className="location-marker">{status.status === 'IN_CLASS' ? '↗' : '◌'}</span><div><span className="eyebrow">CURRENT STATUS</span><strong>{status.status === 'IN_CLASS' ? `Teaching in ${status.currentClass.roomNumber}` : 'No class scheduled now'}</strong><p>{status.currentClass ? `${status.currentClass.subjectCode} · ${status.currentClass.sectionCode} · until ${formatTime(status.currentClass.endTime)}` : 'Their timetable has no active class right now.'}</p></div></div>{status.nextClass && <div className="next-class"><span className="eyebrow">UP NEXT · {formatTime(status.nextClass.startTime)}</span><strong>{status.nextClass.subjectCode} <span>in {status.nextClass.roomNumber}</span></strong><p>{status.nextClass.sectionCode} · until {formatTime(status.nextClass.endTime)}</p></div>}<div className="data-note">Schedule-based location; it does not confirm physical presence.</div></>}</>}</section>
    </div>
  </>
}

function TimetablePage({ credentials }) {
  const [date, setDate] = useState(localDate())
  const [schedule, setSchedule] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    setBusy(true)
    setError('')
    try { setSchedule(await api(`/api/timetable/on-date?date=${date}`, credentials)) }
    catch (problem) { setError(problem.message); setSchedule(null) }
    finally { setBusy(false) }
  }, [date, credentials])
  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])
  const classes = schedule?.classes || []
  return <>
    <div className="page-heading"><span className="eyebrow">THE WEEK, MADE CLEAR</span><h1>Class <span className="heading-serif">timetable.</span></h1><p>Browse the building’s schedule for a date.</p></div>
    <div className="toolbar timetable-toolbar"><label className="date-control"><span>DATE</span><input type="date" value={date} onChange={(event) => setDate(event.target.value)}/></label>{schedule && <span className="week-pill">{schedule.activeWeek}</span>}</div>
    {error && <div className="form-error">{error}</div>}
    <section className="surface timetable-surface"><div className="timetable-date"><div><span className="eyebrow">SCHEDULE FOR</span><h2>{prettyDate(date)}</h2></div><span className="result-count">{classes.length} classes</span></div>{busy ? <EmptyState loading title="Loading timetable" text=""/> : classes.length ? <div className="timeline-list">{classes.map((item) => <ScheduleRow key={`${item.timetableEntryId}-${item.startTime}`} item={item}/>)}</div> : <EmptyState title="No classes scheduled" text="There are no timetable entries for this day."/>}</section>
  </>
}

function MessagesPage({ credentials }) {
  const [audience, setAudience] = useState('GLOBAL')
  const [category, setCategory] = useState('GENERAL')
  const [senderType, setSenderType] = useState('STUDENT')
  const [senderCode, setSenderCode] = useState('')
  const [recipientType, setRecipientType] = useState('TEACHER')
  const [recipientCode, setRecipientCode] = useState('')
  const [sectionCode, setSectionCode] = useState('')
  const [body, setBody] = useState('')
  const [feed, setFeed] = useState([])
  const [busy, setBusy] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [sent, setSent] = useState('')

  const feedPath = useMemo(() => {
    if (audience === 'GLOBAL') return '/api/messages/global'
    if (audience === 'SECTION') return sectionCode ? `/api/messages/section/${encodeURIComponent(sectionCode)}` : null
    if (!senderCode || !recipientCode) return null
    return `/api/messages/direct/${senderType}/${encodeURIComponent(senderCode)}/${recipientType}/${encodeURIComponent(recipientCode)}`
  }, [audience, sectionCode, senderCode, recipientCode, senderType, recipientType])
  const streamPath = useMemo(() => feedPath ? feedPath.replace('/api/messages/', '/api/messages/stream/') : null, [feedPath])

  const loadFeed = useCallback(async () => {
    if (!feedPath) { setFeed([]); return }
    setBusy(true)
    setError('')
    try { setFeed(await api(feedPath, credentials)) }
    catch (problem) { setError(problem.message); setFeed([]) }
    finally { setBusy(false) }
  }, [feedPath, credentials])
  useEffect(() => {
    const timer = window.setTimeout(loadFeed, 0)
    return () => window.clearTimeout(timer)
  }, [loadFeed])

  useEffect(() => {
    if (!streamPath) return undefined
    const controller = new AbortController()
    async function listen() {
      try {
        const response = await fetch(streamPath, { headers: { Authorization: authHeader(credentials) }, signal: controller.signal })
        if (!response.ok || !response.body) return
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        while (true) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const events = buffer.split('\n\n')
          buffer = events.pop() || ''
          for (const event of events) {
            const data = event.split('\n').filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).join('\n')
            if (!data) continue
            try {
              const message = JSON.parse(data)
              setFeed((current) => current.some((item) => item.id === message.id) ? current : [...current, message].slice(-100))
            } catch { /* initial connected event carries a channel name */ }
          }
        }
      } catch (problem) {
        if (problem.name !== 'AbortError') console.info('Live message stream disconnected')
      }
    }
    listen()
    return () => controller.abort()
  }, [streamPath, credentials])

  async function sendMessage(event) {
    event.preventDefault()
    setSending(true)
    setError('')
    setSent('')
    const request = {
      senderType,
      senderCode,
      audience,
      recipientType: audience === 'SECTION' ? 'SECTION' : audience === 'DIRECT' ? recipientType : null,
      recipientCode: audience === 'GLOBAL' ? null : audience === 'SECTION' ? sectionCode : recipientCode,
      category,
      body,
    }
    try {
      await api('/api/messages', credentials, { method: 'POST', body: JSON.stringify(request) })
      setSent('Message sent to the campus chat.')
      setBody('')
      await loadFeed()
    } catch (problem) { setError(problem.message) }
    finally { setSending(false) }
  }

  return <>
    <div className="page-heading"><span className="eyebrow">CAMPUS CONVERSATIONS</span><h1>Messages <span className="heading-serif">that find their people.</span></h1><p>Talk to one person, share with a section, or post to the whole building.</p></div>
    <div className="message-layout">
      <section className="surface chat-panel">
        <div className="chat-header"><div><span className="eyebrow">{audience === 'GLOBAL' ? 'BUILDING-WIDE' : audience === 'SECTION' ? 'SECTION CHAT' : 'DIRECT MESSAGE'}</span><h2>{audience === 'GLOBAL' ? 'Everyone at DT' : audience === 'SECTION' ? (sectionCode || 'Choose a section') : `${senderCode || 'You'} ↔ ${recipientCode || 'Choose a recipient'}`}</h2></div><span className="chat-live"><i/> Live</span></div>
        <div className="audience-tabs"><button className={audience === 'GLOBAL' ? 'active' : ''} onClick={() => setAudience('GLOBAL')}>@ All</button><button className={audience === 'SECTION' ? 'active' : ''} onClick={() => setAudience('SECTION')}>Section</button><button className={audience === 'DIRECT' ? 'active' : ''} onClick={() => setAudience('DIRECT')}>Direct</button></div>
        <div className="message-list">{error && <div className="form-error">{error}</div>}{busy ? <EmptyState loading title="Loading messages" text=""/> : feed.length === 0 ? <EmptyState title="Start a conversation" text="Messages appear here as soon as someone posts."/> : feed.map((message) => <MessageBubble key={message.id} message={message} />)}</div>
        {sent && <div className="success-note">✓ {sent}</div>}
        <form className="composer" onSubmit={sendMessage}>
          <div className="composer-fields"><label>Sending as<select value={senderType} onChange={(event) => setSenderType(event.target.value)}><option value="STUDENT">Student</option><option value="TEACHER">Teacher</option></select></label><label>Your code<input value={senderCode} onChange={(event) => setSenderCode(event.target.value)} placeholder="e.g. A7_44" required/></label>
            {audience === 'SECTION' && <label>Section code<input value={sectionCode} onChange={(event) => setSectionCode(event.target.value)} placeholder="e.g. A7" required/></label>}
            {audience === 'DIRECT' && <><label>Recipient type<select value={recipientType} onChange={(event) => setRecipientType(event.target.value)}><option value="TEACHER">Teacher</option><option value="STUDENT">Student</option></select></label><label>Recipient code<input value={recipientCode} onChange={(event) => setRecipientCode(event.target.value)} placeholder="Staff or student code" required/></label></>}
            <label>Category<select value={category} onChange={(event) => setCategory(event.target.value)}><option value="GENERAL">General</option><option value="ANNOUNCEMENT">Announcement</option><option value="QUESTION">Question</option><option value="LOST_FOUND">Lost & found</option></select></label>
          </div>
          <div className="composer-input"><textarea value={body} onChange={(event) => setBody(event.target.value)} maxLength={2000} placeholder={audience === 'GLOBAL' ? 'Share something with the whole building…' : 'Write a message…'} required/><button className="send-button" disabled={sending || !senderCode.trim()} aria-label="Send message">{sending ? '…' : '↑'}</button></div>
          <div className="composer-hint">Messages reach {audience === 'GLOBAL' ? 'everyone in the building' : audience === 'SECTION' ? 'everyone in this section chat' : 'both people in this direct conversation'}.</div>
        </form>
      </section>
      <aside className="message-aside"><div className="message-aside-icon">✳</div><span className="eyebrow">A MORE CONNECTED CAMPUS</span><h3>One note.<br/><em>Everyone who needs it.</em></h3><p>Use section chats for class updates, direct messages for one-to-one conversations, and the global feed for campus-wide notices.</p><div className="message-tip"><strong>Lost something?</strong><span>Choose “Lost & found” and send it to @ All.</span></div></aside>
    </div>
  </>
}

function MessageBubble({ message }) {
  const initials = message.senderCode?.split(/[_\s-]/).filter(Boolean).slice(-1)[0]?.slice(0, 1).toUpperCase() || '?'
  return <article className="message-item"><span className={`message-avatar ${message.senderType === 'TEACHER' ? 'teacher' : ''}`}>{initials}</span><div className="message-content"><div className="message-meta"><strong>{message.senderCode}</strong><span>{message.senderType?.toLowerCase()}</span><time>{new Date(message.createdAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</time></div><p>{message.body}</p><span className={`category-chip ${message.category?.toLowerCase()}`}>{message.category?.replace('_', ' ')}</span></div></article>
}

export default App
