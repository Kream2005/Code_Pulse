import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Activity,
  BarChart3,
  Bell,
  FileText,
  LayoutDashboard,
  Lock,
  MessageSquareText,
  Settings,
  User,
  Users,
} from 'lucide-react';
import {
  canManageChallenges,
  canManageQuestions,
  canManageUsers,
  canReadFeedbacks,
  canReadLogs,
  canSeeAnalytics,
  clearToken,
  getEmail,
  getRole,
  isAdmin,
  isAdminCodePulse,
  isUser,
} from '../auth';
import { useI18n } from '../i18n/I18nContext';
import SidebarBrand from './sidebar/SidebarBrand';
import SidebarLink from './sidebar/SidebarLink';
import SidebarUser from './sidebar/SidebarUser';

export default function Sidebar() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const role = getRole();
  const name = getEmail();
  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem('sidebarCollapsed') === '1'
  );

  function toggle() {
    setCollapsed((c) => {
      localStorage.setItem('sidebarCollapsed', c ? '0' : '1');
      return !c;
    });
  }

  function logout() {
    clearToken();
    navigate('/login');
  }

  const items = [
    ...(isUser()
      ? [
          {
            to: '/inbox',
            end: true,
            label: t('nav.inbox'),
            icon: <Bell className="h-5 w-5 shrink-0" />,
          },
          {
            to: '/my-feedback',
            end: false,
            label: t('nav.myFeedback'),
            icon: <FileText className="h-5 w-5 shrink-0" />,
          },
          {
            to: '/feedback/form',
            end: false,
            label: t('nav.giveFeedback'),
            icon: <MessageSquareText className="h-5 w-5 shrink-0" />,
          },
        ]
      : []),
    { to: '/profile', end: false, label: t('nav.profile'), icon: <User className="h-5 w-5 shrink-0" /> },
    ...(isAdmin()
      ? [
          {
            to: '/admin',
            end: true,
            label: t('nav.adminDashboard'),
            icon: <LayoutDashboard className="h-5 w-5 shrink-0" />,
          },
          ...(canManageChallenges()
            ? [
                {
                  to: '/admin/notifications',
                  end: false,
                  label: t('nav.notifications'),
                  icon: <Bell className="h-5 w-5 shrink-0" />,
                },
                {
                  to: '/admin/challenges',
                  end: false,
                  label: t('nav.challenges'),
                  icon: <Settings className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
          ...(canReadFeedbacks()
            ? [
                {
                  to: '/admin/feedbacks',
                  end: false,
                  label: t('nav.feedbacks'),
                  icon: <MessageSquareText className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
          ...(canSeeAnalytics()
            ? [
                {
                  to: '/admin/analytics',
                  end: false,
                  label: t('nav.analytics'),
                  icon: <BarChart3 className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
          ...(canManageUsers()
            ? [
                {
                  to: '/admin/users',
                  end: false,
                  label: t('nav.users'),
                  icon: <Users className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
          ...(isAdminCodePulse()
            ? [
                {
                  to: '/admin/password-requests',
                  end: false,
                  label: t('nav.passwordRequests'),
                  icon: <Lock className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
          ...(canManageQuestions()
            ? [
                {
                  to: '/admin/questions',
                  end: false,
                  label: t('nav.questions'),
                  icon: <FileText className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
          ...(canReadLogs()
            ? [
                {
                  to: '/admin/logs',
                  end: false,
                  label: t('nav.logs'),
                  icon: <Activity className="h-5 w-5 shrink-0" />,
                },
              ]
            : []),
        ]
      : []),
  ];

  return (
    <aside
      className={`sticky top-0 z-20 flex h-screen shrink-0 flex-col border-r border-slate-200 bg-white transition-[width] duration-300 ease-in-out dark:border-slate-700 dark:bg-slate-900 ${
        collapsed ? 'w-16' : 'w-60'
      }`}
    >
      <SidebarBrand collapsed={collapsed} onToggle={toggle} />
      <nav className="flex flex-1 flex-col overflow-y-auto py-3">
        {items.map((item) => (
          <SidebarLink key={item.to} {...item} collapsed={collapsed} />
        ))}
      </nav>
      <SidebarUser name={name} role={role} collapsed={collapsed} onLogout={logout} />
    </aside>
  );
}
