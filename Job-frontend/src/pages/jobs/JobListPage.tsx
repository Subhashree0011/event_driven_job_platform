import { useState, useEffect, useCallback, memo } from 'react';
import { useNavigate } from 'react-router-dom';
import { jobService } from '@/services/jobService';
import { useDebounce } from '@/hooks/useDebounce';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { useTestMode } from '@/hooks/useTestMode';
import { useAuth } from '@/hooks/useAuth';
import type { Job, JobType, ExperienceLevel, PageResponse } from '@/types';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Loader } from '@/components/common/Loader';
import { ApiErrorFallback } from '@/components/error/ApiErrorFallback';
import { LoadTestPanel } from '@/components/testing/LoadTestPanel';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Search,
  MapPin,
  Clock,
  DollarSign,
  Briefcase,
  ChevronRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  return `${days}d ago`;
}

function formatSalary(min?: number, max?: number): string | null {
  if (!min && !max) return null;
  const fmt = (n: number) => (n >= 1000 ? `$${(n / 1000).toFixed(0)}K` : `$${n}`);
  if (min && max) return `${fmt(min)} - ${fmt(max)}`;
  if (min) return `From ${fmt(min)}`;
  return `Up to ${fmt(max!)}`;
}

const COMPANY_COLORS = [
  'bg-blue-500', 'bg-emerald-500', 'bg-purple-500', 'bg-orange-500',
  'bg-pink-500', 'bg-teal-500', 'bg-indigo-500', 'bg-rose-500',
];

