// import { useAuth } from "../../context/AuthContext";
import { useAuthStore } from "../../zustand/authStore";
import { Link } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { Button } from "@mui/material";

export default function Dashboard() {
    const navigate = useNavigate();
    const user = useAuthStore(s => s.user);
    const logout = useAuthStore(s => s.logout);
    const isLoading = useAuthStore(s => s.isLoading);


  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <p className="text-gray-600 mb-4">You need to log in first</p>
          <Link
            to="/login"
            className="text-blue-600 hover:text-blue-500 font-medium"
          >
            Go to Login
          </Link>
        </div>
      </div>
    );
  }

  const isAdmin = user.role.includes("ADMIN");
  const isPatient = user.role.includes("PATIENT");

  return (
    <div className="page-container">
      <nav className="nav-header">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <h1 className="text-xl font-bold text-gray-900">MediTracker</h1>
            <div className="flex gap-2">
              <button onClick={() => navigate("/profile")} className="nav-btn-primary">
                My Profile
              </button>
              <button onClick={logout} className="nav-btn-danger">
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="dashboard-card mb-6">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            Welcome back, {user.name}!
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div className="dashboard-info-card bg-blue-50">
              <p className="text-gray-600 font-medium">Email</p>
              <p className="text-gray-900">{user.email}</p>
            </div>
            <div className="dashboard-info-card bg-green-50">
              <p className="text-gray-600 font-medium">Role</p>
              <p className="text-gray-900">{user.role}</p>
            </div>
            <div className="dashboard-info-card bg-purple-50">
              <p className="text-gray-600 font-medium">User ID</p>
              <p className="text-gray-900">{user.userId}</p>
            </div>
          </div>
        </div>

        <div className="dashboard-card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Quick Actions
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <PrimaryButton fullWidth onClick={() => navigate("/medicine")}>
              View Medicine
            </PrimaryButton>
            {isPatient && (
              <PrimaryButton fullWidth onClick={() => navigate("/prescriptions")}>
                View Prescriptions
              </PrimaryButton>
            )}
            {isAdmin && (
              <PrimaryButton fullWidth onClick={() => navigate("/admin/users")}>
                View Users
              </PrimaryButton>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}