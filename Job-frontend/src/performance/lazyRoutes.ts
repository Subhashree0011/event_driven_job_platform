import { lazy } from 'react';

/* ---- auth ---- */
export const LoginPage = lazy(
  () => import(/* webpackChunkName: "login" */ '@/pages/auth/LoginPage'),
);
export const RegisterPage = lazy(
  () => import(/* webpackChunkName: "register" */ '@/pages/auth/RegisterPage'),
);

/* ---- jobs ---- */
export const JobListPage = lazy(
  () => import(/* webpackChunkName: "job-list" */ '@/pages/jobs/JobListPage'),
);
export const JobDetailPage = lazy(
  () => import(/* webpackChunkName: "job-detail" */ '@/pages/jobs/JobDetailPage'),
);
export const JobCreatePage = lazy(
  () => import(/* webpackChunkName: "job-create" */ '@/pages/jobs/JobCreatePage'),
);
export const JobEditPage = lazy(
  () => import(/* webpackChunkName: "job-edit" */ '@/pages/jobs/JobEditPage'),
);
export const ApplyJobPage = lazy(
  () => import(/* webpackChunkName: "apply-job" */ '@/pages/jobs/ApplyJobPage'),
);
export const ManageApplicationsPage = lazy(
  () =>
    import(
      /* webpackChunkName: "manage-apps" */ '@/pages/jobs/ManageApplicationsPage'
    ),
);

/* ---- company ---- */
export const CompanyCreatePage = lazy(
  () =>
    import(
      /* webpackChunkName: "company-create" */ '@/pages/companies/CompanyCreatePage'
    ),
);

/* ---- profile ---- */
export const ProfilePage = lazy(
  () => import(/* webpackChunkName: "profile" */ '@/pages/profile/ProfilePage'),
);

/* ---- dashboard ---- */
export const DashboardPage = lazy(
  () =>
    import(
      /* webpackChunkName: "dashboard" */ '@/pages/analytics/DashboardPage'
    ),
);

/* ---- testing ---- */
export const TestResultsPage = lazy(
  () =>
    import(
      /* webpackChunkName: "test-results" */ '@/pages/testing/TestResultsPage'
    ),
);
