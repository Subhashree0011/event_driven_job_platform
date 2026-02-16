import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { jobService } from '@/services/jobService';
import { useAuth } from '@/hooks/useAuth';
import { useTestMode } from '@/hooks/useTestMode';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import type { Job } from '@/types';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Loader } from '@/components/common/Loader';
import { ApiErrorFallback } from '@/components/error/ApiErrorFallback';
import { LoadTestPanel } from '@/components/testing/LoadTestPanel';
import {
  MapPin,
  DollarSign,
  Clock,
  Briefcase,
  Building2,
  Calendar,
  Eye,
  Users,
  Edit,
  Play,
  Pause,
  XCircle,
  Trash2,
  ArrowLeft,
} from 'lucide-react';

export default function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { user } = useAuth();
  const { isTestMode } = useTestMode();

  const [job, setJob] = useState<Job | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isOwner = user && job && user.id === job.employerId;

  useEffect(() => {
    if (!id) return;
    fetchJob();
  }, [id]);

  const fetchJob = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await jobService.getJob(Number(id));
      setJob(data);
    } catch {
      setError('Failed to load job details.');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (action: 'activate' | 'pause' | 'close') => {
    if (!job) return;
    try {
      let updated: Job;
      switch (action) {
        case 'activate':
          updated = await jobService.activateJob(job.id);
          break;
        case 'pause':
          updated = await jobService.pauseJob(job.id);
          break;
        case 'close':
          updated = await jobService.closeJob(job.id);
          break;
      }
      setJob(updated);
      dispatch(addToast({ type: 'success', message: `Job ${action}d successfully` }));
    } catch {
      dispatch(addToast({ type: 'error', message: `Failed to ${action} job` }));
    }
  };

  const handleDelete = async () => {
    if (!job) return;
    if (!confirm('Are you sure you want to delete this job?')) return;
    try {
      await jobService.deleteJob(job.id);
      dispatch(addToast({ type: 'success', message: 'Job deleted' }));
      navigate('/dashboard');
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to delete job' }));
    }
  };

  if (loading) return <Loader fullPage message="Loading job details..." />;
  if (error) return <ApiErrorFallback error={error} onRetry={fetchJob} />;
  if (!job) return null;

  const salary =
    job.salaryMin && job.salaryMax
      ? `$${job.salaryMin.toLocaleString()} - $${job.salaryMax.toLocaleString()}`
      : job.salaryMin
        ? `From $${job.salaryMin.toLocaleString()}`
        : job.salaryMax
          ? `Up to $${job.salaryMax.toLocaleString()}`
          : null;

  return (
    <div className="min-h-screen">
      {/* Dark Header */}
      <div className="bg-gray-900 px-4 py-12 text-white">
        <div className="mx-auto max-w-4xl">
          <button
            onClick={() => navigate(-1)}
            className="mb-4 flex items-center gap-1 text-sm text-gray-400 hover:text-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Back
          </button>

          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-3xl font-bold">{job.title}</h1>
              <p className="mt-2 flex items-center gap-2 text-gray-300">
                <Building2 className="h-4 w-4" />
                {job.company?.name || 'Unknown Company'}
              </p>
            </div>
            <Badge
              variant={
                job.status === 'ACTIVE' ? 'success' : job.status === 'CLOSED' ? 'destructive' : 'warning'
              }
              className="text-sm"
            >
              {job.status}
            </Badge>
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            <Badge variant="outline" className="border-gray-600 text-gray-300">
              <Briefcase className="mr-1 h-3 w-3" />
              {job.jobType?.replace('_', ' ')}
            </Badge>
            <Badge variant="outline" className="border-gray-600 text-gray-300">
              <MapPin className="mr-1 h-3 w-3" />
              {job.location}
            </Badge>
            <Badge variant="outline" className="border-gray-600 text-gray-300">
              {job.experienceLevel}
            </Badge>
            {salary && (
              <Badge variant="outline" className="border-gray-600 text-gray-300">
                <DollarSign className="mr-1 h-3 w-3" />
                {salary}
              </Badge>
            )}
          </div>

          <div className="mt-4 flex items-center gap-6 text-sm text-gray-400">
            <span className="flex items-center gap-1">
              <Calendar className="h-4 w-4" />
              Posted {new Date(job.createdAt).toLocaleDateString()}
            </span>
            {job.viewCount !== undefined && (
              <span className="flex items-center gap-1">
                <Eye className="h-4 w-4" />
                {job.viewCount} views
              </span>
            )}
            {job.applicationCount !== undefined && (
              <span className="flex items-center gap-1">
                <Users className="h-4 w-4" />
                {job.applicationCount} applicants
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="mx-auto max-w-4xl px-4 py-8">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            <section>
              <h2 className="text-xl font-semibold">Job Description</h2>
              <p className="mt-3 whitespace-pre-wrap text-muted-foreground">{job.description}</p>
            </section>

            {job.skills.length > 0 && (
              <section>
                <h2 className="text-xl font-semibold">Required Skills</h2>
                <div className="mt-3 flex flex-wrap gap-2">
                  {job.skills.map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </section>
            )}

            {job.applicationDeadline && (
              <section>
                <h2 className="text-xl font-semibold">Application Deadline</h2>
                <p className="mt-2 flex items-center gap-2 text-muted-foreground">
                  <Clock className="h-4 w-4" />
                  {new Date(job.applicationDeadline).toLocaleDateString()}
                </p>
              </section>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-4">
            {/* Apply / Owner Actions */}
            {isOwner ? (
              <div className="rounded-lg border p-4 space-y-3">
                <h3 className="font-semibold">Manage Job</h3>
                <Button
                  variant="outline"
                  className="w-full gap-2"
                  onClick={() => navigate(`/jobs/${job.id}/edit`)}
                >
                  <Edit className="h-4 w-4" />
                  Edit Job
                </Button>

                {(job.status === 'DRAFT' || job.status === 'PAUSED') && (
                  <Button
                    className="w-full gap-2 bg-green-600 hover:bg-green-700"
                    onClick={() => handleStatusChange('activate')}
                  >
                    <Play className="h-4 w-4" />
                    Activate
                  </Button>
                )}

                {job.status === 'ACTIVE' && (
                  <Button
                    variant="outline"
                    className="w-full gap-2"
                    onClick={() => handleStatusChange('pause')}
                  >
                    <Pause className="h-4 w-4" />
                    Pause
                  </Button>
                )}

                {job.status !== 'CLOSED' && (
                  <Button
                    variant="outline"
                    className="w-full gap-2 text-destructive"
                    onClick={() => handleStatusChange('close')}
                  >
                    <XCircle className="h-4 w-4" />
                    Close Job
                  </Button>
                )}

                <Button
                  variant="destructive"
                  className="w-full gap-2"
                  onClick={handleDelete}
                >
                  <Trash2 className="h-4 w-4" />
                  Delete Job
                </Button>

                <Button
                  variant="outline"
                  className="w-full gap-2"
                  onClick={() => navigate(`/jobs/${job.id}/applications`)}
                >
                  <Users className="h-4 w-4" />
                  View Applications
                </Button>
              </div>
            ) : user?.role === 'JOB_SEEKER' ? (
              <div className="rounded-lg border p-4">
                <Button
                  className="w-full"
                  onClick={() => navigate(`/jobs/${job.id}/apply`)}
                  disabled={job.status !== 'ACTIVE'}
                >
                  Apply for this Job
                </Button>
                {job.status !== 'ACTIVE' && (
                  <p className="mt-2 text-center text-xs text-muted-foreground">
                    This job is no longer accepting applications
                  </p>
                )}
              </div>
            ) : null}

            {/* Company Info */}
            <div className="rounded-lg border p-4">
              <h3 className="font-semibold">About the Company</h3>
              <p className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                <Building2 className="h-4 w-4" />
                {job.company?.name || 'Unknown Company'}
              </p>
            </div>
          </div>
        </div>

        {/* Load Test Panel */}
        {isTestMode && <LoadTestPanel jobId={job.id} jobTitle={job.title} />}
      </div>
    </div>
  );
}
