import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const homeRouteForRole = () => {
  const role = localStorage.getItem('role');
  if (role === 'FACULTY') return '/faculty';
  if (role === 'ADMIN') return '/admin';
  return '/student';
};

const YEAR_OPTIONS = [
  { value: '', label: 'All years' },
  { value: '1', label: '1st Year' },
  { value: '2', label: '2nd Year' },
  { value: '3', label: '3rd Year' },
  { value: '4', label: '4th Year' }
];

const SORT_OPTIONS = [
  { value: '', label: 'Most relevant' },
  { value: 'newest', label: 'Newest' },
  { value: 'most_viewed', label: 'Most Viewed' },
  { value: 'most_downloaded', label: 'Most Downloaded' },
  { value: 'highest_rated', label: 'Highest Rated' },
  { value: 'trending', label: 'Trending' }
];

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [year, setYear] = useState('');
  const [subject, setSubject] = useState('');
  const [sort, setSort] = useState('');
  const [subjectOptions, setSubjectOptions] = useState([]);
  const [papers, setPapers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  useEffect(() => {
    axios.get(`${API_BASE_URL}/subjects`, { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })
      .then((res) => setSubjectOptions(res.data.data || []))
      .catch(() => setSubjectOptions([]));
  }, []);

  const runSearch = async (params) => {
    const trimmedQuery = (params.q || '').trim();
    const trimmedSubject = (params.subject || '').trim();
    const yearValue = params.year || '';
    const sortValue = params.sort || '';

    if (!trimmedQuery && !trimmedSubject && !yearValue) {
      setPapers([]);
      setSearchParams({});
      setHasSearched(false);
      return;
    }

    setLoading(true);
    setHasSearched(true);
    try {
      const nextParams = {};
      if (trimmedQuery) nextParams.q = trimmedQuery;
      if (yearValue) nextParams.year = yearValue;
      if (trimmedSubject) nextParams.subject = trimmedSubject;
      if (sortValue) nextParams.sort = sortValue;
      setSearchParams(nextParams);

      const response = await axios.get(`${API_BASE_URL}/papers/search`, {
        params: nextParams,
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
    const urlYear = searchParams.get('year') || '';
    const urlSubject = searchParams.get('subject') || '';
    const urlSort = searchParams.get('sort') || '';
    if (urlQuery || urlYear || urlSubject) {
      setQuery(urlQuery);
      setYear(urlYear);
      setSubject(urlSubject);
      setSort(urlSort);
      runSearch({ q: urlQuery, year: urlYear, subject: urlSubject, sort: urlSort });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => runSearch({ q: query, year, subject, sort });

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-6xl">
        <div className="mb-4 flex items-center justify-between">
          <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <button type="button" className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
        </div>

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
              onKeyDown={handleKeyPress}
            />

            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Year</label>
                <select
                  className="w-full rounded-lg border border-slate-300 px-4 py-2.5"
                  value={year}
                  onChange={(e) => setYear(e.target.value)}
                >
                  {YEAR_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Subject</label>
                <input
                  className="w-full rounded-lg border border-slate-300 px-4 py-2.5"
                  placeholder="e.g., Database Management Systems"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  onKeyDown={handleKeyPress}
                  list="subject-options"
                />
                <datalist id="subject-options">
                  {subjectOptions.map((s) => (
                    <option key={s.id} value={s.name} />
                  ))}
                </datalist>
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Sort by</label>
                <select
                  className="w-full rounded-lg border border-slate-300 px-4 py-2.5"
                  value={sort}
                  onChange={(e) => setSort(e.target.value)}
                >
                  {SORT_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex gap-4">
              <button className="btn-primary flex-1" onClick={handleSearch} disabled={loading}>
                {loading ? 'Searching...' : 'Search'}
              </button>
              <button className="btn-secondary" onClick={() => {
                setQuery('');
                setYear('');
                setSubject('');
                setSort('');
                setPapers([]);
                setHasSearched(false);
                setSearchParams({});
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
                      {paper.studentYear && (
                        <span className="inline-block rounded-full bg-indigo-100 px-3 py-1 text-sm text-indigo-700">Year {paper.studentYear}</span>
                      )}
                      {paper.accessType === 'REQUEST_ACCESS' && (
                        <span className="inline-block rounded-full bg-amber-100 px-3 py-1 text-sm text-amber-700">🔒 Access Required</span>
                      )}
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

        {!loading && hasSearched && papers.length === 0 && (
          <div className="card text-center">
            <p className="text-slate-600">No papers found. Try adjusting your search or filters.</p>
          </div>
        )}
      </div>
    </div>
  );
}
