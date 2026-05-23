import { useEffect, useRef } from 'react'

export function useInfiniteScroll({ enabled, hasMore, isLoading, onLoadMore, rootMargin = '240px' }) {
  const sentinelRef = useRef(null)

  useEffect(() => {
    if (!enabled || !hasMore || isLoading || !sentinelRef.current) {
      return undefined
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          onLoadMore()
        }
      },
      { rootMargin },
    )

    observer.observe(sentinelRef.current)

    return () => observer.disconnect()
  }, [enabled, hasMore, isLoading, onLoadMore, rootMargin])

  return sentinelRef
}
