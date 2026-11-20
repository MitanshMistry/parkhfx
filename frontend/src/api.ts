export type SpotType = 'Driveway' | 'Parking pad' | 'Garage' | 'Underground' | 'Condo/apartment' | 'Commercial lot'

export type ListingSummary = {
  id: string
  title: string
  address: string
  neighborhood: string
  hourlyRateCents: number
  dailyMaxCents: number
  vehicleSize: 'COMPACT' | 'STANDARD' | 'LARGE'
  covered: boolean
  evCharger: boolean
  latitude: number
  longitude: number
  photoUrl: string | null
  spotType: SpotType
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export type ListingSearchParams = {
  minLongitude?: number
  minLatitude?: number
  maxLongitude?: number
  maxLatitude?: number
  longitude?: number
  latitude?: number
  radiusKm?: number
}

export async function searchListings(
  params: ListingSearchParams = {},
  signal?: AbortSignal,
): Promise<ListingSummary[]> {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) query.set(key, String(value))
  })
  const suffix = query.size > 0 ? `?${query.toString()}` : ''
  const response = await fetch(`${apiBaseUrl}/api/v1/listings${suffix}`, { signal })
  if (!response.ok) {
    throw new Error(`Listing search failed with status ${response.status}.`)
  }
  return response.json() as Promise<ListingSummary[]>
}
