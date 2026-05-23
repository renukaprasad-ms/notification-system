import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { FaBell, FaChevronRight, FaEnvelope, FaEye, FaEyeSlash, FaKey, FaLock, FaRightFromBracket } from 'react-icons/fa6'
import { sendOtpForLogin } from '../../services/authService'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../utils/apiError'

function LoginForm() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, loginWithPassword, loginWithOtp, logout, user } = useAuth()
  const [loginType, setLoginType] = useState('password')
  const [showPassword, setShowPassword] = useState(false)
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    otp: '',
  })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSendingOtp, setIsSendingOtp] = useState(false)
  const [isSwitchingAccount, setIsSwitchingAccount] = useState(false)

  const updateField = (event) => {
    setFormData((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  const handleSendOtp = async () => {
    setError('')
    setMessage('')

    if (!formData.email) {
      setError('Enter your email before requesting OTP.')
      return
    }

    setIsSendingOtp(true)

    try {
      await sendOtpForLogin({ email: formData.email })
      setMessage('OTP sent to your email.')
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to send OTP. Please try again.'))
    } finally {
      setIsSendingOtp(false)
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setMessage('')
    setIsSubmitting(true)

    try {
      if (loginType === 'password') {
        await loginWithPassword({
          email: formData.email,
          password: formData.password,
        })
      } else {
        await loginWithOtp({
          email: formData.email,
          otp: formData.otp,
        })
      }

      navigate(location.state?.from?.pathname || '/dashboard', { replace: true })
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Login failed. Please check your details.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleContinueToWorkspace = () => {
    navigate('/dashboard', { replace: true })
  }

  const handleSwitchAccount = async () => {
    setIsSwitchingAccount(true)

    try {
      await logout()
      setMessage('Signed out. You can now log in with another account.')
      setError('')
      setFormData({
        email: '',
        password: '',
        otp: '',
      })
      setLoginType('password')
      setShowPassword(false)
      navigate('/login', { replace: true, state: {} })
    } finally {
      setIsSwitchingAccount(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-6 text-slate-100 sm:px-6 lg:px-8">
      <section className="mx-auto grid min-h-[calc(100vh-3rem)] w-full max-w-6xl items-center gap-8 lg:grid-cols-[0.9fr_1.1fr] xl:gap-14">
        <div className="hidden lg:block">
          <div className="max-w-md">
            <div className="mb-8 flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-400 text-slate-950">
              <FaBell className="text-2xl" />
            </div>
            <h1 className="text-4xl font-semibold leading-tight xl:text-5xl">
              Realtime notifications, signed in securely.
            </h1>
            <p className="mt-5 text-base leading-7 text-slate-300 xl:text-lg">
              Access your dashboard with a password or request an OTP when you need a quick login.
            </p>
          </div>
        </div>

        <div className="mx-auto w-full max-w-md rounded-2xl border border-slate-800 bg-white p-5 text-slate-950 shadow-2xl shadow-cyan-950/30 sm:p-7 md:max-w-lg lg:max-w-xl lg:p-9">
          <div className="mb-8">
            <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-100 text-cyan-700 lg:hidden">
              <FaBell className="text-xl" />
            </div>
            <p className="text-sm font-medium text-cyan-700">Welcome back</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">Login to your account</h2>
            <p className="mt-2 text-sm text-slate-500 sm:text-base">Choose password login or OTP login.</p>
          </div>

          {isAuthenticated ? (
            <div className="space-y-5">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <p className="text-sm font-semibold text-slate-950">Already signed in</p>
                <p className="mt-2 text-sm leading-6 text-slate-600">
                  This browser keeps one active session for <span className="font-semibold text-slate-950">notification.localhost</span>.
                  Continue with the current account or sign out to switch to another one.
                </p>
                <div className="mt-4 rounded-2xl bg-white px-4 py-4 shadow-sm">
                  <p className="text-sm font-semibold text-slate-950">{user?.fullName || 'Current account'}</p>
                  <p className="mt-1 break-all text-sm text-slate-500">{user?.email}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {(user?.roles || []).map((role) => (
                      <span
                        key={role}
                        className="rounded-full border border-slate-200 bg-slate-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-600"
                      >
                        {role}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {message && <p className="rounded-xl bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">{message}</p>}

              <button
                type="button"
                onClick={handleContinueToWorkspace}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-400 focus:outline-none focus:ring-4 focus:ring-cyan-200"
              >
                Continue to workspace
                <FaChevronRight />
              </button>

              <button
                type="button"
                onClick={handleSwitchAccount}
                disabled={isSwitchingAccount}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-70"
              >
                <FaRightFromBracket />
                {isSwitchingAccount ? 'Signing out...' : 'Sign out to switch account'}
              </button>
            </div>
          ) : (
            <>
              <div className="mb-6 grid grid-cols-2 rounded-xl bg-slate-100 p-1">
                <button
                  type="button"
                  onClick={() => setLoginType('password')}
                  className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
                    loginType === 'password' ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-600 hover:text-slate-950'
                  }`}
                >
                  Password
                </button>
                <button
                  type="button"
                  onClick={() => setLoginType('otp')}
                  className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
                    loginType === 'otp' ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-600 hover:text-slate-950'
                  }`}
                >
                  OTP
                </button>
              </div>

              <form className="space-y-5" onSubmit={handleSubmit}>
                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Email</span>
                  <span className="relative block">
                    <FaEnvelope className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      name="email"
                      type="email"
                      value={formData.email}
                      onChange={updateField}
                      placeholder="user@example.com"
                      required
                      className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                    />
                  </span>
                </label>

                {loginType === 'password' ? (
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">Password</span>
                    <span className="relative block">
                      <FaLock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                      <input
                        name="password"
                        type={showPassword ? 'text' : 'password'}
                        value={formData.password}
                        onChange={updateField}
                        placeholder="Enter password"
                        required={loginType === 'password'}
                        className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-12 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword((value) => !value)}
                        className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-950"
                        aria-label={showPassword ? 'Hide password' : 'Show password'}
                      >
                        {showPassword ? <FaEyeSlash /> : <FaEye />}
                      </button>
                    </span>
                  </label>
                ) : (
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">OTP</span>
                    <span className="relative block">
                      <FaKey className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                      <input
                        name="otp"
                        type="text"
                        inputMode="numeric"
                        value={formData.otp}
                        onChange={updateField}
                        placeholder="Enter OTP"
                        required={loginType === 'otp'}
                        className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                      />
                    </span>
                  </label>
                )}

                {loginType === 'otp' && (
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    disabled={isSendingOtp}
                    className="w-full rounded-xl border border-cyan-200 px-4 py-3 text-sm font-semibold text-cyan-700 transition hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {isSendingOtp ? 'Sending OTP...' : 'Send OTP'}
                  </button>
                )}

                {loginType === 'password' && (
                  <div className="flex justify-end">
                    <Link to="/forgot-password" className="text-sm font-semibold text-cyan-700 hover:text-cyan-800">
                      Forgot password?
                    </Link>
                  </div>
                )}

                {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{error}</p>}
                {message && <p className="rounded-xl bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">{message}</p>}

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-400 focus:outline-none focus:ring-4 focus:ring-cyan-200 disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {isSubmitting ? 'Logging in...' : 'Login'}
                </button>
              </form>
            </>
          )}

          {!isAuthenticated && (
            <p className="mt-7 text-center text-sm text-slate-600">
              Do not have an account?{' '}
              <Link to="/signup" className="font-semibold text-cyan-700 hover:text-cyan-800">
                Sign up
              </Link>
            </p>
          )}
        </div>
      </section>
    </main>
  )
}

export default LoginForm
