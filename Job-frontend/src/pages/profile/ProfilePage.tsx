import { useEffect, useState, useCallback } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { authService } from '@/services/authService';
import { useAppDispatch } from '@/store/hooks';
import { addToast } from '@/store/uiSlice';
import { setUser } from '@/store/authSlice';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Loader } from '@/components/common/Loader';
import { Loader2, Save, User } from 'lucide-react';

export default function ProfilePage() {
  const { user } = useAuth();
  const dispatch = useAppDispatch();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    phone: '',
    bio: '',
    profilePictureUrl: '',
    resumeUrl: '',
    location: '',
  });

  const loadProfile = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const profile = await authService.getProfile();
      setForm({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        phone: profile.phone || '',
        bio: profile.bio || '',
        profilePictureUrl: profile.profilePictureUrl || '',
        resumeUrl: profile.resumeUrl || '',
        location: profile.location || '',
      });
    } catch {
      // use what we have from Redux
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleSave = async () => {
    setSaving(true);
    try {
      const updated = await authService.updateProfile({
        firstName: form.firstName || undefined,
        lastName: form.lastName || undefined,
        phone: form.phone || undefined,
        bio: form.bio || undefined,
        profilePictureUrl: form.profilePictureUrl || undefined,
        resumeUrl: form.resumeUrl || undefined,
        location: form.location || undefined,
      });
      dispatch(setUser(updated));
      dispatch(addToast({ type: 'success', message: 'Profile updated!' }));
      setEditing(false);
    } catch {
      dispatch(addToast({ type: 'error', message: 'Failed to update profile' }));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Loader fullPage />;
  if (!user) return null;

  const initials = `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || 'U';

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="grid gap-8 md:grid-cols-3">
        {/* Left column – avatar card */}
        <Card className="md:col-span-1 overflow-hidden">
          <div className="h-24 bg-linear-to-r from-primary to-blue-600" />
          <div className="-mt-10 flex flex-col items-center px-6 pb-6">
            <Avatar className="h-20 w-20 border-4 border-background">
              <AvatarImage src={user.profilePictureUrl} alt={user.firstName} />
              <AvatarFallback className="text-xl">{initials}</AvatarFallback>
            </Avatar>
            <h2 className="mt-3 text-lg font-semibold">
              {user.firstName} {user.lastName}
            </h2>
            <p className="text-sm text-muted-foreground">{user.email}</p>
            <Badge variant="outline" className="mt-2">
              {user.role?.replace('_', ' ')}
            </Badge>
          </div>
        </Card>

        {/* Right column – profile form */}
        <Card className="md:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5" />
              Profile Details
            </CardTitle>
            {!editing && (
              <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
                Edit
              </Button>
            )}
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="firstName">First Name</Label>
                {editing ? (
                  <Input
                    id="firstName"
                    value={form.firstName}
                    onChange={(e) => setForm((p) => ({ ...p, firstName: e.target.value }))}
                  />
                ) : (
                  <p className="text-sm text-muted-foreground">{form.firstName || '—'}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="lastName">Last Name</Label>
                {editing ? (
                  <Input
                    id="lastName"
                    value={form.lastName}
                    onChange={(e) => setForm((p) => ({ ...p, lastName: e.target.value }))}
                  />
                ) : (
                  <p className="text-sm text-muted-foreground">{form.lastName || '—'}</p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="phone">Phone</Label>
              {editing ? (
                <Input
                  id="phone"
                  value={form.phone}
                  onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))}
                />
              ) : (
                <p className="text-sm text-muted-foreground">{form.phone || '—'}</p>
              )}
            </div>

            <Separator />

            <div className="space-y-2">
              <Label htmlFor="bio">Bio</Label>
              {editing ? (
                <textarea
                  id="bio"
                  rows={3}
                  value={form.bio}
                  onChange={(e) => setForm((p) => ({ ...p, bio: e.target.value }))}
                  className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              ) : (
                <p className="text-sm text-muted-foreground">{form.bio || '—'}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="location">Location</Label>
              {editing ? (
                <Input
                  id="location"
                  value={form.location}
                  onChange={(e) => setForm((p) => ({ ...p, location: e.target.value }))}
                  placeholder="e.g. San Francisco, CA"
                />
              ) : (
                <p className="text-sm text-muted-foreground">{form.location || '—'}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="profilePictureUrl">Profile Picture URL</Label>
              {editing ? (
                <Input
                  id="profilePictureUrl"
                  type="url"
                  value={form.profilePictureUrl}
                  onChange={(e) => setForm((p) => ({ ...p, profilePictureUrl: e.target.value }))}
                />
              ) : (
                <p className="text-sm text-muted-foreground">{form.profilePictureUrl || '—'}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="resumeUrl">Resume URL</Label>
              {editing ? (
                <Input
                  id="resumeUrl"
                  type="url"
                  value={form.resumeUrl}
                  onChange={(e) => setForm((p) => ({ ...p, resumeUrl: e.target.value }))}
                />
              ) : (
                <p className="text-sm text-muted-foreground">{form.resumeUrl || '—'}</p>
              )}
            </div>

            {editing && (
              <div className="flex gap-3">
                <Button onClick={handleSave} disabled={saving}>
                  {saving ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="mr-2 h-4 w-4" />
                  )}
                  Save Changes
                </Button>
                <Button variant="outline" onClick={() => setEditing(false)}>
                  Cancel
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
