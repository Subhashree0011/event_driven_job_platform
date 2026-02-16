import { Link } from 'react-router-dom';
import { Briefcase, Github, Linkedin, Twitter } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

export function Footer() {
  return (
    <footer className="border-t bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-4">
          {/* Brand */}
          <div>
            <div className="flex items-center gap-2">
              <Briefcase className="h-6 w-6 text-primary" />
              <span className="text-lg font-bold">JobPlatform</span>
            </div>
            <p className="mt-3 text-sm text-muted-foreground">
              Connecting talented professionals with amazing companies. Find your next opportunity today.
            </p>
            <div className="mt-4 flex gap-3">
              <a href="#" className="text-muted-foreground hover:text-foreground">
                <Twitter className="h-5 w-5" />
              </a>
              <a href="#" className="text-muted-foreground hover:text-foreground">
                <Linkedin className="h-5 w-5" />
              </a>
              <a href="#" className="text-muted-foreground hover:text-foreground">
                <Github className="h-5 w-5" />
              </a>
            </div>
          </div>

          {/* Job Seekers */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-foreground">
              Job Seekers
            </h3>
            <ul className="mt-4 space-y-2">
              <li><Link to="/jobs" className="text-sm text-muted-foreground hover:text-foreground">Browse Jobs</Link></li>
              <li><Link to="/dashboard" className="text-sm text-muted-foreground hover:text-foreground">My Applications</Link></li>
              <li><a href="#" className="text-sm text-muted-foreground hover:text-foreground">Salary Tools</a></li>
              <li><a href="#" className="text-sm text-muted-foreground hover:text-foreground">Career Advice</a></li>
            </ul>
          </div>

          {/* Employers */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-foreground">
              Employers
            </h3>
            <ul className="mt-4 space-y-2">
              <li><Link to="/jobs/create" className="text-sm text-muted-foreground hover:text-foreground">Post a Job</Link></li>
              <li><Link to="/companies/create" className="text-sm text-muted-foreground hover:text-foreground">Register Company</Link></li>
              <li><a href="#" className="text-sm text-muted-foreground hover:text-foreground">Solutions</a></li>
              <li><a href="#" className="text-sm text-muted-foreground hover:text-foreground">Pricing</a></li>
            </ul>
          </div>

          {/* Newsletter */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-foreground">
              Stay Updated
            </h3>
            <p className="mt-4 text-sm text-muted-foreground">
              Subscribe to our newsletter for the latest opportunities.
            </p>
            <div className="mt-3 flex gap-2">
              <Input type="email" placeholder="Enter your email" className="h-9" />
              <Button size="sm">Subscribe</Button>
            </div>
          </div>
        </div>

        <div className="mt-8 border-t pt-8 text-center">
          <p className="text-xs text-muted-foreground">
            &copy; {new Date().getFullYear()} JobPlatform. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
