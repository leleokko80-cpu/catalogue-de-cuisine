import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import http from '../api/http';

export default function RecipeDetailPage() {
  const { id } = useParams();
  const [recipe, setRecipe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    http
      .get(`/recipes/${id}`)
      .then((res) => setRecipe(res.data))
      .catch((err) => {
        if (err.response?.status === 404) setError('Recipe not found.');
        else setError('Could not load the recipe. Is the backend running?');
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p>Loading...</p>;
  if (error) {
    return (
      <div>
        <p style={{ color: 'crimson' }}>{error}</p>
        <Link to="/recipes">← Back to recipes</Link>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 700, margin: '0 auto' }}>
      <Link to="/recipes">← Back to recipes</Link>
      <h2>{recipe.title}</h2>
      <div style={{ color: '#666', marginBottom: '1rem' }}>
        by {recipe.authorName} · {new Date(recipe.createdAt).toLocaleDateString()}
      </div>

      {recipe.imageUrl && (
        <img
          src={recipe.imageUrl}
          alt={recipe.title}
          style={{ width: '100%', maxHeight: 320, objectFit: 'cover', borderRadius: 8 }}
        />
      )}

      {recipe.description && <p>{recipe.description}</p>}

      <div style={{ display: 'flex', gap: '2rem', margin: '1rem 0' }}>
        <div><strong>Prep</strong><br />{recipe.prepTimeMinutes} min</div>
        <div><strong>Cook</strong><br />{recipe.cookTimeMinutes} min</div>
        <div><strong>Total</strong><br />{recipe.prepTimeMinutes + recipe.cookTimeMinutes} min</div>
        <div><strong>Servings</strong><br />{recipe.servings}</div>
      </div>

      <h3>Ingredients</h3>
      <ul>
        {recipe.ingredients.map((ing, i) => (
          <li key={i}>
            {ing.quantity ? `${ing.quantity} ` : ''}{ing.name}
          </li>
        ))}
      </ul>

      <h3>Steps</h3>
      <ol>
        {recipe.steps.map((step, i) => (
          <li key={i} style={{ marginBottom: '0.5rem' }}>{step}</li>
        ))}
      </ol>
    </div>
  );
}