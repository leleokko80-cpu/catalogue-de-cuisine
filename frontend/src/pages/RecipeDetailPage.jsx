import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import http from '../api/http';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import FavoriteButton from '../components/FavoriteButton';
import DeleteRecipeButton from '../components/DeleteRecipeButton';
import Alert from '../components/Alert';
import RecipeImage from '../components/RecipeImage';
import { getCategoryMeta } from '../data/categoryList';
import {
  CategoryIcon,
  CuteBudgetIcon,
  CuteChefHatIcon,
  CuteClockIcon,
  CuteFlameIcon,
  CutePrinterIcon,
  CuteSparkleIcon,
  CuteStarIcon,
  CuteUtensilsIcon,
} from '../data/CuteIcons';

function scaleQuantity(quantityStr, ratio) {
  if (!quantityStr || ratio === 1) return quantityStr;
  return quantityStr.replace(/(\d+(?:\.\d+)?)/g, (match) => {
    const val = parseFloat(match) * ratio;
    if (Number.isNaN(val)) return match;
    const rounded = Math.round(val * 10) / 10;
    return Number.isInteger(rounded) ? String(rounded) : String(rounded);
  });
}

export default function RecipeDetailPage() {
  const { id } = useParams();
  const { isAuthenticated, user } = useAuth();
  const { showToast } = useToast();

  const [state, setState] = useState({ loadedId: null, recipe: null, reviews: [], error: '' });
  const [servingsOverride, setServingsOverride] = useState(null);
  const [checkedIngredients, setCheckedIngredients] = useState({});
  const [checkedSteps, setCheckedSteps] = useState({});

  // New Review Form
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const [reviewError, setReviewError] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([http.get(`/recipes/${id}`), http.get(`/recipes/${id}/reviews`)])
      .then(([recRes, revRes]) => {
        if (!cancelled) {
          setServingsOverride(null);
          setCheckedIngredients({});
          setCheckedSteps({});
          setState({
            loadedId: id,
            recipe: recRes.data,
            reviews: revRes.data || [],
            error: '',
          });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            loadedId: id,
            recipe: null,
            reviews: [],
            error:
              err.response?.status === 404
                ? 'Recipe not found.'
                : 'Could not load the recipe. Is the backend running?',
          });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const loading = state.loadedId !== id;
  const { recipe, reviews, error } = state;

  async function refreshRecipeAndReviews() {
    const [recRes, revRes] = await Promise.all([
      http.get(`/recipes/${id}`),
      http.get(`/recipes/${id}/reviews`),
    ]);
    setState({
      loadedId: id,
      recipe: recRes.data,
      reviews: revRes.data || [],
      error: '',
    });
  }

  async function handleReviewSubmit(e) {
    e.preventDefault();
    setReviewError('');
    if (!comment.trim()) {
      setReviewError('Please write a short comment before posting your rating.');
      return;
    }
    setSubmittingReview(true);
    try {
      await http.post(`/recipes/${id}/reviews`, {
        rating,
        comment: comment.trim(),
      });
      setComment('');
      setRating(5);
      await refreshRecipeAndReviews();
      showToast('Review posted! Thank you for rating.', 'success');
    } catch (err) {
      setReviewError(err.response?.data?.message || 'Could not submit your review.');
    } finally {
      setSubmittingReview(false);
    }
  }

  async function handleDeleteReview(reviewId) {
    try {
      await http.delete(`/recipes/${id}/reviews/${reviewId}`);
      await refreshRecipeAndReviews();
      showToast('Comment removed.', 'info');
    } catch {
      showToast('Could not delete comment.', 'error');
    }
  }

  if (loading) {
    return (
      <div style={{ maxWidth: 920, margin: '0 auto' }}>
        <div className="skeleton-line" style={{ height: 32, width: '40%', borderRadius: 8, marginBottom: 16 }} />
        <div className="skeleton-media" style={{ height: 340, borderRadius: 16 }} />
      </div>
    );
  }

  if (error || !recipe) {
    return (
      <div>
        <Alert type="error">{error || 'Recipe not found.'}</Alert>
        <Link to="/recipes">← Back to recipes</Link>
      </div>
    );
  }

  const cat = getCategoryMeta(recipe.category);
  const baseServings = Math.max(1, recipe.servings || 1);
  const activeServings = servingsOverride ?? baseServings;
  const ratio = activeServings / baseServings;

  return (
    <div style={{ maxWidth: 940, margin: '0 auto' }}>
      <Link to="/recipes" className="no-print" style={{ fontWeight: 600 }}>
        ← Back to recipes
      </Link>

      <div className="page-header" style={{ marginTop: 'var(--space-2)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <CategoryIcon categoryKey={cat.key} size={28} />
          <h2 style={{ margin: 0 }}>{recipe.title}</h2>
        </div>
        <div className="page-actions">
          <button
            type="button"
            className="btn btn-sm"
            onClick={() =>
              window.dispatchEvent(
                new CustomEvent('mrmayo:ask', {
                  detail: { recipeId: recipe.id, recipeTitle: recipe.title },
                })
              )
            }
            title="Ask Mr. Mayo about this recipe"
          >
            <CuteSparkleIcon size={15} />
            <span>Ask Mr. Mayo</span>
          </button>
          <button
            type="button"
            className="btn btn-sm"
            onClick={() => window.print()}
            title="Print this recipe"
          >
            <CutePrinterIcon size={15} />
            <span>Print</span>
          </button>
          {isAuthenticated && user && recipe.authorId === user.id && (
            <Link to={`/recipes/${recipe.id}/edit`} className="btn btn-sm">
              ✏️ Edit
            </Link>
          )}
          <DeleteRecipeButton recipeId={recipe.id} authorId={recipe.authorId} />
          <FavoriteButton recipeId={recipe.id} />
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          marginBottom: 'var(--space-3)',
          fontSize: '0.92rem',
        }}
      >
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
          <CuteStarIcon size={17} />
          <strong>{recipe.averageRating > 0 ? recipe.averageRating.toFixed(1) : 'No ratings yet'}</strong>
          {recipe.ratingCount > 0 && <span className="muted">({recipe.ratingCount} reviews)</span>}
        </span>
        <span className="muted">·</span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }} className="muted">
          <CuteChefHatIcon size={16} /> by {recipe.authorName}
        </span>
        <span className="muted">· {new Date(recipe.createdAt).toLocaleDateString()}</span>
        {recipe.estimatedCostTnd != null && (
          <span className="muted">· ~{recipe.estimatedCostTnd} TND</span>
        )}
        <span
          style={{
            padding: '2px 10px',
            borderRadius: 999,
            background: cat.bg,
            color: cat.accent,
            fontWeight: 700,
            fontSize: '0.8rem',
          }}
        >
          {cat.label}
        </span>
      </div>

      {/* Large Hero Image with onError Fallback */}
      <RecipeImage src={recipe.imageUrl} alt={recipe.title} category={recipe.category} className="detail-hero-img" />

      {recipe.funNote && (
        <div
          style={{
            marginTop: 'var(--space-3)',
            padding: '14px 16px',
            borderRadius: 'var(--card-radius)',
            background: '#fff7ed',
            borderLeft: '4px solid var(--primary)',
            color: '#7c2d12',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}
        >
          <CuteSparkleIcon size={18} />
          <span>{recipe.funNote}</span>
        </div>
      )}

      {recipe.description && (
        <p style={{ fontSize: '1.05rem', marginTop: 'var(--space-3)' }}>{recipe.description}</p>
      )}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
          gap: 'var(--space-2)',
          margin: 'var(--space-3) 0 var(--space-4)',
          padding: 'var(--space-3)',
          background: '#fff',
          border: '1px solid var(--border)',
          borderRadius: 'var(--card-radius)',
        }}
      >
        <div>
          <span className="muted" style={{ fontSize: '0.8rem' }}>Prep</span>
          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 5 }}>
            <CuteClockIcon size={15} /> {recipe.prepTimeMinutes} min
          </div>
        </div>
        <div>
          <span className="muted" style={{ fontSize: '0.8rem' }}>Cook</span>
          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 5 }}>
            <CuteFlameIcon size={15} /> {recipe.cookTimeMinutes} min
          </div>
        </div>
        <div>
          <span className="muted" style={{ fontSize: '0.8rem' }}>Total</span>
          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 5 }}>
            <CuteClockIcon size={15} /> {recipe.prepTimeMinutes + recipe.cookTimeMinutes} min
          </div>
        </div>
        <div>
          <span className="muted" style={{ fontSize: '0.8rem' }}>Servings</span>
          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 5 }}>
            <CuteUtensilsIcon size={15} /> {activeServings}
          </div>
        </div>
        <div>
          <span className="muted" style={{ fontSize: '0.8rem' }}>Budget</span>
          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 5 }}>
            <CuteBudgetIcon size={15} />
            {recipe.budgetLevel === 'BUDGET' ? 'Budget' : recipe.budgetLevel === 'GOURMET' ? 'Gourmet' : 'Moderate'}
          </div>
        </div>
      </div>

      {/* Sticky Ingredients Panel + Checkable Steps */}
      <div className="detail-columns">
        <section className="form-card detail-ingredients-sticky" style={{ margin: 0, maxWidth: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 'var(--space-3)', flexWrap: 'wrap' }}>
            <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 6 }}>
              <CuteUtensilsIcon size={17} />
              <span>Ingredients</span>
            </h3>

            <div className="scaler-controls no-print" aria-label="Servings scaler">
              <button
                type="button"
                className="scaler-btn"
                onClick={() => setServingsOverride((s) => Math.max(1, (s ?? baseServings) - 1))}
                aria-label="Decrease servings"
              >
                -
              </button>
              <span style={{ fontSize: '0.82rem', fontWeight: 700 }}>{activeServings} serv.</span>
              <button
                type="button"
                className="scaler-btn"
                onClick={() => setServingsOverride((s) => Math.min(24, (s ?? baseServings) + 1))}
                aria-label="Increase servings"
              >
                +
              </button>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {recipe.ingredients.map((ing, i) => {
              const checked = Boolean(checkedIngredients[i]);
              const scaledQty = scaleQuantity(ing.quantity, ratio);
              return (
                <label key={i} className={`check-row ${checked ? 'check-row--done' : ''}`} style={{ fontWeight: 400 }}>
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() =>
                      setCheckedIngredients((prev) => ({ ...prev, [i]: !prev[i] }))
                    }
                  />
                  <span>
                    {scaledQty ? <strong>{scaledQty} </strong> : null}
                    {ing.name}
                  </span>
                </label>
              );
            })}
          </div>
        </section>

        <section className="form-card" style={{ margin: 0, maxWidth: '100%' }}>
          <h3 style={{ marginTop: 0, display: 'flex', alignItems: 'center', gap: 6 }}>
            <CuteChefHatIcon size={18} />
            <span>Preparation Steps</span>
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {recipe.steps.map((step, i) => {
              const checked = Boolean(checkedSteps[i]);
              return (
                <label key={i} className={`check-row ${checked ? 'check-row--done' : ''}`} style={{ fontWeight: 400 }}>
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() =>
                      setCheckedSteps((prev) => ({ ...prev, [i]: !prev[i] }))
                    }
                  />
                  <span>
                    <strong>Step {i + 1}. </strong>
                    {step}
                  </span>
                </label>
              );
            })}
          </div>
        </section>
      </div>

      {/* Ratings & Comments Section */}
      <section className="form-card no-print" style={{ marginTop: 'var(--space-4)', maxWidth: '100%' }}>
        <h3 style={{ marginTop: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
          <CuteStarIcon size={20} />
          <span>Ratings &amp; Comments ({reviews.length})</span>
        </h3>

        {isAuthenticated ? (
          <form onSubmit={handleReviewSubmit} style={{ marginBottom: 'var(--space-4)', paddingBottom: 'var(--space-3)', borderBottom: '1px dashed var(--border)' }}>
            <div style={{ marginBottom: 'var(--space-2)' }}>
              <span style={{ fontWeight: 600, fontSize: '0.9rem', display: 'block', marginBottom: 6 }}>
                Your Rating:
              </span>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                    aria-label={`Rate ${star} stars`}
                    style={{
                      background: 'none',
                      border: 'none',
                      cursor: 'pointer',
                      padding: 2,
                      transform: (hoverRating || rating) >= star ? 'scale(1.12)' : 'scale(1)',
                      transition: 'transform 0.12s',
                    }}
                  >
                    <CuteStarIcon filled={(hoverRating || rating) >= star} size={26} />
                  </button>
                ))}
                <span className="muted" style={{ marginLeft: 8, fontSize: '0.88rem' }}>
                  {rating}/5 stars
                </span>
              </div>
            </div>

            {reviewError && <Alert type="error">{reviewError}</Alert>}

            <textarea
              rows={3}
              placeholder="Share how it turned out, tips, or ingredient swaps..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />

            <div style={{ marginTop: 'var(--space-2)' }}>
              <button type="submit" className="btn btn-primary btn-sm" disabled={submittingReview}>
                {submittingReview ? 'Posting...' : 'Post Rating & Comment'}
              </button>
            </div>
          </form>
        ) : (
          <div
            style={{
              padding: 'var(--space-3)',
              borderRadius: 'var(--radius)',
              background: '#fff7ed',
              border: '1px solid #fed7aa',
              marginBottom: 'var(--space-3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: 'var(--space-2)',
            }}
          >
            <span>Want to rate this recipe and leave a comment?</span>
            <Link to="/login" state={{ from: location }} className="btn btn-sm btn-primary">
              Log in to Comment
            </Link>
          </div>
        )}

        {reviews.length === 0 ? (
          <p className="muted">No comments yet — be the first chef to review this dish!</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
            {reviews.map((rev) => (
              <div
                key={rev.id}
                style={{
                  padding: 'var(--space-3)',
                  borderRadius: 'var(--radius)',
                  background: '#fcfaf7',
                  border: '1px solid var(--border)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <strong>{rev.reviewerName}</strong>
                    <span style={{ display: 'inline-flex', gap: 2 }}>
                      {[1, 2, 3, 4, 5].map((s) => (
                        <CuteStarIcon key={s} filled={s <= rev.rating} size={14} />
                      ))}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span className="muted" style={{ fontSize: '0.78rem' }}>
                      {new Date(rev.createdAt).toLocaleDateString()}
                    </span>
                    {user && rev.authorId === user.id && (
                      <button
                        type="button"
                        className="btn btn-sm"
                        style={{ padding: '2px 8px', fontSize: '0.75rem', color: 'var(--danger)' }}
                        onClick={() => handleDeleteReview(rev.id)}
                      >
                        Delete
                      </button>
                    )}
                  </div>
                </div>
                <p style={{ margin: '6px 0 0', color: '#374151' }}>{rev.comment}</p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
