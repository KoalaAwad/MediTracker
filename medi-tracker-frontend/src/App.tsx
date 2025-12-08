import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Dashboard from './pages/profile/DashboardPage';
import UsersPage from './pages/admin/UsersPage';
import NotFound from './pages/NotFound';
import Unauthorized from './pages/Unauthorized';

            // Protected Route Component
            function ProtectedRoute({ children }: { children: React.ReactNode }) {
              const { isAuthenticated, isLoading } = useAuth();

              if (isLoading) {
                return (
                  <div className="min-h-screen flex items-center justify-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                  </div>
                );
              }

              if (!isAuthenticated) {
                return <Navigate to="/login" replace />;
              }

              return <>{children}</>;
            }

            function App() {
              return (
                <BrowserRouter>
                  <AuthProvider>
                    <ErrorBoundary>
                      <Routes>
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/unauthorized" element={<Unauthorized />} />

                        <Route
                          path="/dashboard"
                          element={
                            <ProtectedRoute>
                              <Dashboard />
                            </ProtectedRoute>
                          }
                        />

                        <Route
                          path="/admin/users"
                          element={
                            <ProtectedRoute>
                              <UsersPage />
                            </ProtectedRoute>
                          }
                        />

                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="*" element={<NotFound />} />
                      </Routes>
                    </ErrorBoundary>
                  </AuthProvider>
                </BrowserRouter>
              );
            }

            export default App;