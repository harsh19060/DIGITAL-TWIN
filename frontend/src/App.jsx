import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import './App.css'
import BranchedMenu from './components/BranchedMenu'
import FuseButton from './components/FuseButton'

const NAV = [
  { id: 'overview', label: 'Overview', icon: '◫' },
  { id: 'rooms', label: 'Rooms', icon: '▦' },
  { id: 'teachers', label: 'Teachers', icon: '♙' },
  { id: 'timetable', label: 'Timetable', icon: '▤' },
  { id: 'messages', label: 'Messages', icon: '⌁' },
]

const MENU_ITEMS = [
  {
    label: 'Campus',
    children: [
      { value: 'overview', label: 'Overview', icon: '◫' },
      { value: 'rooms', label: 'Rooms', icon: '▦' },
    ],
  },
  {
    label: 'Academics',
    children: [
      { value: 'teachers', label: 'Teachers', icon: '♙' },
      { value: 'timetable', label: 'Timetable', icon: '▤' },
    ],
  },
  {
    label: 'Community',
    children: [
      { value: 'messages', label: 'Messages', icon: '⌁' },
    ],
  },
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
      message = parsed.detail || parsed.message || parsed.error || message
    } catch { /* backend may return plain text */ }
    const problem = new Error(message)
    problem.status = response.status
    throw problem
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
  const [identity, setIdentity] = useState(null)
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('campus-twin-theme') === 'dark')
  const [rooms, setRooms] = useState([])
  const [teachers, setTeachers] = useState([])
  const [todaySchedule, setTodaySchedule] = useState(null)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)

  const refresh = useCallback(async () => {
    if (!credentials || !identity || identity.role === 'ADMIN') return
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
  }, [credentials, identity])

  useEffect(() => {
    if (!credentials || identity) return undefined
    let active = true
    api('/api/session', credentials).then((account) => {
      if (active) {
        setIdentity(account)
        if (account.role === 'ADMIN') setPage('timetable')
      }
    }).catch(() => {
      if (active) {
        sessionStorage.removeItem('dt-credentials')
        setIdentity(null)
        setIdentity(null)
    setCredentials(null)
      }
    })
    return () => { active = false }
  }, [credentials, identity])


  useEffect(() => {
    const timer = window.setTimeout(refresh, 0)
    return () => window.clearTimeout(timer)
  }, [refresh])

  function toggleTheme() {
    const next = !darkMode
    localStorage.setItem('campus-twin-theme', next ? 'dark' : 'light')
    setDarkMode(next)
  }

  function signOut() {
    sessionStorage.removeItem('dt-credentials')
    setIdentity(null)
    setCredentials(null)
    setRooms([])
    setTeachers([])
    setTodaySchedule(null)
  }

  if (!credentials) return <Login onLogin={(value, account) => {
    sessionStorage.setItem('dt-credentials', JSON.stringify(value))
    setCredentials(value)
    setIdentity(account)
    setPage(account.role === 'ADMIN' ? 'timetable' : 'overview')
  }} />
  if (!identity) return <main className="login-page"><div className="login-panel"><p className="eyebrow">CONNECTING TO CAMPUS ACCOUNT…</p></div></main>

  const activePage = identity.role === 'ADMIN' ? 'timetable' : page
  const visibleNav = identity.role === 'ADMIN' ? NAV.filter((item) => item.id === 'timetable') : NAV
  const visibleMenuItems = MENU_ITEMS.map((group) => ({ ...group, children: group.children.filter((item) => visibleNav.some((nav) => nav.id === item.value)) })).filter((group) => group.children.length > 0)

  return (
    <div className={`app-shell ${darkMode ? 'dark-mode' : ''}`}>
      <aside className="sidebar">
        <a className="brand" href="#overview" onClick={() => setPage(identity.role === 'ADMIN' ? 'timetable' : 'overview')}>
          <span className="brand-mark">d<span>t</span></span>
          <span className="brand-copy"><strong>campus twin</strong><small>BUILDING, IN SYNC</small></span>
        </a>
        <div className="sidebar-label">WORKSPACE</div>
        <BranchedMenu
          className="campus-branched-menu"
          items={visibleMenuItems}
          activeValue={activePage}
          defaultOpen={[0]}
          onSelect={setPage}
          color="#b7c3c6"
          accentColor="#b9da88"
          lineColor="rgba(255, 255, 255, .16)"
          width={206}
          rowHeight={34}
          indent={36}
          trunk={10}
          radius={9}
          lineWidth={1.4}
          fontSize={12}
          drawDuration={350}
          foldDuration={250}
        />
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
          <div className="breadcrumb"><span>Digital Twin</span><i>/</i><strong>{NAV.find((item) => item.id === activePage)?.label}</strong></div>
          <div className="topbar-actions">
            <span className="connection"><i />{loadError ? "API needs attention" : lastUpdated ? "API connected" : "Connecting"}</span>
            <button className="icon-button theme-toggle" onClick={toggleTheme} aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'} aria-pressed={darkMode}>{darkMode ? '☀' : '☾'}</button>
            <button className="icon-button refresh-button" onClick={refresh} disabled={loading} aria-label="Refresh data">{loading ? '…' : '↻'}</button>
            <span className="top-avatar">{credentials.username.slice(0, 1).toUpperCase()}</span>
          </div>
        </header>

        <div className="page-content">
          {loadError && <div className="error-banner"><span>!</span><div><strong>Some building data could not load</strong><small>{loadError}</small></div><button onClick={refresh}>Retry</button></div>}
          {activePage === 'overview' && identity.role !== 'ADMIN' && <Overview rooms={rooms} teachers={teachers} schedule={todaySchedule} lastUpdated={lastUpdated} loading={loading} onNavigate={setPage} />}
          {activePage === 'rooms' && <RoomsPage rooms={rooms} loading={loading} />}
          {activePage === 'teachers' && <TeachersPage teachers={teachers} credentials={credentials} />}
          {activePage === 'timetable' && <TimetablePage credentials={credentials} adminOnly={identity.role === 'ADMIN'} />}
          {activePage === 'messages' && <MessagesPage credentials={credentials} identity={identity} />}
        </div>
      </main>

      <nav className="mobile-nav" aria-label="Mobile navigation">
        {visibleNav.map((item) => <button key={item.id} className={activePage === item.id ? 'active' : ''} onClick={() => setPage(item.id)}><span>{item.icon}</span><small>{item.label}</small></button>)}
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
    event?.preventDefault?.()
    setBusy(true)
    setError('')
    const credentials = { username, password }
    try {
      const account = await api('/api/session', credentials)
      onLogin(credentials, account)
    } catch (problem) {
      setError(problem.message.includes('401') ? 'That username or password was not accepted.' : problem.message)
    } finally { setBusy(false) }
  }

  return <main className="login-page">
    <div className="login-panel">
      <a className="brand login-brand" href="#"><span className="brand-mark">d<span>t</span></span><span className="brand-copy"><strong>campus twin</strong><small>BUILDING, IN SYNC</small></span></a>
      <div className="login-art"><span className="art-sun"/><span className="art-building"><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/></span><span className="art-ground"/></div>
      <div className="login-copy"><span className="eyebrow">YOUR CAMPUS, CONNECTED</span><h1>Find your place<br/>on campus.</h1><p>Rooms, classes, teachers, and campus conversations in one place.</p></div>
      <div className="demo-accounts" aria-label="Choose a local demo account">
        <span>LOCAL DEMO ACCOUNTS</span>
        <div>
          <button type="button" onClick={() => { setUsername('1234'); setPassword('1234') }}>Admin</button>
          <button type="button" onClick={() => { setUsername('student'); setPassword('student-only-change-me') }}>Student · A7_44</button>
          <button type="button" onClick={() => { setUsername('teacher'); setPassword('teacher-only-change-me') }}>Teacher · ALS</button>
        </div>
      </div>
      <form className="login-form" onSubmit={submit}>
        <label>Username<input autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} required /></label>
        <label>Password<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Enter your backend password" required /></label>
        {error && <div className="form-error">{error}</div>}
        <button className="primary-button" disabled={busy}>{busy ? 'Connecting…' : 'Connect to campus'} <span>→</span></button>
        <p className="login-hint">Choose a local demo account above. Change demo passwords in backend environment variables.</p>
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

