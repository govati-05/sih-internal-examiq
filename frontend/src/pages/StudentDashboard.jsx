import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export default function StudentDashboard() {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [stats, setStats] = useState({
    recentPapersCount: 0,
    bookmarksCount: 0,
    notificationsCount: 0,
    topRatedValue: 0
  });
  const [recentPapers, setRecentPapers] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    const fetchDashboard = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/student/dashboard`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        const payload = response.data.data || {};
        setStats(payload.stats || stats);
        setRecentPapers(payload.recentPapers || []);
        setNotifications(payload.notifications || []);
      } catch (error) {
        console.error('Dashboard load error:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, [navigate]);

  const handleSmartSearch = () => {
    const trimmed = searchTerm.trim();
    if (!trimmed) {
      navigate('/search');
      return;
    }
    navigate(`/search?q=${encodeURIComponent(trimmed)}`);
  };

  const handlePaperView = (paper) => {
    navigate(`/paper/${paper.id}`);
  };

  const quickStats = [
    { label: 'Recent papers', value: stats.recentPapersCount ?? 0 },
    { label: 'Bookmarks', value: stats.bookmarksCount ?? 0 },
    { label: 'Notifications', value: stats.notificationsCount ?? 0 },
    { label: 'Top rated', value: stats.topRatedValue ? stats.topRatedValue.toFixed(1) : '0.0' }
  ];

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 flex items-center justify-between rounded-2xl bg-slate-900 px-6 py-4 text-white">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-blue-300">EXAMIQ</p>
            <h1 className="mt-1 text-2xl font-bold">Student dashboard</h1>
          </div>
          <div className="flex gap-3 text-sm">
            <button className="btn-secondary bg-slate-800 text-white border-slate-700" onClick={() => navigate('/search')}>Search</button>
            <button className="btn-primary" onClick={() => navigate('/upload')}>Upload</button>
          </div>
        </header>

        <div className="mb-8 grid gap-4 md:grid-cols-4">
          {quickStats.map((stat) => (
            <div key={stat.label} className="card">
              <p className="text-sm text-slate-500">{stat.label}</p>
              <p className="mt-2 text-3xl font-bold text-slate-900">{stat.value}</p>
            </div>
          ))}
        </div>

        <div className="mb-8 card">
          <label className="mb-2 block text-sm font-medium text-slate-700">Smart search</label>
          <div className="flex flex-col gap-3 md:flex-row">
            <input
              className="flex-1 rounded-lg border border-slate-300 px-4 py-3"
              placeholder="Search by subject, topic, university, author or concept"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleSmartSearch();
                }
              }}
            />
            <button className="btn-primary" onClick={handleSmartSearch}>Search papers</button>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <div className="card">
              <h2 className="mb-4 text-xl font-semibold">Recently uploaded papers</h2>
              {loading ? (
                <p className="text-slate-500">Loading papers...</p>
              ) : recentPapers.length === 0 ? (
                <p className="text-slate-500">No approved papers yet.</p>
              ) : (
                <div className="space-y-4">
                  {recentPapers.map((paper) => (
                    <div key={paper.id} className="flex items-center justify-between rounded-xl border border-slate-200 p-4">
                      <div>
                        <h3 className="font-semibold text-slate-900">{paper.title}</h3>
                        <p className="text-sm text-slate-500">{paper.universityName} • {paper.subjectName} • {paper.year}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm text-yellow-600">★ {paper.averageRating ? paper.averageRating.toFixed(1) : 'N/A'}</p>
                        <button className="mt-2 text-sm font-medium text-blue-600" onClick={() => handlePaperView(paper)}>View</button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="space-y-6">
            <div className="card">
              <h2 className="mb-4 text-xl font-semibold">Notifications</h2>
              {notifications.length === 0 ? (
                <p className="text-sm text-slate-500">No notifications.</p>
              ) : (
                <ul className="space-y-3 text-sm text-slate-600">
                  {notifications.slice(0, 4).map((item) => (
                    <li key={item.id}>• {item.title}</li>
                  ))}
                </ul>
              )}
            </div>
            <div className="card">
              <h2 className="mb-4 text-xl font-semibold">Quick actions</h2>
              <div className="space-y-3">
                <button className="btn-secondary w-full" onClick={() => navigate('/student/uploads')}>My uploads</button>
                <button className="btn-secondary w-full" onClick={() => navigate('/student/bookmarks')}>Bookmarks</button>
                <button className="btn-secondary w-full" onClick={() => navigate('/student/profile')}>Profile</button>
                <button className="btn-secondary w-full" onClick={() => navigate('/student/notifications')}>Notifications</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
