import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { jobService } from '@/services/jobService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import type { Job } from '@/types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Loader } from '@/components/common/Loader';
import { Loader2, ArrowLeft } from 'lucide-react';

const editSchema = z.object({
  title: z.string().min(1, 'Title is required'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  role: z.string().min(1, 'Role is required'),
  location: z.string().min(1, 'Location is required'),
  skills: z.string().optional(),
  salaryMin: z.coerce.number().optional(),
  salaryMax: z.coerce.number().optional(),
  deadline: z.string().optional(),
});

type EditFormData = z.infer<typeof editSchema>;

export default function JobEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const [job, setJob] = useState<Job | null>(null);
  const [loading, setLoading] = useState(true);
  const [jobType, setJobType] = useState('FULL_TIME');
  const [experienceLevel, setExperienceLevel] = useState('MID');
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditFormData>({
    resolver: zodResolver(editSchema),
  });

  useEffect(() => {
    if (!id) return;
    loadJob();
  }, [id]);

  const loadJob = async () => {
    try {
      const data = await jobService.getJob(Number(id));
      setJob(data);
      setJobType(data.jobType || 'FULL_TIME');
      setExperienceLevel(data.experienceLevel);
      reset({
        title: data.title,
        description: data.description,
        role: data.role,
        location: data.location,
        skills: Array.isArray(data.skills) ? data.skills.join(', ') : '',
        salaryMin: data.salaryMin || undefined,
        salaryMax: data.salaryMax || undefined,
        deadline: data.applicationDeadline?.split('T')[0] || undefined,
      });
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to load job' }));
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = async (data: EditFormData) => {
    if (!job) return;
    setSubmitting(true);
    try {
      const skills = data.skills ? data.skills.split(',').map((s) => s.trim()).filter(Boolean) : [];
      await jobService.updateJob(job.id, {
        title: data.title,
        description: data.description,
        role: data.role,
        location: data.location,
        jobType: jobType as 'FULL_TIME',
        experienceLevel: experienceLevel as 'MID',
        salaryMin: data.salaryMin || undefined,
        salaryMax: data.salaryMax || undefined,
        skills,
        applicationDeadline: data.deadline || undefined,
      });
      dispatch(addToast({ type: 'success', message: 'Job updated successfully!' }));
      navigate(`/jobs/${job.id}`);
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to update job' }));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Loader fullPage message="Loading job..." />;
  if (!job) return null;

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="mb-6 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Back
      </button>

      <Card>
        <CardHeader>
          <CardTitle>Edit Job</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Job Title</Label>
              <Input id="title" {...register('title')} />
              {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="role">Role</Label>
              <Input id="role" {...register('role')} />
              {errors.role && <p className="text-xs text-destructive">{errors.role.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                rows={6}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                {...register('description')}
              />
              {errors.description && (
                <p className="text-xs text-destructive">{errors.description.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="location">Location</Label>
              <Input id="location" {...register('location')} />
              {errors.location && <p className="text-xs text-destructive">{errors.location.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Job Type</Label>
                <Select value={jobType} onValueChange={setJobType}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="FULL_TIME">Full Time</SelectItem>
                    <SelectItem value="PART_TIME">Part Time</SelectItem>
                    <SelectItem value="CONTRACT">Contract</SelectItem>
                    <SelectItem value="INTERNSHIP">Internship</SelectItem>
                    <SelectItem value="FREELANCE">Freelance</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Experience Level</Label>
                <Select value={experienceLevel} onValueChange={setExperienceLevel}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ENTRY">Entry Level</SelectItem>
                    <SelectItem value="JUNIOR">Junior</SelectItem>
                    <SelectItem value="MID">Mid Level</SelectItem>
                    <SelectItem value="SENIOR">Senior</SelectItem>
                    <SelectItem value="LEAD">Lead</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="salaryMin">Min Salary (USD)</Label>
                <Input id="salaryMin" type="number" {...register('salaryMin')} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="salaryMax">Max Salary (USD)</Label>
                <Input id="salaryMax" type="number" {...register('salaryMax')} />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="skills">Skills (comma-separated)</Label>
              <Input id="skills" {...register('skills')} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="deadline">Application Deadline</Label>
              <Input id="deadline" type="date" {...register('deadline')} />
            </div>

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Saving...
                </>
              ) : (
                'Save Changes'
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
