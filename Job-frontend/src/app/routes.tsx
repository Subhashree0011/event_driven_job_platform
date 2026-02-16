import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import {
  LoginPage,
  RegisterPage,
  JobListPage,
  JobDetailPage,
  JobCreatePage,
  JobEditPage,
  ApplyJobPage,
  ManageApplicationsPage,
  CompanyCreatePage,
  ProfilePage,
  DashboardPage,
  TestResultsPage,
} from '@/performance/lazyRoutes';

export function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<Navigate to="/jobs" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/jobs" element={<JobListPage />} />
      <Route path="/jobs/:id" element={<JobDetailPage />} />

      {/* Protected – any authenticated user */}
      <Route element={<ProtectedRoute />}>
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/test-results" element={<TestResultsPage />} />
      </Route>

      {/* Protected – JOB_SEEKER */}
      <Route element={<ProtectedRoute allowedRoles={['JOB_SEEKER']} />}>
        <Route path="/jobs/:id/apply" element={<ApplyJobPage />} />
      </Route>

      {/* Protected – EMPLOYER */}
      <Route element={<ProtectedRoute allowedRoles={['EMPLOYER']} />}>
        <Route path="/jobs/create" element={<JobCreatePage />} />
        <Route path="/jobs/:id/edit" element={<JobEditPage />} />
        <Route path="/jobs/:id/applications" element={<ManageApplicationsPage />} />
        <Route path="/companies/create" element={<CompanyCreatePage />} />
      </Route>

      {/* Catch-all */}
      <Route
        path="*"
        element={
          <div className="flex min-h-[60vh] flex-col items-center justify-center gap-2">
            <h1 className="text-4xl font-bold">404</h1>
            <p className="text-muted-foreground">Page not found</p>
          </div>
        }
      />
    </Routes>
  );
}
