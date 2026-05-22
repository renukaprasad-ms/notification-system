import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FaBell, FaEnvelope, FaEye, FaEyeSlash, FaLock, FaUser } from 'react-icons/fa6'
import { signupUser } from '../../services/authService'

function RegisterForm() {
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    setFormData((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (formData.password !== formData.confirmPassword) {
      setError('Password and confirm password must match.')
      return
    }

    setIsSubmitting(true)

    try {
      await signupUser({
        email: formData.email,
        password: formData.password,
        fullName: formData.fullName,
      })

      navigate('/dashboard', { replace: true })
    } catch (apiError) {
      setError(apiError.response?.data?.message || 'Signup failed. Please check your details.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-6 text-slate-100 sm:px-6 lg:px-8">
      <section className="mx-auto grid min-h-[calc(100vh-3rem)] w-full max-w-6xl items-center gap-8 lg:grid-cols-[1.1fr_0.9fr] xl:gap-14">
        <div className="mx-auto w-full max-w-md rounded-2xl border border-slate-800 bg-white p-5 text-slate-950 shadow-2xl shadow-cyan-950/30 sm:p-7 md:max-w-lg lg:max-w-xl lg:p-9">
          <div className="mb-8">
            <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-100 text-cyan-700 lg:hidden">
              <FaBell className="text-xl" />
            </div>
            <p className="text-sm font-medium text-cyan-700">Create account</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">Sign up for notifications</h2>
            <p className="mt-2 text-sm text-slate-500 sm:text-base">Use your name, email, and a secure password.</p>
          </div>

          <form className="space-y-5" onSubmit={handleSubmit}>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Full name</span>
              <span className="relative block">
                <FaUser className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  name="fullName"
                  type="text"
                  value={formData.fullName}
                  onChange={updateField}
                  placeholder="Test User"
                  required
                  className="h-12 w-full rounded-xl border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-100"
                />
              </span>
            </label>

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

            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700">Password</span>
              <span className="relative block">
                <FaLock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={updateField}
                  placeholder="Password@123"
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
              <span className="mb-2 block text-sm font-medium text-slate-700">Confirm password</span>
              <span className="relative block">
                <FaLock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={formData.confirmPassword}
                  onChange={updateField}
                  placeholder="Confirm password"
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

            {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{error}</p>}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-400 focus:outline-none focus:ring-4 focus:ring-cyan-200 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSubmitting ? 'Creating account...' : 'Create account'}
            </button>
          </form>

          <p className="mt-7 text-center text-sm text-slate-600">
            Already have an account?{' '}
            <Link to="/login" className="font-semibold text-cyan-700 hover:text-cyan-800">
              Login
            </Link>
          </p>
        </div>

        <div className="hidden lg:flex lg:justify-end">
          <div className="max-w-md">
            <div className="mb-8 flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-400 text-slate-950">
              <FaBell className="text-2xl" />
            </div>
            <h1 className="text-4xl font-semibold leading-tight xl:text-5xl">
              Create your notification workspace.
            </h1>
            <p className="mt-5 text-base leading-7 text-slate-300 xl:text-lg">
              This signup form matches the user payload with full name, email, password, and confirmation.
            </p>
          </div>
        </div>
      </section>
    </main>
  )
}

export default RegisterForm
