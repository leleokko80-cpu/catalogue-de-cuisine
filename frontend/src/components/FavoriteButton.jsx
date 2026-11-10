import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useFavorites } from "../context/FavoritesContext";
import "./FavoriteButton.css";

export default function FavoriteButton({ recipeId }) {
  const { isAuthenticated } = useAuth();
  const { isFavorite, toggle } = useFavorites();
  const navigate = useNavigate();
  const location = useLocation();
  const [busy, setBusy] = useState(false);

  const active = isFavorite(recipeId);
  const label = active ? "Retirer des favoris" : "Ajouter aux favoris";

  async function handleClick(e) {
    // Le bouton est dans une carte cliquable (lien) : on empêche la navigation de la carte
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      navigate("/login", { state: { from: location } });
      return;
    }
    if (busy) return;

    setBusy(true);
    try {
      await toggle(recipeId);
    } catch (err) {
      if (err?.response?.status === 401) {
        navigate("/login", { state: { from: location } });
      } else {
        window.alert("Impossible de modifier vos favoris. Réessayez.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <button
      type="button"
      className={`fav-btn${active ? " fav-btn--on" : ""}`}
      onClick={handleClick}
      disabled={busy}
      aria-pressed={active}
      aria-label={label}
      title={label}
    >
      <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
        <path
          d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
          fill={active ? "currentColor" : "none"}
          stroke="currentColor"
          strokeWidth="2"
          strokeLinejoin="round"
        />
      </svg>
    </button>
  );
}
