import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export default function BookmarksPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchBookmarks = async () => {
      setLoading(true);
      try {
        const response = await axios.get(`${API_BASE_URL}/student/bookmarks`, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        });
        setItems(response.data.data || []);
      } catch (err) {
        setError(err.response?.data?.message || 'Unable to load bookmarks.');
      } finally {
        setLoading(false);
      }
    };

    fetchBookmarks();
  }, []);

  if (loading) return <div className="min-h-screen p-8 text-center">Loading bookmarks...</div>;
  if (error) return <div className="min-h-screen p-8 text-center text-red-600">{error}</div>;

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-5xl card">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-3xl font-bold">Bookmarks</h1>
          <button className="btn-secondary" onClick={() => navigate('/student')}>Back</button>
        </div>

        {items.length === 0 ? (
          <p className="text-slate-600">No bookmarked papers yet.</p>
        ) : (
          <div className="space-y-4">
            {items.map((paper) => (
              <div key={paper.id} className="rounded-xl border p-4 flex items-center justify-between">
                <div>
                  <h3 className="font-semibold text-slate-900">{paper.title}</h3>
                  <p className="text-sm text-slate-500">{paper.universityName} • {paper.subjectName} • {paper.year}</p>
                </div>
                <button className="btn-primary" onClick={() => navigate(`/paper/${paper.id}`)}>View</button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
