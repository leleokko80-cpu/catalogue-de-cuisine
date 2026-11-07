import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import http from '../api/http';

export default function RecipeListPage() {
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    http
      .get('/recipes')
      .then((res) => setRecipes(res.data))
      .catch(() => setError('Could not load recipes. Is the backend running?'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading recipes...</p>;
  if (error) return <p style={{ color: 'crimson' }}>{error}</p>;
  if (recipes.length === 0) {
    return (
      <div>
        <h2>Recipes</h2>
        <p>No recipes yet. <Link to="/recipes/new">Add the first one!</Link></p>
      </div>
    );
  }

  return (
    <div>
      <h2>Recipes</h2>
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
    </div>
  );
}