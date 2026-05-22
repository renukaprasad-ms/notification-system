import { Link } from 'react-router-dom'
import { FaBell, FaShieldHalved, FaUser } from 'react-icons/fa6'
import { useAuth } from '../hooks/useAuth'

function DashboardPage() {
  const { user, hasRole } = useAuth()

  return (
    <section className="space-y-6">
      <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.12)] sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <FaBell className="text-xl" />
            </div>
            <p className="text-sm font-medium text-cyan-700">Dashboard</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">You are logged in</h1>
            <p className="mt-2 text-sm text-slate-500 sm:text-base">
              {user?.email} is signed in with {user?.roles?.join(', ') || 'no'} role access.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            {hasRole(['ADMIN']) && (
              <Link
                to="/admin"
                className="inline-flex h-12 items-center justify-center gap-2 rounded-2xl border border-slate-300 px-5 text-sm font-bold text-slate-700 transition hover:bg-slate-50"
              >
                <FaShieldHalved />
                Admin
              </Link>
            )}

            <Link
              to="/profile"
              className="inline-flex h-12 items-center justify-center gap-2 rounded-2xl bg-slate-950 px-5 text-sm font-bold text-white transition hover:bg-slate-800"
            >
              <FaUser />
              Profile
            </Link>
          </div>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Role Access</p>
          <p className="mt-3 text-2xl font-semibold text-slate-950">{user?.roles?.length || 0}</p>
          <p className="mt-2 text-sm text-slate-600">Active roles attached to this account.</p>
        </div>
        <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Email Status</p>
          <p className="mt-3 text-2xl font-semibold text-slate-950">{user?.emailVerified ? 'Verified' : 'Pending'}</p>
          <p className="mt-2 text-sm text-slate-600">Used for login OTP and account recovery flows.</p>
        </div>
        <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)]">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Workspace</p>
          <p className="mt-3 text-2xl font-semibold text-slate-950">{hasRole(['ADMIN']) ? 'Admin enabled' : 'User mode'}</p>
          <p className="mt-2 text-sm text-slate-600">Your sidebar adapts to the permissions on this account.</p>
        </div>
      </div>
    </section>
  )
}

export default DashboardPage
