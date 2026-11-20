import { useState } from 'react'
import { Link } from 'react-router-dom'

export function HowItWorksPage() {
  return (
    <main className="info-page">
      <InfoHeader activePage="how-it-works" />
      <section className="info-hero">
        <p className="eyebrow">Simple by design</p>
        <h1>Parking that fits<br /><em>your day.</em></h1>
        <p>ParkHFX connects drivers with convenient local parking spots across Halifax—without the circling.</p>
      </section>
      <section className="info-steps">
        <InfoStep number="01" title="Find a spot" copy="Search the map or browse nearby listings by neighborhood, price, and features." />
        <InfoStep number="02" title="Choose your time" copy="Pick the date and hours that work for you. You only pay for the time you need." />
        <InfoStep number="03" title="Park with confidence" copy="Get clear directions, host details, and a simple reservation summary before checkout." />
      </section>
      <section className="info-callout">
        <p className="eyebrow">Built for Halifax</p>
        <h2>More time exploring.<br />Less time searching.</h2>
        <Link className="button button-coral" to="/explore">Explore parking <span>→</span></Link>
      </section>
    </main>
  )
}

export function HostPage() {
  return (
    <main className="info-page">
      <InfoHeader activePage="host" />
      <section className="info-hero host-hero">
        <p className="eyebrow">Turn space into opportunity</p>
        <h1>Have a spot?<br /><em>Share it.</em></h1>
        <p>Help drivers park closer while earning from a driveway, garage, or unused parking space.</p>
        <Link className="button button-dark" to="/register">Get started <span>→</span></Link>
      </section>
      <section className="host-benefits">
        <div><strong>Set your own price</strong><span>You decide when and how much your spot earns.</span></div>
        <div><strong>Stay in control</strong><span>Choose your availability and update it whenever you like.</span></div>
        <div><strong>Meet local demand</strong><span>Make parking easier for Halifax drivers.</span></div>
      </section>
      <p className="demo-note host-demo-note">Demo mode: host onboarding is visual only.</p>
    </main>
  )
}

function InfoHeader({ activePage }: { activePage: 'how-it-works' | 'host' }) {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  return (
    <header className="topbar">
      <Link className="brand" to="/explore"><span className="brand-mark">P</span><span>Park<span>HFX</span></span></Link>
      <nav className={`main-nav ${mobileNavOpen ? 'mobile-open' : ''}`} aria-label="Main navigation">
        <Link onClick={() => setMobileNavOpen(false)} to="/explore">Explore</Link>
        <Link className={activePage === 'how-it-works' ? 'active' : ''} onClick={() => setMobileNavOpen(false)} to="/how-it-works">How it works</Link>
        <Link className={activePage === 'host' ? 'active' : ''} onClick={() => setMobileNavOpen(false)} to="/host">List your spot</Link>
      </nav>
      <button
        aria-expanded={mobileNavOpen}
        aria-label={mobileNavOpen ? 'Close navigation' : 'Open navigation'}
        className="mobile-menu-button"
        onClick={() => setMobileNavOpen((open) => !open)}
        type="button"
      >{mobileNavOpen ? '×' : '☰'}</button>
      <div className="account-actions">
        <Link className="link-button" to="/login">Log in</Link>
        <Link className="button button-dark" to="/login">Demo login <span>↗</span></Link>
      </div>
    </header>
  )
}

function InfoStep({ number, title, copy }: { number: string; title: string; copy: string }) {
  return <article className="info-step"><span>{number}</span><h2>{title}</h2><p>{copy}</p></article>
}
