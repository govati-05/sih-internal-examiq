import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const homeRouteForRole = () => {
  const role = localStorage.getItem('role');
  if (role === 'FACULTY') return '/faculty';
  if (role === 'ADMIN') return '/admin';
  return '/student';
};

export default function UploadPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    title: '',
    subject: '',
    university: '',
    year: '',
    examType: '',
    author: '',
    studentYear: '',
    accessType: 'PUBLIC'
  });
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [warnings, setWarnings] = useState([]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file || !formData.title) {
      setMessage('Please fill all required fields');
      return;
    }

    setUploading(true);
    setWarnings([]);
    const data = new FormData();
    data.append('file', file);
    Object.entries(formData).forEach(([key, value]) => {
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        data.append(key, value);
      }
    });

    try {
      const response = await axios.post(`${API_BASE_URL}/papers/upload`, data, {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      });

      const result = response?.data?.data;
      if (result?.status === 'REJECTED') {
        setMessage('Upload rejected: this paper does not match the selected subject/exam type, or already exists, and was not added to your uploads.');
      } else {
        setMessage('Paper uploaded successfully!');
      }
      setWarnings(result?.warnings || []);

      setFormData({ title: '', subject: '', university: '', year: '', examType: '', author: '', studentYear: '', accessType: 'PUBLIC' });
      setFile(null);
    } catch (error) {
      setMessage('Upload failed: ' + (error.response?.data?.message || error.message));
    }
    setUploading(false);
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-2xl">
        <div className="mb-4 flex items-center justify-between">
          <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
          <button type="button" className="btn-secondary" onClick={() => navigate(homeRouteForRole())}>Home</button>
        </div>

        <div className="mb-8 rounded-2xl bg-blue-600 px-8 py-8 text-white">
          <h1 className="text-3xl font-bold">Upload Exam Paper</h1>
          <p className="mt-2">Share previous exam papers and help students prepare</p>
        </div>

        {message && (
          <div className="mb-4 rounded-lg bg-blue-50 p-4 text-blue-800">
            {message}
          </div>
        )}

        {warnings.length > 0 && (
          <div className="mb-6 space-y-2">
            {warnings.map((warning, idx) => (
              <div key={idx} className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                ⚠ {warning}
              </div>
            ))}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6 card">
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Paper title *</label>
            <input
              required
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
              placeholder="e.g., Database Management Systems - Final Exam 2023"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">Subject</label>
              <input
                className="w-full rounded-lg border border-slate-300 px-4 py-2"
                placeholder="e.g., DBMS"
                value={formData.subject}
                onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">Exam Year</label>
              <input
                className="w-full rounded-lg border border-slate-300 px-4 py-2"
                placeholder="2023"
                type="number"
                value={formData.year}
                onChange={(e) => setFormData({ ...formData, year: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">Academic Year *</label>
              <select
                required
                className="w-full rounded-lg border border-slate-300 px-4 py-2"
                value={formData.studentYear}
                onChange={(e) => setFormData({ ...formData, studentYear: e.target.value })}
              >
                <option value="">Select academic year</option>
                <option value="1">1st Year</option>
                <option value="2">2nd Year</option>
                <option value="3">3rd Year</option>
                <option value="4">4th Year</option>
              </select>
            </div>
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">Access</label>
              <select
                className="w-full rounded-lg border border-slate-300 px-4 py-2"
                value={formData.accessType}
                onChange={(e) => setFormData({ ...formData, accessType: e.target.value })}
              >
                <option value="PUBLIC">Public</option>
                <option value="REQUEST_ACCESS">Request Access</option>
              </select>
            </div>
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">University</label>
            <input
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
              placeholder="e.g., NIT Trichy"
              value={formData.university}
              onChange={(e) => setFormData({ ...formData, university: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Exam type</label>
            <select
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
              value={formData.examType}
              onChange={(e) => setFormData({ ...formData, examType: e.target.value })}
            >
              <option value="">Select exam type</option>
              <option value="Midterm">Midterm</option>
              <option value="Final">Final</option>
              <option value="Quiz">Quiz</option>
              <option value="Assignment">Assignment</option>
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">PDF or image file *</label>
            <div className="rounded-lg border-2 border-dashed border-slate-300 p-8 text-center">
              <input
                type="file"
                required
                accept=".pdf,.jpg,.png,.jpeg"
                onChange={(e) => setFile(e.target.files?.[0])}
                className="hidden"
                id="file-input"
              />
              <label htmlFor="file-input" className="cursor-pointer">
                <p className="text-sm text-slate-600">Click to select or drag and drop</p>
                <p className="mt-1 text-xs text-slate-500">PDF, JPG, or PNG (max 20MB)</p>
                {file && <p className="mt-2 font-semibold text-blue-600">{file.name}</p>}
              </label>
            </div>
          </div>

          <button
            type="submit"
            disabled={uploading}
            className="w-full btn-primary bg-blue-600 hover:bg-blue-700"
          >
            {uploading ? 'Uploading...' : 'Upload Paper'}
          </button>
        </form>
      </div>
    </div>
  );
}
