import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { apiErrorMessage, EMAIL_REGEX } from "./authUtils";
import Alert from "../components/Alert";
import { CuteChefHatIcon, CuteSparkleIcon, CuteStarIcon, CuteTrophyIcon } from "../data/CuteIcons";
import "./auth.css";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const strength =
    password.length === 0
      ? 0
      : password.length < 6
        ? 30
        : password.length < 10
          ? 65
          : 100;
  const strengthColor =
    strength <= 30 ? "#ef4444" : strength <= 65 ? "#f59e0b" : "#16a34a";

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (!name.trim()) {
      setError("Entrez votre nom.");
      return;
    }
    if (!EMAIL_REGEX.test(email.trim())) {
      setError("Entrez une adresse email valide.");
      return;
    }
    if (password.length < 6) {
      setError("Le mot de passe doit contenir au moins 6 caractères.");
      return;
    }

    setLoading(true);
    try {
      await register(name.trim(), email.trim(), password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-split">
        <div
          className="auth-showcase"
          style={{
            backgroundImage:
              "url('https://images.unsplash.com/photo-1551024601-bec78aea704b?auto=format&fit=crop&w=1000&q=80')",
          }}
        >
          <div className="auth-showcase-content">
            <CuteSparkleIcon size={30} />
            <h2>Join Our Kitchen Community</h2>
            <p>
              Create your free chef profile in seconds and unlock the full Catalogue de Cuisine experience.
            </p>
            <div className="auth-perks">
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                <CuteChefHatIcon size={15} /> Publish entrées, plats, desserts &amp; sweets
              </span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                <CuteStarIcon size={15} /> Leave ratings &amp; cooking tips under recipes
              </span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                <CuteTrophyIcon size={15} /> Climb the community recipe leaderboard
              </span>
            </div>
          </div>
        </div>

        <form className="auth-card" onSubmit={handleSubmit} noValidate>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <CuteChefHatIcon size={24} />
            <h1 className="auth-title">Créer un compte</h1>
          </div>
          <p className="auth-subtitle">Un compte vous permet d&apos;ajouter et noter des recettes.</p>

          {error && <Alert type="error">{error}</Alert>}

          <label className="auth-label" htmlFor="register-name">Nom / Chef Name</label>
          <input
            id="register-name"
            type="text"
            placeholder="Chef Amira"
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <label className="auth-label" htmlFor="register-email">Email</label>
          <input
            id="register-email"
            type="email"
            placeholder="chef@example.com"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <label className="auth-label" htmlFor="register-password">
            Mot de passe (6 caractères minimum)
          </label>
          <div className="auth-password-wrap">
            <input
              id="register-password"
              type={showPassword ? "text" : "password"}
              placeholder="At least 6 characters"
              autoComplete="new-password"
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
          {password.length > 0 && (
            <div className="auth-strength" aria-hidden="true">
              <div
                className="auth-strength-bar"
                style={{ width: `${strength}%`, background: strengthColor }}
              />
            </div>
          )}

          <button className="auth-button" type="submit" disabled={loading}>
            {loading ? "Création..." : "Créer mon compte"}
          </button>

          <p className="auth-switch">
            Déjà un compte ? <Link to="/login">Se connecter</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
