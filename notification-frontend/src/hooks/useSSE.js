import { useEffect, useRef } from 'react'

export function useSSE({ enabled, createConnection, onOpen, onError, handlers }) {
  const handlersRef = useRef(handlers)
  const onOpenRef = useRef(onOpen)
  const onErrorRef = useRef(onError)

  useEffect(() => {
    handlersRef.current = handlers
  }, [handlers])

  useEffect(() => {
    onOpenRef.current = onOpen
  }, [onOpen])

  useEffect(() => {
    onErrorRef.current = onError
  }, [onError])

  useEffect(() => {
    if (!enabled) {
      return undefined
    }

    const eventSource = createConnection()

    if (onOpenRef.current) {
      eventSource.onopen = onOpenRef.current
    }

    if (onErrorRef.current) {
      eventSource.onerror = onErrorRef.current
    }

    Object.entries(handlersRef.current || {}).forEach(([eventName, handler]) => {
      eventSource.addEventListener(eventName, handler)
    })

    return () => {
      eventSource.close()
    }
  }, [createConnection, enabled])
}
