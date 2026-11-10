import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import http from '../api/http';
import FavoriteButton from '../components/FavoriteButton';
import SearchBar from '../components/SearchBar';
import Alert from '../components/Alert';

export default function RecipeListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') || '';

  const [input, setInput] = useState(q);
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Wait 300 ms after the last keystroke, then put the search in the URL (?q=...)
  useEffect(() => {
    const timer = setTimeout(() => {
      if (input.trim() !== q) {
        setSearchParams(input.trim() ? { q: input.trim() } : {}, { replace: true });
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [input, q, setSearchParams]);

  // Load the recipes whenever the search in the URL changes
  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError('');
    http
      .get('/recipes', { params: q ? { q } : {} })
      .then((res) => {
        if (!cancelled) setRecipes(res.data);
      })
      .catch(() => {
        if (!cancelled) setError('Could not load recipes. Is the backend running?');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [q]);

  return (
    <div>
      <h2>Recipes</h2>
      <SearchBar value={input} onChange={setInput} />

      {loading && <p className="muted">Loading recipes...</p>}
      {error && <Alert type="error">{error}</Alert>}

      {!loading && !error && recipes.length === 0 && (
        <p>
          {q ? (
            <>No recipes match “{q}”.</>
          ) : (
            <>
              No recipes yet. <Link to="/recipes/new">Add the first one!</Link>
            </>
          )}
        </p>
      )}

      {!loading && !error && recipes.length > 0 && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
            gap: '1rem',
          }}
        >
          {recipes.map((r) => (
            <Link
              key={r.id}
              className="recipe-card"
              to={`/recipes/${r.id}`}
              style={{
                position: 'relative',
                border: '1px solid #ddd',
                borderRadius: 8,
                overflow: 'hidden',
                textDecoration: 'none',
                color: 'inherit',
              }}
            >
              <div style={{ position: 'absolute', top: 10, right: 10, zIndex: 2 }}>
                <FavoriteButton recipeId={r.id} />
              </div>

              {r.imageUrl ? (
                <img
                  src={r.imageUrl}
                  alt={r.title}
                  style={{ width: '100%', height: 150, objectFit: 'cover', display: 'block' }}
                />
              ) : (
                <div className="recipe-placeholder">No image</div>
              )}

              <div style={{ padding: '0.75rem' }}>
                <h3 style={{ margin: '0 0 0.5rem' }}>{r.title}</h3>
                <div className="muted" style={{ fontSize: '0.85rem' }}>
                  {r.prepTimeMinutes + r.cookTimeMinutes} min · by {r.authorName}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}