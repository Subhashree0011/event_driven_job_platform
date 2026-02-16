import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { jobService } from '@/services/jobService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
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

const companySchema = z.object({
  name: z.string().min(1, 'Company name is required'),
  description: z.string().optional(),
  website: z.string().optional(),
  logoUrl: z.string().optional(),
  industry: z.string().optional(),
  location: z.string().optional(),
});

type CompanyFormData = z.infer<typeof companySchema>;

export default function CompanyCreatePage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [size, setSize] = useState('SMALL');
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CompanyFormData>({
    resolver: zodResolver(companySchema),
  });

  const onSubmit = async (data: CompanyFormData) => {
    setSubmitting(true);
    try {
      await jobService.createCompany({
        ...data,
        companySize: size as 'SMALL',
      });
      dispatch(addToast({ type: 'success', message: 'Company registered successfully!' }));
      navigate('/jobs/create');
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to register company' }));
    } finally {
      setSubmitting(false);
    }
  };

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
          <CardTitle>Register a Company</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="name">Company Name</Label>
              <Input id="name" placeholder="Acme Inc." {...register('name')} />
              {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                rows={4}
                placeholder="Tell us about your company..."
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                {...register('description')}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="website">Website</Label>
              <Input id="website" type="url" placeholder="https://example.com" {...register('website')} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="logoUrl">Logo URL</Label>
              <Input id="logoUrl" type="url" placeholder="https://example.com/logo.png" {...register('logoUrl')} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="industry">Industry</Label>
              <Input id="industry" placeholder="Technology" {...register('industry')} />
            </div>

            <div className="space-y-2">
              <Label>Company Size</Label>
              <Select value={size} onValueChange={setSize}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="STARTUP">Startup (1-10)</SelectItem>
                  <SelectItem value="SMALL">Small (11-50)</SelectItem>
                  <SelectItem value="MEDIUM">Medium (51-200)</SelectItem>
                  <SelectItem value="LARGE">Large (201-1000)</SelectItem>
                  <SelectItem value="ENTERPRISE">Enterprise (1000+)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="location">Location</Label>
              <Input id="location" placeholder="San Francisco, CA" {...register('location')} />
            </div>

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Registering...
                </>
              ) : (
                'Register Company'
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
