import client from './client';
import type { LoginResponse, SetupAccountInfo } from './types';
import { setToken } from '../auth';

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await client.post<LoginResponse>('/auth/login', { email, password });
  setToken(data.accessToken);
  return data;
}

export async function completeAccount(body: {
  token: string;
  password: string;
  nom?: string | null;
  prenom?: string | null;
  userName?: string | null;
}): Promise<LoginResponse> {
  const { data } = await client.post<LoginResponse>('/auth/complete-account', body);
  setToken(data.accessToken);
  return data;
}

export async function getSetupInfo(token: string): Promise<SetupAccountInfo> {
  const { data } = await client.get<SetupAccountInfo>('/auth/setup-info', { params: { token } });
  return data;
}

export async function forgotPassword(email: string): Promise<{ message: string }> {
  const { data } = await client.post<{ message: string }>('/auth/forgot-password', { email });
  return data;
}

export async function getResetInfo(token: string): Promise<{ email: string }> {
  const { data } = await client.get<{ email: string }>('/auth/reset-info', { params: { token } });
  return data;
}

export async function resetPassword(token: string, password: string): Promise<LoginResponse> {
  const { data } = await client.post<LoginResponse>('/auth/reset-password', { token, password });
  setToken(data.accessToken);
  return data;
}
