import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { FaBell, FaRightFromBracket } from 'react-icons/fa6'
import { logoutUser } from '../services/authService'

function DashboardPage() {
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const handleLogout = async () => {
    setIsLoggingOut(true)

    try {
      await logoutUser()
    } finally {
      setIsLoggingOut(false)
      navigate('/login', { replace: true })
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-6 text-slate-100 sm:px-6 lg:px-8">
      <section className="mx-auto flex min-h-[calc(100vh-3rem)] w-full max-w-5xl flex-col justify-center">
        <div className="rounded-2xl border border-slate-800 bg-white p-6 text-slate-950 shadow-2xl shadow-cyan-950/30 sm:p-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-100 text-cyan-700">
                <FaBell className="text-xl" />
              </div>
              <p className="text-sm font-medium text-cyan-700">Dashboard</p>
              <h1 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl">You are logged in</h1>
              <p className="mt-2 text-sm text-slate-500 sm:text-base">Your auth cookies are being sent with API requests.</p>
            </div>

            <button
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="inline-flex h-12 items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
            >
              <FaRightFromBracket />
              {isLoggingOut ? 'Logging out...' : 'Logout'}
            </button>
          </div>
        </div>
      </section>
    </main>
  )
}

export default DashboardPage
