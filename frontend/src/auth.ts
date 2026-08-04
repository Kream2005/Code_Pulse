const TOKEN_KEY = 'codepulse_token';

export interface JwtPayload {
  sub?: string;
  roles?: string[] | string;
  uid?: number;
  exp?: number;
  name?: string;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function decodeToken(): JwtPayload | null {
  const token = getToken();
  if (!token) return null;
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const payload = JSON.parse(atob(padded)) as JwtPayload;
    if (payload.exp && payload.exp * 1000 < Date.now()) return null;
    return payload;
  } catch {
    return null;
  }
}

export function isAuthenticated(): boolean {
  return decodeToken() !== null;
}

export function getRoles(): string[] {
  const roles = decodeToken()?.roles;
  if (!roles) return [];
  return Array.isArray(roles) ? roles : [roles];
}

export function hasAnyRole(required: string[]): boolean {
  const roles = getRoles();
  return required.some((r) => roles.includes(r));
}

export function isUser(): boolean {
  return hasAnyRole(['USER']);
}

export function isAdminCodingChallenge(): boolean {
  return hasAnyRole(['ADMIN_CODING_CHALLENGE']);
}

export function isManagerRh(): boolean {
  return hasAnyRole(['MANAGER_RH']);
}

export function isAdminCodePulse(): boolean {
  return hasAnyRole(['ADMIN_CODEPULSE']);
}

/** Any staff role with an admin area. */
export function isAdmin(): boolean {
  return hasAnyRole(['ADMIN_CODING_CHALLENGE', 'ADMIN_CODEPULSE', 'MANAGER_RH']);
}

export function canManageChallenges(): boolean {
  return hasAnyRole(['ADMIN_CODING_CHALLENGE', 'ADMIN_CODEPULSE']);
}

export function canReadFeedbacks(): boolean {
  return hasAnyRole(['ADMIN_CODING_CHALLENGE', 'MANAGER_RH', 'ADMIN_CODEPULSE']);
}

export function canSeeAnalytics(): boolean {
  return hasAnyRole(['MANAGER_RH', 'ADMIN_CODEPULSE']);
}

export function canManageQuestions(): boolean {
  return isAdminCodePulse();
}

export function canReadLogs(): boolean {
  return isAdminCodePulse();
}

export function canManageUsers(): boolean {
  return isAdminCodePulse();
}

export function getUserId(): number | null {
  const uid = decodeToken()?.uid;
  return uid === undefined || uid === null ? null : Number(uid);
}

export function getEmail(): string | null {
  return decodeToken()?.sub ?? null;
}

export function getUserName(): string | null {
  return getEmail();
}

export function getRole(): string | null {
  return getRoles()[0] ?? null;
}
