import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';
const SERVER_ORIGIN = 'http://localhost:8080';

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
    profile.role,
    profile.branch,
    profile.year,
    profile.profilePictureUrl
  ];

  const completed = checks.filter((value) => typeof value === 'string' ? value.trim() : value).length;
  return Math.min(100, Math.round((completed / checks.length) * 100));
};

const homeRouteForRole = () => {
  const role = localStorage.getItem('role');
  if (role === 'FACULTY') return '/faculty';
  if (role === 'ADMIN') return '/admin';
  return '/student';
};

export default function ProfilePage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saveState, setSaveState] = useState({ loading: false, success: false, error: false });
  const [pictureState, setPictureState] = useState({ loading: false, error: '' });

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
          university: profile.university || '',
          branch: profile.branch || '',
          year: profile.year != null ? String(profile.year) : '',
          section: profile.section || '',
          bio: profile.bio || ''
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

  const handlePictureSelect = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      setPictureState({ loading: false, error: 'Only JPG, PNG, or WEBP images are allowed.' });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setPictureState({ loading: false, error: 'Profile picture must be smaller than 5MB.' });
      return;
    }

    setPictureState({ loading: true, error: '' });
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await axios.post(`${API_BASE_URL}/profile/picture`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      });
      const { profilePictureUrl } = response.data.data || {};
      setProfile((prev) => ({ ...prev, profilePictureUrl }));
      setPictureState({ loading: false, error: '' });
    } catch (err) {
      setPictureState({ loading: false, error: err.response?.data?.message || 'Unable to upload picture.' });
    }
  };

  const handleRemovePicture = async () => {
    setPictureState({ loading: true, error: '' });
    try {
      await axios.delete(`${API_BASE_URL}/profile/picture`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setProfile((prev) => ({ ...prev, profilePictureUrl: null }));
      setPictureState({ loading: false, error: '' });
    } catch (err) {
      setPictureState({ loading: false, error: err.response?.data?.message || 'Unable to remove picture.' });
    }
  };

  const completion = getProfileCompletion(profile);
  const badgeText = (profile?.status || 'ACTIVE').toUpperCase();
  const avatarUrl = profile?.profilePictureUrl ? `${SERVER_ORIGIN}${profile.profilePictureUrl}` : null;

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
            <div className="mt-6 flex justify-center gap-3">
              <button
                type="button"
                onClick={fetchProfile}
                className="inline-flex items-center justify-center rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-blue-200 transition hover:-translate-y-0.5 hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-200"
              >
                Try Again
              </button>
              <button
                type="button"
                onClick={() => navigate(homeRouteForRole())}
                className="btn-secondary"
              >
                Home
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-8 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-6xl animate-fade-up">
        <div className="mb-4 flex items-center justify-between">
          <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <button type="button" className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
        </div>

        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-[0_25px_80px_rgba(15,23,42,0.08)] sm:p-8 lg:p-10">
          <header className="mb-8 flex flex-col gap-6 rounded-[24px] bg-gradient-to-r from-slate-50 via-blue-50 to-indigo-50 p-5 sm:p-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-4">
              <div className="profile-avatar-wrap relative">
                {avatarUrl ? (
                  <img src={avatarUrl} alt="Profile avatar" className="profile-avatar object-cover" style={{ padding: 0 }} />
                ) : (
                  <div className="profile-avatar" aria-label="Profile avatar">
                    {getInitials(profile?.fullName || profile?.username || 'Student')}
                  </div>
                )}
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
                  {profile?.contributorScore?.badge && (
                    <span className="inline-flex items-center gap-2 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-amber-700">
                      🏅 {profile.contributorScore.badge} ({profile.contributorScore.points} pts)
                    </span>
                  )}
                </div>

                <div className="flex flex-wrap items-center gap-2 pt-1">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={handlePictureSelect}
                  />
                  <button
                    type="button"
                    className="btn-secondary text-xs"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={pictureState.loading}
                  >
                    {avatarUrl ? 'Replace photo' : 'Upload photo'}
                  </button>
                  {avatarUrl && (
                    <button
                      type="button"
                      className="btn-secondary text-xs"
                      onClick={handleRemovePicture}
                      disabled={pictureState.loading}
                    >
                      Remove photo
                    </button>
                  )}
                  {pictureState.loading && <span className="text-xs text-slate-500">Saving…</span>}
                </div>
                {pictureState.error && <p className="text-xs text-red-600">{pictureState.error}</p>}
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

                  <div className="field-group">
                    <label htmlFor="branch" className="field-label">Branch / Department</label>
                    <input
                      id="branch"
                      name="branch"
                      type="text"
                      value={profile?.branch || ''}
                      onChange={handleChange}
                      className="field-input"
                      placeholder="e.g., Computer Science"
                    />
                  </div>

                  <div className="field-group">
                    <label htmlFor="year" className="field-label">Year</label>
                    <select
                      id="year"
                      name="year"
                      value={profile?.year || ''}
                      onChange={handleChange}
                      className="field-input"
                    >
                      <option value="">Select year</option>
                      <option value="1">1st Year</option>
                      <option value="2">2nd Year</option>
                      <option value="3">3rd Year</option>
                      <option value="4">4th Year</option>
                    </select>
                  </div>

                  <div className="field-group">
                    <label htmlFor="section" className="field-label">Section</label>
                    <input
                      id="section"
                      name="section"
                      type="text"
                      value={profile?.section || ''}
                      onChange={handleChange}
                      className="field-input"
                      placeholder="e.g., A"
                    />
                  </div>

                  <div className="field-group md:col-span-2">
                    <label htmlFor="bio" className="field-label">Bio</label>
                    <textarea
                      id="bio"
                      name="bio"
                      rows={3}
                      value={profile?.bio || ''}
                      onChange={handleChange}
                      className="field-input"
                      placeholder="Tell others a bit about yourself"
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
                  <p className="info-label">Contributor Score</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {profile?.contributorScore?.points ?? 0} pts · {profile?.contributorScore?.badge || 'Contributor'}
                  </p>
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
