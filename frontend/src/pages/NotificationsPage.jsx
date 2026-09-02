import { useEffect, useState } from 'react';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export default function NotificationsPage() {
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

  if (loading) return <div className="min-h-screen p-8 text-center">Loading notifications...</div>;
  if (error) return <div className="min-h-screen p-8 text-center text-red-600">{error}</div>;

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-4xl card">
        <h1 className="mb-6 text-3xl font-bold">Notifications</h1>
        {items.length === 0 ? (
          <p className="text-slate-600">No notifications.</p>
        ) : (
          <div className="space-y-3">
            {items.map((n) => (
              <div key={n.id} className={`rounded-xl border p-4 ${n.isRead ? 'bg-slate-50' : 'bg-blue-50'}`}>
                <div className="flex justify-between gap-3">
                  <div>
                    <h3 className="font-semibold">{n.title}</h3>
                    <p className="text-sm text-slate-600">{n.message}</p>
                  </div>
                  {!n.isRead && (
                    <button className="btn-secondary" onClick={() => markRead(n.id)}>Mark read</button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