function TimetablePage({ credentials, adminOnly = false }) {
  const [date, setDate] = useState(localDate())
  const [schedule, setSchedule] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [importFile, setImportFile] = useState(null)
  const [importBusy, setImportBusy] = useState(false)
  const [importMessage, setImportMessage] = useState('')
  const [importError, setImportError] = useState('')

  const load = useCallback(async () => {
    if (adminOnly) return
    setBusy(true)
    setError('')
    try { setSchedule(await api('/api/timetable/on-date?date=' + date, credentials)) }
    catch (problem) { setError(problem.message); setSchedule(null) }
    finally { setBusy(false) }
  }, [date, credentials, adminOnly])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const classes = schedule?.classes || []

  async function downloadTemplate() {
    setImportError('')
    try {
      const response = await fetch('/api/timetable/import/template', { headers: { Authorization: authHeader(credentials) } })
      if (!response.ok) throw new Error((await response.text()) || 'Template download failed (' + response.status + ')')
      const url = URL.createObjectURL(await response.blob())
      const link = document.createElement('a')
      link.href = url
      link.download = 'university-timetable-template.xlsx'
      link.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (problem) { setImportError(problem.message) }
  }

  async function importTimetable(event) {
    event.preventDefault()
    if (!importFile) return
    const confirmed = window.confirm('Replace the current base timetable with ' + importFile.name + '? This changes the schedule for the whole building.')
    if (!confirmed) return
    setImportBusy(true)
    setImportError('')
    setImportMessage('')
    try {
      const formData = new FormData()
      formData.append('file', importFile)
      const response = await fetch('/api/timetable/import', {
        method: 'POST',
        headers: { Authorization: authHeader(credentials) },
        body: formData,
      })
      const detail = await response.text()
      if (!response.ok) {
        let message = detail || 'Import failed (' + response.status + ')'
        try { const parsed = JSON.parse(detail); message = parsed.detail || parsed.message || parsed.error || message } catch { /* backend may return plain text */ }
        throw new Error(message)
      }
      const result = JSON.parse(detail)
      setImportMessage(result.importedEntries + ' timetable entries imported. ' + result.message)
      setImportFile(null)
      event.target.reset()
    } catch (problem) { setImportError(problem.message) }
    finally { setImportBusy(false) }
  }

  return <>
    <div className="page-heading"><span className="eyebrow">{adminOnly ? 'ADMIN · BASE SCHEDULE' : 'THE WEEK, MADE CLEAR'}</span><h1>{adminOnly ? <>Manage the <span className="heading-serif">timetable.</span></> : <>Class <span className="heading-serif">timetable.</span></>}</h1><p>{adminOnly ? 'Upload the recurring building schedule from one workbook.' : 'Browse the building’s schedule for a date.'}</p></div>
    {adminOnly && <section className="surface timetable-import">
      <div className="timetable-import-copy"><span className="eyebrow">BASE SCHEDULE</span><h2>Import a timetable</h2><p>Use the workbook template to validate and replace the building’s recurring timetable.</p></div>
      <button className="secondary-button template-button" type="button" onClick={downloadTemplate}>↓ &nbsp; Download Excel template</button>
      <form className="import-form" onSubmit={importTimetable}>
        <label className="import-file-label">Excel workbook (.xlsx)<input type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event) => { setImportFile(event.target.files?.[0] || null); setImportError(''); setImportMessage('') }}/></label>
        <button className="primary-button import-submit" type="submit" disabled={!importFile || importBusy}>{importBusy ? 'Importing…' : 'Replace base timetable'} <span>→</span></button>
      </form>
      {importFile && <p className="import-selected">Selected: <strong>{importFile.name}</strong></p>}
      <p className="import-warning">Import replaces the current base schedule across the building. The existing schedule stays intact if validation fails.</p>
      {importError && <div className="form-error" role="alert">{importError}</div>}
      {importMessage && <div className="success-note" role="status">✓ {importMessage}</div>}
    </section>}
    {!adminOnly && <>
      <div className="toolbar timetable-toolbar"><label className="date-control"><span>DATE</span><input type="date" value={date} onChange={(event) => setDate(event.target.value)}/></label>{schedule && <span className="week-pill">{schedule.activeWeek}</span>}</div>
      {error && <div className="form-error">{error}</div>}
      <section className="surface timetable-surface"><div className="timetable-date"><div><span className="eyebrow">SCHEDULE FOR</span><h2>{prettyDate(date)}</h2></div><span className="result-count">{classes.length} classes</span></div>{busy ? <EmptyState loading title="Loading timetable" text=""/> : classes.length ? <div className="timeline-list">{classes.map((item) => <ScheduleRow key={item.timetableEntryId + '-' + item.startTime} item={item}/>)}</div> : <EmptyState title="No classes scheduled" text="There are no timetable entries for this day."/>}</section>
    </>}
  </>
}
function MessagesPage({ credentials, identity }) {
  const [contacts, setContacts] = useState([])
  const [selectedKey, setSelectedKey] = useState('')
  const selectedKeyRef = useRef('')
  const selectedContactRef = useRef(null)
  const conversationRequestRef = useRef(0)
  const [activeTarget, setActiveTarget] = useState(null)
  const [unreadBoundary, setUnreadBoundary] = useState(null)
  const [feed, setFeed] = useState([])
  const [draft, setDraft] = useState('')
  const [search, setSearch] = useState('')
  const [busy, setBusy] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const selected = contacts.find((contact) => contactKey(contact) === selectedKey) || null
  const filteredContacts = contacts.filter((contact) => (contact.displayName + ' ' + contact.code).toLowerCase().includes(search.toLowerCase()))

  const refreshContacts = useCallback(async () => {
    try {
      const items = await api('/api/messages/contacts', credentials)
      setContacts((current) => JSON.stringify(current) === JSON.stringify(items) ? current : items)
      if (!selectedKeyRef.current && items.length) {
        const initial = items.find((item) => item.conversationType === 'DIRECT' && item.participantType === 'SYSTEM') || items[0]
        selectedKeyRef.current = contactKey(initial)
        setSelectedKey(contactKey(initial))
        setActiveTarget(initial)
        setUnreadBoundary(initial.unreadCount > 0
          ? { key: contactKey(initial), after: initial.lastReadAt, through: initial.lastMessageAt, show: true }
          : null)
      }
      return items
    } catch (problem) {
      setError(problem.message)
      return []
    }
  }, [credentials])

  const markConversationRead = useCallback(async (contact) => {
    const readAt = await api('/api/messages/read', credentials, { method: 'POST', body: JSON.stringify({
      conversationType: contact.conversationType,
      participantType: contact.participantType,
      code: contact.code,
    }) })
    const key = contactKey(contact)
    setContacts((current) => current.map((item) => contactKey(item) === key
      ? { ...item, unreadCount: 0, lastReadAt: readAt }
      : item))
  }, [credentials])

  const loadConversation = useCallback(async (contact) => {
    const requestId = ++conversationRequestRef.current
    if (!contact) { setFeed([]); setBusy(false); return }
    let path
    if (contact.conversationType === 'GLOBAL') path = '/api/messages/global'
    else if (contact.conversationType === 'SECTION') path = '/api/messages/section/' + encodeURIComponent(contact.code)
    else path = '/api/messages/direct/' + identity.participantType + '/' + encodeURIComponent(identity.participantCode) +
      '/' + contact.participantType + '/' + encodeURIComponent(contact.code)
    setBusy(true)
    setError('')
    try {
      const messages = await api(path, credentials)
      if (requestId === conversationRequestRef.current) {
        setFeed(messages)
        if (contact.unreadCount > 0) markConversationRead(contact).catch((problem) => setError(problem.message))
      }
    } catch (problem) {
      if (requestId === conversationRequestRef.current) { setFeed([]); setError(problem.message) }
    } finally {
      if (requestId === conversationRequestRef.current) setBusy(false)
    }
  }, [identity, credentials, markConversationRead])

  useEffect(() => {
    const timer = window.setTimeout(refreshContacts, 0)
    return () => window.clearTimeout(timer)
  }, [refreshContacts])

  useEffect(() => {
    if (selected) selectedContactRef.current = selected
  }, [selected])

  useEffect(() => {
    const timer = window.setTimeout(() => loadConversation(selectedContactRef.current), 0)
    return () => {
      window.clearTimeout(timer)
      conversationRequestRef.current += 1
    }
  }, [loadConversation, selectedKey])
  useEffect(() => {
    const timer = window.setInterval(refreshContacts, 10000)
    return () => window.clearInterval(timer)
  }, [refreshContacts])

  const streamPath = useMemo(() => {
    if (!selectedKey) return null
    const [conversationType, participantType, code] = selectedKey.split(':')
    if (conversationType === 'GLOBAL') return '/api/messages/stream/global'
    if (conversationType === 'SECTION') return '/api/messages/stream/section/' + encodeURIComponent(code)
    return '/api/messages/stream/direct/' + identity.participantType + '/' + encodeURIComponent(identity.participantCode) +
      '/' + participantType + '/' + encodeURIComponent(code)
  }, [selectedKey, identity])

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
              const own = message.senderCode?.toLowerCase() === identity.participantCode.toLowerCase()
                && message.senderType === identity.participantType
              const currentConversation = selectedContactRef.current
              if (!own && currentConversation) markConversationRead(currentConversation).catch((problem) => setError(problem.message))
              refreshContacts()
            } catch { /* The initial connection event has no message payload. */ }
          }
        }
      } catch (problem) {
        if (problem.name !== 'AbortError') console.info('Conversation stream disconnected')
      }
    }
    listen()
    return () => controller.abort()
  }, [streamPath, credentials, identity, markConversationRead, refreshContacts])

  async function resolveContact(target) {
    const known = contacts.find((contact) => contact.code.toLowerCase() === target.toLowerCase())
    if (known) return known
    if (/^A\d+(?:-[A-Z0-9_-]+)?$/i.test(target)) {
      return { conversationType: 'SECTION', participantType: 'SECTION', code: target.toUpperCase(), displayName: 'Class ' + target.toUpperCase() }
    }
    const [teachers, students] = await Promise.all([
      api('/api/teachers', credentials),
      api('/api/students', credentials),
    ])
    const teacher = teachers.find((item) => item.staffCode.toLowerCase() === target.toLowerCase())
    if (teacher) return { conversationType: 'DIRECT', participantType: 'TEACHER', code: teacher.staffCode, displayName: teacher.fullName + ' · ' + teacher.staffCode }
    const student = students.find((item) => item.studentCode.toLowerCase() === target.toLowerCase())
    if (student) return { conversationType: 'DIRECT', participantType: 'STUDENT', code: student.studentCode, displayName: student.fullName + ' · ' + student.studentCode }
    throw new Error('No teacher, student, or class matches @' + target + '. Check the short code.')
  }

  function openContact(contact) {
    const key = contactKey(contact)
    setUnreadBoundary(contact.unreadCount > 0 ? { key, after: contact.lastReadAt, through: contact.lastMessageAt, show: true } : null)
    setActiveTarget(contact)
    selectedKeyRef.current = key
    setSelectedKey(key)
    selectedContactRef.current = contact
  }

  async function submitMessage(event) {
    event?.preventDefault?.()
    if (sending) return
    const trimmed = draft.trim()
    const startsWithMention = trimmed.startsWith('@')
    const match = startsWithMention ? trimmed.match(/^@(\S+)(?:\s+([\s\S]*))?$/) : null
    if (startsWithMention && !match) { setError('Use a contact mention like @ALS followed by your message.'); return }

    let target
    let body
    if (match) {
      const targetName = match[1].replace(/,$/, '')
      body = (match[2] || '').trim()
      if (!body) { setError('Write a message after the @contact.'); return }
      if (targetName.toLowerCase() === 'system') {
        target = contacts.find((item) => item.conversationType === 'DIRECT' && item.participantType === 'SYSTEM')
          || { conversationType: 'DIRECT', participantType: 'SYSTEM', code: 'SYSTEM', displayName: 'Campus system' }
      } else if (targetName.toLowerCase() === 'all') {
        target = contacts.find((item) => item.conversationType === 'GLOBAL')
          || { conversationType: 'GLOBAL', participantType: null, code: 'ALL', displayName: 'Building-wide' }
      } else {
        try { target = await resolveContact(targetName) }
        catch (problem) { setError(problem.message); return }
      }
    } else {
      target = activeTarget
      body = trimmed
      if (!target) { setError('Start with a contact such as @system, @A7, @ALS, @AS, or @all.'); return }
      if (!body) return
    }

    setSending(true)
    setError('')
    try {
      if (target.conversationType === 'DIRECT' && target.participantType === 'SYSTEM') {
        await api('/api/system/command', credentials, { method: 'POST', body: JSON.stringify({ command: body }) })
      } else if (target.conversationType === 'GLOBAL') {
        await api('/api/messages', credentials, { method: 'POST', body: JSON.stringify({
          senderType: identity.participantType, senderCode: identity.participantCode, audience: 'GLOBAL',
          recipientType: null, recipientCode: null, category: 'GENERAL', body,
        }) })
      } else {
        const section = target.conversationType === 'SECTION'
        await api('/api/messages', credentials, { method: 'POST', body: JSON.stringify({
          senderType: identity.participantType, senderCode: identity.participantCode,
          audience: section ? 'SECTION' : 'DIRECT',
          recipientType: section ? 'SECTION' : target.participantType,
          recipientCode: target.code, category: 'GENERAL', body,
        }) })
      }
      setDraft('')
      setActiveTarget(target)
      selectedKeyRef.current = contactKey(target)
      setSelectedKey(contactKey(target))
      selectedContactRef.current = target
      setUnreadBoundary(target.unreadCount > 0
        ? { key: contactKey(target), after: target.lastReadAt, through: target.lastMessageAt, show: true }
        : null)
      const updatedContacts = await refreshContacts()
      const savedTarget = updatedContacts.find((item) => contactKey(item) === contactKey(target)) || target
      selectedContactRef.current = savedTarget
      await loadConversation(savedTarget)
    } catch (problem) { setError(problem.message) }
    finally { setSending(false) }
  }

  const selectedConversationKey = selected ? contactKey(selected) : ''
  let unreadDividerShown = false
  return <>
    <div className="page-heading"><span className="eyebrow">YOUR CAMPUS INBOX</span><h1>Messages <span className="heading-serif">with the right people.</span></h1><p>Start with @contact, then keep messaging that person until you mention someone else.</p></div>
    <div className="messenger-layout">
      <aside className="surface contacts-panel">
        <div className="contacts-heading"><div><span className="eyebrow">INBOX</span><h2>Your conversations</h2></div><span className="contact-count">{contacts.length}</span></div>
        <label className="contact-search"><span>⌕</span><input placeholder="Find a contact" value={search} onChange={(event) => setSearch(event.target.value)}/></label>
        <div className="contact-list">{filteredContacts.map((contact) => <button key={contactKey(contact)} className={'contact-row ' + (contactKey(contact) === selectedKey ? 'active ' : '') + (contact.unreadCount > 0 ? 'unread' : '')} onClick={() => openContact(contact)}>
          <span className={'contact-avatar ' + (contact.participantType === 'SYSTEM' ? 'system' : contact.conversationType === 'SECTION' ? 'section' : '')}>{contact.conversationType === 'GLOBAL' ? '@' : contact.participantType === 'SYSTEM' ? '✳' : contact.code.slice(0, 1).toUpperCase()}</span>
          <span className="contact-copy"><strong>{contact.displayName}</strong><small>{contact.lastMessage || (contact.conversationType === 'GLOBAL' ? 'Building-wide messages' : 'Start a conversation')}</small></span>
          {contact.unreadCount > 0 && <span className="unread-badge" aria-label={`${contact.unreadCount} unread messages`}>{contact.unreadCount > 99 ? '99+' : contact.unreadCount}</span>}
          {contact.conversationType === 'SECTION' && <span className="contact-kind">CLASS</span>}
        </button>)}</div>
        <div className="account-identity"><span className="avatar">{identity.participantCode.slice(0, 1).toUpperCase()}</span><span><strong>{identity.role === 'TEACHER' ? 'Teacher' : 'Student'}</strong><small>{identity.participantCode}</small></span></div>
      </aside>
      <section className="surface conversation-panel">
        <div className="conversation-header">{selected ? <><div><span className="eyebrow">{selected.conversationType === 'GLOBAL' ? 'BUILDING-WIDE' : selected.conversationType === 'SECTION' ? 'CLASS CHAT' : selected.participantType === 'SYSTEM' ? 'CAMPUS ASSISTANT' : 'DIRECT MESSAGE'}</span><h2>{selected.displayName}</h2></div><span className="chat-live"><i/> Live</span></> : <div><span className="eyebrow">MESSAGES</span><h2>Choose a conversation</h2></div>}</div>
        <div className="conversation-feed">{error && <div className="form-error" role="alert">{error}</div>}{busy ? <EmptyState loading title="Loading conversation" text=""/> : feed.length ? feed.map((message) => {
          const own = message.senderCode?.toLowerCase() === identity.participantCode.toLowerCase() && message.senderType === identity.participantType
          const unread = unreadBoundary?.key === selectedConversationKey && unreadBoundary.show && !own
            && (!unreadBoundary.after || new Date(message.createdAt) > new Date(unreadBoundary.after))
            && (!unreadBoundary.through || new Date(message.createdAt) <= new Date(unreadBoundary.through))
          const showDivider = unread && !unreadDividerShown
          if (showDivider) unreadDividerShown = true
          return <Fragment key={message.id}>{showDivider && <div className="unread-divider">Unread messages</div>}<MessageBubble message={message} own={own} unread={unread}/></Fragment>
        }) : <EmptyState title={selected ? 'No messages here yet' : 'Your inbox is ready'} text={selected ? 'Send the first note using the @contact box below.' : 'Type @system, @A7, or a teacher code to start a chat.'}/>}</div>
        <form className="command-composer" onSubmit={submitMessage}>
          <div className="composer-account"><span>Signed in as</span><strong>{identity.participantType} · {identity.participantCode}</strong></div>
          <div className="command-input"><textarea value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submitMessage() } }} maxLength={2000} placeholder={activeTarget ? `Message to ${activeTarget.displayName}; start with @ to switch` : 'Start with @system, @A7, @ALS, @AS, or @all'} aria-label="Message with an @contact mention" />
            <FuseButton className="message-send-fuse" label={sending ? 'Sending' : 'Send'} undoLabel="Undo" doneLabel="Sent"
              icon={<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 19V5m-7 7 7-7 7 7" /></svg>}
              color="#ffffff" background="#506f58" fuseColor="#e8c77a" size="sm" radius={9} undoWindow={1400}
              fuse="bottom" fuseThickness={2} commitOn="fuseEnd" pauseOnHover={false} settle="reset"
              disabled={sending || !draft.trim()} onCommit={submitMessage}/>
          </div>
          <div className="composer-hint">{activeTarget ? <>Sending to <strong>{activeTarget.displayName}</strong>. Keep typing to continue; mention <strong>@</strong> followed by another contact to switch.</> : <>Start with <strong>@system</strong>, <strong>@A7</strong>, <strong>@ALS</strong>/<strong>@AS</strong>, or <strong>@all</strong>.</>} Enter sends; Shift+Enter adds a line.</div>
        </form>
      </section>
    </div>
  </>
}
function contactKey(contact) {
  return contact.conversationType + ':' + (contact.participantType || '') + ':' + contact.code
}
function MessageBubble({ message, own = false, unread = false }) {
  const initials = message.senderCode?.split(/[_\s-]/).filter(Boolean).slice(-1)[0]?.slice(0, 1).toUpperCase() || '?'
  return <article className={`message-item ${own ? 'own-message' : ''} ${unread ? 'unread-message' : ''}`}><span className={`message-avatar ${message.senderType === 'TEACHER' ? 'teacher' : message.senderType === 'SYSTEM' ? 'system' : ''}`}>{initials}</span><div className="message-content"><div className="message-meta"><strong>{message.senderType === 'SYSTEM' ? 'Campus system' : message.senderCode}</strong><span>{message.senderType?.toLowerCase()}</span><time>{new Date(message.createdAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</time></div><p>{message.body}</p><span className={`category-chip ${message.category?.toLowerCase()}`}>{message.category?.replace('_', ' ')}</span></div></article>
}

export default App
