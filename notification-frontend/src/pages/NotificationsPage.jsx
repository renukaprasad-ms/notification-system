import {
  FaChevronDown,
  FaChevronUp,
  FaCircleCheck,
  FaCircleDot,
  FaTrashCan,
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
    loadNotificationDetail,
    deleteNotificationById,
  } = useNotifications()
  const [deleteError, setDeleteError] = useState('')
  const [detailError, setDetailError] = useState('')
  const [expandedRecipientId, setExpandedRecipientId] = useState('')
  const [deletingNotificationId, setDeletingNotificationId] = useState('')
  const [loadingDetailRecipientId, setLoadingDetailRecipientId] = useState('')
  const isAdmin = hasRole(['ADMIN'])

  const loadMoreRef = useInfiniteScroll({
    enabled: true,
    hasMore: hasMoreNotifications,
    isLoading: isLoadingNotifications || isLoadingMoreNotifications,
    onLoadMore: loadMoreNotifications,
  })

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
        await loadNotificationDetail(notification.recipientId)
      }
    } catch (apiError) {
      setDetailError(getApiErrorMessage(apiError, 'Unable to load notification details.'))
    } finally {
      setLoadingDetailRecipientId('')
    }
  }

  return (
    <section className="space-y-6">
      <section className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm text-slate-500">Search, open, and clear messages from the same live workspace.</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Notification inbox</h2>
          </div>
          <div className="rounded-full border border-cyan-100 bg-cyan-50 px-4 py-2 text-sm font-semibold text-cyan-700">
            {notifications.length} loaded
          </div>
        </div>

        <label className="relative mt-6 block">
          <FaMagnifyingGlass className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={notificationSearchQuery}
            onChange={(event) => setNotificationSearchQuery(event.target.value)}
            placeholder="Search notifications by title, type, or priority"
            className="h-12 w-full rounded-2xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
          />
        </label>
      </section>

      <div className="grid gap-4 lg:grid-cols-3">
        <article className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_35px_rgba(15,23,42,0.05)]">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Total loaded</p>
          <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">{notifications.length}</p>
          <p className="mt-2 text-sm text-slate-500">Messages currently visible in this inbox view.</p>
        </article>
        <article className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_35px_rgba(15,23,42,0.05)]">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Unread</p>
          <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
            {notifications.filter((notification) => !notification.readAt).length}
          </p>
          <p className="mt-2 text-sm text-slate-500">Still waiting for your attention.</p>
        </article>
        <article className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_35px_rgba(15,23,42,0.05)]">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Read</p>
          <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
            {notifications.filter((notification) => notification.readAt).length}
          </p>
          <p className="mt-2 text-sm text-slate-500">Already reviewed from the current list.</p>
        </article>
      </div>

      <div className="rounded-[30px] border border-slate-200 bg-white p-5 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h3 className="text-xl font-semibold tracking-tight text-slate-950">All notifications</h3>
            <p className="mt-1 text-sm text-slate-500">Open any row to load its full message body.</p>
          </div>
        </div>
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

      {detailError && (
        <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          {detailError}
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
                className="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_35px_rgba(15,23,42,0.05)] transition hover:border-cyan-200 hover:shadow-[0_18px_35px_rgba(14,165,233,0.07)]"
              >
                <div className="flex flex-col gap-4">
                  <div className="flex items-start justify-between gap-4">
                    <button
                      type="button"
                      onClick={() => handleToggleNotification(notification)}
                      className="min-w-0 flex-1 text-left"
                    >
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className="rounded-full bg-cyan-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-cyan-800"
                        >
                          {notification.type}
                        </span>
                        <span
                          className="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-700"
                        >
                          {notification.priority}
                        </span>
                      </div>
                      <div className="mt-4 flex items-center gap-3">
                        <h2 className="min-w-0 truncate text-xl font-semibold tracking-tight text-slate-950">
                          {notification.title}
                        </h2>
                        {expandedRecipientId === notification.recipientId ? (
                          <FaChevronUp className="shrink-0 text-slate-400" />
                        ) : (
                          <FaChevronDown className="shrink-0 text-slate-400" />
                        )}
                      </div>
                    </button>

                    <div className="flex shrink-0 items-center gap-3">
                      <div className="flex flex-col items-end gap-2">
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

                        <p className="text-xs font-medium text-slate-400">
                          {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}
                        </p>
                      </div>

                      {isAdmin && (
                        <button
                          type="button"
                          onClick={() => handleDeleteNotification(notification.notificationId)}
                          disabled={deletingNotificationId === notification.notificationId}
                          aria-label="Delete notification"
                          title="Delete notification"
                          className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-rose-200 bg-rose-50 text-rose-700 transition hover:border-rose-300 hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          <FaTrashCan />
                        </button>
                      )}
                    </div>
                  </div>

                  {expandedRecipientId === notification.recipientId && (
                    <div className="rounded-[22px] border border-slate-200 bg-slate-50 px-5 py-4">
                      {loadingDetailRecipientId === notification.recipientId && !notification.detailLoaded ? (
                        <p className="text-sm text-slate-500">Loading notification details...</p>
                      ) : (
                        <>
                          <p className="text-sm leading-7 text-slate-700">
                            {notification.message}
                          </p>
                          <div className="mt-4 flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                            <span>{notification.deliveryStatus}</span>
                            <span>{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}</span>
                          </div>
                        </>
                      )}
                    </div>
                  )}
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
