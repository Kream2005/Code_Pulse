import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { clearToken, isAdmin, isAuthenticated, isUser } from '../auth';

type Props = {
  children: ReactNode;
  /** Notification deep links: wrong role → login with returnUrl instead of admin home. */
  redirectWrongRoleToLogin?: boolean;
};

/** Candidate / collaborateur pages. */
export default function UserRoute({ children, redirectWrongRoleToLogin = false }: Props) {
  const location = useLocation();

  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!isUser()) {
    if (redirectWrongRoleToLogin) {
      clearToken();
      const returnUrl = encodeURIComponent(location.pathname + location.search);
      return <Navigate to={`/login?returnUrl=${returnUrl}`} replace />;
    }
    return <Navigate to={isAdmin() ? '/admin' : '/login'} replace />;
  }
  return <>{children}</>;
}
