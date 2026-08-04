import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import {
  canManageChallenges,
  canManageQuestions,
  canManageUsers,
  canReadFeedbacks,
  canReadLogs,
  canSeeAnalytics,
  isAdmin,
  isAdminCodePulse,
  isAuthenticated,
} from '../auth';

export function AdminRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!isAdmin()) return <Navigate to="/inbox" replace />;
  return <>{children}</>;
}

export function AdminCodePulseRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!isAdminCodePulse()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export function ChallengesAdminRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!canManageChallenges()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export function FeedbacksAdminRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!canReadFeedbacks()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export function AnalyticsRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!canSeeAnalytics()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export function QuestionsAdminRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!canManageQuestions()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export function LogsAdminRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!canReadLogs()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export function UsersAdminRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!canManageUsers()) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}
