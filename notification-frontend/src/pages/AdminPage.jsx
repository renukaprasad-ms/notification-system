import { useEffect, useMemo, useState } from 'react'
import { FaBell, FaCheck, FaFilter, FaMagnifyingGlass, FaPaperPlane, FaUsers } from 'react-icons/fa6'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useInfiniteScroll } from '../hooks/useInfiniteScroll'
import {
  createNotificationForAll,
  createNotificationForSelected,
} from '../services/notificationService'
import { fetchAllUsers } from '../services/userService'
import { getApiErrorMessage } from '../utils/apiError'

const notificationTypes = ['SYSTEM', 'ACCOUNT', 'SECURITY', 'PROMOTION']
const notificationPriorities = ['LOW', 'NORMAL', 'HIGH', 'URGENT']

function AdminPage() {
  const [deliveryMode, setDeliveryMode] = useState('ALL')
  const [users, setUsers] = useState([])
  const [selectedUserIds, setSelectedUserIds] = useState([])
  const [selectedUsersById, setSelectedUsersById] = useState({})
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({
    title: '',
    message: '',
    type: 'SYSTEM',
    priority: 'NORMAL',
  })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoadingUsers, setIsLoadingUsers] = useState(true)
  const [isLoadingMoreUsers, setIsLoadingMoreUsers] = useState(false)
  const [userPageInfo, setUserPageInfo] = useState({
    page: 0,
    size: 20,
    totalItems: 0,
    totalPages: 0,
    hasNext: false,
  })
  const debouncedSearchTerm = useDebouncedValue(searchTerm, 300)

  useEffect(() => {
    let isMounted = true

    const loadAdminData = async () => {
      try {
        const usersResponse = await fetchAllUsers({
          page: 0,
          size: userPageInfo.size,
          search: debouncedSearchTerm,
        })

        if (!isMounted) {
          return
        }

        setUsers(usersResponse.data?.items || [])
        setUserPageInfo({
          page: usersResponse.data?.page ?? 0,
          size: usersResponse.data?.size ?? userPageInfo.size,
          totalItems: usersResponse.data?.totalItems ?? 0,
          totalPages: usersResponse.data?.totalPages ?? 0,
          hasNext: usersResponse.data?.hasNext ?? false,
        })
        setError('')
      } catch (apiError) {
        if (isMounted) {
          setError(getApiErrorMessage(apiError, 'Unable to load admin data.'))
        }
      } finally {
        if (isMounted) {
          setIsLoadingUsers(false)
          setIsLoadingMoreUsers(false)
        }
      }
    }

    setIsLoadingUsers(true)
    loadAdminData()

    return () => {
      isMounted = false
    }
  }, [debouncedSearchTerm, userPageInfo.size])

  const loadMoreUsers = async () => {
    if (isLoadingUsers || isLoadingMoreUsers || !userPageInfo.hasNext) {
      return
    }

    setIsLoadingMoreUsers(true)

    try {
      const usersResponse = await fetchAllUsers({
        page: userPageInfo.page + 1,
        size: userPageInfo.size,
        search: debouncedSearchTerm,
      })

      const nextItems = usersResponse.data?.items || []
      setUsers((current) => {
        const existingIds = new Set(current.map((user) => user.userId))
        const merged = [...current]
        nextItems.forEach((user) => {
          if (!existingIds.has(user.userId)) {
            merged.push(user)
          }
        })
        return merged
      })
      setUserPageInfo({
        page: usersResponse.data?.page ?? userPageInfo.page + 1,
        size: usersResponse.data?.size ?? userPageInfo.size,
        totalItems: usersResponse.data?.totalItems ?? userPageInfo.totalItems,
        totalPages: usersResponse.data?.totalPages ?? userPageInfo.totalPages,
        hasNext: usersResponse.data?.hasNext ?? false,
      })
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to load more users.'))
    } finally {
      setIsLoadingMoreUsers(false)
    }
  }

  const loadMoreUsersRef = useInfiniteScroll({
    enabled: true,
    hasMore: userPageInfo.hasNext,
    isLoading: isLoadingUsers || isLoadingMoreUsers,
    onLoadMore: loadMoreUsers,
  })

  const updateField = (event) => {
    setFormData((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  const activeUsers = useMemo(() => users.filter((recipient) => recipient.active), [users])
  const selectedRecipients = useMemo(
    () => selectedUserIds.map((userId) => selectedUsersById[userId]).filter(Boolean),
    [selectedUserIds, selectedUsersById],
  )

  const toggleUserSelection = (recipient) => {
    if (deliveryMode !== 'SELECTED') {
      return
    }

    const { userId } = recipient

    setSelectedUserIds((current) =>
      current.includes(userId) ? current.filter((id) => id !== userId) : [...current, userId],
    )

    setSelectedUsersById((current) => {
      if (current[userId]) {
        const next = { ...current }
        delete next[userId]
        return next
      }

      return {
        ...current,
        [userId]: recipient,
      }
    })
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setMessage('')

    if (deliveryMode === 'SELECTED' && selectedUserIds.length === 0) {
      setError('Select at least one user before sending a targeted notification.')
      return
    }

    setIsSubmitting(true)

    try {
      const payload = {
        title: formData.title,
        message: formData.message,
        type: formData.type,
        priority: formData.priority,
      }

      const response =
        deliveryMode === 'ALL'
          ? await createNotificationForAll(payload)
          : await createNotificationForSelected({
              ...payload,
              recipientUserIds: selectedUserIds,
            })

      const recipientCount = response.data?.recipientCount || 0
      setMessage(`Notification sent to ${recipientCount} recipient${recipientCount === 1 ? '' : 's'}.`)
      setFormData({
        title: '',
        message: '',
        type: 'SYSTEM',
        priority: 'NORMAL',
      })
      setSelectedUserIds([])
      setSelectedUsersById({})
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to send notification.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="space-y-6">
      {(error || message) && (
        <div className="grid gap-3 lg:grid-cols-2">
          {error && <p className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{error}</p>}
          {message && (
            <p className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
              {message}
            </p>
          )}
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(420px,0.82fr)_minmax(0,1.18fr)]">
        <form
          onSubmit={handleSubmit}
          className="rounded-[32px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.1)] sm:p-8"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <FaBell />
            </div>
            <div>
              <p className="text-sm font-medium text-cyan-700">Compose</p>
              <h2 className="text-2xl font-semibold tracking-tight text-slate-950">Notification details</h2>
            </div>
          </div>

          <div className="mt-7 grid grid-cols-2 rounded-2xl bg-slate-100 p-1.5">
            <button
              type="button"
              onClick={() => setDeliveryMode('ALL')}
              className={`rounded-xl px-4 py-3 text-sm font-semibold transition ${
                deliveryMode === 'ALL' ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-600 hover:text-slate-950'
              }`}
            >
              Send to all
            </button>
            <button
              type="button"
              onClick={() => setDeliveryMode('SELECTED')}
              className={`rounded-xl px-4 py-3 text-sm font-semibold transition ${
                deliveryMode === 'SELECTED' ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-600 hover:text-slate-950'
              }`}
            >
              Send to selected
            </button>
          </div>

          <div className="mt-7 space-y-5">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Title</span>
              <input
                name="title"
                type="text"
                value={formData.title}
                onChange={updateField}
                placeholder="System maintenance update"
                required
                className="h-12 w-full rounded-2xl border border-slate-300 bg-white px-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
              />
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Message</span>
              <textarea
                name="message"
                value={formData.message}
                onChange={updateField}
                placeholder="Write the notification message here..."
                required
                rows={7}
                className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
              />
            </label>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">Type</span>
                <select
                  name="type"
                  value={formData.type}
                  onChange={updateField}
                  className="h-12 w-full rounded-2xl border border-slate-300 bg-white px-4 text-sm outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                >
                  {notificationTypes.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>

              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">Priority</span>
                <select
                  name="priority"
                  value={formData.priority}
                  onChange={updateField}
                  className="h-12 w-full rounded-2xl border border-slate-300 bg-white px-4 text-sm outline-none transition focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                >
                  {notificationPriorities.map((priority) => (
                    <option key={priority} value={priority}>
                      {priority}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-7 inline-flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-cyan-500 px-4 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-not-allowed disabled:opacity-70"
          >
            <FaPaperPlane />
            {isSubmitting
              ? 'Sending...'
              : deliveryMode === 'ALL'
                ? 'Send to all users'
                : `Send to ${selectedUserIds.length || 0} selected user${selectedUserIds.length === 1 ? '' : 's'}`}
          </button>
        </form>

        <div className="rounded-[32px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.1)] sm:p-8">
          <div className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
                <FaUsers />
              </div>
              <div>
                <p className="text-sm font-medium text-cyan-700">Recipients</p>
                <h2 className="text-2xl font-semibold tracking-tight text-slate-950">Audience preview</h2>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-600">
                <FaFilter />
                {userPageInfo.totalItems} users
              </span>
              {deliveryMode === 'SELECTED' && (
                <span className="inline-flex items-center gap-2 rounded-full bg-cyan-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-cyan-700">
                  <FaCheck />
                  {selectedUserIds.length} selected
                </span>
              )}
            </div>
          </div>

          <div className="mt-5">
            <label className="relative block">
              <FaMagnifyingGlass className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Search by name, email, or role"
                className="h-12 w-full rounded-2xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
              />
            </label>
          </div>

          {deliveryMode === 'ALL' && (
            <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
              This send will go to every active user in the system.
            </div>
          )}

          {deliveryMode === 'SELECTED' && selectedRecipients.length > 0 && (
            <div className="mt-5 rounded-2xl border border-cyan-200 bg-cyan-50 p-4">
              <p className="text-sm font-semibold text-cyan-900">Selected recipients</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {selectedRecipients.map((recipient) => (
                  <span
                    key={recipient.userId}
                    className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-2 text-sm font-medium text-slate-700"
                  >
                    {recipient.fullName}
                    <button
                      type="button"
                      onClick={() => toggleUserSelection(recipient)}
                      className="text-cyan-700 transition hover:text-cyan-900"
                    >
                      x
                    </button>
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="mt-5 grid gap-3">
            {isLoadingUsers ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
                Loading recipients...
              </div>
            ) : activeUsers.length === 0 ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
                No users matched your search.
              </div>
            ) : (
              activeUsers.map((recipient) => {
                const isSelected = selectedUserIds.includes(recipient.userId)

                return (
                  <button
                    key={recipient.userId}
                    type="button"
                    onClick={() => toggleUserSelection(recipient)}
                    disabled={deliveryMode !== 'SELECTED'}
                    className={`rounded-[24px] border p-5 text-left transition ${
                      deliveryMode !== 'SELECTED'
                        ? 'cursor-default border-slate-200 bg-slate-50'
                        : isSelected
                          ? 'border-cyan-500 bg-cyan-50 shadow-[0_18px_36px_rgba(6,182,212,0.14)]'
                          : 'border-slate-200 bg-white hover:border-cyan-300 hover:bg-cyan-50/40'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-base font-semibold text-slate-950">{recipient.fullName}</p>
                          {recipient.roles?.includes('ADMIN') && (
                            <span className="rounded-full bg-slate-950 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-white">
                              Admin
                            </span>
                          )}
                        </div>
                        <p className="mt-1 break-all text-sm text-slate-600">{recipient.email}</p>
                        <p className="mt-3 text-xs font-medium uppercase tracking-[0.12em] text-slate-500">
                          {(recipient.roles || []).join(', ')}
                          {recipient.emailVerified ? ' | Email verified' : ' | Email pending'}
                        </p>
                      </div>

                      <div
                        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-sm ${
                          isSelected
                            ? 'border-cyan-500 bg-cyan-500 text-slate-950'
                            : 'border-slate-300 bg-white text-slate-400'
                        }`}
                      >
                        <FaCheck />
                      </div>
                    </div>
                  </button>
                )
              })
            )}

            {isLoadingMoreUsers && (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
                Loading more users...
              </div>
            )}

            <div ref={loadMoreUsersRef} className="h-2 w-full" />
          </div>
        </div>
      </div>
    </section>
  )
}

export default AdminPage
