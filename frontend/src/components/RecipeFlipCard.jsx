import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useFavorites } from '../context/FavoritesContext';
import FavoriteButton from './FavoriteButton';
import RecipeImage from './RecipeImage';
import { getCategoryMeta } from '../data/categoryList';
import {
  CategoryIcon,
  CuteStarIcon,
  CuteClockIcon,
  CuteChefHatIcon,
  CuteBudgetIcon,
  CuteSparkleIcon,
} from '../data/CuteIcons';

function getDietBadgeClass(tag) {
  const upper = tag.trim().toUpperCase();
  if (upper === 'VEGAN') return 'diet-tag diet-tag--vegan';
  if (upper === 'VEGETARIAN') return 'diet-tag diet-tag--vegetarian';
  if (upper === 'QUICK') return 'diet-tag diet-tag--quick';
  if (upper === 'DATE_NIGHT') return 'diet-tag diet-tag--date';
  if (upper === 'GLUTEN_FREE' || upper === 'DAIRY_FREE') return 'diet-tag diet-tag--gf';
  return 'diet-tag diet-tag--default';
}

function formatDietLabel(tag) {
  return tag.trim().toLowerCase().replace(/_/g, ' ');
}

export default function RecipeFlipCard({ recipe }) {
  const [flipped, setFlipped] = useState(false);
  const { isFavorite } = useFavorites();
  const cat = getCategoryMeta(recipe.category);
  const totalMin = recipe.prepTimeMinutes + recipe.cookTimeMinutes;
  const favActive = isFavorite(recipe.id);

  const budgetLabel =
    recipe.estimatedCostTnd != null
      ? `~${recipe.estimatedCostTnd} TND`
      : recipe.budgetLevel === 'BUDGET'
        ? 'Budget'
        : recipe.budgetLevel === 'GOURMET'
          ? 'Gourmet'
          : 'Moderate';

  const dietList = recipe.dietaryTags
    ? recipe.dietaryTags.split(',').map((t) => t.trim()).filter(Boolean).slice(0, 3)
    : [];

  const topNotes = recipe.topNotes || [];

  function handleCardClick() {
    setFlipped((f) => !f);
  }

  return (
    <div className="flip-card-wrap">
      <div
        className={`flip-card-inner ${flipped ? 'flip-card-inner--flipped' : ''}`}
        onClick={handleCardClick}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleCardClick();
          }
        }}
        aria-label={`Flip card for ${recipe.title} to read community notes`}
      >
        {/* FRONT FACE — normal flow layout, zero overlapping content */}
        <article className="flip-face flip-face--front">
          <div className="card-header-row">
            <span className="recipe-cat-chip" style={{ color: cat.accent }}>
              <CategoryIcon categoryKey={cat.key} size={20} />
              <span>{cat.label}</span>
            </span>

            <div
              className={`card-fav-slot ${favActive ? 'card-fav-slot--active' : ''}`}
              onClick={(e) => e.stopPropagation()}
            >
              <FavoriteButton recipeId={recipe.id} />
            </div>
          </div>

          <div className="recipe-media-wrap">
            <RecipeImage src={recipe.imageUrl} alt={recipe.title} category={recipe.category} />
          </div>

          <div className="recipe-card-body">
            <div>
              <div className="recipe-title-row">
                <CuteChefHatIcon size={18} />
                <h3>{recipe.title}</h3>
              </div>

              <div className="recipe-meta-icons" style={{ marginTop: '6px' }}>
                <span className="meta-pill">
                  <CuteStarIcon filled={recipe.averageRating > 0} size={15} />
                  <strong>{recipe.averageRating > 0 ? recipe.averageRating.toFixed(1) : 'New'}</strong>
                  {recipe.ratingCount > 0 && <span className="muted">({recipe.ratingCount})</span>}
                </span>
                <span className="meta-pill">
                  <CuteClockIcon size={14} />
                  <span>{totalMin} min</span>
                </span>
                <span className="meta-pill">
                  <CuteBudgetIcon size={14} />
                  <span>{budgetLabel}</span>
                </span>
              </div>
            </div>

            {dietList.length > 0 && (
              <div className="diet-tags-row">
                {dietList.map((tag) => (
                  <span key={tag} className={getDietBadgeClass(tag)}>
                    {formatDietLabel(tag)}
                  </span>
                ))}
              </div>
            )}

            <div className="recipe-card-footer" onClick={(e) => e.stopPropagation()}>
              <span className="muted" style={{ fontSize: '0.78rem' }}>
                by {recipe.authorName}
              </span>
              <Link to={`/recipes/${recipe.id}`} className="btn btn-sm btn-primary">
                View Recipe →
              </Link>
            </div>
          </div>
        </article>

        {/* BACK FACE — Real community review notes (or funNote fallback) */}
        <article className="flip-face flip-face--back">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
            <span className="flip-note-badge">
              <CuteSparkleIcon size={14} />
              <span>{topNotes.length > 0 ? 'Community Notes' : "Chef's Note"}</span>
            </span>
            <span className="muted" style={{ fontSize: '0.74rem' }}>Click card to flip back</span>
          </div>

          <h3 style={{ margin: 0, color: '#7c2d12', fontSize: '1.05rem' }}>{recipe.title}</h3>

          <div className="flip-back-notes">
            {topNotes.length > 0 ? (
              topNotes.map((note, i) => (
                <div key={i} className="flip-note-quote-card">
                  <div className="flip-note-quote-head">
                    <span>{note.reviewerName}</span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 2 }}>
                      <CuteStarIcon size={13} />
                      <span>{note.rating}/5</span>
                    </span>
                  </div>
                  <p className="flip-note-quote-text">&ldquo;{note.comment}&rdquo;</p>
                </div>
              ))
            ) : (
              <div className="flip-note-quote-card">
                <p className="flip-note-quote-text" style={{ WebkitLineClamp: 4 }}>
                  {recipe.funNote}
                </p>
                <div style={{ marginTop: 8 }} onClick={(e) => e.stopPropagation()}>
                  <Link
                    to={`/recipes/${recipe.id}`}
                    style={{ fontSize: '0.8rem', fontWeight: 700 }}
                  >
                    Be the first to leave a note →
                  </Link>
                </div>
              </div>
            )}
          </div>

          <div
            className="recipe-card-footer"
            style={{ borderTopColor: '#fed7aa' }}
            onClick={(e) => e.stopPropagation()}
          >
            <span className="muted" style={{ fontSize: '0.8rem' }}>
              {recipe.favoriteCount || 0} saves
            </span>
            <Link to={`/recipes/${recipe.id}`} className="btn btn-sm btn-primary">
              Open Full Recipe →
            </Link>
          </div>
        </article>
      </div>
    </div>
  );
}
