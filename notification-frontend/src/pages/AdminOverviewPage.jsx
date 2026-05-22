import { useEffect, useState } from 'react'
import { FaBell, FaChartLine, FaShieldHalved, FaUsers } from 'react-icons/fa6'
import { Link } from 'react-router-dom'
import { fetchAdminNotificationOverview, fetchUnreadNotificationCount } from '../services/notificationService'
import { fetchAllUsers } from '../services/userService'
import { getApiErrorMessage } from '../utils/apiError'

function AdminOverviewPage() {
  const [overview, setOverview] = useState({
    notificationsSent: 0,
    activeUsers: 0,
  })
  const [userCount, setUserCount] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let isMounted = true

    const loadOverview = async () => {
      try {
        const [overviewResponse, usersResponse, unreadResponse] = await Promise.all([
          fetchAdminNotificationOverview(),
          fetchAllUsers(),
          fetchUnreadNotificationCount(),
        ])

        if (!isMounted) {
          return
        }

        setOverview(overviewResponse.data || { notificationsSent: 0, activeUsers: 0 })
        setUserCount(usersResponse.data?.length || 0)
        setUnreadCount(unreadResponse.data?.unreadCount || 0)
      } catch (apiError) {
        if (isMounted) {
          setError(getApiErrorMessage(apiError, 'Unable to load the admin overview.'))
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    loadOverview()

    return () => {
      isMounted = false
    }
  }, [])

  const overviewCards = [
    {
      label: 'Notifications Sent',
      value: overview.notificationsSent,
      helper: 'Outbound notifications created from this admin account.',
      icon: FaBell,
    },
    {
      label: 'Active Users',
      value: overview.activeUsers,
      helper: 'Users currently eligible to receive notifications.',
      icon: FaUsers,
    },
    {
      label: 'All Users',
      value: userCount,
      helper: 'Total accounts available in the system right now.',
      icon: FaChartLine,
    },
    {
      label: 'Unread For You',
      value: unreadCount,
      helper: 'Personal unread messages still waiting in your inbox.',
      icon: FaShieldHalved,
    },
  ]

  return (
    <section className="space-y-6">
      {error && <p className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{error}</p>}

      <div className="grid gap-4 xl:grid-cols-4">
        {overviewCards.map((card) => {
          const Icon = card.icon

          return (
            <article
              key={card.label}
              className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_rgba(15,23,42,0.08)]"
            >
              <div className="flex items-center justify-between gap-4">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
                  <Icon />
                </div>
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{card.label}</p>
              </div>
              <p className="mt-5 text-4xl font-semibold tracking-tight text-slate-950">
                {isLoading ? '--' : card.value}
              </p>
              <p className="mt-3 text-sm leading-6 text-slate-600">{card.helper}</p>
            </article>
          )
        })}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <section className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
          <p className="text-sm font-medium text-cyan-700">Workspace summary</p>
          <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
            Keep delivery operations visible before you send anything new.
          </h2>
          <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600">
            This view gives admins a quick read on audience size, outbound volume, and personal inbox state, so sending is grounded in context instead of guesswork.
          </p>

          <div className="mt-8 grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Broadcast</p>
              <p className="mt-3 text-lg font-semibold text-slate-950">All active users</p>
              <p className="mt-2 text-sm text-slate-600">Use when everyone should receive the same operational message.</p>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Targeted</p>
              <p className="mt-3 text-lg font-semibold text-slate-950">Selected recipients</p>
              <p className="mt-2 text-sm text-slate-600">Use for narrower communication tied to a user segment or incident.</p>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Inbox</p>
              <p className="mt-3 text-lg font-semibold text-slate-950">Unread tracking</p>
              <p className="mt-2 text-sm text-slate-600">Monitor what still needs your own attention from the user side.</p>
            </div>
          </div>
        </section>

        <section className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
          <p className="text-sm font-medium text-cyan-700">Quick actions</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Move between overview, sending, and inbox without hunting.</h2>

          <div className="mt-6 grid gap-4">
            <Link
              to="/admin"
              className="rounded-[24px] border border-slate-200 bg-slate-50 p-5 transition hover:border-cyan-300 hover:bg-cyan-50"
            >
              <p className="text-base font-semibold text-slate-950">Open admin send console</p>
              <p className="mt-2 text-sm leading-6 text-slate-600">Compose a new broadcast or targeted notification.</p>
            </Link>

            <Link
              to="/notifications"
              className="rounded-[24px] border border-slate-200 bg-slate-50 p-5 transition hover:border-cyan-300 hover:bg-cyan-50"
            >
              <p className="text-base font-semibold text-slate-950">View notification inbox</p>
              <p className="mt-2 text-sm leading-6 text-slate-600">See incoming notifications and your current unread state.</p>
            </Link>

            <Link
              to="/profile"
              className="rounded-[24px] border border-slate-200 bg-slate-50 p-5 transition hover:border-cyan-300 hover:bg-cyan-50"
            >
              <p className="text-base font-semibold text-slate-950">Review profile and roles</p>
              <p className="mt-2 text-sm leading-6 text-slate-600">Confirm the identity and access level active in this session.</p>
            </Link>
          </div>
        </section>
      </div>
    </section>
  )
}

export default AdminOverviewPage
