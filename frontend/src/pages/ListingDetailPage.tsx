import { useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { CircleMarker, MapContainer, TileLayer } from 'react-leaflet'
import { getDemoListingDetail } from '../data/demoDetails'
import parkhfxMark from '../assets/parkhfx-mark.svg'
import parkingPlaceholder from '../assets/parking-placeholder.svg'

export function ListingDetailPage() {
  const { id } = useParams()
  const listing = id ? getDemoListingDetail(id) : undefined
  const [saved, setSaved] = useState(false)
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  if (!listing) return <Navigate replace to="/explore" />

  return (
    <main className="detail-page">
      <header className="detail-header">
        <Link className="brand" to="/explore">
          <img alt="" className="brand-mark" src={parkhfxMark} />
          <span>Park<span>HFX</span></span>
        </Link>
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
        <Link className="back-link" to="/explore">← Back to explore</Link>
        <Link className="button button-dark" to="/login">Log in</Link>
      </header>

      <div className="detail-content">
        <div className="detail-breadcrumb"><Link to="/explore">Explore</Link><span>/</span>{listing.neighborhood}</div>
        <div className="photo-gallery">
          <img className="gallery-main" onError={(event) => { event.currentTarget.src = parkingPlaceholder }} src={listing.photoUrls[0]} alt={listing.title} />
          <img onError={(event) => { event.currentTarget.src = parkingPlaceholder }} src={listing.photoUrls[1]} alt="" />
          <img onError={(event) => { event.currentTarget.src = parkingPlaceholder }} src={listing.photoUrls[2]} alt="" />
        </div>

        <div className="detail-grid">
          <article>
            <div className="detail-title-row">
              <div>
                <p className="eyebrow">{listing.spotType} · {listing.neighborhood}</p>
                <h1>{listing.title}</h1>
                <p className="detail-address">{listing.address} · Halifax, NS</p>
              </div>
              <button aria-pressed={saved} className={`save-button ${saved ? 'saved' : ''}`} onClick={() => setSaved((current) => !current)} type="button">{saved ? '♥ Saved' : '♡ Save'}</button>
            </div>
            <div className="detail-rating">★ {listing.rating} <span>· {listing.reviewCount} reviews</span></div>
            <p className="detail-description">{listing.description}</p>

            <div className="detail-facts">
              <div><span className="fact-icon">▣</span><strong>{listing.covered ? 'Covered' : 'Outdoor'} parking</strong><span>Protected spot</span></div>
              <div><span className="fact-icon">↔</span><strong>{listing.vehicleSize} vehicle</strong><span>Space size</span></div>
              <div><span className="fact-icon">⚡</span><strong>{listing.evCharger ? 'EV charger' : 'Easy access'}</strong><span>{listing.evCharger ? 'Available on site' : 'Pull in and park'}</span></div>
            </div>

            <section className="detail-section">
              <h2>About this spot</h2>
              <p>{listing.availability}. The spot is located in a quiet, well-lit area with easy access to nearby businesses and transit.</p>
            </section>

            <section className="detail-section">
              <h2>Meet your host</h2>
              <div className="host-card">
                <div className="host-avatar">AM</div>
                <div><strong>{listing.hostName}</strong><span>{listing.hostSince} · {listing.rating} rating</span></div>
                <span className="host-badge">Verified host</span>
              </div>
            </section>

            <section className="detail-section">
              <h2>Where you’ll park</h2>
              <p>{listing.address}, {listing.neighborhood}, Halifax</p>
              <MapContainer className="detail-map" center={[listing.latitude, listing.longitude]} zoom={16} zoomControl={false} scrollWheelZoom={false}>
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <CircleMarker center={[listing.latitude, listing.longitude]} pathOptions={{ color: '#fff', fillColor: '#a66a3f', fillOpacity: 1, weight: 3 }} radius={12} />
              </MapContainer>
            </section>
          </article>

          <aside className="booking-card">
            <div className="booking-price"><strong>${(listing.hourlyRateCents / 100).toFixed(0)}</strong> <span>/ hour</span></div>
            <p className="booking-note">You won’t be charged yet</p>
            <div className="booking-row"><span>Availability</span><strong>Open today</strong></div>
            <div className="booking-row"><span>Daily maximum</span><strong>${(listing.dailyMaxCents / 100).toFixed(0)}</strong></div>
            <Link className="button button-coral booking-cta" to={`/listings/${listing.id}/book`}>Choose a time <span>→</span></Link>
            <p className="booking-footnote">Free cancellation before your booking starts.</p>
          </aside>
        </div>
      </div>
    </main>
  )
}
