import { useEffect } from 'react'
import { FaCircleCheck, FaCircleDot, FaEye, FaEnvelopeOpenText } from 'react-icons/fa6'
import { useNotifications } from '../hooks/useNotifications'

function NotificationsPage() {
  const {
    notifications,
    notificationsError,
    isLoadingNotifications,
    markNotificationRead,
    markNotificationViewed,
  } = useNotifications()

  useEffect(() => {
    const unseenNotifications = notifications.filter((notification) => !notification.viewedAt)
    unseenNotifications.forEach((notification) => {
      markNotificationViewed(notification.recipientId).catch(() => {})
    })
  }, [markNotificationViewed, notifications])

  return (
    <section className="space-y-6">
      {notificationsError && (
        <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          {notificationsError}
        </p>
      )}

      <div className="space-y-4">
        {isLoadingNotifications ? (
          <div className="rounded-[28px] border border-slate-200 bg-white px-6 py-5 text-sm text-slate-600 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
            Loading notifications...
          </div>
        ) : notifications.length === 0 ? (
          <div className="rounded-[28px] border border-slate-200 bg-white px-6 py-5 text-sm text-slate-600 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
            No notifications available yet.
          </div>
        ) : (
          notifications.map((notification) => (
            <article
              key={notification.recipientId}
              className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)]"
            >
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-cyan-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-cyan-800">
                      {notification.type}
                    </span>
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-700">
                      {notification.priority}
                    </span>
                  </div>
                  <h2 className="mt-4 text-xl font-semibold tracking-tight text-slate-950">{notification.title}</h2>
                  <p className="mt-3 text-sm leading-7 text-slate-600">{notification.message}</p>
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
                    <>
                      Unread
                    </>
                  )}
                  </div>

                  {!notification.readAt && (
                    <button
                      type="button"
                      onClick={() => markNotificationRead(notification.recipientId)}
                      className="inline-flex items-center gap-2 rounded-full border border-slate-300 bg-white px-3 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-700 transition hover:border-cyan-400 hover:text-cyan-700"
                    >
                      <FaEnvelopeOpenText />
                      Mark as read
                    </button>
                  )}
                </div>
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  )
}

export default NotificationsPage
