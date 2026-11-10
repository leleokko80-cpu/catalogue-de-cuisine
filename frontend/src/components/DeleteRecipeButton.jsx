import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import http from '../api/http';
import { useAuth } from '../context/AuthContext';
import ConfirmDialog from './ConfirmDialog';

export default function DeleteRecipeButton({ recipeId, authorId }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  if (!user || authorId == null || user.id !== authorId) return null;

  const handleDelete = async () => {
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

  const handleCancel = () => {
    setOpen(false);
    setError('');
  };

  return (
    <>
      <button type="button" className="btn btn-danger" onClick={() => setOpen(true)}>
        Delete
      </button>
      <ConfirmDialog
        open={open}
        title="Delete this recipe?"
        message="This action cannot be undone."
        confirmLabel={busy ? 'Deleting...' : 'Delete'}
        busy={busy}
        error={error}
        onConfirm={handleDelete}
        onCancel={handleCancel}
      />
    </>
  );
}