import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FaBell, FaEnvelope, FaEye, FaEyeSlash, FaKey, FaLock } from 'react-icons/fa6'
import { resetUserPassword, sendOtpForPasswordReset } from '../../services/authService'
import { getApiErrorMessage } from '../../utils/apiError'

function ForgotPasswordForm() {
  const navigate = useNavigate()
  const [step, setStep] = useState('request')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [formData, setFormData] = useState({
    email: '',
    otp: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    setFormData((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  const handleRequestOtp = async (event) => {
    event.preventDefault()
    setError('')
    setMessage('')
    setIsSubmitting(true)

    try {
      await sendOtpForPasswordReset({ email: formData.email })
      setStep('reset')
      setMessage('OTP sent to your email. Enter it below to reset your password.')
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to send reset OTP. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleResetPassword = async (event) => {
    event.preventDefault()
    setError('')
    setMessage('')

    if (formData.newPassword !== formData.confirmPassword) {
      setError('Password and confirm password must match.')
      return
    }

    setIsSubmitting(true)

    try {
      await resetUserPassword({
        email: formData.email,
        otp: formData.otp,
        newPassword: formData.newPassword,
      })
      navigate('/login', {
        replace: true,
        state: { resetMessage: 'Password reset successful. Please login with your new password.' },
      })
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to reset password. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-6 text-slate-100 sm:px-6 lg:px-8">
      <section className="mx-auto grid min-h-[calc(100vh-3rem)] w-full max-w-6xl items-center gap-8 lg:grid-cols-[0.95fr_1.05fr] xl:gap-14">
        <div className="hidden lg:block">
          <div className="max-w-md">
            <div className="mb-8 flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-400 text-slate-950">
              <FaBell className="text-2xl" />
            </div>
            <h1 className="text-4xl font-semibold leading-tight xl:text-5xl">Reset your password without losing momentum.</h1>
            <p className="mt-5 text-base leading-7 text-slate-300 xl:text-lg">
              Request an email OTP, confirm it, and set a new password in one short flow.
            </p>
          </div>
        </div>

        <div className="mx-auto w-full max-w-md rounded-2xl border border-slate-800 bg-white p-5 text-slate-950 shadow-2xl shadow-cyan-950/30 sm:p-7 md:max-w-lg lg:max-w-xl lg:p-9">
          <div className="mb-8">
            <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-100 text-cyan-700 lg:hidden">
              <FaBell className="text-xl" />
            </div>
            <p className="text-sm font-medium text-cyan-700">Password reset</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">
              {step === 'request' ? 'Request reset OTP' : 'Create a new password'}
            </h2>
            <p className="mt-2 text-sm text-slate-500 sm:text-base">
              {step === 'request'
                ? 'Enter your email to receive a one-time password.'
                : 'Use the OTP from your email and choose a new password.'}
            </p>
          </div>

          <form className="space-y-5" onSubmit={step === 'request' ? handleRequestOtp : handleResetPassword}>
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
                  disabled={step === 'reset'}
                  className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100 disabled:bg-slate-50"
                />
              </span>
            </label>

            {step === 'reset' && (
              <>
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
                      required
                      className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                    />
                  </span>
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">New password</span>
                  <span className="relative block">
                    <FaLock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      name="newPassword"
                      type={showPassword ? 'text' : 'password'}
                      value={formData.newPassword}
                      onChange={updateField}
                      placeholder="Enter new password"
                      required
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

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Confirm new password</span>
                  <span className="relative block">
                    <FaLock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      name="confirmPassword"
                      type={showConfirmPassword ? 'text' : 'password'}
                      value={formData.confirmPassword}
                      onChange={updateField}
                      placeholder="Confirm new password"
                      required
                      className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-12 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((value) => !value)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-950"
                      aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                    >
                      {showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
                    </button>
                  </span>
                </label>
              </>
            )}

            {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{error}</p>}
            {message && <p className="rounded-xl bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">{message}</p>}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-400 focus:outline-none focus:ring-4 focus:ring-cyan-200 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSubmitting
                ? step === 'request'
                  ? 'Sending OTP...'
                  : 'Resetting password...'
                : step === 'request'
                  ? 'Send reset OTP'
                  : 'Reset password'}
            </button>
          </form>

          <p className="mt-7 text-center text-sm text-slate-600">
            <Link to="/login" className="font-semibold text-cyan-700 hover:text-cyan-800">
              Back to login
            </Link>
          </p>
        </div>
      </section>
    </main>
  )
}

export default ForgotPasswordForm
