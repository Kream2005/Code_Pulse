import axios from 'axios';
import { clearToken, getToken } from '../auth';

const client = axios.create({
  baseURL: '',
});

const PUBLIC_AUTH_PATHS = [
  '/auth/login',
  '/auth/complete-account',
  '/auth/setup-info',
  '/auth/forgot-password',
  '/auth/reset-info',
  '/auth/reset-password',
];

function isPublicAuthRequest(url?: string) {
  if (!url) return false;
  return PUBLIC_AUTH_PATHS.some((p) => url === p || url.startsWith(`${p}?`));
}

client.interceptors.request.use((config) => {
  // Never attach a session JWT on public auth endpoints (setup/reset links).
  if (isPublicAuthRequest(config.url)) {
    return config;
  }
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      const path = window.location.pathname + window.location.search;
      clearToken();
      const publicPaths = ['/login', '/complete-account', '/forgot-password', '/reset-password'];
      if (!publicPaths.some((p) => window.location.pathname.startsWith(p))) {
        const returnUrl = encodeURIComponent(path);
        window.location.href = `/login?returnUrl=${returnUrl}`;
      }
    }
    return Promise.reject(err);
  }
);

export default client;
