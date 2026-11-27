import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import {
  CircleMarker,
  MapContainer,
  TileLayer,
  Tooltip,
  useMap,
  useMapEvents,
} from 'react-leaflet'
import { Link } from 'react-router-dom'
import type { ListingSearchParams, ListingSummary } from './api'
import { searchDemoListings } from './services/demoService'
import parkhfxMark from './assets/parkhfx-mark.svg'
import parkingPlaceholder from './assets/parking-placeholder.svg'
import 'leaflet/dist/leaflet.css'
import './App.css'

type Listing = ListingSummary & {
  image: string
  distanceKm: number
}

type IconName = 'arrow' | 'calendar' | 'clock' | 'filter' | 'heart' | 'location' | 'menu' | 'search' | 'shield' | 'spark'

function Icon({ name, size = 16 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, ReactNode> = {
    arrow: <><path d="M3 8h12" /><path d="m10 3 5 5-5 5" /></>,
    calendar: <><rect x="3" y="4" width="10" height="10" rx="1.5" /><path d="M6 2v4M10 2v4M3 7h10" /></>,
    clock: <><circle cx="8" cy="8" r="5.5" /><path d="M8 5v3l2 1" /></>,
    filter: <><path d="M2.5 4h11M4.5 8h7M6.5 12h3" /></>,
    heart: <path d="M8 13.5S2.5 10.2 2.5 6.5A3 3 0 0 1 8 5a3 3 0 0 1 5.5 1.5c0 3.7-5.5 7-5.5 7Z" />,
    location: <><path d="M8 14s4-3.6 4-7a4 4 0 1 0-8 0c0 3.4 4 7 4 7Z" /><circle cx="8" cy="7" r="1.3" /></>,
    menu: <><path d="M2.5 4.5h11M2.5 8h11M2.5 11.5h11" /></>,
    search: <><circle cx="7" cy="7" r="4.5" /><path d="m10.5 10.5 3 3" /></>,
    shield: <path d="m8 2 5 2v3.5c0 3.1-2.1 5.3-5 6.5-2.9-1.2-5-3.4-5-6.5V4l5-2Z" />,
    spark: <><path d="m8 2 1.2 4.8L14 8l-4.8 1.2L8 14l-1.2-4.8L2 8l4.8-1.2L8 2Z" /></>,
  }

  return <svg aria-hidden="true" className="icon" fill="none" height={size} viewBox="0 0 16 16" width={size} xmlns="http://www.w3.org/2000/svg">{paths[name]}</svg>
}

const fallbackImage =
  'https://images.unsplash.com/photo-1590674899484-d5640e854abe?auto=format&fit=crop&w=900&q=80'

function toListing(summary: ListingSummary): Listing {
  const latitudeDelta = (summary.latitude - halifaxCenter[0]) * 111
  const longitudeDelta = (summary.longitude - halifaxCenter[1]) * 111 * Math.cos((halifaxCenter[0] * Math.PI) / 180)
  return {
    ...summary,
    distanceKm: Math.sqrt(latitudeDelta ** 2 + longitudeDelta ** 2),
    image: summary.photoUrl ?? fallbackImage,
  }
}

const halifaxCenter: [number, number] = [44.6476, -63.5752]

function MapViewport({
  listings,
  selectedListing,
  onSelect,
  onBoundsChange,
}: {
  listings: Listing[]
  selectedListing: Listing | null
  onSelect: (listing: Listing) => void
  onBoundsChange: (params: ListingSearchParams) => void
}) {
  const map = useMap()
  const refreshTimer = useRef<number | undefined>(undefined)
  const selectedId = selectedListing?.id
  const selectedLatitude = selectedListing?.latitude
  const selectedLongitude = selectedListing?.longitude

  useEffect(() => {
    if (selectedId === undefined || selectedLatitude === undefined || selectedLongitude === undefined) return
    map.panTo([selectedLatitude, selectedLongitude], { animate: true })
  }, [map, selectedId, selectedLatitude, selectedLongitude])

  useMapEvents({
    moveend: () => {
      window.clearTimeout(refreshTimer.current)
      refreshTimer.current = window.setTimeout(() => {
        const bounds = map.getBounds()
        onBoundsChange({
          minLongitude: bounds.getWest(),
          minLatitude: bounds.getSouth(),
          maxLongitude: bounds.getEast(),
          maxLatitude: bounds.getNorth(),
        })
      }, 350)
    },
  })

  useEffect(() => () => window.clearTimeout(refreshTimer.current), [])

  return (
    <>
      {listings.map((listing) => (
        <CircleMarker
          center={[listing.latitude, listing.longitude]}
          eventHandlers={{ click: () => onSelect(listing) }}
          key={listing.id}
          pathOptions={{
            color: '#fff',
            fillColor: selectedListing?.id === listing.id ? '#a66a3f' : '#075b63',
            fillOpacity: 1,
            weight: 3,
          }}
          radius={selectedListing?.id === listing.id ? 11 : 8}
        >
          <Tooltip>{listing.title}</Tooltip>
        </CircleMarker>
      ))}
    </>
  )
}

function MapPanel({
  listings,
  selectedListing,
  onSelect,
  onBoundsChange,
}: {
  listings: Listing[]
  selectedListing: Listing | null
  onSelect: (listing: Listing) => void
  onBoundsChange: (params: ListingSearchParams) => void
}) {
  const [mapError, setMapError] = useState(false)

  return (
    <div className="map-shell">
      <MapContainer
        className="map-canvas"
        center={halifaxCenter}
        zoom={13}
        zoomControl
        scrollWheelZoom
        aria-label="OpenStreetMap of Halifax listings"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          eventHandlers={{ tileerror: () => setMapError(true) }}
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapViewport
          listings={listings}
          onBoundsChange={onBoundsChange}
          onSelect={onSelect}
          selectedListing={selectedListing}
        />
      </MapContainer>
      {mapError && <p className="map-error" role="status">Map tiles are unavailable. Use the listing cards to browse spots.</p>}
    </div>
  )
}

function App() {
  const [listings, setListings] = useState<Listing[]>([])
  const [selectedListing, setSelectedListing] = useState<Listing | null>(null)
  const [search, setSearch] = useState('')
  const [searchDate, setSearchDate] = useState('')
  const [searchTime, setSearchTime] = useState('10:00')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [coveredOnly, setCoveredOnly] = useState(false)
  const [evOnly, setEvOnly] = useState(false)
  const [savedIds, setSavedIds] = useState<Set<string>>(new Set())
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const requestController = useRef<AbortController | null>(null)
  const listingsRef = useRef<HTMLDivElement>(null)

  const loadListings = useCallback((params: ListingSearchParams = {}) => {
    requestController.current?.abort()
    const controller = new AbortController()
    requestController.current = controller
    setIsLoading(true)
    setError(null)
    searchDemoListings(params, controller.signal)
      .then((results) => {
        const nextListings = results.map(toListing)
        setListings(nextListings)
        setSelectedListing((current) =>
          current
            ? nextListings.find((listing) => listing.id === current.id) ?? null
            : nextListings[0] ?? null,
        )
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof Error ? cause.message : 'Unable to load listings.')
      })
      .finally(() => {
        if (requestController.current === controller) setIsLoading(false)
      })
  }, [])

  useEffect(() => {
    // The initial listing request is an external synchronization, not derived state.
    // oxlint-disable-next-line react/set-state-in-effect
    loadListings()
    return () => requestController.current?.abort()
  }, [loadListings])

  const visibleListings = listings.filter((listing) =>
    `${listing.title} ${listing.neighborhood} ${listing.address}`
      .toLowerCase()
      .includes(search.toLowerCase())
      && (!coveredOnly || listing.covered)
      && (!evOnly || listing.evCharger),
  )

  function toggleSaved(id: string) {
    setSavedIds((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/explore">
          <img alt="" className="brand-mark" src={parkhfxMark} />
          <span>Park<span>HFX</span></span>
        </Link>
        <nav className={`main-nav ${mobileNavOpen ? 'mobile-open' : ''}`} aria-label="Main navigation">
          <a className="active" href="#explore" onClick={() => setMobileNavOpen(false)}>Explore</a>
          <Link onClick={() => setMobileNavOpen(false)} to="/how-it-works">How it works</Link>
          <Link onClick={() => setMobileNavOpen(false)} to="/host">List your spot</Link>
        </nav>
        <button
          aria-expanded={mobileNavOpen}
          aria-label={mobileNavOpen ? 'Close navigation' : 'Open navigation'}
          className="mobile-menu-button"
          onClick={() => setMobileNavOpen((open) => !open)}
          type="button"
        ><Icon name="menu" /></button>
        <div className="account-actions">
          <Link className="link-button" to="/login">Log in</Link>
          <Link className="button button-dark" to="/login">Try demo <Icon name="arrow" /></Link>
        </div>
      </header>

      <section className="hero-section" id="explore">
        <div className="hero-copy-block">
          <p className="eyebrow">Parking, made local</p>
          <h1>Find your spot<br /><em>in Halifax.</em></h1>
          <p className="hero-copy">Skip the circling. Discover convenient, affordable parking from local hosts.</p>
        </div>
        <div className="search-panel">
          <div className="search-fields">
            <label className="search-field">
              <Icon name="location" />
              <span><small>Destination</small><input
                aria-label="Search by neighborhood or address"
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Neighborhood or address"
                value={search}
              /></span>
            </label>
            <label className="search-field search-field-compact">
              <Icon name="calendar" />
              <span><small>Date</small><input aria-label="Parking date" onChange={(event) => setSearchDate(event.target.value)} type="date" value={searchDate} /></span>
            </label>
            <label className="search-field search-field-compact">
              <Icon name="clock" />
              <span><small>Arrive</small><input aria-label="Arrival time" onChange={(event) => setSearchTime(event.target.value)} type="time" value={searchTime} /></span>
            </label>
          </div>
          <button className="button button-coral" onClick={() => listingsRef.current?.scrollIntoView({ behavior: 'smooth' })} type="button">Search spots <Icon name="arrow" /></button>
        </div>
      </section>

      <section className="explore-layout">
        <div className="listings-column" ref={listingsRef}>
          <div className="section-heading">
            <div>
              <p className="eyebrow">Near you</p>
              <h2>{search ? `Spots near ${search}` : 'Available in Halifax'} <span aria-live="polite">({visibleListings.length})</span></h2>
            </div>
            <button aria-expanded={filtersOpen} className="filter-button" onClick={() => setFiltersOpen((open) => !open)} type="button"><Icon name="filter" /> Filters</button>
          </div>
          {filtersOpen && (
            <div className="filter-panel">
              <label><input checked={coveredOnly} onChange={(event) => setCoveredOnly(event.target.checked)} type="checkbox" /> Covered parking</label>
              <label><input checked={evOnly} onChange={(event) => setEvOnly(event.target.checked)} type="checkbox" /> EV charger</label>
            </div>
          )}
          {isLoading && <p className="status-message">Finding parking spots near you…</p>}
          {error && <p className="status-message error-message">{error}</p>}
          {!isLoading && !error && (
            <div className="listing-grid">
              {visibleListings.map((listing) => (
                <article
                  aria-current={selectedListing?.id === listing.id}
                  className={`listing-card ${selectedListing?.id === listing.id ? 'selected' : ''}`}
                  key={listing.id}
                  onClick={() => setSelectedListing(listing)}
                >
                  <div className="listing-image-wrap">
                    <img src={listing.image} onError={(event) => { event.currentTarget.src = parkingPlaceholder }} alt="" className="listing-image" />
                    <span className="listing-badge">Available now</span>
                    <button
                      aria-label={`${savedIds.has(listing.id) ? 'Remove' : 'Save'} ${listing.title}`}
                      className={`heart-button ${savedIds.has(listing.id) ? 'saved' : ''}`}
                      onClick={(event) => { event.stopPropagation(); toggleSaved(listing.id) }}
                      type="button"
                    ><Icon name="heart" /></button>
                  </div>
                  <div className="listing-content">
                    <div className="listing-title-row">
                      <h3>{listing.title}</h3>
                      <span className="vehicle-size">{listing.spotType}</span>
                    </div>
                    <p className="muted">{listing.neighborhood} · {listing.address}</p>
                    <div className="listing-meta">
                      <span><Icon name="location" /> {listing.distanceKm.toFixed(1)} km away</span>
                      <span><Icon name="shield" /> Verified local host</span>
                    </div>
                    <div className="listing-footer">
                      <span className="price"><strong>${(listing.hourlyRateCents / 100).toFixed(0)}</strong> / hour</span>
                      <span className="amenities">{listing.covered ? 'Covered' : 'Outdoor'} {listing.evCharger ? '· EV' : ''}</span>
                    </div>
                    <Link className="listing-detail-link" to={`/listings/${listing.id}`}>View spot <Icon name="arrow" /></Link>
                  </div>
                </article>
              ))}
            </div>
          )}
          {!isLoading && !error && visibleListings.length === 0 && (
            <p className="empty-state">No spots match that search yet.</p>
          )}
        </div>
        <aside className="map-panel">
          <MapPanel
            listings={visibleListings}
            onBoundsChange={loadListings}
            onSelect={setSelectedListing}
            selectedListing={selectedListing}
          />
          <div className="map-caption">
            <span><i className="live-dot" /> Live availability</span>
            <span>Halifax, NS</span>
          </div>
        </aside>
      </section>

      <section className="trust-strip" id="how-it-works">
        <div><span className="trust-icon"><Icon name="clock" /></span><strong>Book by the hour</strong><span>Only pay for what you need</span></div>
        <div><span className="trust-icon"><Icon name="shield" /></span><strong>Hosts you can trust</strong><span>Real local listings</span></div>
        <div><span className="trust-icon"><Icon name="spark" /></span><strong>Built for Halifax</strong><span>Park closer to your day</span></div>
      </section>
    </main>
  )
}

export default App
