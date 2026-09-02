import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export default function MyUploadsPage() {
  const navigate = useNavigate();
  const [uploads, setUploads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchUploads = async () => {
      setLoading(true);
      try {
        const response = await axios.get(`${API_BASE_URL}/student/uploads`, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
        });
        setUploads(response.data.data || []);
      } catch (err) {
        setError(err.response?.data?.message || 'Unable to load your uploads.');
      } finally {
        setLoading(false);
      }
    };

    fetchUploads();
  }, []);

  if (loading) return <div className="min-h-screen p-8 text-center">Loading uploads...</div>;
  if (error) return <div className="min-h-screen p-8 text-center text-red-600">{error}</div>;

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-5xl card">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-3xl font-bold">My uploads</h1>
          <button className="btn-secondary" onClick={() => navigate('/student')}>Back</button>
        </div>

        {uploads.length === 0 ? (
          <p className="text-slate-600">No uploads yet.</p>
        ) : (
          <div className="space-y-4">
            {uploads.map((upload) => (
              <div key={upload.id} className="rounded-xl border p-4 flex items-center justify-between">
                <div>
                  <h3 className="font-semibold text-slate-900">{upload.paperTitle}</h3>
                  <p className="text-sm text-slate-500">{upload.fileName}</p>
                  <p className="text-sm text-slate-500">Status: {upload.status}</p>
                </div>
                {upload.paperId && (
                  <button className="btn-primary" onClick={() => navigate(`/paper/${upload.paperId}`)}>View</button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
