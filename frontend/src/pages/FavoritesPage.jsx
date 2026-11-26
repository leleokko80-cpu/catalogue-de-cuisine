import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import http from '../api/http';
import { useFavorites } from '../context/FavoritesContext';
import Alert from '../components/Alert';
import RecipeCard from '../components/RecipeCard';
import SkeletonCard from '../components/SkeletonCard';
import { CuteHeartIcon } from '../data/CuteIcons';

export default function FavoritesPage() {
  const { isFavorite } = useFavorites();
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    http
      .get('/favorites')
      .then((res) => {
        if (!cancelled) setRecipes(res.data || []);
      })
      .catch(() => {
        if (!cancelled) setError('Could not load your favorites. Please try again.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // Immediately filter out any recipe that gets un-favorited via FavoritesContext
  const visibleFavorites = recipes.filter((r) => isFavorite(r.id));

  return (
    <div>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <CuteHeartIcon filled size={24} />
          <div>
            <h2 style={{ margin: 0 }}>My Favorite Recipes</h2>
            <p className="muted" style={{ margin: '2px 0 0', fontSize: '0.92rem' }}>
              Your saved dishes, most recently favorited first.
            </p>
          </div>
        </div>
        <Link to="/recipes" className="btn btn-sm page-actions">
          Explore More Recipes
        </Link>
      </div>

      {error && <Alert type="error">{error}</Alert>}

      {loading && (
        <div className="recipes-grid">
          {Array.from({ length: 4 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      )}

      {!loading && !error && visibleFavorites.length === 0 && (
        <div className="form-card" style={{ textAlign: 'center', marginTop: 'var(--space-4)' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 'var(--space-2)' }}>
            <CuteHeartIcon filled={false} size={42} />
          </div>
          <h3 style={{ margin: '0 0 var(--space-2)' }}>No favorites yet</h3>
          <p className="muted" style={{ marginBottom: 'var(--space-3)' }}>
            Tap the heart on any recipe card to save your favorite meals here.
          </p>
          <Link to="/recipes" className="btn btn-primary">
            Discover Recipes →
          </Link>
        </div>
      )}

      {!loading && !error && visibleFavorites.length > 0 && (
        <div className="recipes-grid">
          {visibleFavorites.map((r) => (
            <RecipeCard key={r.id} recipe={r} />
          ))}
        </div>
      )}
    </div>
  );
}
