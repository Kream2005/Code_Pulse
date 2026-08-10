import axios from 'axios';
import { clearToken, getToken } from '../auth';

const client = axios.create({
  baseURL: '',
});

client.interceptors.request.use((config) => {
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
