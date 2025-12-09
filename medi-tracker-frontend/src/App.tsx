import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ErrorBoundary } from './components/ErrorBoundary';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Dashboard from './pages/profile/DashboardPage';
import ProfilePage from './pages/profile/ProfilePage';
import UsersPage from './pages/admin/UsersPage';
import MedicineListPage from './pages/medicine/MedicineListPage';
import MedicineFormPage from './pages/medicine/MedicineFormPage';
import NotFound from './pages/NotFound';
import Unauthorized from './pages/Unauthorized';
import { useEffect } from 'react';
import { useAuthStore } from './zustand/authStore';
import Loading from './components/ui/Loading';

            // Protected Route Component
            function ProtectedRoute({ children }: { children: React.ReactNode }) {
              const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
              const isLoading = useAuthStore((s) => s.isLoading);

              if (isLoading) {
                return <Loading label="Loading..." />;
              }

              if (!isAuthenticated) {
                return <Navigate to="/login" replace />;
              }

              return <>{children}</>;
            }

            function App() {
              const init = useAuthStore((s) => s.init);
              const isLoading = useAuthStore((s) => s.isLoading);

              // Initialize auth from localStorage on app mount
              useEffect(() => {
                init();
              }, [init]);

              return (
                <BrowserRouter>
                  {/* AuthProvider removed; Zustand store is global */}
                  <ErrorBoundary>
                    {/* Optionally show a top-level loading while init runs */}
                    {/* {isLoading && <Loading label="Initializing..." />} */}
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
                        path="/profile"
                        element={
                          <ProtectedRoute>
                            <ProfilePage />
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

                      <Route
                        path="/medicine"
                        element={
                          <ProtectedRoute>
                            <MedicineListPage />
                          </ProtectedRoute>
                        }
                      />

                      <Route
                        path="/medicine/add"
                        element={
                          <ProtectedRoute>
                            <MedicineFormPage />
                          </ProtectedRoute>
                        }
                      />

                      <Route
                        path="/medicine/edit/:id"
                        element={
                          <ProtectedRoute>
                            <MedicineFormPage />
                          </ProtectedRoute>
                        }
                      />

                      <Route path="/" element={<Navigate to="/dashboard" replace />} />
                      <Route path="*" element={<NotFound />} />
                    </Routes>
                  </ErrorBoundary>
                </BrowserRouter>
              );
            }

            export default App;