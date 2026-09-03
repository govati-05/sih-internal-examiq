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

const STATUS_STYLES = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-emerald-100 text-emerald-700',
  REJECTED: 'bg-red-100 text-red-700'
};

export default function AccessRequestsPage() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionState, setActionState] = useState({});

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/access-requests/owner`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setRequests(response.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load access requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchRequests(); }, []);

  const decide = async (id, approve) => {
    setActionState((prev) => ({ ...prev, [id]: true }));
    try {
      await axios.put(`${API_BASE_URL}/access-requests/${id}/${approve ? 'approve' : 'reject'}`, {}, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      await fetchRequests();
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to update this request.');
    } finally {
      setActionState((prev) => ({ ...prev, [id]: false }));
    }
  };

  const pending = requests.filter((r) => r.status === 'PENDING');
  const decided = requests.filter((r) => r.status !== 'PENDING');

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-5xl space-y-4">
        <div className="flex items-center justify-between">
          <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <button className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
        </div>

        <div className="card">
          <h1 className="mb-2 text-3xl font-bold">Access Requests</h1>
          <p className="mb-6 text-sm text-slate-500">Manage who can view or download your private resources.</p>

          {loading ? (
            <p className="text-slate-500">Loading requests...</p>
          ) : error ? (
            <p className="text-red-600">{error}</p>
          ) : requests.length === 0 ? (
            <p className="text-slate-600">No access requests yet.</p>
          ) : (
            <div className="space-y-6">
              {pending.length > 0 && (
                <div>
                  <h2 className="mb-3 text-lg font-semibold text-slate-800">Pending ({pending.length})</h2>
                  <div className="space-y-3">
                    {pending.map((req) => (
                      <div key={req.id} className="rounded-xl border border-amber-200 bg-amber-50/40 p-4">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="font-semibold text-slate-900">{req.requesterName}</p>
                            <p className="text-sm text-slate-500">
                              {req.requesterBranch || 'Branch N/A'} • {req.requesterYear ? `${req.requesterYear} Year` : 'Year N/A'}
                            </p>
                            <p className="mt-2 text-sm text-slate-700">
                              Requesting <strong>{req.permissionLevel === 'VIEW_DOWNLOAD' ? 'View + Download' : 'View Only'}</strong> access to
                              <span className="font-medium"> {req.paperTitle}</span>
                            </p>
                            {req.message && <p className="mt-1 text-sm italic text-slate-500">"{req.message}"</p>}
                            <p className="mt-1 text-xs text-slate-400">{new Date(req.createdAt).toLocaleString()}</p>
                          </div>
                          <div className="flex gap-2">
                            <button
                              className="btn-primary"
                              disabled={actionState[req.id]}
                              onClick={() => decide(req.id, true)}
                            >
                              Approve
                            </button>
                            <button
                              className="btn-secondary"
                              disabled={actionState[req.id]}
                              onClick={() => decide(req.id, false)}
                            >
                              Reject
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {decided.length > 0 && (
                <div>
                  <h2 className="mb-3 text-lg font-semibold text-slate-800">History</h2>
                  <div className="space-y-2">
                    {decided.map((req) => (
                      <div key={req.id} className="flex items-center justify-between rounded-xl border border-slate-200 p-3">
                        <div>
                          <p className="text-sm font-medium text-slate-800">{req.requesterName} — {req.paperTitle}</p>
                          <p className="text-xs text-slate-500">{req.permissionLevel === 'VIEW_DOWNLOAD' ? 'View + Download' : 'View Only'}</p>
                        </div>
                        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLES[req.status] || 'bg-slate-100 text-slate-700'}`}>
                          {req.status}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
