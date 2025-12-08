import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Unauthorized() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
        <div className="mx-auto h-16 w-16 bg-yellow-100 rounded-full flex items-center justify-center mb-4">
          <svg className="h-8 w-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Access Denied</h1>
        <p className="text-gray-600 mb-6">
          {isAuthenticated
            ? "You don't have permission to access this page."
            : "Please log in to access this page."}
        </p>
        <Link
          to={isAuthenticated ? "/dashboard" : "/login"}
          className="inline-block bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition-colors"
        >
          {isAuthenticated ? "Go to Dashboard" : "Login"}
        </Link>
      </div>
    </div>
  );
}