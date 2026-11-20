import { useMemo, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate, useParams } from 'react-router-dom'
import { getDemoListingDetail } from '../data/demoDetails'
import parkingPlaceholder from '../assets/parking-placeholder.svg'

const timeOptions = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00']

function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

export type BookingSelection = {
  date: string
  startTime: string
  endTime: string
  hours: number
  subtotalCents: number
}

export function BookingPage() {
  const { id } = useParams()
  const listing = id ? getDemoListingDetail(id) : undefined
  const navigate = useNavigate()
  const [date, setDate] = useState(tomorrow)
  const [startTime, setStartTime] = useState('10:00')
  const [endTime, setEndTime] = useState('13:00')
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  const hours = Math.max(0, Number(endTime.slice(0, 2)) - Number(startTime.slice(0, 2)))
  const subtotalCents = Math.min(hours * (listing?.hourlyRateCents ?? 0), listing?.dailyMaxCents ?? 0)
  const isValid = Boolean(date && hours > 0)
  const formattedDate = useMemo(() => {
    if (!date) return 'Choose a date'
    return new Intl.DateTimeFormat('en-CA', { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date(`${date}T12:00:00`))
  }, [date])

  if (!listing) return <Navigate replace to="/explore" />
  const selectedListing = listing

  function continueToCheckout() {
    if (!isValid) return
    const selection: BookingSelection = { date, startTime, endTime, hours, subtotalCents }
    navigate(`/listings/${selectedListing.id}/checkout`, { state: selection })
  }

  return (
    <main className="booking-page">
      <header className="detail-header">
        <Link className="brand" to="/explore"><span className="brand-mark">P</span><span>Park<span>HFX</span></span></Link>
        <nav className={`main-nav ${mobileNavOpen ? 'mobile-open' : ''}`} aria-label="Main navigation">
          <Link className="active" onClick={() => setMobileNavOpen(false)} to="/explore">Explore</Link>
          <Link onClick={() => setMobileNavOpen(false)} to="/how-it-works">How it works</Link>
          <Link onClick={() => setMobileNavOpen(false)} to="/host">List your spot</Link>
        </nav>
        <button
          aria-expanded={mobileNavOpen}
          aria-label={mobileNavOpen ? 'Close navigation' : 'Open navigation'}
          className="mobile-menu-button"
          onClick={() => setMobileNavOpen((open) => !open)}
          type="button"
        >☰</button>
        <Link className="back-link" to={`/listings/${listing.id}`}>← Back to listing</Link>
        <span className="demo-pill">DEMO MODE</span>
      </header>
      <div className="booking-layout">
        <section>
          <div className="detail-breadcrumb"><Link to={`/listings/${listing.id}`}>{listing.title}</Link><span>/</span>Choose a time</div>
          <p className="eyebrow">Reserve your spot</p>
          <h1 className="booking-heading">When do you need<br /><em>to park?</em></h1>
          <p className="booking-intro">Choose a time that works for you. This is a demo reservation—no payment is collected.</p>

          <div className="time-form">
            <label>Date<input min={tomorrow()} onChange={(event) => setDate(event.target.value)} type="date" value={date} /></label>
            <div className="time-row">
              <label>Arrive<select onChange={(event) => setStartTime(event.target.value)} value={startTime}>{timeOptions.slice(0, -1).map((time) => <option key={time}>{time}</option>)}</select></label>
              <span className="time-arrow">→</span>
              <label>Leave<select onChange={(event) => setEndTime(event.target.value)} value={endTime}>{timeOptions.slice(1).map((time) => <option key={time}>{time}</option>)}</select></label>
            </div>
            {!isValid && <p className="form-error">Choose an end time after your arrival time.</p>}
          </div>

          <div className="availability-note"><span>✓</span><div><strong>Available for your selected time</strong><small>{listing.availability}</small></div></div>
        </section>

        <aside className="booking-card booking-summary-card">
          <img src={listing.photoUrls[0]} onError={(event) => { event.currentTarget.src = parkingPlaceholder }} alt="" className="summary-image" />
          <div className="summary-listing"><strong>{listing.title}</strong><span>{listing.neighborhood} · {listing.address}</span></div>
          <div className="summary-divider" />
          <div className="booking-row"><span>{formattedDate}</span><strong>{startTime}–{endTime}</strong></div>
          <div className="booking-row"><span>${(listing.hourlyRateCents / 100).toFixed(0)} × {hours || 0} hours</span><strong>${(subtotalCents / 100).toFixed(2)}</strong></div>
          <div className="booking-row total-row"><span>Total</span><strong>${(subtotalCents / 100).toFixed(2)}</strong></div>
          <button className="button button-coral booking-cta" disabled={!isValid} onClick={continueToCheckout} type="button">Continue to checkout <span>→</span></button>
          <p className="booking-footnote">Your spot will be held for 10 minutes in the demo flow.</p>
        </aside>
      </div>
    </main>
  )
}

export function CheckoutPage() {
  const { id } = useParams()
  const listing = id ? getDemoListingDetail(id) : undefined
  const location = useLocation()
  const selection = location.state as BookingSelection | null
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  if (!listing || !selection) return <Navigate replace to={id ? `/listings/${id}/book` : '/explore'} />

  return (
    <main className="booking-page">
      <header className="detail-header">
        <Link className="brand" to="/explore"><span className="brand-mark">P</span><span>Park<span>HFX</span></span></Link>
        <nav className={`main-nav ${mobileNavOpen ? 'mobile-open' : ''}`} aria-label="Main navigation">
          <Link className="active" onClick={() => setMobileNavOpen(false)} to="/explore">Explore</Link>
          <Link onClick={() => setMobileNavOpen(false)} to="/how-it-works">How it works</Link>
          <Link onClick={() => setMobileNavOpen(false)} to="/host">List your spot</Link>
        </nav>
        <button
          aria-expanded={mobileNavOpen}
          aria-label={mobileNavOpen ? 'Close navigation' : 'Open navigation'}
          className="mobile-menu-button"
          onClick={() => setMobileNavOpen((open) => !open)}
          type="button"
        >☰</button>
        <span className="checkout-step">Checkout <span>1</span> — Payment <span>2</span></span>
        <span className="demo-pill">DEMO MODE</span>
      </header>
      <div className="checkout-layout">
        <section>
          <p className="eyebrow">Almost there</p>
          <h1 className="booking-heading">Review your<br /><em>reservation.</em></h1>
          <div className="demo-banner"><span>◎</span><div><strong>This is a demo checkout</strong><small>Everything here is simulated. No payment or reservation will be processed.</small></div></div>
          <div className="checkout-field-group">
            <h2>Driver details</h2>
            <label>Full name<input placeholder="Your name" /></label>
            <label>Email address<input placeholder="you@example.com" type="email" /></label>
          </div>
          <div className="checkout-field-group">
            <h2>Payment</h2>
            <div className="payment-disabled"><span>▣</span><div><strong>Payment is disabled in demo mode</strong><small>Real payments will be added after MVP validation.</small></div></div>
          </div>
        </section>
        <aside className="booking-card checkout-summary">
          <p className="eyebrow">Your reservation</p>
          <div className="summary-listing"><strong>{listing.title}</strong><span>{listing.address}, {listing.neighborhood}</span></div>
          <div className="summary-divider" />
          <div className="booking-row"><span>{selection.date}</span><strong>{selection.startTime}–{selection.endTime}</strong></div>
          <div className="booking-row total-row"><span>Total</span><strong>${(selection.subtotalCents / 100).toFixed(2)}</strong></div>
          <button className="button button-dark booking-cta" disabled type="button">Complete payment <span>→</span></button>
          <Link className="checkout-back" to={`/listings/${listing.id}/book`}>← Edit reservation</Link>
        </aside>
      </div>
    </main>
  )
}
