import { useEffect, useState } from 'react';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const getInitials = (value) => {
  if (!value || typeof value !== 'string') return 'S';
  const words = value.trim().split(/\s+/).filter(Boolean);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return `${words[0][0]}${words[1][0]}`.toUpperCase();
};

const formatDate = (value) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  }).format(date);
};

const getProfileCompletion = (profile) => {
  if (!profile) return 0;
  const checks = [
    profile.username,
    profile.fullName,
    profile.email,
    profile.university,
    profile.role
  ];

  const completed = checks.filter((value) => typeof value === 'string' ? value.trim() : value).length;
  return Math.min(100, Math.round((completed / checks.length) * 100));
};

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saveState, setSaveState] = useState({ loading: false, success: false, error: false });

  const fetchProfile = async () => {
    setLoading(true);
    setError('');
    setSaveState((prev) => ({ ...prev, success: false, error: false }));

    try {
      const response = await axios.get(`${API_BASE_URL}/student/profile`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setProfile(response.data.data || null);
    } catch (err) {
      setProfile(null);
      setError(err.response?.data?.message || 'Unable to load your profile');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setProfile((prev) => ({ ...prev, [name]: value }));
    setSaveState((prev) => ({ ...prev, success: false, error: false }));
  };

  const handleUpdate = async (event) => {
    event.preventDefault();
    if (!profile) return;

    setSaveState({ loading: true, success: false, error: false });

    try {
      const response = await axios.put(
        `${API_BASE_URL}/student/profile`,
        {
          fullName: profile.fullName,
          email: profile.email,
          university: profile.university || ''
        },
        {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        }
      );

      const updatedProfile = response.data.data || profile;
      setProfile(updatedProfile);
      setError('');
      setSaveState({ loading: false, success: true, error: false });
    } catch (err) {
      setSaveState({ loading: false, success: false, error: true });
      setError(err.response?.data?.message || 'Profile update failed. Please try again.');
    }
  };

  const completion = getProfileCompletion(profile);
  const badgeText = (profile?.status || 'ACTIVE').toUpperCase();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-100 px-4 py-8 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-6xl animate-fade-up">
          <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_20px_60px_rgba(15,23,42,0.08)] sm:p-8">
            <div className="mb-8 flex items-center gap-4">
              <div className="skeleton-avatar shimmer" />
              <div className="flex-1 space-y-3">
                <div className="skeleton-line shimmer w-2/3" />
                <div className="skeleton-line shimmer w-1/2" />
                <div className="skeleton-line shimmer w-1/3" />
              </div>
            </div>

            <div className="grid gap-6 lg:grid-cols-[1.4fr_0.6fr]">
              <div className="space-y-4">
                <div className="skeleton-card shimmer" />
                <div className="skeleton-card shimmer" />
                <div className="skeleton-card shimmer" />
              </div>
              <div className="space-y-4">
                <div className="skeleton-card shimmer" />
                <div className="skeleton-card shimmer" />
                <div className="skeleton-card shimmer" />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error && !profile) {
    return (
      <div className="min-h-screen bg-slate-100 px-4 py-8 sm:px-6">
        <div className="mx-auto max-w-md">
          <div className="rounded-[28px] border border-red-100 bg-white p-8 text-center shadow-[0_18px_50px_rgba(15,23,42,0.08)] animate-fade-up">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-2xl text-red-500">!</div>
            <h1 className="text-2xl font-bold text-slate-900">Unable to load your profile</h1>
            <p className="mt-3 text-sm text-slate-600">{error}</p>
            <button
              type="button"
              onClick={fetchProfile}
              className="mt-6 inline-flex items-center justify-center rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-blue-200 transition hover:-translate-y-0.5 hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-200"
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-6xl animate-fade-up">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_25px_80px_rgba(15,23,42,0.08)] sm:p-8 lg:p-10">
          <header className="mb-8 flex flex-col gap-6 rounded-[24px] bg-gradient-to-r from-slate-50 via-blue-50 to-indigo-50 p-5 sm:p-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-4">
              <div className="profile-avatar-wrap">
                <div className="profile-avatar" aria-label="Profile avatar">
                  {getInitials(profile?.fullName || profile?.username || 'Student')}
                </div>
              </div>

              <div className="space-y-2">
                <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
                  {profile?.fullName || 'Student'}
                </h1>
                <p className="text-sm text-slate-600">@{profile?.username || 'student'}</p>
                <div className="flex flex-wrap items-center gap-3 text-xs font-medium">
                  <span className="role-badge">{profile?.role || 'STUDENT'}</span>
                  <span className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-emerald-700">
                    <span className="h-2 w-2 rounded-full bg-emerald-500" aria-hidden="true" />
                    {badgeText === 'ACTIVE' ? 'Active' : badgeText}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 text-sm text-slate-500">
              <div className="rounded-full border border-slate-200 bg-white px-3 py-1.5 shadow-sm">{profile?.role || 'STUDENT'}</div>
            </div>
          </header>

          <div className="grid gap-6 lg:grid-cols-[1.5fr_0.8fr]">
            <section className="rounded-[24px] border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
              <div className="mb-6 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-600">Profile</p>
                  <h2 className="mt-2 text-xl font-bold text-slate-900">Personal Information</h2>
                </div>
                <div className="rounded-full bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700">
                  {completion}% complete
                </div>
              </div>

              <form onSubmit={handleUpdate} className="space-y-5">
                <div className="grid gap-5 md:grid-cols-2">
                  <div className="field-group">
                    <label htmlFor="username" className="field-label">Username</label>
                    <input
                      id="username"
                      name="username"
                      value={profile?.username || ''}
                      disabled
                      readOnly
                      className="field-input read-only"
                    />
                  </div>

                  <div className="field-group">
                    <label htmlFor="role" className="field-label">Role</label>
                    <input
                      id="role"
                      name="role"
                      value={profile?.role || 'STUDENT'}
                      disabled
                      readOnly
                      className="field-input read-only"
                    />
                  </div>

                  <div className="field-group md:col-span-2">
                    <label htmlFor="fullName" className="field-label">Full Name</label>
                    <input
                      id="fullName"
                      name="fullName"
                      type="text"
                      value={profile?.fullName || ''}
                      onChange={handleChange}
                      className="field-input"
                      placeholder="Enter your full name"
                    />
                  </div>

                  <div className="field-group md:col-span-2">
                    <label htmlFor="email" className="field-label">Email</label>
                    <input
                      id="email"
                      name="email"
                      type="email"
                      value={profile?.email || ''}
                      onChange={handleChange}
                      className="field-input"
                      placeholder="Enter your email"
                    />
                  </div>

                  <div className="field-group md:col-span-2">
                    <label htmlFor="university" className="field-label">University</label>
                    <input
                      id="university"
                      name="university"
                      type="text"
                      value={profile?.university || ''}
                      onChange={handleChange}
                      className="field-input"
                      placeholder="Enter your university"
                    />
                  </div>
                </div>

                {error && !saveState.success && !saveState.loading && (
                  <div className={`inline-flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 ${saveState.error ? 'animate-shake' : ''}`} role="alert">
                    <span aria-hidden="true">⚠</span>
                    {error}
                  </div>
                )}

                {saveState.success && (
                  <div className="success-message" role="status" aria-live="polite">
                    <span className="success-check" aria-hidden="true">✓</span>
                    Profile updated successfully
                  </div>
                )}

                <button
                  type="submit"
                  disabled={saveState.loading}
                  className="save-button"
                >
                  {saveState.loading ? (
                    <>
                      <span className="save-spinner" aria-hidden="true" />
                      Saving...
                    </>
                  ) : (
                    'Save Changes'
                  )}
                </button>
              </form>
            </section>

            <aside className="space-y-4">
              <div className="rounded-[24px] border border-slate-200 bg-white p-4 shadow-sm sm:p-5 animate-card">
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-sm font-semibold text-slate-600">Profile completion</p>
                  <span className="text-sm font-semibold text-blue-700">{completion}%</span>
                </div>
                <div className="h-3 overflow-hidden rounded-full bg-slate-200">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-blue-500 to-indigo-500 transition-[width] duration-700 ease-out"
                    style={{ width: `${completion}%` }}
                  />
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-1">
                <div className="info-card animate-card delay-100">
                  <p className="info-label">Account Status</p>
                  <div className="mt-2 flex items-center gap-2 text-lg font-semibold text-slate-900">
                    <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" aria-hidden="true" />
                    Active
                  </div>
                </div>

                <div className="info-card animate-card delay-200">
                  <p className="info-label">Role</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">{profile?.role || 'STUDENT'}</p>
                </div>

                <div className="info-card animate-card delay-300">
                  <p className="info-label">Member Since</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">{formatDate(profile?.createdAt)}</p>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </div>
    </div>
  );
}
