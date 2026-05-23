import { useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  FaBell,
  FaBars,
  FaChevronRight,
  FaChartLine,
  FaRightFromBracket,
  FaShieldHalved,
  FaUser,
  FaXmark,
} from 'react-icons/fa6'
import { useAuth } from '../hooks/useAuth'
import { useNotifications } from '../hooks/useNotifications'

function MainLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, hasRole, logout } = useAuth()
  const { unreadCount, isStreamConnected } = useNotifications()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  const pageMeta = useMemo(() => {
    if (location.pathname.startsWith('/admin/overview')) {
      return {
        eyebrow: 'Overview',
        title: 'Admin overview',
        description: 'Track activity, audience size, and outbound notification health from one place.',
      }
    }

    if (location.pathname.startsWith('/admin')) {
      return {
        eyebrow: 'Admin',
        title: 'Send notifications',
        description: 'Create broadcast or targeted messages and deliver them to the right audience.',
      }
    }

    if (location.pathname.startsWith('/profile')) {
      return {
        eyebrow: 'Profile',
        title: 'Account details',
        description: 'Review your identity, roles, and account-level access in the workspace.',
      }
    }

    return {
      eyebrow: 'Notifications',
      title: 'Inbox',
      description: 'Review incoming messages and keep an eye on what still needs attention.',
    }
  }, [location.pathname])

  const handleLogout = async () => {
    setIsLoggingOut(true)

    try {
      await logout()
    } finally {
      setIsLoggingOut(false)
      navigate('/login', { replace: true })
    }
  }

  const navLinkClassName = ({ isActive }) =>
    `flex items-center justify-between rounded-2xl px-4 py-3 text-sm font-semibold transition ${
      isActive ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'
    }`

  const closeSidebar = () => setIsSidebarOpen(false)

  const handleNavigateHome = () => {
    closeSidebar()
    navigate(hasRole(['ADMIN']) ? '/admin/overview' : '/notifications')
  }

  const renderSidebar = () => (
    <>
      <button
        type="button"
        onClick={handleNavigateHome}
        className="flex items-center gap-3 rounded-2xl px-2 py-2 text-left"
      >
        <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-500 text-slate-950">
          <FaBell />
        </div>
        <div>
          <p className="text-sm font-semibold text-slate-950">Notification Hub</p>
          <p className="text-xs text-slate-500">Realtime workspace</p>
        </div>
      </button>

      <div className="mt-6 rounded-[24px] bg-slate-950 px-4 py-4 text-white">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cyan-300">Signed in</p>
        <p className="mt-3 text-lg font-semibold">{user?.fullName || 'User'}</p>
        <p className="mt-1 break-all text-sm text-slate-300">{user?.email}</p>
        <div className="mt-4 flex flex-wrap gap-2">
          {(user?.roles || []).map((role) => (
            <span
              key={role}
              className="rounded-full border border-white/10 bg-white/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-200"
            >
              {role}
            </span>
          ))}
        </div>
      </div>

      <nav className="mt-6 space-y-2">
        {hasRole(['ADMIN']) && (
          <NavLink to="/admin/overview" className={navLinkClassName} onClick={closeSidebar}>
            <span className="flex items-center gap-3">
              <FaChartLine />
              Overview
            </span>
            <FaChevronRight className="text-xs opacity-70" />
          </NavLink>
        )}

        <NavLink to="/notifications" className={navLinkClassName} onClick={closeSidebar}>
          <span className="flex items-center gap-3">
            <FaBell />
            Notifications
          </span>
          <FaChevronRight className="text-xs opacity-70" />
        </NavLink>

        <NavLink to="/profile" className={navLinkClassName} onClick={closeSidebar}>
          <span className="flex items-center gap-3">
            <FaUser />
            Profile
          </span>
          <FaChevronRight className="text-xs opacity-70" />
        </NavLink>

        {hasRole(['ADMIN']) && (
          <NavLink to="/admin" className={navLinkClassName} onClick={closeSidebar}>
            <span className="flex items-center gap-3">
              <FaShieldHalved />
              Admin
            </span>
            <FaChevronRight className="text-xs opacity-70" />
          </NavLink>
        )}
      </nav>

      <div className="mt-6 border-t border-slate-200 pt-6">
        <button
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
          className="inline-flex h-12 w-full items-center justify-center gap-2 rounded-2xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-70"
        >
          <FaRightFromBracket />
          {isLoggingOut ? 'Logging out...' : 'Logout'}
        </button>
      </div>
    </>
  )

  return (
    <main className="h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,_rgba(6,182,212,0.16),_transparent_28%),linear-gradient(180deg,_#e2e8f0_0%,_#f8fafc_36%,_#f8fafc_100%)] px-4 py-4 text-slate-950 sm:px-6 lg:px-8">
      <section className="grid h-full w-full gap-4 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="hidden h-full overflow-y-auto rounded-[30px] border border-slate-200 bg-white/90 p-5 shadow-[0_24px_70px_rgba(15,23,42,0.12)] backdrop-blur lg:flex lg:flex-col">
          {renderSidebar()}
        </aside>

        <section className="min-w-0 overflow-y-auto">
          <div className="space-y-4 pr-0.5">
            <header className="flex items-center justify-between rounded-[24px] border border-slate-200 bg-white/90 px-4 py-3 shadow-[0_18px_50px_rgba(15,23,42,0.08)] backdrop-blur lg:hidden">
              <button
                type="button"
                onClick={() => setIsSidebarOpen(true)}
                className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-700 transition hover:border-cyan-300 hover:text-slate-950"
                aria-label="Open navigation menu"
              >
                <FaBars />
              </button>

              <button
                type="button"
                onClick={handleNavigateHome}
                className="flex min-w-0 items-center gap-3 text-left"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-cyan-500 text-slate-950">
                  <FaBell />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-slate-950">Notification Hub</p>
                  <p className="truncate text-xs text-slate-500">{pageMeta.eyebrow}</p>
                </div>
              </button>

              <button
                type="button"
                onClick={() => navigate('/notifications')}
                className="relative inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 text-slate-950"
                aria-label="Open notifications"
              >
                <FaBell />
                <span className="absolute -right-1 -top-1 inline-flex min-h-5 min-w-5 items-center justify-center rounded-full bg-cyan-500 px-1 text-[11px] font-semibold text-slate-950">
                  {unreadCount}
                </span>
              </button>
            </header>

          <header className="rounded-[30px] border border-slate-200 bg-white/90 px-6 py-5 shadow-[0_24px_70px_rgba(15,23,42,0.08)] backdrop-blur sm:px-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cyan-700">{pageMeta.eyebrow}</p>
                <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">{pageMeta.title}</h1>
                <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600">{pageMeta.description}</p>
              </div>

              <button
                type="button"
                onClick={() => navigate('/notifications')}
                className="inline-flex shrink-0 items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:border-cyan-300 hover:bg-cyan-50"
              >
                <div className="relative flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-white">
                  <FaBell />
                  <span className="absolute -right-1 -top-1 inline-flex min-h-5 min-w-5 items-center justify-center rounded-full bg-cyan-500 px-1 text-[11px] font-semibold text-slate-950">
                    {unreadCount}
                  </span>
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                    {isStreamConnected ? 'Live unread' : 'Unread'}
                  </p>
                  <p className="text-lg font-semibold text-slate-950">{unreadCount}</p>
                </div>
              </button>
            </div>
          </header>

          <Outlet />
          </div>
        </section>
      </section>

      <div
        className={`fixed inset-0 z-40 bg-slate-950/35 backdrop-blur-[2px] transition lg:hidden ${
          isSidebarOpen ? 'pointer-events-auto opacity-100' : 'pointer-events-none opacity-0'
        }`}
        onClick={closeSidebar}
        aria-hidden="true"
      />

      <aside
        className={`fixed inset-y-4 left-4 z-50 flex w-[min(84vw,320px)] flex-col overflow-y-auto rounded-[30px] border border-slate-200 bg-white/95 p-5 shadow-[0_24px_70px_rgba(15,23,42,0.18)] backdrop-blur transition-transform duration-300 lg:hidden ${
          isSidebarOpen ? 'translate-x-0' : '-translate-x-[calc(100%+1rem)]'
        }`}
        aria-hidden={!isSidebarOpen}
      >
        <div className="mb-3 flex items-center justify-end">
          <button
            type="button"
            onClick={closeSidebar}
            className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-600 transition hover:border-cyan-300 hover:text-slate-950"
            aria-label="Close navigation menu"
          >
            <FaXmark />
          </button>
        </div>
        {renderSidebar()}
      </aside>
    </main>
  )
}

export default MainLayout
