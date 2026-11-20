import { demoListings } from '../data/demoListings'
import type { ListingSearchParams, ListingSummary } from '../api'

export function searchDemoListings(
  params: ListingSearchParams = {},
  signal?: AbortSignal,
): Promise<ListingSummary[]> {
  return new Promise((resolve, reject) => {
    const timer = globalThis.setTimeout(() => {
      if (signal?.aborted) {
        reject(new DOMException('The demo search was cancelled.', 'AbortError'))
        return
      }

      const results = demoListings.filter((listing) => {
        if (params.minLongitude !== undefined && listing.longitude < params.minLongitude) return false
        if (params.maxLongitude !== undefined && listing.longitude > params.maxLongitude) return false
        if (params.minLatitude !== undefined && listing.latitude < params.minLatitude) return false
        if (params.maxLatitude !== undefined && listing.latitude > params.maxLatitude) return false
        return true
      })
      resolve(results)
    }, 250)

    signal?.addEventListener('abort', () => {
      globalThis.clearTimeout(timer)
      reject(new DOMException('The demo search was cancelled.', 'AbortError'))
    }, { once: true })
  })
}
