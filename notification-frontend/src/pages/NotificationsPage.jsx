import { useEffect } from 'react'
import {
  FaCircleCheck,
  FaCircleDot,
  FaTrashCan,
  FaEnvelopeOpenText,
  FaEye,
  FaMagnifyingGlass,
} from 'react-icons/fa6'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../utils/apiError'
import { useInfiniteScroll } from '../hooks/useInfiniteScroll'
import { useNotifications } from '../hooks/useNotifications'
import { useState } from 'react'

function NotificationsPage() {
  const { hasRole } = useAuth()
  const {
    notifications,
    notificationsError,
    isLoadingNotifications,
    isLoadingMoreNotifications,
    notificationSearchQuery,
    hasMoreNotifications,
    loadMoreNotifications,
    setNotificationSearchQuery,
    markNotificationRead,
    markNotificationViewed,
    deleteNotificationById,
  } = useNotifications()
  const [deleteError, setDeleteError] = useState('')
  const [deletingNotificationId, setDeletingNotificationId] = useState('')
  const isAdmin = hasRole(['ADMIN'])

  const loadMoreRef = useInfiniteScroll({
    enabled: true,
    hasMore: hasMoreNotifications,
    isLoading: isLoadingNotifications || isLoadingMoreNotifications,
    onLoadMore: loadMoreNotifications,
  })

  useEffect(() => {
    const unseenNotifications = notifications.filter((notification) => !notification.viewedAt)
    unseenNotifications.forEach((notification) => {
      markNotificationViewed(notification.recipientId).catch(() => {})
    })
  }, [markNotificationViewed, notifications])

  const handleDeleteNotification = async (notificationId) => {
    try {
      setDeleteError('')
      setDeletingNotificationId(notificationId)
      await deleteNotificationById(notificationId)
    } catch (apiError) {
      setDeleteError(getApiErrorMessage(apiError, 'Unable to delete notification.'))
    } finally {
      setDeletingNotificationId('')
    }
  }

  return (
    <section className="space-y-6">
      <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
        <label className="relative block">
          <FaMagnifyingGlass className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={notificationSearchQuery}
            onChange={(event) => setNotificationSearchQuery(event.target.value)}
            placeholder="Search notifications by title, message, type, or priority"
            className="h-12 w-full rounded-2xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
          />
        </label>
      </div>

      {notificationsError && (
        <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          {notificationsError}
        </p>
      )}

      {deleteError && (
        <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          {deleteError}
        </p>
      )}

      <div className="space-y-4">
        {isLoadingNotifications ? (
          <div className="rounded-[28px] border border-slate-200 bg-white px-6 py-5 text-sm text-slate-600 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
            Loading notifications...
          </div>
        ) : notifications.length === 0 ? (
          <div className="rounded-[28px] border border-slate-200 bg-white px-6 py-5 text-sm text-slate-600 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
            No notifications matched your search.
          </div>
        ) : (
          <>
            {notifications.map((notification) => (
              <article
                key={notification.recipientId}
                className={`rounded-[28px] border p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] transition ${
                  notification.readAt
                    ? 'border-slate-200 bg-white'
                    : notification.viewedAt
                      ? 'border-amber-200 bg-[linear-gradient(180deg,_#fffdf7_0%,_#ffffff_100%)]'
                      : 'border-cyan-300 bg-[linear-gradient(180deg,_rgba(236,254,255,0.95)_0%,_#ffffff_55%)] ring-1 ring-cyan-100'
                }`}
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={`rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] ${
                          notification.readAt
                            ? 'bg-cyan-50 text-cyan-800'
                            : 'bg-white/90 text-cyan-900 shadow-sm'
                        }`}
                      >
                        {notification.type}
                      </span>
                      <span
                        className={`rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] ${
                          notification.readAt
                            ? 'bg-slate-100 text-slate-700'
                            : 'bg-slate-100/90 text-slate-800'
                        }`}
                      >
                        {notification.priority}
                      </span>
                    </div>
                    <h2 className="mt-4 text-xl font-semibold tracking-tight text-slate-950">
                      {notification.title}
                    </h2>
                    <p className={`mt-3 text-sm leading-7 ${notification.readAt ? 'text-slate-600' : 'text-slate-700'}`}>
                      {notification.message}
                    </p>
                  </div>

                  <div className="flex shrink-0 flex-col items-start gap-3 sm:items-end">
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                      {notification.viewedAt && !notification.readAt && (
                        <>
                          <FaEye className="text-amber-600" />
                          Viewed
                        </>
                      )}
                      {!notification.viewedAt && !notification.readAt && (
                        <>
                          <FaCircleDot className="text-cyan-600" />
                          New
                        </>
                      )}
                    </div>

                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                      {notification.readAt ? (
                        <>
                          <FaCircleCheck className="text-emerald-600" />
                          Read
                        </>
                      ) : (
                        <>Unread</>
                      )}
                    </div>

                    {!notification.readAt && (
                      <button
                        type="button"
                        onClick={() => markNotificationRead(notification.recipientId)}
                        className={`inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] transition ${
                          notification.viewedAt
                            ? 'border border-amber-200 bg-white text-slate-700 hover:border-amber-300 hover:text-amber-700'
                            : 'border border-cyan-300 bg-white text-cyan-800 hover:border-cyan-400 hover:bg-cyan-50'
                        }`}
                      >
                        <FaEnvelopeOpenText />
                        Mark as read
                      </button>
                    )}

                    {isAdmin && (
                      <button
                        type="button"
                        onClick={() => handleDeleteNotification(notification.notificationId)}
                        disabled={deletingNotificationId === notification.notificationId}
                        className="inline-flex items-center gap-2 rounded-full border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-rose-700 transition hover:border-rose-300 hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        <FaTrashCan />
                        {deletingNotificationId === notification.notificationId ? 'Deleting...' : 'Delete'}
                      </button>
                    )}
                  </div>
                </div>
              </article>
            ))}

            {isLoadingMoreNotifications && (
              <div className="rounded-[28px] border border-slate-200 bg-white px-6 py-5 text-sm text-slate-600 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
                Loading more notifications...
              </div>
            )}

            <div ref={loadMoreRef} className="h-2 w-full" />

            {!hasMoreNotifications && notifications.length > 0 && (
              <div className="px-2 text-center text-xs font-medium uppercase tracking-[0.14em] text-slate-400">
                You&apos;ve reached the end of your notifications
              </div>
            )}
          </>
        )}
      </div>
    </section>
  )
}

export default NotificationsPage
