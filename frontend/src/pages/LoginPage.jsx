import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { apiErrorMessage, EMAIL_REGEX } from "./authUtils";
import "./auth.css";
import Alert from "../components/Alert";
import { CuteChefHatIcon, CuteHeartIcon, CuteSparkleIcon, CuteStarIcon } from "../data/CuteIcons";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from?.pathname || "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (!EMAIL_REGEX.test(email.trim())) {
      setError("Entrez une adresse email valide.");
      return;
    }
    if (!password) {
      setError("Entrez votre mot de passe.");
      return;
    }

    setLoading(true);
    try {
      await login(email.trim(), password);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-split">
        <div className="auth-showcase">
          <div className="auth-showcase-content">
            <CuteChefHatIcon size={34} />
            <h2>Welcome back, Chef!</h2>
            <p>
              Sign in to publish recipes, save your favorites, rate dishes, and chat with mr.mayo.
            </p>
            <div className="auth-perks">
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                <CuteHeartIcon filled size={14} /> Save &amp; organize your favorite recipes
              </span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                <CuteStarIcon size={14} /> Rate &amp; review community dishes
              </span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                <CuteSparkleIcon size={14} /> Share your creations with smart autocorrect
              </span>
            </div>
          </div>
        </div>

        <form className="auth-card" onSubmit={handleSubmit} noValidate>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <CuteChefHatIcon size={24} />
            <h1 className="auth-title">Se connecter</h1>
          </div>
          <p className="auth-subtitle">Accédez à votre espace et ajoutez vos recettes.</p>

          {error && <Alert type="error">{error}</Alert>}

          <label className="auth-label" htmlFor="login-email">Email</label>
          <input
            id="login-email"
            type="email"
            placeholder="chef@example.com"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label className="auth-label" htmlFor="login-password">Mot de passe</label>
          <div className="auth-password-wrap">
            <input
              id="login-password"
              type={showPassword ? "text" : "password"}
              placeholder="••••••••"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <button
              type="button"
              className="auth-eye-btn"
              onClick={() => setShowPassword((s) => !s)}
            >
              {showPassword ? "Hide" : "Show"}
            </button>
          </div>

          <button className="auth-button" type="submit" disabled={loading}>
            {loading ? "Connexion..." : "Se connecter →"}
          </button>

          <p className="auth-switch">
            Pas encore de compte ? <Link to="/register">Créer un compte</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
