import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { jobService } from '@/services/jobService';
import { applicationService } from '@/services/applicationService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import type { Job } from '@/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Loader } from '@/components/common/Loader';
import { ArrowLeft, CheckCircle, Loader2 } from 'lucide-react';

export default function ApplyJobPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const [job, setJob] = useState<Job | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [coverLetter, setCoverLetter] = useState('');
  const [resumeUrl, setResumeUrl] = useState('');

  useEffect(() => {
    if (!id) return;
    jobService
      .getJob(Number(id))
      .then(setJob)
      .catch(() => dispatch(addToast({ type: 'error', message: 'Failed to load job' })))
      .finally(() => setLoading(false));
  }, [id, dispatch]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!job) return;

    setSubmitting(true);
    try {
      await applicationService.apply({
        jobId: job.id,
        coverLetter: coverLetter || undefined,
        resumeUrl: resumeUrl || undefined,
      });
      setSubmitted(true);
      dispatch(addToast({ type: 'success', message: 'Application submitted!' }));
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to submit application' }));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Loader fullPage message="Loading..." />;
  if (!job) return null;

  if (submitted) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4">
        <Card className="w-full max-w-md text-center">
          <CardContent className="pt-8 pb-8">
            <CheckCircle className="mx-auto h-16 w-16 text-green-500" />
            <h2 className="mt-4 text-2xl font-bold">Application Submitted!</h2>
            <p className="mt-2 text-muted-foreground">
              Your application for <strong>{job.title}</strong> has been successfully submitted.
            </p>
            <div className="mt-6 flex gap-3 justify-center">
              <Button onClick={() => navigate('/dashboard')}>Go to Dashboard</Button>
              <Button variant="outline" onClick={() => navigate('/jobs')}>
                Browse More Jobs
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="mb-6 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Back
      </button>

      <Card>
        <CardHeader>
          <CardTitle>Apply for {job.title}</CardTitle>
          <CardDescription>
            at {job.companyName || `Company #${job.companyId}`} · {job.location}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="coverLetter">Cover Letter</Label>
              <textarea
                id="coverLetter"
                rows={8}
                maxLength={5000}
                placeholder="Tell the employer why you're a great fit for this role..."
                value={coverLetter}
                onChange={(e) => setCoverLetter(e.target.value)}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              <p className="text-xs text-muted-foreground text-right">
                {coverLetter.length}/5000
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="resumeUrl">Resume URL</Label>
              <Input
                id="resumeUrl"
                type="url"
                placeholder="https://example.com/your-resume.pdf"
                value={resumeUrl}
                onChange={(e) => setResumeUrl(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                Link to your resume (Google Drive, Dropbox, etc.)
              </p>
            </div>

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Submitting...
                </>
              ) : (
                'Submit Application'
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
