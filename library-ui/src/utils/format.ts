/** LocalDateTime ISO 字符串 → 2026-10-01 10:00 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ').slice(0, 16)
}