export default function JobListPage() {
  const navigate = useNavigate();
  const { isTestMode } = useTestMode();
  const { user } = useAuth();

  // Search state
  const [keyword, setKeyword] = useState('');
  const [location, setLocation] = useState('');
  const [jobType, setJobType] = useState<JobType | ''>('');
  const [experienceLevel, setExperienceLevel] = useState<ExperienceLevel | ''>('');

  const debouncedKeyword = useDebounce(keyword, 400);
  const debouncedLocation = useDebounce(location, 400);

  // Data state
  const [jobs, setJobs] = useState<Job[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);

  const fetchJobs = useCallback(
    async (pageNum: number, append = false) => {
      setLoading(true);
      setError(null);
      try {
        const params = {
          keyword: debouncedKeyword || undefined,
          location: debouncedLocation || undefined,
          jobType: jobType || undefined,
          experienceLevel: experienceLevel || undefined,
          page: pageNum,
          size: 20,
        };
        const result: PageResponse<Job> = await jobService.searchJobs(params);
        setJobs((prev) => (append ? [...prev, ...result.content] : result.content));
        setHasMore(!result.last);
        if (!append && result.content.length > 0) {
          setSelectedJob(result.content[0]);
        }
      } catch {
        setError('Failed to load jobs. Please try again.');
      } finally {
        setLoading(false);
      }
    },
    [debouncedKeyword, debouncedLocation, jobType, experienceLevel]
  );

  // Reset on filter change
  useEffect(() => {
    setPage(0);
    fetchJobs(0);
  }, [fetchJobs]);

  const loadMore = useCallback(() => {
    const next = page + 1;
    setPage(next);
    fetchJobs(next, true);
  }, [page, fetchJobs]);

  const { sentinelRef } = useInfiniteScroll({ loading, hasMore, onLoadMore: loadMore });

  const salary = selectedJob ? formatSalary(selectedJob.salaryMin, selectedJob.salaryMax) : null;

  return (
    <div className="min-h-screen">
      {/* Hero Search */}
      <div className="bg-linear-to-br from-primary to-blue-700 px-4 py-16 text-center text-white">
        <h1 className="text-4xl font-bold sm:text-5xl">Find Your Dream Job</h1>
        <p className="mx-auto mt-3 max-w-2xl text-lg text-blue-100">
          Discover thousands of opportunities from leading companies
        </p>
        <div className="mx-auto mt-8 flex max-w-2xl flex-col gap-3 sm:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
            <Input
              placeholder="Job title, keyword, or company"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="h-12 bg-white pl-10 text-foreground"
            />
          </div>
          <div className="relative flex-1">
            <MapPin className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
            <Input
              placeholder="City or remote"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="h-12 bg-white pl-10 text-foreground"
            />
          </div>
          <Button
            size="lg"
            className="h-12 bg-white text-primary hover:bg-gray-100"
            onClick={() => fetchJobs(0)}
          >
            Search
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="border-b bg-white">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-3">
          <Select value={jobType} onValueChange={(v) => setJobType(v as JobType | '')}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Job Type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Types</SelectItem>
              <SelectItem value="FULL_TIME">Full Time</SelectItem>
              <SelectItem value="PART_TIME">Part Time</SelectItem>
              <SelectItem value="CONTRACT">Contract</SelectItem>
              <SelectItem value="INTERNSHIP">Internship</SelectItem>
              <SelectItem value="FREELANCE">Freelance</SelectItem>
            </SelectContent>
          </Select>

          <Select value={experienceLevel} onValueChange={(v) => setExperienceLevel(v as ExperienceLevel | '')}>
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Experience Level" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Levels</SelectItem>
              <SelectItem value="ENTRY">Entry Level</SelectItem>
              <SelectItem value="JUNIOR">Junior</SelectItem>
              <SelectItem value="MID">Mid Level</SelectItem>
              <SelectItem value="SENIOR">Senior</SelectItem>
              <SelectItem value="LEAD">Lead</SelectItem>
            </SelectContent>
          </Select>

          <span className="ml-auto text-sm text-muted-foreground">
            Sort by: Most relevant
          </span>
        </div>
      </div>

      {/* Two-Panel Layout */}
      <div className="mx-auto max-w-7xl px-4 py-6">
        {error ? (
          <ApiErrorFallback error={error} onRetry={() => fetchJobs(0)} />
        ) : (
          <div className="flex gap-6">
            {/* Left Panel — Job List */}
            <div className="w-full space-y-3 xl:w-100 xl:min-w-100">
              {jobs.map((job) => (
                <JobCard
                  key={job.id}
                  job={job}
                  isSelected={selectedJob?.id === job.id}
                  onClick={() => setSelectedJob(job)}
                />
              ))}
              {loading && <Loader message="Loading jobs..." className="py-8" />}
              <div ref={sentinelRef} className="h-1" />
              {!loading && jobs.length === 0 && (
                <div className="py-12 text-center text-muted-foreground">
                  No jobs found. Try adjusting your search criteria.
                </div>
              )}
            </div>

            {/* Right Panel — Selected Job Detail */}
            <div className="hidden flex-1 xl:block">
              {selectedJob ? (
                <div className="sticky top-20 rounded-lg border bg-white p-6">
                  <h2 className="text-2xl font-bold">{selectedJob.title}</h2>
                  <p className="mt-1 text-muted-foreground">
                    {selectedJob.company?.name || 'Unknown Company'}
                  </p>

                  <div className="mt-4 flex flex-wrap gap-2">
                    <Badge variant="secondary">{selectedJob.jobType?.replace('_', ' ')}</Badge>
                    <Badge variant="secondary">{selectedJob.experienceLevel}</Badge>
                    <Badge variant="outline">
                      <MapPin className="mr-1 h-3 w-3" />
                      {selectedJob.location}
                    </Badge>
                    {salary && (
                      <Badge variant="outline">
                        <DollarSign className="mr-1 h-3 w-3" />
                        {salary}
                      </Badge>
                    )}
                  </div>

                  <div className="mt-6 space-y-4">
                    <div>
                      <h3 className="font-semibold">Description</h3>
                      <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                        {selectedJob.description}
                      </p>
                    </div>

                    {selectedJob.skills.length > 0 && (
                      <div>
                        <h3 className="font-semibold">Skills</h3>
                        <div className="mt-2 flex flex-wrap gap-1.5">
                          {selectedJob.skills.map((skill) => (
                            <Badge key={skill} variant="secondary" className="text-xs">
                              {skill}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="mt-6 flex gap-3">
                    {user?.role === 'JOB_SEEKER' && (
                      <Button onClick={() => navigate(`/jobs/${selectedJob.id}/apply`)}>
                        Apply Now
                      </Button>
                    )}
                    <Button variant="outline" onClick={() => navigate(`/jobs/${selectedJob.id}`)}>
                      View Full Details
                      <ChevronRight className="ml-1 h-4 w-4" />
                    </Button>
                  </div>

                  {isTestMode && (
                    <LoadTestPanel jobId={selectedJob.id} jobTitle={selectedJob.title} />
                  )}
                </div>
              ) : (
                <div className="flex h-96 items-center justify-center rounded-lg border text-muted-foreground">
                  Select a job to see details
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── JobCard Component ──────────────────────────────────
const JobCard = memo(function JobCard({
  job,
  isSelected,
  onClick,
}: {
  job: Job;
  isSelected: boolean;
  onClick: () => void;
}) {
  const colorIndex = (job.company?.id || job.id || 0) % COMPANY_COLORS.length;
  const salary = formatSalary(job.salaryMin, job.salaryMax);

  return (
    <button
      onClick={onClick}
      className={cn(
        'w-full rounded-lg border p-4 text-left transition-all hover:shadow-md',
        isSelected ? 'border-primary bg-primary/5 shadow-md' : 'bg-white hover:border-gray-300'
      )}
    >
      <div className="flex gap-3">
        {/* Company Logo Placeholder */}
        <div
          className={cn(
            'flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-sm font-bold text-white',
            COMPANY_COLORS[colorIndex]
          )}
        >
          {(job.company?.name || 'C')[0].toUpperCase()}
        </div>

        <div className="min-w-0 flex-1">
          <h3 className="truncate font-semibold">{job.title}</h3>
          <p className="text-sm text-muted-foreground">
            {job.company?.name || 'Unknown Company'}
          </p>

          <div className="mt-2 flex flex-wrap gap-1.5">
            <Badge variant="secondary" className="text-xs">
              <Briefcase className="mr-1 h-3 w-3" />
              {job.jobType?.replace('_', ' ')}
            </Badge>
            <Badge variant="outline" className="text-xs">
              <MapPin className="mr-1 h-3 w-3" />
              {job.location}
            </Badge>
            {salary && (
              <Badge variant="outline" className="text-xs">
                <DollarSign className="mr-1 h-3 w-3" />
                {salary}
              </Badge>
            )}
          </div>

          <div className="mt-2 flex items-center text-xs text-muted-foreground">
            <Clock className="mr-1 h-3 w-3" />
            {timeAgo(job.createdAt)}
          </div>
        </div>
      </div>
    </button>
  );
});
