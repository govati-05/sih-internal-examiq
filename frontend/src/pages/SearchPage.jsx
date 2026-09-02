import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [papers, setPapers] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (nextQuery = query) => {
    const trimmed = (nextQuery || '').trim();
    if (!trimmed) {
      setPapers([]);
      setSearchParams({});
      return;
    }

    setLoading(true);
    try {
      setSearchParams({ q: trimmed });
      const response = await axios.get(`${API_BASE_URL}/papers/search`, {
        params: { q: trimmed },
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setPapers(response.data.data || []);
    } catch (error) {
      console.error('Search error:', error);
      setPapers([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    const urlQuery = searchParams.get('q') || '';
    if (urlQuery) {
      setQuery(urlQuery);
      handleSearch(urlQuery);
    }
  }, [searchParams]);

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch(query);
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 px-8 py-12 text-white">
          <h1 className="text-4xl font-bold">Smart Paper Search</h1>
          <p className="mt-2 text-lg opacity-90">Find previous exam papers, questions, and study materials across universities</p>
        </div>

        <div className="card mb-8">
          <div className="flex flex-col gap-4">
            <input
              className="rounded-lg border border-slate-300 px-4 py-3"
              placeholder="Search by subject, topic, university, exam type, or keywords..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyPress={handleKeyPress}
            />
            <div className="flex gap-4">
              <button className="btn-primary flex-1" onClick={handleSearch} disabled={loading}>
                {loading ? 'Searching...' : 'Search'}
              </button>
              <button className="btn-secondary" onClick={() => {
                setQuery('');
                setPapers([]);
              }}>
                Clear
              </button>
            </div>
          </div>
        </div>

        {papers.length > 0 && (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">Found {papers.length} papers</p>
            {papers.map((paper) => (
              <div key={paper.id} className="card">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-900">{paper.title}</h3>
                    <div className="mt-2 flex flex-wrap gap-2">
                      <span className="inline-block rounded-full bg-blue-100 px-3 py-1 text-sm text-blue-700">{paper.subjectName}</span>
                      <span className="inline-block rounded-full bg-green-100 px-3 py-1 text-sm text-green-700">{paper.universityName}</span>
                      <span className="inline-block rounded-full bg-gray-100 px-3 py-1 text-sm text-gray-700">{paper.year}</span>
                    </div>
                    <p className="mt-2 text-sm text-slate-600">By {paper.author} • {paper.examType}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-yellow-600">★ {paper.averageRating?.toFixed(1) || 'N/A'}</p>
                    <button className="mt-3 btn-primary" onClick={() => navigate(`/paper/${paper.id}`)}>View paper</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {!loading && papers.length === 0 && query && (
          <div className="card text-center">
            <p className="text-slate-600">No papers found for "{query}"</p>
          </div>
        )}
      </div>
    </div>
  );
}
