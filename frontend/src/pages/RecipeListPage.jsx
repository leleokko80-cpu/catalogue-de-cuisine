import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import http from '../api/http';
import SearchBar from '../components/SearchBar';

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

      {loading && <p>Loading recipes...</p>}
      {error && <p style={{ color: 'crimson' }}>{error}</p>}

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
              to={`/recipes/${r.id}`}
              style={{
                border: '1px solid #ddd',
                borderRadius: 8,
                overflow: 'hidden',
                textDecoration: 'none',
                color: 'inherit',
              }}
            >
              {r.imageUrl ? (
                <img
                  src={r.imageUrl}
                  alt={r.title}
                  style={{ width: '100%', height: 150, objectFit: 'cover', display: 'block' }}
                />
              ) : (
                <div
                  style={{
                    height: 150,
                    background: '#eee',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '2rem',
                  }}
                >
                  🍽️
                </div>
              )}
              <div style={{ padding: '0.75rem' }}>
                <h3 style={{ margin: '0 0 0.5rem' }}>{r.title}</h3>
                <div style={{ fontSize: '0.85rem', color: '#666' }}>
                  ⏱ {r.prepTimeMinutes + r.cookTimeMinutes} min · by {r.authorName}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}