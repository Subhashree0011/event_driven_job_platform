import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { jobService } from '@/services/jobService';
import { applicationService } from '@/services/applicationService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Loader } from '@/components/common/Loader';
import type { Job, Application } from '@/types';
import { Briefcase, FileText, Clock, TrendingUp, ExternalLink, Building2 } from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();
  const dispatch = useAppDispatch();
  const [loading, setLoading] = useState(true);
  const [myJobs, setMyJobs] = useState<Job[]>([]);
  const [myApplications, setMyApplications] = useState<Application[]>([]);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      if (user.role === 'EMPLOYER') {
        const jobs = await jobService.getMyJobs();
        setMyJobs(jobs);
      } else {
        const apps = await applicationService.getMyApplications();
        setMyApplications(apps);
      }
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to load dashboard data' }));
    } finally {
      setLoading(false);
    }
  }, [user, dispatch]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <Loader fullPage />;
  if (!user) return null;

  const isEmployer = user.role === 'EMPLOYER';

  const statusColor = (s: string) => {
    const map: Record<string, 'success' | 'warning' | 'destructive' | 'secondary' | 'outline'> = {
      ACTIVE: 'success',
      SUBMITTED: 'warning',
      UNDER_REVIEW: 'warning',
      SHORTLISTED: 'success',
      INTERVIEW: 'success',
      OFFERED: 'success',
      REJECTED: 'destructive',
      WITHDRAWN: 'secondary',
      PAUSED: 'warning',
      CLOSED: 'outline',
    };
    return map[s] || 'secondary';
  };

  /* ---------- stat cards ---------- */
  const statCards = isEmployer
    ? [
        { icon: Briefcase, label: 'Posted Jobs', value: myJobs.length },
        { icon: TrendingUp, label: 'Active', value: myJobs.filter((j) => j.status === 'ACTIVE').length },
        { icon: Clock, label: 'Paused', value: myJobs.filter((j) => j.status === 'PAUSED').length },
        { icon: FileText, label: 'Closed', value: myJobs.filter((j) => j.status === 'CLOSED').length },
      ]
    : [
        { icon: FileText, label: 'Applications', value: myApplications.length },
        { icon: Clock, label: 'Submitted', value: myApplications.filter((a) => a.status === 'SUBMITTED').length },
        { icon: TrendingUp, label: 'Offered', value: myApplications.filter((a) => a.status === 'OFFERED').length },
        { icon: Briefcase, label: 'Rejected', value: myApplications.filter((a) => a.status === 'REJECTED').length },
      ];

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="mb-2 text-2xl font-bold">Dashboard</h1>
      <p className="mb-8 text-muted-foreground">
        Welcome back, {user.firstName}!
      </p>

      {/* Stat cards */}
      <div className="mb-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((s) => (
          <Card key={s.label}>
            <CardContent className="flex items-center gap-4 py-5">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                <s.icon className="h-6 w-6 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">{s.label}</p>
                <p className="text-2xl font-bold">{s.value}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Content area */}
      {isEmployer ? (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Building2 className="h-5 w-5" />
              My Posted Jobs
            </CardTitle>
            <Link to="/jobs/create">
              <Button size="sm">Post New Job</Button>
            </Link>
          </CardHeader>
          <CardContent>
            {myJobs.length === 0 ? (
              <p className="py-8 text-center text-muted-foreground">
                No jobs posted yet.{' '}
                <Link to="/jobs/create" className="text-primary underline">
                  Create your first job
                </Link>
              </p>
            ) : (
              <div className="divide-y">
                {myJobs.map((job) => (
                  <div
                    key={job.id}
                    className="flex items-center justify-between py-4 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <Link
                        to={`/jobs/${job.id}`}
                        className="font-medium hover:text-primary hover:underline"
                      >
                        {job.title}
                      </Link>
                      <p className="text-sm text-muted-foreground">{job.location}</p>
                    </div>
                    <div className="flex items-center gap-3">
                      <Badge variant={statusColor(job.status)}>{job.status}</Badge>
                      <Link to={`/jobs/${job.id}/applications`}>
                        <Button variant="outline" size="sm" className="gap-1">
                          <ExternalLink className="h-3 w-3" />
                          Manage
                        </Button>
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileText className="h-5 w-5" />
              My Applications
            </CardTitle>
          </CardHeader>
          <CardContent>
            {myApplications.length === 0 ? (
              <p className="py-8 text-center text-muted-foreground">
                No applications yet.{' '}
                <Link to="/jobs" className="text-primary underline">
                  Browse jobs
                </Link>
              </p>
            ) : (
              <div className="divide-y">
                {myApplications.map((app) => (
                  <div
                    key={app.id}
                    className="flex items-center justify-between py-4 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <Link
                        to={`/jobs/${app.jobId}`}
                        className="font-medium hover:text-primary hover:underline"
                      >
                        {`Job #${app.jobId}`}
                      </Link>
                      <p className="text-xs text-muted-foreground">
                        Applied {new Date(app.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <Badge variant={statusColor(app.status)}>{app.status}</Badge>
                      {app.status === 'SUBMITTED' && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-destructive hover:text-destructive"
                          onClick={async () => {
                            try {
                              await applicationService.withdraw(app.id);
                              setMyApplications((prev) =>
                                prev.map((a) =>
                                  a.id === app.id ? { ...a, status: 'WITHDRAWN' as const } : a,
                                ),
                              );
                              dispatch(addToast({ type: 'success', message: 'Application withdrawn' }));
                            } catch {
                              dispatch(addToast({ type: 'error', message: 'Failed to withdraw' }));
                            }
                          }}
                        >
                          Withdraw
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
