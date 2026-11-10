import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import http from '../api/http';
import { useAuth } from '../context/AuthContext';

export default function DeleteRecipeButton({ recipeId, authorId }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  if (!user || authorId == null || user.id !== authorId) return null;

  const handleDelete = async () => {
    if (!window.confirm('Delete this recipe? This cannot be undone.')) return;
    setBusy(true);
    setError('');
    try {
      await http.delete(`/recipes/${recipeId}`);
      navigate('/recipes');
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete the recipe.');
      setBusy(false);
    }
  };

  return (
    <div style={{ margin: '0.5rem 0' }}>
      <button type="button" onClick={handleDelete} disabled={busy} style={{ color: 'crimson' }}>
        {busy ? 'Deleting...' : '🗑 Delete recipe'}
      </button>
      {error && <div style={{ color: 'crimson' }}>{error}</div>}
    </div>
  );
}