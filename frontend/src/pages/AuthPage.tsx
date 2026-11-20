import { useState } from 'react'
import { Link } from 'react-router-dom'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const isLogin = mode === 'login'
  const [submitted, setSubmitted] = useState(false)
  const [forgotSubmitted, setForgotSubmitted] = useState(false)

  return (
    <main className="auth-page">
      <Link className="brand auth-brand" to="/explore">
        <span className="brand-mark">P</span>
        <span>Park<span>HFX</span></span>
      </Link>
      <section className="auth-card">
        <p className="eyebrow">{isLogin ? 'Welcome back' : 'Join ParkHFX'}</p>
        <h1>{isLogin ? 'Log in to your account.' : 'Create your account.'}</h1>
        <p className="auth-copy">
          {isLogin
            ? 'Save your favorite spots and keep your bookings in one place.'
            : 'Find better parking and make every Halifax trip easier.'}
        </p>
        <form onSubmit={(event) => { event.preventDefault(); setSubmitted(true) }}>
          {!isLogin && <label>Display name<input maxLength={80} required placeholder="Your name" /></label>}
          <label>Email address<input maxLength={254} required type="email" placeholder="you@example.com" /></label>
          <label>Password<input maxLength={72} minLength={8} required type="password" placeholder="8 or more characters" /></label>
          {isLogin && <button className="forgot-link" onClick={() => setForgotSubmitted(true)} type="button">Forgot password?</button>}
          <button className="button button-dark auth-submit" type="submit">
            {isLogin ? 'Log in' : 'Create account'} <span>→</span>
          </button>
        </form>
        {submitted && <p className="auth-demo-alert" role="status">Demo only: no account was created and no credentials were sent.</p>}
        {forgotSubmitted && <p className="auth-demo-alert" role="status">Demo only: password recovery is not connected.</p>}
        <p className="demo-note">Demo mode: account actions are visual only.</p>
        <p className="auth-switch">
          {isLogin ? 'New to ParkHFX?' : 'Already have an account?'}{' '}
          <Link to={isLogin ? '/register' : '/login'}>{isLogin ? 'Create an account' : 'Log in'}</Link>
        </p>
      </section>
    </main>
  )
}
