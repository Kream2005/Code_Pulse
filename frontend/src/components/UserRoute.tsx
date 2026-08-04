import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { isAdmin, isAuthenticated, isUser } from '../auth';

/** Candidate / collaborateur pages. */
export default function UserRoute({ children }: { children: ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!isUser()) {
    return <Navigate to={isAdmin() ? '/admin' : '/login'} replace />;
  }
  return <>{children}</>;
}
