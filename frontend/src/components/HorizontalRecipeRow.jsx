import { useRef } from 'react';
import { Link } from 'react-router-dom';
import RecipeCard from './RecipeCard';
import SkeletonCard from './SkeletonCard';
import { CuteChevronLeftIcon, CuteChevronRightIcon } from '../data/CuteIcons';

export default function HorizontalRecipeRow({ title, recipes, loading, seeAllTo, icon }) {
  const rowRef = useRef(null);

  if (!loading && (!recipes || recipes.length < 3)) {
    return null;
  }

  function scrollRow(direction) {
    if (!rowRef.current) return;
    rowRef.current.scrollBy({ left: direction * 300, behavior: 'smooth' });
  }

  return (
    <section className="dashboard-row-section">
      <div className="page-header">
        <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {icon}
          <span>{title}</span>
        </h2>

        <div className="page-actions fresh-row-controls">
          {seeAllTo && (
            <Link to={seeAllTo} className="btn btn-sm" style={{ marginRight: 4 }}>
              See all →
            </Link>
          )}
          <button
            type="button"
            className="carousel-arrow-btn"
            onClick={() => scrollRow(-1)}
            aria-label={`Scroll ${title} left`}
            title="Scroll left"
          >
            <CuteChevronLeftIcon size={18} />
          </button>
          <button
            type="button"
            className="carousel-arrow-btn"
            onClick={() => scrollRow(1)}
            aria-label={`Scroll ${title} right`}
            title="Scroll right"
          >
            <CuteChevronRightIcon size={18} />
          </button>
        </div>
      </div>

      <div className="fresh-picks-row" ref={rowRef}>
        {loading
          ? Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} horizontal />)
          : recipes.slice(0, 12).map((r) => (
              <div key={r.id} className="horizontal-card-slot">
                <RecipeCard recipe={r} />
              </div>
            ))}
      </div>
    </section>
  );
}
