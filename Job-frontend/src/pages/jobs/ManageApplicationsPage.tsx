import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { applicationService } from '@/services/applicationService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import type { Application, ApplicationStatus } from '@/types';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Loader } from '@/components/common/Loader';
import { ApiErrorFallback } from '@/components/error/ApiErrorFallback';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { ArrowLeft, User, FileText, ExternalLink } from 'lucide-react';

const STATUS_FLOW: ApplicationStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'SHORTLISTED',
  'INTERVIEW',
  'OFFERED',
  'REJECTED',
];

const statusColors: Record<ApplicationStatus, string> = {
  SUBMITTED: 'bg-blue-100 text-blue-800',
  UNDER_REVIEW: 'bg-purple-100 text-purple-800',
  SHORTLISTED: 'bg-indigo-100 text-indigo-800',
  INTERVIEW: 'bg-amber-100 text-amber-800',
  OFFERED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  WITHDRAWN: 'bg-gray-100 text-gray-800',
};

export default function ManageApplicationsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchApplications();
  }, [id]);

  const fetchApplications = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await applicationService.getApplicationsForJob(Number(id));
      setApplications(data);
    } catch {
      setError('Failed to load applications');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (appId: number, status: ApplicationStatus) => {
    try {
      const updated = await applicationService.updateStatus(appId, status);
      setApplications((prev) =>
        prev.map((a) => (a.id === appId ? updated : a))
      );
      dispatch(addToast({ type: 'success', message: `Status updated to ${status.replace('_', ' ')}` }));
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to update status' }));
    }
  };

  if (loading) return <Loader fullPage message="Loading applications..." />;
  if (error) return <ApiErrorFallback error={error} onRetry={fetchApplications} />;

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="mb-6 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Back
      </button>

      <h1 className="text-2xl font-bold mb-6">
        Applications ({applications.length})
      </h1>

      {applications.length === 0 ? (
        <div className="py-12 text-center text-muted-foreground">
          No applications received yet.
        </div>
      ) : (
        <div className="space-y-4">
          {applications.map((app) => (
            <Card key={app.id}>
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <User className="h-4 w-4 text-muted-foreground" />
                      <span className="font-medium">
                        {`Applicant #${app.userId}`}
                      </span>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      Applied {new Date(app.createdAt).toLocaleDateString()}
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <Badge className={statusColors[app.status]}>
                      {app.status.replace('_', ' ')}
                    </Badge>
                    {app.status !== 'WITHDRAWN' && (
                      <Select
                        value={app.status}
                        onValueChange={(v) => handleStatusChange(app.id, v as ApplicationStatus)}
                      >
                        <SelectTrigger className="w-36 h-8 text-xs">
                          <SelectValue placeholder="Change status" />
                        </SelectTrigger>
                        <SelectContent>
                          {STATUS_FLOW.map((s) => (
                            <SelectItem key={s} value={s}>
                              {s.replace('_', ' ')}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  </div>
                </div>

                {app.coverLetter && (
                  <div className="mt-4">
                    <div className="flex items-center gap-1 text-sm font-medium">
                      <FileText className="h-4 w-4" />
                      Cover Letter
                    </div>
                    <p className="mt-1 text-sm text-muted-foreground whitespace-pre-wrap">
                      {app.coverLetter}
                    </p>
                  </div>
                )}

                {app.resumeUrl && (
                  <a
                    href={app.resumeUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mt-3 inline-flex items-center gap-1 text-sm text-primary hover:underline"
                  >
                    <ExternalLink className="h-3 w-3" />
                    View Resume
                  </a>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
