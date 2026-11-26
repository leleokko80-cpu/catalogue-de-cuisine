import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import http from '../api/http';
import Alert from '../components/Alert';
import RecipeImage from '../components/RecipeImage';
import SkeletonCard from '../components/SkeletonCard';
import DeleteRecipeButton from '../components/DeleteRecipeButton';
import { getCategoryMeta } from '../data/categoryList';
import { CuteChefHatIcon, CuteClockIcon, CuteStarIcon } from '../data/CuteIcons';

export default function MyRecipesPage() {
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    http.get('/recipes/mine')
      .then((res) => setRecipes(res.data || []))
      .catch(() => setError('Could not load your recipes. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  function handleDeleted(id) {
    setRecipes((prev) => prev.filter((r) => r.id !== id));
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
            <CuteChefHatIcon size={24} />
            My Recipes
          </h2>
          <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.9rem' }}>
            All recipes you&apos;ve published — edit or delete them here.
          </p>
        </div>
        <Link to="/recipes/new" className="btn btn-primary page-actions">
          + Add New Recipe
        </Link>
      </div>

      {error && <Alert type="error">{error}</Alert>}

      {loading && (
        <div className="my-recipes-grid">
          {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      )}

      {!loading && !error && recipes.length === 0 && (
        <div className="form-card" style={{ textAlign: 'center', marginTop: 'var(--space-4)' }}>
          <CuteChefHatIcon size={48} />
          <h3 style={{ margin: 'var(--space-2) 0' }}>No recipes yet</h3>
          <p className="muted" style={{ marginBottom: 'var(--space-3)' }}>
            You haven&apos;t published any recipes. Start cooking and share your creations!
          </p>
          <Link to="/recipes/new" className="btn btn-primary">Create Your First Recipe →</Link>
        </div>
      )}

      {!loading && !error && recipes.length > 0 && (
        <div className="my-recipes-grid">
          {recipes.map((r) => {
            const cat = getCategoryMeta(r.category);
            const totalMin = r.prepTimeMinutes + r.cookTimeMinutes;
            return (
              <article key={r.id} className="my-recipe-card">
                <div className="my-recipe-card__img">
                  <RecipeImage src={r.imageUrl} alt={r.title} style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
                </div>
                <div className="my-recipe-card__body">
                  <h3 className="my-recipe-card__title">{r.title}</h3>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.82rem', color: 'var(--muted)', flexWrap: 'wrap' }}>
                    <span style={{ color: cat.accent, fontWeight: 700, fontSize: '0.75rem' }}>{cat.label}</span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                      <CuteStarIcon size={13} />
                      {r.averageRating > 0 ? r.averageRating.toFixed(1) : 'New'}
                      {r.ratingCount > 0 && <span>({r.ratingCount})</span>}
                    </span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                      <CuteClockIcon size={13} />
                      {totalMin} min
                    </span>
                  </div>
                </div>
                <div className="my-recipe-card__actions">
                  <Link to={`/recipes/${r.id}`} className="btn btn-sm" style={{ flex: 1 }}>View</Link>
                  <Link to={`/recipes/${r.id}/edit`} className="btn btn-sm" style={{ flex: 1 }}>✏️ Edit</Link>
                  <DeleteRecipeButton
                    recipeId={r.id}
                    onDeleted={() => handleDeleted(r.id)}
                  />
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
