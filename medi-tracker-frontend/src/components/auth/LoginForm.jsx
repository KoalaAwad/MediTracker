import { useState, React } from "react";
import { login } from "../../api/auth.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useNavigate } from "react-router-dom";

export default function LoginForm() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const { login: setUser } = useAuth();

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      await login(username, password);
      setUser({ name: username });
      navigate("/dashboard");
    } catch (err) {
      setError(err.error || err.message || "Login failed");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="login-form">
      {error && <p className="error"> {error} </p>}
      <input
        type="text"
        placeholder="Email"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        required
      />
      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
      />
      <button type="submut">Login</button>
    </form>
  );
}
