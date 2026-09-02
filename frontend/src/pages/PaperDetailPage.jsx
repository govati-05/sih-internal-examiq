import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export default function PaperDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [paper, setPaper] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bookmarked, setBookmarked] = useState(false);

  useEffect(() => {
    const fetchPaper = async () => {
      setLoading(true);
      try {
        const response = await axios.get(`${API_BASE_URL}/papers/${id}`, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        });
        setPaper(response.data.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Paper could not be loaded.');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchPaper();
    }
  }, [id]);

  const handleBookmarkToggle = async () => {
    if (!paper) return;
    try {
      if (bookmarked) {
        await axios.delete(`${API_BASE_URL}/student/bookmarks/${paper.id}`, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        });
        setBookmarked(false);
      } else {
        await axios.post(`${API_BASE_URL}/student/bookmarks/${paper.id}`, {}, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        });
        setBookmarked(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to update bookmark.');
    }
  };

  if (loading) return <div className="min-h-screen p-8 text-center">Loading paper...</div>;
  if (error) return <div className="min-h-screen p-8 text-center text-red-600">{error}</div>;
  if (!paper) return <div className="min-h-screen p-8 text-center">Paper not found.</div>;

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-4xl card">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-3xl font-bold">{paper.title}</h1>
          <button className="btn-secondary" onClick={() => navigate('/student')}>Back</button>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg border p-4">
            <p className="text-sm text-slate-500">Subject</p>
            <p className="mt-1 font-semibold">{paper.subjectName}</p>
          </div>
          <div className="rounded-lg border p-4">
            <p className="text-sm text-slate-500">University</p>
            <p className="mt-1 font-semibold">{paper.universityName}</p>
          </div>
          <div className="rounded-lg border p-4">
            <p className="text-sm text-slate-500">Year</p>
            <p className="mt-1 font-semibold">{paper.year || 'N/A'}</p>
          </div>
          <div className="rounded-lg border p-4">
            <p className="text-sm text-slate-500">Exam type</p>
            <p className="mt-1 font-semibold">{paper.examType || 'N/A'}</p>
          </div>
          <div className="rounded-lg border p-4">
            <p className="text-sm text-slate-500">Author</p>
            <p className="mt-1 font-semibold">{paper.author || 'N/A'}</p>
          </div>
          <div className="rounded-lg border p-4">
            <p className="text-sm text-slate-500">Rating</p>
            <p className="mt-1 font-semibold">{paper.averageRating ?? 'N/A'}</p>
          </div>
        </div>

        <div className="mt-6 flex gap-3">
          <button className="btn-primary" onClick={handleBookmarkToggle}>{bookmarked ? 'Remove bookmark' : 'Bookmark paper'}</button>
          {paper.fileUrl && (
            <a href={`http://localhost:8080${paper.fileUrl.replace(/\\/g, '/')}`} target="_blank" rel="noreferrer" className="btn-secondary">
              Open file
            </a>
          )}
        </div>
      </div>
    </div>
  );
}
