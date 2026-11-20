import type { ListingSummary } from '../api'

const photos = [
  'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1486006920555-c77dcf18193c?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1590674899484-d5640e854abe?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1621929747188-0b4dc28498d2?auto=format&fit=crop&w=900&q=80',
]
const photoByType: Record<ListingSummary['spotType'], string> = {
  Driveway: photos[0],
  'Parking pad': photos[1],
  Garage: photos[2],
  Underground: photos[3],
  'Condo/apartment': photos[4],
  'Commercial lot': photos[5],
}
const listingPhotoOverrides: Record<string, string> = {
  'central-1': 'https://images.unsplash.com/photo-1605146769289-440113cc3d00?auto=format&fit=crop&w=900&q=80',
}

export const demoListings: ListingSummary[] = [
  ['downtown-1', 'Barrington Private Driveway', 'Barrington Street 184', 'Downtown Halifax', 300, false, false, 44.6476, -63.5752, 'Driveway'],
  ['central-1', 'Quinpool Home Driveway', 'Quinpool Road 120', 'Central Halifax', 325, false, false, 44.6461, -63.6082, 'Driveway'],
  ['north-end-1', 'Gottingen Parking Pad', 'Gottingen Street 52', 'North End', 400, false, true, 44.6598, -63.5944, 'Parking pad'],
  ['north-end-2', 'Isleville Parking Pad', 'Isleville Street 77', 'North End', 275, false, true, 44.6648, -63.6041, 'Parking pad'],
  ['south-end-1', 'Spring Garden Garage', 'Spring Garden Road 91', 'South End', 500, true, true, 44.6389, -63.5759, 'Garage'],
  ['dartmouth-1', 'Ferry-Friendly Garage', 'Portland Street 18', 'Dartmouth', 250, true, false, 44.6653, -63.5676, 'Garage'],
  ['downtown-2', 'Robie Underground Parking', 'Robie Street 210', 'Downtown Halifax', 350, true, false, 44.6512, -63.5902, 'Underground'],
  ['south-end-2', 'Queen Street Underground', 'Queen Street 44', 'South End', 450, true, false, 44.6327, -63.5793, 'Underground'],
  ['downtown-3', 'Harbourview Condo Spot', 'Hollis Street 158', 'Downtown Halifax', 425, true, true, 44.6425, -63.5708, 'Condo/apartment'],
  ['north-end-3', 'North End Condo Space', 'Young Street 26', 'North End', 300, true, false, 44.6574, -63.6031, 'Condo/apartment'],
  ['downtown-4', 'After-Hours Office Lot', 'Morris Street 35', 'Downtown Halifax', 275, false, false, 44.6405, -63.5688, 'Commercial lot'],
  ['central-2', 'Quinpool Business Lot', 'Quinpool Road 260', 'Central Halifax', 250, false, false, 44.6468, -63.6143, 'Commercial lot'],
].map(([id, title, address, neighborhood, hourlyRateCents, covered, evCharger, latitude, longitude, spotType], index) => ({
  id: id as string,
  title: title as string,
  address: address as string,
  neighborhood: neighborhood as string,
  hourlyRateCents: hourlyRateCents as number,
  dailyMaxCents: (hourlyRateCents as number) * 5,
  vehicleSize: index % 3 === 0 ? 'LARGE' : index % 2 === 0 ? 'STANDARD' : 'COMPACT',
  covered: covered as boolean,
  evCharger: evCharger as boolean,
  latitude: latitude as number,
  longitude: longitude as number,
  photoUrl: listingPhotoOverrides[id as string] ?? photoByType[spotType as ListingSummary['spotType']],
  spotType: spotType as ListingSummary['spotType'],
}))
