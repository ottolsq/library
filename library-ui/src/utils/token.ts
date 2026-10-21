/**
 * 令牌持久化：只在这里读写 localStorage，避免各页面散落 key。
 */
const TOKEN_KEY = 'library_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
