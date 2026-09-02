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

const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });

const YEAR_OPTIONS = [
  { value: '', label: 'Any year' },
  { value: '1', label: '1st Year' },
  { value: '2', label: '2nd Year' },
  { value: '3', label: '3rd Year' },
  { value: '4', label: '4th Year' }
];

const DIFFICULTY_OPTIONS = [
  { value: '', label: 'Any difficulty' },
  { value: 'EASY', label: 'Easy' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HARD', label: 'Hard' }
];

export default function QuizPage() {
  const navigate = useNavigate();
  const [stage, setStage] = useState('setup'); // setup | taking | result
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [year, setYear] = useState('');
  const [numQuestions, setNumQuestions] = useState(5);
  const [difficulty, setDifficulty] = useState('');
  const [history, setHistory] = useState([]);
  const [loadingSetup, setLoadingSetup] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [setupError, setSetupError] = useState('');

  const [quiz, setQuiz] = useState(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const load = async () => {
      setLoadingSetup(true);
      try {
        const [subjectsRes, historyRes] = await Promise.all([
          axios.get(`${API_BASE_URL}/subjects`, { headers: authHeaders() }),
          axios.get(`${API_BASE_URL}/quiz/history`, { headers: authHeaders() }).catch(() => null)
        ]);
        setSubjects(subjectsRes.data.data || []);
        if (historyRes) {
          setHistory(historyRes.data.data || []);
        }
      } catch (err) {
        setSetupError('Unable to load subjects. Please try again.');
      } finally {
        setLoadingSetup(false);
      }
    };
    load();
  }, []);

  const handleGenerate = async (e) => {
    e.preventDefault();
    if (!subjectId) {
      setSetupError('Please select a subject.');
      return;
    }
    setGenerating(true);
    setSetupError('');
    try {
      const response = await axios.post(`${API_BASE_URL}/quiz/generate`, {
        subjectId: Number(subjectId),
        studentYear: year ? Number(year) : null,
        numQuestions: Number(numQuestions),
        difficulty: difficulty || null
      }, { headers: authHeaders() });

      const data = response.data.data;
      setQuiz(data);
      setAnswers(new Array(data.questions.length).fill(null));
      setCurrentIndex(0);
      setResult(null);
      setStage('taking');
    } catch (err) {
      setSetupError(err.response?.data?.message || 'Unable to generate a quiz right now.');
    } finally {
      setGenerating(false);
    }
  };

  const selectAnswer = (optionIndex) => {
    setAnswers((prev) => {
      const next = [...prev];
      next[currentIndex] = optionIndex;
      return next;
    });
  };

  const handleSubmit = async () => {
    if (!quiz) return;
    setSubmitting(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/quiz/${quiz.quizId}/submit`, { answers }, {
        headers: authHeaders()
      });
      setResult(response.data.data);
      setStage('result');
    } catch (err) {
      setSetupError(err.response?.data?.message || 'Unable to submit the quiz.');
    } finally {
      setSubmitting(false);
    }
  };

  const startOver = () => {
    setStage('setup');
    setQuiz(null);
    setResult(null);
    setAnswers([]);
    setCurrentIndex(0);
    axios.get(`${API_BASE_URL}/quiz/history`, { headers: authHeaders() })
      .then((res) => setHistory(res.data.data || []))
      .catch(() => {});
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-3xl space-y-4">
        <div className="flex items-center justify-between">
          <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <button className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
        </div>

        <div className="mb-4 rounded-2xl bg-gradient-to-br from-indigo-600 to-purple-700 px-8 py-8 text-white">
          <h1 className="text-3xl font-bold">📝 Practice Quiz</h1>
          <p className="mt-2 opacity-90">Test how well you recognize frequently repeated exam topics.</p>
        </div>

        {stage === 'setup' && (
          <div className="card">
            {loadingSetup ? (
              <p className="text-slate-500">Loading subjects...</p>
            ) : (
              <form onSubmit={handleGenerate} className="space-y-5">
                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-700">Subject *</label>
                  <select
                    required
                    className="w-full rounded-lg border border-slate-300 px-4 py-2.5"
                    value={subjectId}
                    onChange={(e) => setSubjectId(e.target.value)}
                  >
                    <option value="">Select a subject</option>
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                  {subjects.length === 0 && (
                    <p className="mt-1 text-xs text-slate-500">No subjects with approved papers yet.</p>
                  )}
                </div>

                <div className="grid gap-4 sm:grid-cols-3">
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">Academic Year</label>
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
                    <label className="mb-2 block text-sm font-medium text-slate-700"># Questions</label>
                    <select
                      className="w-full rounded-lg border border-slate-300 px-4 py-2.5"
                      value={numQuestions}
                      onChange={(e) => setNumQuestions(e.target.value)}
                    >
                      {[5, 6, 7, 8, 9, 10].map((n) => (
                        <option key={n} value={n}>{n}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-2 block text-sm font-medium text-slate-700">Difficulty</label>
                    <select
                      className="w-full rounded-lg border border-slate-300 px-4 py-2.5"
                      value={difficulty}
                      onChange={(e) => setDifficulty(e.target.value)}
                    >
                      {DIFFICULTY_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </select>
                  </div>
                </div>

                {setupError && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                    ⚠ {setupError}
                  </div>
                )}

                <button type="submit" className="w-full btn-primary" disabled={generating || subjects.length === 0}>
                  {generating ? 'Generating quiz…' : 'Generate Practice Quiz'}
                </button>
              </form>
            )}

            {history.length > 0 && (
              <div className="mt-8 border-t border-slate-200 pt-6">
                <h2 className="mb-3 text-lg font-semibold text-slate-800">Your recent attempts</h2>
                <div className="space-y-2">
                  {history.slice(0, 5).map((h) => (
                    <div key={h.id} className="flex items-center justify-between rounded-lg border border-slate-200 p-3 text-sm">
                      <div>
                        <p className="font-medium text-slate-800">{h.subjectName}</p>
                        <p className="text-xs text-slate-500">{new Date(h.createdAt).toLocaleDateString()}</p>
                      </div>
                      <span className="font-semibold text-slate-700">
                        {h.status === 'COMPLETED' ? `${h.correctCount}/${h.totalQuestions} (${h.scorePercentage}%)` : 'Incomplete'}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {stage === 'taking' && quiz && (
          <div className="card">
            <div className="mb-4 flex items-center justify-between text-sm text-slate-500">
              <span>Question {currentIndex + 1} of {quiz.questions.length}</span>
              <span>{quiz.subjectName}</span>
            </div>
            <div className="mb-4 h-2 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full rounded-full bg-indigo-500 transition-all"
                style={{ width: `${((currentIndex + 1) / quiz.questions.length) * 100}%` }}
              />
            </div>

            <h2 className="mb-4 text-lg font-semibold text-slate-900">
              {quiz.questions[currentIndex].prompt}
            </h2>

            <div className="space-y-3">
              {quiz.questions[currentIndex].options.map((option, idx) => (
                <label
                  key={idx}
                  className={`block cursor-pointer rounded-xl border p-3 text-sm transition ${
                    answers[currentIndex] === idx ? 'border-indigo-500 bg-indigo-50' : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="radio"
                    name={`question-${currentIndex}`}
                    className="mr-3"
                    checked={answers[currentIndex] === idx}
                    onChange={() => selectAnswer(idx)}
                  />
                  {option}
                </label>
              ))}
            </div>

            <div className="mt-6 flex items-center justify-between">
              <button
                className="btn-secondary"
                disabled={currentIndex === 0}
                onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
              >
                Previous
              </button>

              {currentIndex < quiz.questions.length - 1 ? (
                <button
                  className="btn-primary"
                  onClick={() => setCurrentIndex((i) => Math.min(quiz.questions.length - 1, i + 1))}
                >
                  Next
                </button>
              ) : (
                <button className="btn-primary" onClick={handleSubmit} disabled={submitting}>
                  {submitting ? 'Submitting…' : 'Submit Quiz'}
                </button>
              )}
            </div>

            {setupError && <p className="mt-3 text-sm text-red-600">{setupError}</p>}

            <p className="mt-4 text-center text-xs text-slate-400">
              Answered {answers.filter((a) => a !== null).length} of {quiz.questions.length}
            </p>
          </div>
        )}

        {stage === 'result' && result && (
          <div className="space-y-4">
            <div className="card text-center">
              <h2 className="text-2xl font-bold text-slate-900">Quiz Result</h2>
              <p className="mt-2 text-4xl font-bold text-indigo-600">{result.correctCount}/{result.totalQuestions}</p>
              <p className="mt-1 text-slate-500">{result.scorePercentage}% correct</p>
              <div className="mt-4 flex justify-center gap-6 text-sm">
                <span className="text-emerald-600">✓ {result.correctCount} correct</span>
                <span className="text-red-600">✗ {result.totalQuestions - result.correctCount} incorrect</span>
              </div>
            </div>

            {result.topicsNeedingRevision?.length > 0 && (
              <div className="card">
                <h3 className="mb-3 text-lg font-semibold text-slate-800">Topics that need revision</h3>
                <div className="flex flex-wrap gap-2">
                  {result.topicsNeedingRevision.map((topic, idx) => (
                    <span key={idx} className="rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-800">
                      {topic}
                    </span>
                  ))}
                </div>
              </div>
            )}

            <div className="card">
              <h3 className="mb-3 text-lg font-semibold text-slate-800">Review your answers</h3>
              <div className="space-y-4">
                {result.questions.map((q, idx) => (
                  <div key={idx} className={`rounded-xl border p-4 ${q.isCorrect ? 'border-emerald-200 bg-emerald-50/40' : 'border-red-200 bg-red-50/40'}`}>
                    <p className="font-medium text-slate-900">{idx + 1}. {q.prompt}</p>
                    <div className="mt-2 space-y-1 text-sm">
                      {q.options.map((option, optIdx) => {
                        const isCorrectOption = optIdx === q.correctIndex;
                        const isSelected = optIdx === q.selectedIndex;
                        return (
                          <p key={optIdx} className={
                            isCorrectOption ? 'font-semibold text-emerald-700' : isSelected ? 'font-semibold text-red-700' : 'text-slate-600'
                          }>
                            {isCorrectOption ? '✓ ' : isSelected ? '✗ ' : '• '}{option}
                          </p>
                        );
                      })}
                    </div>
                    {q.topicLabel && (
                      <p className="mt-2 text-xs text-slate-500">Topic: {q.topicLabel} • {q.recurrenceLabel}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div className="flex gap-3">
              <button className="btn-primary flex-1" onClick={startOver}>Try another quiz</button>
              <button className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Back to dashboard</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
