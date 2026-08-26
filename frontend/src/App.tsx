import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import UserRoute from './components/UserRoute';
import {
  AdminCodePulseRoute,
  AdminRoute,
  AnalyticsRoute,
  ChallengesAdminRoute,
  FeedbacksAdminRoute,
  LogsAdminRoute,
  QuestionsAdminRoute,
  UsersAdminRoute,
} from './components/AdminRoute';
import Login from './pages/Login';
import CompleteAccount from './pages/CompleteAccount';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Inbox from './pages/Inbox';
import MyFeedback from './pages/MyFeedback';
import FeedbackFormPage from './pages/FeedbackForm';
import FeedbackDetails from './pages/FeedbackDetails';
import Profile from './pages/Profile';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminUsers from './pages/admin/AdminUsers';
import PasswordRequests from './pages/admin/PasswordRequests';
import AdminChallenges from './pages/admin/AdminChallenges';
import AdminFeedbacks from './pages/admin/AdminFeedbacks';
import AdminLogs from './pages/admin/AdminLogs';
import AdminQuestions from './pages/admin/AdminQuestions';
import AdminAnalytics from './pages/admin/AdminAnalytics';
import AdminSmartSearch from './pages/admin/AdminSmartSearch';
import { isAdmin, isUser } from './auth';

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <Layout>{children}</Layout>
    </ProtectedRoute>
  );
}

function HomeRedirect() {
  if (isAdmin() && !isUser()) return <Navigate to="/admin" replace />;
  return <Navigate to="/inbox" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/complete-account" element={<CompleteAccount />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/register" element={<Navigate to="/login" replace />} />

      <Route path="/" element={<HomeRedirect />} />
      <Route
        path="/inbox"
        element={
          <Shell>
            <UserRoute>
              <Inbox />
            </UserRoute>
          </Shell>
        }
      />
      <Route
        path="/my-feedback"
        element={
          <Shell>
            <UserRoute>
              <MyFeedback />
            </UserRoute>
          </Shell>
        }
      />
      <Route
        path="/feedback/form"
        element={
          <Shell>
            <UserRoute>
              <FeedbackFormPage />
            </UserRoute>
          </Shell>
        }
      />
      <Route
        path="/feedback/:id"
        element={
          <Shell>
            <UserRoute>
              <FeedbackDetails backTo="/my-feedback" />
            </UserRoute>
          </Shell>
        }
      />
      <Route
        path="/profile"
        element={
          <Shell>
            <Profile />
          </Shell>
        }
      />

      <Route
        path="/admin"
        element={
          <Shell>
            <AdminRoute>
              <AdminDashboard />
            </AdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/notifications"
        element={
          <Shell>
            <ChallengesAdminRoute>
              <Inbox admin />
            </ChallengesAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/users"
        element={
          <Shell>
            <UsersAdminRoute>
              <AdminUsers />
            </UsersAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/password-requests"
        element={
          <Shell>
            <AdminCodePulseRoute>
              <PasswordRequests />
            </AdminCodePulseRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/challenges"
        element={
          <Shell>
            <ChallengesAdminRoute>
              <AdminChallenges />
            </ChallengesAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/feedbacks"
        element={
          <Shell>
            <FeedbacksAdminRoute>
              <AdminFeedbacks />
            </FeedbacksAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/feedbacks/:id"
        element={
          <Shell>
            <FeedbacksAdminRoute>
              <FeedbackDetails backTo="/admin/feedbacks" />
            </FeedbacksAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/logs"
        element={
          <Shell>
            <LogsAdminRoute>
              <AdminLogs />
            </LogsAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/questions"
        element={
          <Shell>
            <QuestionsAdminRoute>
              <AdminQuestions />
            </QuestionsAdminRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/analytics"
        element={
          <Shell>
            <AnalyticsRoute>
              <AdminAnalytics />
            </AnalyticsRoute>
          </Shell>
        }
      />
      <Route
        path="/admin/smart-search"
        element={
          <Shell>
            <AdminRoute>
              <AdminSmartSearch />
            </AdminRoute>
          </Shell>
        }
      />

      <Route path="*" element={<HomeRedirect />} />
    </Routes>
  );
}
