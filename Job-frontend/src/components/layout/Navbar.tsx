import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { useTestMode } from '@/hooks/useTestMode';
import {
  Briefcase,
  LayoutDashboard,
  User,
  LogOut,
  PlusCircle,
  Menu,
  BarChart3,
} from 'lucide-react';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';
import { TestModeToggle } from '@/components/testing/TestModeToggle';
import { TestModeBanner } from '@/components/testing/TestModeBanner';

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const { isTestMode } = useTestMode();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const isEmployer = user?.role === 'EMPLOYER';

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const initials = user
    ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase()
    : '';

  const navLinks = (
    <>
      <Link
        to="/jobs"
        className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        onClick={() => setMobileOpen(false)}
      >
        <Briefcase className="h-4 w-4" />
        Find Jobs
      </Link>
      {isAuthenticated && (
        <Link
          to="/dashboard"
          className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
          onClick={() => setMobileOpen(false)}
        >
          <LayoutDashboard className="h-4 w-4" />
          Dashboard
        </Link>
      )}
      {isTestMode && (
        <Link
          to="/test-results"
          className="flex items-center gap-2 text-sm font-medium text-amber-600 hover:text-amber-700 transition-colors"
          onClick={() => setMobileOpen(false)}
        >
          <BarChart3 className="h-4 w-4" />
          Test Results
        </Link>
      )}
    </>
  );

  return (
    <>
      {isTestMode && <TestModeBanner />}
      <nav className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-backdrop-filter:bg-background/60">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Logo */}
          <Link to="/jobs" className="flex items-center gap-2">
            <Briefcase className="h-6 w-6 text-primary" />
            <span className="text-xl font-bold text-foreground">JobPlatform</span>
          </Link>

          {/* Desktop Nav */}
          <div className="hidden items-center gap-6 md:flex">
            {navLinks}
          </div>

          {/* Right Section */}
          <div className="flex items-center gap-3">
            <TestModeToggle />

            {isAuthenticated ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button className="flex items-center gap-2 rounded-full focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback className="bg-primary text-primary-foreground text-xs">
                        {initials}
                      </AvatarFallback>
                    </Avatar>
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-48">
                  <DropdownMenuLabel>
                    {user?.firstName} {user?.lastName}
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={() => navigate('/dashboard')}>
                    <LayoutDashboard className="mr-2 h-4 w-4" />
                    Dashboard
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => navigate('/profile')}>
                    <User className="mr-2 h-4 w-4" />
                    Profile
                  </DropdownMenuItem>
                  {isEmployer && (
                    <DropdownMenuItem onClick={() => navigate('/jobs/create')}>
                      <PlusCircle className="mr-2 h-4 w-4" />
                      Post Job
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={handleLogout}>
                    <LogOut className="mr-2 h-4 w-4" />
                    Logout
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <div className="hidden gap-2 md:flex">
                <Button variant="ghost" onClick={() => navigate('/login')}>
                  Sign In
                </Button>
                <Button onClick={() => navigate('/register')}>Get Started</Button>
              </div>
            )}

            {/* Mobile Menu */}
            <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
              <SheetTrigger asChild className="md:hidden">
                <Button variant="ghost" size="icon">
                  <Menu className="h-5 w-5" />
                </Button>
              </SheetTrigger>
              <SheetContent side="right">
                <SheetHeader>
                  <SheetTitle>Menu</SheetTitle>
                </SheetHeader>
                <div className="mt-6 flex flex-col gap-4">
                  {navLinks}
                  {!isAuthenticated && (
                    <>
                      <Button variant="ghost" onClick={() => { navigate('/login'); setMobileOpen(false); }}>
                        Sign In
                      </Button>
                      <Button onClick={() => { navigate('/register'); setMobileOpen(false); }}>
                        Get Started
                      </Button>
                    </>
                  )}
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </nav>
    </>
  );
}
