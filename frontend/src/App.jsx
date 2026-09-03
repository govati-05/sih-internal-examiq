import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import StudentDashboard from './pages/StudentDashboard';
import FacultyDashboard from './pages/FacultyDashboard';
import AdminDashboard from './pages/AdminDashboard';
import SearchPage from './pages/SearchPage';
import UploadPage from './pages/UploadPage';
import PaperDetailPage from './pages/PaperDetailPage';
import ProfilePage from './pages/ProfilePage';
import BookmarksPage from './pages/BookmarksPage';
import MyUploadsPage from './pages/MyUploadsPage';
import NotificationsPage from './pages/NotificationsPage';
import AccessRequestsPage from './pages/AccessRequestsPage';
import QuizPage from './pages/QuizPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/student" element={<StudentDashboard />} />
      <Route path="/student/profile" element={<ProfilePage />} />
      <Route path="/student/bookmarks" element={<BookmarksPage />} />
      <Route path="/student/uploads" element={<MyUploadsPage />} />
      <Route path="/student/notifications" element={<NotificationsPage />} />
      <Route path="/faculty" element={<FacultyDashboard />} />
      <Route path="/admin" element={<AdminDashboard />} />
      <Route path="/search" element={<SearchPage />} />
      <Route path="/upload" element={<UploadPage />} />
      <Route path="/paper/:id" element={<PaperDetailPage />} />
      <Route path="/access-requests" element={<AccessRequestsPage />} />
      <Route path="/quiz" element={<QuizPage />} />
    </Routes>
  );
}

export default App;
