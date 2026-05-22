import { FaEnvelope, FaShieldHalved, FaUser } from 'react-icons/fa6'
import { useAuth } from '../hooks/useAuth'

function ProfilePage() {
  const { user } = useAuth()

  return (
    <section className="space-y-6">
      <div className="rounded-[30px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.12)] sm:p-8">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
            <FaUser />
          </div>
          <div>
            <p className="text-sm font-medium text-cyan-700">Profile</p>
            <h1 className="text-3xl font-semibold tracking-tight text-slate-950">Account details</h1>
          </div>
        </div>

        <div className="mt-8 grid gap-4 md:grid-cols-2">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Full Name</p>
            <p className="mt-3 text-lg font-semibold text-slate-950">{user?.fullName || 'Unknown user'}</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Email</p>
            <p className="mt-3 text-lg font-semibold text-slate-950">{user?.email}</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Verification</p>
            <p className="mt-3 text-lg font-semibold text-slate-950">{user?.emailVerified ? 'Verified' : 'Not verified'}</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Roles</p>
            <div className="mt-3 flex flex-wrap gap-2">
              {(user?.roles || []).map((role) => (
                <span
                  key={role}
                  className="inline-flex items-center gap-2 rounded-full border border-slate-300 bg-white px-3 py-1 text-sm font-semibold text-slate-700"
                >
                  <FaShieldHalved className="text-xs" />
                  {role}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.1)]">
          <div className="flex items-center gap-3 text-slate-950">
            <FaEnvelope className="text-cyan-700" />
            <h2 className="text-xl font-semibold tracking-tight">Identity</h2>
          </div>
          <p className="mt-4 text-sm leading-7 text-slate-600">
            Your account is the basis for notification delivery, SSE subscriptions, and role-based access.
          </p>
        </div>

        <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.1)]">
          <div className="flex items-center gap-3 text-slate-950">
            <FaShieldHalved className="text-cyan-700" />
            <h2 className="text-xl font-semibold tracking-tight">Access</h2>
          </div>
          <p className="mt-4 text-sm leading-7 text-slate-600">
            Admin accounts can manage outbound notifications, while user accounts receive and track them.
          </p>
        </div>
      </div>
    </section>
  )
}

export default ProfilePage
