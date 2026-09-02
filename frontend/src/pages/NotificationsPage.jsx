import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const homeRouteForRole = () => {
  const role = localStorage.getItem('role');
  if (role === 'FACULTY') return '/faculty';
  if (role === 'ADMIN') return '/admin';
  return '/student';
};

// Maps a notification's type/message to somewhere useful to land the user.
const resolveNotificationTarget = (notification) => {
  const type = (notification.type || '').toUpperCase();
  if (type.startsWith('ACCESS_')) {
    return '/student/uploads';
  }
  if (type.startsWith('PAPER_') || type.includes('DUPLICATE') || type.includes('UPLOAD') || type.includes('SUBJECT')) {
    return '/student/uploads';
  }
  if (type.startsWith('ACCOUNT_')) {
    return '/student/profile';
  }
  return null;
};

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/student/notifications`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setItems(response.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load notifications.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchNotifications(); }, []);

  const markRead = async (id) => {
    try {
      await axios.put(`${API_BASE_URL}/student/notifications/${id}/read`, {}, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      fetchNotifications();
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to mark notification as read.');
    }
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.isRead) {
      try {
        await axios.put(`${API_BASE_URL}/student/notifications/${notification.id}/read`, {}, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        });
      } catch (err) {
        // Non-fatal: still navigate even if marking-as-read fails.
      }
    }
    const target = resolveNotificationTarget(notification);
    if (target) {
      navigate(target);
    } else {
      fetchNotifications();
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-4xl">
        <div className="mb-4 flex items-center justify-between">
          <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <button type="button" className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
        </div>

        <div className="card">
          <h1 className="mb-6 text-3xl font-bold">Notifications</h1>

          {loading ? (
            <p className="text-slate-500">Loading notifications...</p>
          ) : error ? (
            <p className="text-red-600">{error}</p>
          ) : items.length === 0 ? (
            <p className="text-slate-600">No notifications.</p>
          ) : (
            <div className="space-y-3">
              {items.map((n) => (
                <div
                  key={n.id}
                  className={`cursor-pointer rounded-xl border p-4 transition hover:border-blue-300 hover:shadow-sm ${n.isRead ? 'bg-slate-50' : 'bg-blue-50'}`}
                  onClick={() => handleNotificationClick(n)}
                  role="button"
                  tabIndex={0}
                >
                  <div className="flex justify-between gap-3">
                    <div>
                      <h3 className="font-semibold">{n.title}</h3>
                      <p className="text-sm text-slate-600">{n.message}</p>
                    </div>
                    {!n.isRead && (
                      <button
                        className="btn-secondary shrink-0"
                        onClick={(e) => {
                          e.stopPropagation();
                          markRead(n.id);
                        }}
                      >
                        Mark read
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
