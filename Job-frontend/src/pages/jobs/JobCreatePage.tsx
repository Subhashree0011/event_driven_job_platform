import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { jobService } from '@/services/jobService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import type { Company } from '@/types';
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
import { Loader2, ArrowLeft } from 'lucide-react';

const jobSchema = z.object({
  title: z.string().min(1, 'Title is required'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  role: z.string().min(1, 'Role is required'),
  location: z.string().min(1, 'Location is required'),
  skills: z.string().optional(),
  salaryMin: z.coerce.number().optional(),
  salaryMax: z.coerce.number().optional(),
  deadline: z.string().optional(),
});

type JobFormData = z.infer<typeof jobSchema>;

export default function JobCreatePage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const [companies, setCompanies] = useState<Company[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string>('');
  const [jobType, setJobType] = useState('FULL_TIME');
  const [experienceLevel, setExperienceLevel] = useState('MID');
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<JobFormData>({
    resolver: zodResolver(jobSchema),
  });

  useEffect(() => {
    loadCompanies();
  }, []);

  const loadCompanies = async () => {
    try {
      const data = await jobService.getMyCompanies();
      setCompanies(data);
      if (data.length > 0) setSelectedCompanyId(String(data[0].id));
    } catch {
      try {
        const data = await jobService.getAllCompanies();
        setCompanies(data);
        if (data.length > 0) setSelectedCompanyId(String(data[0].id));
      } catch {
        // ignore
      }
    }
  };

  const onSubmit = async (data: JobFormData) => {
    if (!selectedCompanyId) {
      dispatch(addToast({ type: 'error', message: 'Please select a company' }));
      return;
    }

    setSubmitting(true);
    try {
      const skills = data.skills ? data.skills.split(',').map((s) => s.trim()).filter(Boolean) : [];
      const job = await jobService.createJob({
        title: data.title,
        description: data.description,
        role: data.role,
        companyId: Number(selectedCompanyId),
        location: data.location,
        jobType: jobType as 'FULL_TIME',
        experienceLevel: experienceLevel as 'MID',
        salaryMin: data.salaryMin || undefined,
        salaryMax: data.salaryMax || undefined,
        skills,
        applicationDeadline: data.deadline || undefined,
      });
      dispatch(addToast({ type: 'success', message: 'Job posted successfully!' }));
      navigate(`/jobs/${job.id}`);
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to create job' }));
    } finally {
      setSubmitting(false);
    }
  };

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
          <CardTitle>Post a New Job</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Company Selector */}
            <div className="space-y-2">
              <Label>Company</Label>
              <Select value={selectedCompanyId} onValueChange={setSelectedCompanyId}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a company" />
                </SelectTrigger>
                <SelectContent>
                  {companies.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {companies.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  No companies found.{' '}
                  <button
                    type="button"
                    onClick={() => navigate('/companies/create')}
                    className="text-primary hover:underline"
                  >
                    Register a company first
                  </button>
                </p>
              )}
            </div>

            {/* Title */}
            <div className="space-y-2">
              <Label htmlFor="title">Job Title</Label>
              <Input id="title" placeholder="e.g. Senior React Developer" {...register('title')} />
              {errors.title && <p className="text-xs text-destructive">{errors.title.message}</p>}
            </div>

            {/* Role */}
            <div className="space-y-2">
              <Label htmlFor="role">Role</Label>
              <Input id="role" placeholder="e.g. Frontend Engineer" {...register('role')} />
              {errors.role && <p className="text-xs text-destructive">{errors.role.message}</p>}
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                rows={6}
                placeholder="Describe the job responsibilities, requirements, and benefits..."
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                {...register('description')}
              />
              {errors.description && (
                <p className="text-xs text-destructive">{errors.description.message}</p>
              )}
            </div>

            {/* Location */}
            <div className="space-y-2">
              <Label htmlFor="location">Location</Label>
              <Input id="location" placeholder="e.g. New York, NY or Remote" {...register('location')} />
              {errors.location && <p className="text-xs text-destructive">{errors.location.message}</p>}
            </div>

            {/* Type & Level */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Job Type</Label>
                <Select value={jobType} onValueChange={setJobType}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
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
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
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

            {/* Salary */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="salaryMin">Min Salary (USD)</Label>
                <Input id="salaryMin" type="number" placeholder="50000" {...register('salaryMin')} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="salaryMax">Max Salary (USD)</Label>
                <Input id="salaryMax" type="number" placeholder="100000" {...register('salaryMax')} />
              </div>
            </div>

            {/* Skills */}
            <div className="space-y-2">
              <Label htmlFor="skills">Skills (comma-separated)</Label>
              <Input id="skills" placeholder="React, TypeScript, Node.js" {...register('skills')} />
            </div>

            {/* Deadline */}
            <div className="space-y-2">
              <Label htmlFor="deadline">Application Deadline</Label>
              <Input id="deadline" type="date" {...register('deadline')} />
            </div>

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Posting...
                </>
              ) : (
                'Post Job'
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
