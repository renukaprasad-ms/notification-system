import { useEffect, useMemo, useState } from 'react'
import {
  FaArrowTrendUp,
  FaBell,
  FaChevronDown,
  FaChevronUp,
  FaCircle,
  FaCircleCheck,
  FaCircleDot,
  FaEye,
  FaInbox,
  FaPaperPlane,
  FaShieldHalved,
  FaUsers,
} from 'react-icons/fa6'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useNotifications } from '../hooks/useNotifications'
import { fetchMyNotificationStats } from '../services/notificationService'
import { getApiErrorMessage } from '../utils/apiError'

function DashboardPage() {
  const { hasRole } = useAuth()
  const {
    notifications,
    isLoadingNotifications,
    notificationsError,
    loadNotificationDetail,
    unreadCount,
  } = useNotifications()
  const [stats, setStats] = useState({
    totalNotifications: 0,
    unreadNotifications: 0,
    readNotifications: 0,
  })
  const [statsError, setStatsError] = useState('')
  const [isLoadingStats, setIsLoadingStats] = useState(true)
  const [expandedRecipientId, setExpandedRecipientId] = useState('')
  const [loadingDetailRecipientId, setLoadingDetailRecipientId] = useState('')
  const [detailError, setDetailError] = useState('')
  const isAdmin = hasRole(['ADMIN'])

  useEffect(() => {
    let isMounted = true

    const loadStats = async () => {
      try {
        const response = await fetchMyNotificationStats()
        if (!isMounted) {
          return
        }

        setStats(response.data || { totalNotifications: 0, unreadNotifications: 0, readNotifications: 0 })
        setStatsError('')
      } catch (apiError) {
        if (isMounted) {
          setStatsError(getApiErrorMessage(apiError, 'Unable to load notification stats.'))
        }
      } finally {
        if (isMounted) {
          setIsLoadingStats(false)
        }
      }
    }

    loadStats()

    return () => {
      isMounted = false
    }
  }, [])

  const latestNotifications = useMemo(() => notifications.slice(0, 5), [notifications])

  const statCards = [
    {
      label: 'Total Notifications',
      value: stats.totalNotifications,
      helper: 'All time received',
      icon: FaInbox,
      accent: 'bg-blue-100 text-blue-700',
    },
    {
      label: 'Unread Notifications',
      value: stats.unreadNotifications,
      helper: 'Needs your attention',
      icon: FaBell,
      accent: 'bg-amber-100 text-amber-700',
    },
    {
      label: 'Read Notifications',
      value: stats.readNotifications,
      helper: 'Already read',
      icon: FaCircleCheck,
      accent: 'bg-emerald-100 text-emerald-700',
    },
  ]

  const adminSections = [
    {
      title: 'Send notifications',
      description: 'Compose broadcasts or targeted messages for the right audience.',
      to: '/admin',
      icon: FaPaperPlane,
      accent: 'bg-cyan-100 text-cyan-700',
    },
    {
      title: 'Open inbox',
      description: 'Review incoming messages and track unread activity in detail.',
      to: '/notifications',
      icon: FaBell,
      accent: 'bg-blue-100 text-blue-700',
    },
    {
      title: 'Audience and roles',
      description: 'Jump into profile and access areas to verify the active workspace setup.',
      to: '/profile',
      icon: FaUsers,
      accent: 'bg-emerald-100 text-emerald-700',
    },
  ]

  const handleToggleNotification = async (notification) => {
    const isExpanded = expandedRecipientId === notification.recipientId
    if (isExpanded) {
      setExpandedRecipientId('')
      return
    }

    try {
      setDetailError('')
      setExpandedRecipientId(notification.recipientId)

      if (!notification.detailLoaded) {
        setLoadingDetailRecipientId(notification.recipientId)
        const updated = await loadNotificationDetail(notification.recipientId)
        if (updated?.readAt) {
          setStats((current) => ({
            ...current,
            unreadNotifications: Math.max(0, current.unreadNotifications - 1),
            readNotifications: current.readNotifications + 1,
          }))
        }
      }
    } catch (apiError) {
      setDetailError(getApiErrorMessage(apiError, 'Unable to load notification details.'))
    } finally {
      setLoadingDetailRecipientId('')
    }
  }

  const relativeTime = (value) => {
    if (!value) {
      return ''
    }

    const diffMs = Date.now() - new Date(value).getTime()
    const diffMinutes = Math.max(1, Math.floor(diffMs / 60000))
    if (diffMinutes < 60) {
      return `${diffMinutes} min${diffMinutes === 1 ? '' : 's'} ago`
    }

    const diffHours = Math.floor(diffMinutes / 60)
    if (diffHours < 24) {
      return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`
    }

    const diffDays = Math.floor(diffHours / 24)
    return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`
  }

  return (
    <section className="space-y-6">
      {(statsError || notificationsError || detailError) && (
        <p className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          {statsError || notificationsError || detailError}
        </p>
      )}

      <section className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm text-slate-500">Here&apos;s what&apos;s happening with your notifications.</p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
              {isAdmin ? 'Admin control center' : 'Your notification pulse'}
            </h2>
          </div>

          {isAdmin && (
            <div className="inline-flex items-center gap-3 rounded-[24px] border border-cyan-100 bg-[linear-gradient(135deg,_rgba(236,254,255,0.95)_0%,_rgba(248,250,252,0.92)_100%)] px-5 py-4 shadow-[0_12px_30px_rgba(14,165,233,0.08)]">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-500 text-slate-950">
                <FaShieldHalved />
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-cyan-700">Admin mode</p>
                <p className="mt-1 text-sm text-slate-600">Operate sending, visibility, and inbox flows from one place.</p>
              </div>
            </div>
          )}
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          {statCards.map((card) => {
            const Icon = card.icon

            return (
              <article key={card.label} className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_35px_rgba(15,23,42,0.05)]">
                <div className="flex items-center gap-4">
                  <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${card.accent}`}>
                    <Icon />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-500">{card.label}</p>
                    <p className="mt-1 text-4xl font-semibold tracking-tight text-slate-950">
                      {isLoadingStats ? '--' : card.value}
                    </p>
                  </div>
                </div>
                <p className="mt-4 text-sm text-slate-500">{card.helper}</p>
              </article>
            )
          })}
        </div>
      </section>

      {isAdmin && (
        <section className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-white">
              <FaArrowTrendUp />
            </div>
            <div>
              <p className="text-sm font-medium text-cyan-700">Admin actions</p>
              <h2 className="text-2xl font-semibold tracking-tight text-slate-950">Everything you need .</h2>
            </div>
          </div>

          <div className="mt-6 grid gap-4 xl:grid-cols-3">
            {adminSections.map((section) => {
              const Icon = section.icon

              return (
                <Link
                  key={section.title}
                  to={section.to}
                  className="group rounded-[24px] border border-slate-200 bg-[linear-gradient(180deg,_#ffffff_0%,_#f8fafc_100%)] p-5 shadow-[0_12px_30px_rgba(15,23,42,0.05)] transition hover:-translate-y-0.5 hover:border-cyan-200 hover:shadow-[0_18px_35px_rgba(14,165,233,0.08)]"
                >
                  <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${section.accent}`}>
                    <Icon />
                  </div>
                  <h3 className="mt-4 text-lg font-semibold text-slate-950 transition group-hover:text-cyan-800">{section.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{section.description}</p>
                </Link>
              )
            })}
          </div>
        </section>
      )}

      <section className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <h2 className="text-2xl font-semibold tracking-tight text-slate-950">Latest Notifications</h2>
            <span className="inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
              <FaCircle className="text-[8px]" />
              Live
            </span>
          </div>

          <div className="flex items-center gap-3">
            <span className="rounded-full border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-semibold text-slate-600">
              Unread: {unreadCount}
            </span>
            <Link
              to="/notifications"
              className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-cyan-300 hover:text-cyan-700"
            >
              View all
            </Link>
          </div>
        </div>

        <div className="mt-6 space-y-4">
          {isLoadingNotifications ? (
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-slate-600">
              Loading latest notifications...
            </div>
          ) : latestNotifications.length === 0 ? (
            <div className="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-slate-600">
              No notifications yet.
            </div>
          ) : (
            latestNotifications.map((notification) => (
              <article
                key={notification.recipientId}
                className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_35px_rgba(15,23,42,0.04)]"
              >
                <div className="flex items-start gap-4">
                  <div
                    className={`mt-1 flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ${
                      !notification.readAt
                        ? 'bg-red-50 text-red-500'
                        : notification.viewedAt
                          ? 'bg-amber-50 text-amber-500'
                          : 'bg-emerald-50 text-emerald-500'
                    }`}
                  >
                    {!notification.readAt ? <FaCircleDot /> : notification.viewedAt ? <FaEye /> : <FaCircleCheck />}
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <button
                        type="button"
                        onClick={() => handleToggleNotification(notification)}
                        className="min-w-0 text-left"
                      >
                        <div className="flex items-center gap-2">
                          <h3 className="truncate text-base font-semibold text-slate-950">{notification.title}</h3>
                          {expandedRecipientId === notification.recipientId ? (
                            <FaChevronUp className="text-slate-400" />
                          ) : (
                            <FaChevronDown className="text-slate-400" />
                          )}
                        </div>
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-700">
                            {notification.priority}
                          </span>
                          <span className="rounded-full bg-cyan-50 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-cyan-700">
                            {notification.type}
                          </span>
                        </div>
                      </button>

                      <div className="flex items-center gap-3 text-sm text-slate-500">
                        <span>{relativeTime(notification.createdAt)}</span>
                        <span className={`${notification.readAt ? 'text-slate-300' : 'text-blue-500'}`}>
                          <FaCircle className="text-[10px]" />
                        </span>
                      </div>
                    </div>

                    {expandedRecipientId === notification.recipientId && (
                      <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                        {loadingDetailRecipientId === notification.recipientId && !notification.detailLoaded ? (
                          <p className="text-sm text-slate-500">Loading notification details...</p>
                        ) : (
                          <p className="text-sm leading-7 text-slate-700">{notification.message}</p>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </article>
            ))
          )}
        </div>

        {!isLoadingNotifications && latestNotifications.length > 0 && (
          <div className="mt-6 text-center text-sm text-slate-400">No more notifications</div>
        )}
      </section>
    </section>
  )
}

export default DashboardPage
