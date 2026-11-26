import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useFavorites } from '../context/FavoritesContext';
import { useToast } from '../context/ToastContext';
import './FavoriteButton.css';

export default function FavoriteButton({ recipeId, className = '' }) {
  const { isAuthenticated } = useAuth();
  const { isFavorite, toggle } = useFavorites();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [busy, setBusy] = useState(false);

  const active = isFavorite(recipeId);
  const label = active ? 'Retirer des favoris' : 'Ajouter aux favoris';

  async function handleClick(e) {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      showToast('Connectez-vous pour enregistrer vos favoris.', 'info');
      navigate('/login', { state: { from: location } });
      return;
    }
    if (busy) return;

    setBusy(true);
    try {
      await toggle(recipeId);
      showToast(
        active ? 'Recette retirée des favoris.' : 'Recette ajoutée aux favoris !',
        'success'
      );
    } catch (err) {
      if (err?.response?.status === 401) {
        navigate('/login', { state: { from: location } });
      } else {
        showToast('Impossible de modifier vos favoris. Réessayez.', 'error');
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <button
      type="button"
      className={`fav-btn${active ? ' fav-btn--on' : ''} ${className}`.trim()}
      onClick={handleClick}
      disabled={busy}
      aria-pressed={active}
      aria-label={label}
      title={label}
    >
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
        <path
          d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
          fill={active ? 'currentColor' : 'none'}
          stroke="currentColor"
          strokeWidth="2"
          strokeLinejoin="round"
        />
      </svg>
    </button>
  );
}
