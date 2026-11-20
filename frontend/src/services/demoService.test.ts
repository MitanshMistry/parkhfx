// Runtime validation coverage for: local demo search and listing detail behavior.
import { describe, expect, it } from 'vitest'
import { demoListings } from '../data/demoListings'
import { getDemoListingDetail } from '../data/demoDetails'
import { searchDemoListings } from './demoService'

describe('demo listing experience', () => {
  it('returns all local listings without backend access', async () => {
    await expect(searchDemoListings()).resolves.toHaveLength(demoListings.length)
  })

  it('filters listings by map bounds', async () => {
    const results = await searchDemoListings({
      minLatitude: 44.65,
      maxLatitude: 44.67,
      minLongitude: -63.61,
      maxLongitude: -63.58,
    })

    expect(results.length).toBeGreaterThan(0)
    expect(results.every((listing) => listing.latitude >= 44.65 && listing.latitude <= 44.67)).toBe(true)
  })

  it('resolves a complete listing detail record', () => {
    const detail = getDemoListingDetail('downtown-1')

    expect(detail).toMatchObject({
      id: 'downtown-1',
      hostName: 'Alex M.',
      reviewCount: 28,
    })
    expect(detail?.photoUrls.length).toBeGreaterThanOrEqual(3)
  })
})
