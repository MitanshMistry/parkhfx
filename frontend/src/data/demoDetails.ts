import { demoListings } from './demoListings'
import type { ListingSummary } from '../api'

export type DemoListingDetail = ListingSummary & {
  description: string
  hostName: string
  hostSince: string
  rating: number
  reviewCount: number
  photoUrls: string[]
  availability: string
}

const descriptions: Record<ListingSummary['spotType'], string> = {
  Driveway: 'A private residential driveway with a straightforward pull-in space, ideal for errands, appointments, and workdays nearby.',
  'Parking pad': 'A dedicated off-street parking pad in a residential neighborhood, giving you an easy place to leave your car while exploring Halifax.',
  Garage: 'A private garage space that keeps your vehicle protected from weather, with convenient access to nearby neighborhoods and transit.',
  Underground: 'A secure underground parking space with weather protection and easy access to downtown destinations.',
  'Condo/apartment': 'A reserved residential building space with dependable access for visitors, commuters, and downtown trips.',
  'Commercial lot': 'A private business parking space available outside peak hours, offering an affordable option close to local shops and offices.',
}

export function getDemoListingDetail(id: string): DemoListingDetail | undefined {
  const listing = demoListings.find((candidate) => candidate.id === id)
  if (!listing) return undefined

  return {
    ...listing,
    description: descriptions[listing.spotType],
    hostName: 'Alex M.',
    hostSince: 'Hosting since 2024',
    rating: 4.9,
    reviewCount: 28,
    photoUrls: [listing.photoUrl, listing.photoUrl, listing.photoUrl].filter((photo): photo is string => photo !== null),
    availability: 'Available Monday–Sunday, 6:00 AM–11:00 PM',
  }
}
