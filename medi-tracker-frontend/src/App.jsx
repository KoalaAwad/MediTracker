import { useEffect, useState } from "react";
import { AuthProvider } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";


function App() {
  const [message, setMessage] = useState("Loading...");
  return (
    <AuthProvider>
      <Router>
        <Route path="/" element={<LoginPage />} />
        <Route path="/dashboard" element={<DashboardPage  />} />
      </Router>
    </AuthProvider>
  );
}

export default App;
