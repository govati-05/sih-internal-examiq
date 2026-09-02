import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const homeRouteForRole = () => {
  const role = localStorage.getItem('role');
  if (role === 'FACULTY') return '/faculty';
  if (role === 'ADMIN') return '/admin';
  return '/student';
};

const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });

export default function PaperDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [paper, setPaper] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bookmarked, setBookmarked] = useState(false);
  const [accessStatus, setAccessStatus] = useState(null);
  const [requestForm, setRequestForm] = useState({ permissionLevel: 'VIEW', message: '' });
  const [requestState, setRequestState] = useState({ loading: false, error: '', success: false });
  const [repeatedQuestions, setRepeatedQuestions] = useState([]);
  const [fileBusy, setFileBusy] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [paperRes, statusRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/papers/${id}`, { headers: authHeaders() }),
        axios.get(`${API_BASE_URL}/papers/${id}/access-status`, { headers: authHeaders() }).catch(() => null)
      ]);
      setPaper(paperRes.data.data);
      if (statusRes) {
        setAccessStatus(statusRes.data.data);
      }
      try {
        const rq = await axios.get(`${API_BASE_URL}/papers/${id}/repeated-questions`, { headers: authHeaders() });
        setRepeatedQuestions(rq.data.data || []);
      } catch (e) {
        setRepeatedQuestions([]);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Paper could not be loaded.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchAll();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleBookmarkToggle = async () => {
    if (!paper) return;
    try {
      if (bookmarked) {
        await axios.delete(`${API_BASE_URL}/student/bookmarks/${paper.id}`, { headers: authHeaders() });
        setBookmarked(false);
      } else {
        await axios.post(`${API_BASE_URL}/student/bookmarks/${paper.id}`, {}, { headers: authHeaders() });
        setBookmarked(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to update bookmark.');
    }
  };

  const openFile = async (mode) => {
    setFileBusy(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/papers/${paper.id}/file`, {
        headers: authHeaders(),
        params: { mode },
        responseType: 'blob'
      });
      const blobUrl = window.URL.createObjectURL(response.data);
      if (mode === 'download') {
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = paper.title || 'resource';
        document.body.appendChild(link);
        link.click();
        link.remove();
      } else {
        window.open(blobUrl, '_blank', 'noreferrer');
      }
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 30000);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to access this file.');
    } finally {
      setFileBusy(false);
    }
  };

  const handleAccessRequest = async (e) => {
    e.preventDefault();
    setRequestState({ loading: true, error: '', success: false });
    try {
      await axios.post(`${API_BASE_URL}/papers/${paper.id}/access-requests`, requestForm, { headers: authHeaders() });
      setRequestState({ loading: false, error: '', success: true });
      const statusRes = await axios.get(`${API_BASE_URL}/papers/${id}/access-status`, { headers: authHeaders() });
      setAccessStatus(statusRes.data.data);
    } catch (err) {
      setRequestState({ loading: false, error: err.response?.data?.message || 'Unable to submit request.', success: false });
    }
  };

  if (loading) return <div className="min-h-screen p-8 text-center">Loading paper...</div>;
  if (error && !paper) return <div className="min-h-screen p-8 text-center text-red-600">{error}</div>;
  if (!paper) return <div className="min-h-screen p-8 text-center">Paper not found.</div>;

  const isPrivate = paper.accessType === 'REQUEST_ACCESS';
  const accessLevel = accessStatus?.accessLevel || 'NONE';
  const isOwner = accessStatus?.isOwner;
  const canView = !isPrivate || accessLevel !== 'NONE' || isOwner;
  const canDownload = !isPrivate || accessLevel === 'VIEW_DOWNLOAD' || isOwner;
  const latestRequestStatus = accessStatus?.latestRequestStatus;

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-4xl space-y-4">
        <div className="flex items-center justify-between">
          <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <div className="flex gap-2">
            {isOwner && (
              <button className="btn-secondary" onClick={() => navigate('/access-requests')}>Access Requests</button>
            )}
            <button className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
          </div>
        </div>

        <div className="card">
          <div className="mb-6 flex items-center justify-between">
            <h1 className="text-3xl font-bold">{paper.title}</h1>
            {isPrivate && (
              <span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-semibold text-amber-800">🔒 Access Required</span>
            )}
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
              <p className="text-sm text-slate-500">Exam Year</p>
              <p className="mt-1 font-semibold">{paper.year || 'N/A'}</p>
            </div>
            <div className="rounded-lg border p-4">
              <p className="text-sm text-slate-500">Academic Year</p>
              <p className="mt-1 font-semibold">{paper.studentYear ? `${paper.studentYear} Year` : 'N/A'}</p>
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
            <div className="rounded-lg border p-4">
              <p className="text-sm text-slate-500">Views / Downloads</p>
              <p className="mt-1 font-semibold">{paper.viewCount ?? 0} views · {paper.downloadCount ?? 0} downloads</p>
            </div>
          </div>

          {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

          <div className="mt-6 flex flex-wrap gap-3">
            <button className="btn-primary" onClick={handleBookmarkToggle}>{bookmarked ? 'Remove bookmark' : 'Bookmark paper'}</button>
            {canView && (
              <button className="btn-secondary" disabled={fileBusy} onClick={() => openFile('view')}>
                {fileBusy ? 'Opening…' : 'View file'}
              </button>
            )}
            {canDownload && (
              <button className="btn-secondary" disabled={fileBusy} onClick={() => openFile('download')}>
                {fileBusy ? 'Please wait…' : 'Download file'}
              </button>
            )}
          </div>
        </div>

        {isPrivate && !isOwner && accessLevel !== 'VIEW_DOWNLOAD' && (
          <div className="card">
            <h2 className="mb-3 text-xl font-semibold">Request Access</h2>
            {latestRequestStatus === 'PENDING' ? (
              <p className="text-sm text-amber-700">Your access request is pending review by the resource owner.</p>
            ) : (
              <>
                {latestRequestStatus === 'REJECTED' && (
                  <p className="mb-3 text-sm text-red-600">Your previous request was rejected. You may request again below.</p>
                )}
                <form onSubmit={handleAccessRequest} className="space-y-4">
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">Permission requested</label>
                    <div className="flex gap-4">
                      <label className="flex items-center gap-2 text-sm">
                        <input
                          type="radio"
                          checked={requestForm.permissionLevel === 'VIEW'}
                          onChange={() => setRequestForm((f) => ({ ...f, permissionLevel: 'VIEW' }))}
                        />
                        👁 View Only
                      </label>
                      <label className="flex items-center gap-2 text-sm">
                        <input
                          type="radio"
                          checked={requestForm.permissionLevel === 'VIEW_DOWNLOAD'}
                          onChange={() => setRequestForm((f) => ({ ...f, permissionLevel: 'VIEW_DOWNLOAD' }))}
                        />
                        👁 View + ⬇ Download
                      </label>
                    </div>
                  </div>
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">Message (optional)</label>
                    <textarea
                      className="w-full rounded-lg border border-slate-300 px-4 py-2"
                      rows={2}
                      placeholder="Let the owner know why you need access"
                      value={requestForm.message}
                      onChange={(e) => setRequestForm((f) => ({ ...f, message: e.target.value }))}
                    />
                  </div>
                  {requestState.error && <p className="text-sm text-red-600">{requestState.error}</p>}
                  {requestState.success && <p className="text-sm text-emerald-600">Request submitted successfully.</p>}
                  <button type="submit" className="btn-primary" disabled={requestState.loading}>
                    {requestState.loading ? 'Submitting…' : 'Request Access'}
                  </button>
                </form>
              </>
            )}
          </div>
        )}

        {repeatedQuestions.length > 0 && (
          <div className="card">
            <h2 className="mb-3 text-xl font-semibold">Frequently Asked in {paper.subjectName}</h2>
            <p className="mb-4 text-sm text-slate-500">
              Based on questions extracted from previously uploaded papers in this subject. This highlights recurring
              topics — it is not a prediction of future exam content.
            </p>
            <div className="space-y-3">
              {repeatedQuestions.map((rq, idx) => (
                <div key={idx} className="rounded-xl border border-slate-200 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-medium text-slate-900">{rq.questionText}</p>
                    <span className="shrink-0 rounded-full bg-purple-100 px-3 py-1 text-xs font-semibold text-purple-700">{rq.tag}</span>
                  </div>
                  <p className="mt-1 text-sm text-slate-500">{rq.recurrenceLabel}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
