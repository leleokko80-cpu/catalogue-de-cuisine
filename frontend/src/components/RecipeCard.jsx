import { Link } from 'react-router-dom';
import FavoriteButton from './FavoriteButton';
import RecipeImage from './RecipeImage';
import { getCategoryMeta } from '../data/categoryList';
import {
  CategoryIcon,
  CuteStarIcon,
  CuteClockIcon,
  CuteBudgetIcon,
} from '../data/CuteIcons';

export default function RecipeCard({ recipe }) {
  const cat = getCategoryMeta(recipe.category);
  const totalMin = recipe.prepTimeMinutes + recipe.cookTimeMinutes;

  const budgetLabel =
    recipe.estimatedCostTnd != null
      ? `~${recipe.estimatedCostTnd} TND`
      : recipe.budgetLevel === 'BUDGET'
        ? 'Budget'
        : recipe.budgetLevel === 'GOURMET'
          ? 'Gourmet'
          : 'Moderate';

  return (
    <article className="recipe-card">
      <div className="card-header-row">
        <span className="recipe-cat-chip" style={{ color: cat.accent }}>
          <CategoryIcon categoryKey={cat.key} size={18} />
          <span>{cat.label}</span>
        </span>
        <FavoriteButton recipeId={recipe.id} />
      </div>

      <div className="recipe-card__img-wrap">
        <RecipeImage src={recipe.imageUrl} alt={recipe.title} category={recipe.category} />
      </div>

      <div className="recipe-card__body">
        <h3 className="recipe-card__title">{recipe.title}</h3>

        <div className="recipe-card__meta">
          <span className="meta-pill">
            <CuteStarIcon filled={recipe.averageRating > 0} size={14} />
            <strong>{recipe.averageRating > 0 ? recipe.averageRating.toFixed(1) : 'New'}</strong>
            {recipe.ratingCount > 0 && <span className="muted">({recipe.ratingCount})</span>}
          </span>
          <span className="meta-pill">
            <CuteClockIcon size={13} />
            <span>{totalMin} min</span>
          </span>
          <span className="meta-pill">
            <CuteBudgetIcon size={13} />
            <span>{budgetLabel}</span>
          </span>
        </div>

        <div className="recipe-card__footer">
          <span className="muted" style={{ fontSize: '0.78rem' }}>
            by {recipe.authorName}
          </span>
          <Link to={`/recipes/${recipe.id}`} className="btn btn-sm btn-primary">
            View Recipe →
          </Link>
        </div>
      </div>
    </article>
  );
}
