import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import http from '../api/http';
import Alert from '../components/Alert';
import RecipeImage from '../components/RecipeImage';
import SkeletonCard from '../components/SkeletonCard';
import { getCategoryMeta } from '../data/categoryList';
import {
  CategoryIcon,
  CuteClockIcon,
  CuteHeartIcon,
  CuteStarIcon,
  CuteTrophyIcon,
} from '../data/CuteIcons';

export default function RankingPage() {
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    http
      .get('/recipes/ranking')
      .then((res) => setRecipes(res.data || []))
      .catch(() => setError('Could not load recipe rankings.'))
      .finally(() => setLoading(false));
  }, []);

  const podium = recipes.slice(0, 3);
  const rest = recipes.slice(3);

  return (
    <div>
      <div className="page-header">
        <div>
          <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <CuteTrophyIcon size={24} />
            <span>Recipe Ranking &amp; Hall of Fame</span>
          </h2>
          <p className="muted" style={{ margin: '4px 0 0' }}>
            Ranked by community star ratings, review count, and total saves.
          </p>
        </div>
        <Link to="/recipes" className="btn btn-sm page-actions">
          Browse All Recipes
        </Link>
      </div>

      {loading && (
        <div className="recipes-grid">
          {Array.from({ length: 3 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      )}

      {error && <Alert type="error">{error}</Alert>}

      {!loading && !error && podium.length > 0 && (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
              gap: 'var(--space-3)',
              margin: 'var(--space-3) 0 var(--space-4)',
            }}
          >
            {podium.map((r, index) => {
              const cat = getCategoryMeta(r.category);
              const label = index === 0 ? '#1 Champion' : index === 1 ? '#2 Runner-Up' : '#3 Bronze';
              const borderCol = index === 0 ? '#f59e0b' : index === 1 ? '#94a3b8' : '#d97706';

              return (
                <Link
                  key={r.id}
                  to={`/recipes/${r.id}`}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    background: '#fff',
                    borderRadius: 'var(--card-radius)',
                    overflow: 'hidden',
                    border: `2px solid ${borderCol}`,
                    boxShadow: 'var(--shadow-lg)',
                    color: 'inherit',
                  }}
                >
                  <div className="card-header-row">
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        fontWeight: 800,
                        fontSize: '0.84rem',
                        color: index === 0 ? '#b45309' : '#374151',
                      }}
                    >
                      <CuteTrophyIcon size={16} />
                      <span>{label}</span>
                    </span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: '0.78rem', fontWeight: 700, color: cat.accent }}>
                      <CategoryIcon categoryKey={cat.key} size={20} />
                      {cat.label}
                    </span>
                  </div>

                  <div style={{ height: 170, overflow: 'hidden' }}>
                    <RecipeImage
                      src={r.imageUrl}
                      alt={r.title}
                      category={r.category}
                      style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                    />
                  </div>

                  <div style={{ padding: 'var(--space-3)' }}>
                    <h3 style={{ margin: '0 0 var(--space-2)' }}>{r.title}</h3>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: '0.86rem', flexWrap: 'wrap' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                        <CuteStarIcon size={16} />
                        <strong>{r.averageRating > 0 ? r.averageRating.toFixed(1) : 'New'}</strong>
                        <span className="muted">
                          {r.ratingCount > 0 ? `based on ${r.ratingCount} ${r.ratingCount === 1 ? 'review' : 'reviews'}` : '(0 reviews)'}
                        </span>
                      </span>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }} className="muted">
                        <CuteClockIcon size={14} /> {r.prepTimeMinutes + r.cookTimeMinutes} min
                      </span>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }} className="muted">
                        <CuteHeartIcon filled size={14} /> {r.favoriteCount || 0}
                      </span>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>

          {rest.length > 0 && (
            <div
              style={{
                background: '#fff',
                border: '1px solid var(--border)',
                borderRadius: 'var(--card-radius)',
                overflow: 'hidden',
                boxShadow: 'var(--shadow)',
              }}
            >
              {rest.map((r, idx) => {
                const cat = getCategoryMeta(r.category);
                return (
                  <Link
                    key={r.id}
                    to={`/recipes/${r.id}`}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 'var(--space-3)',
                      padding: '14px var(--space-3)',
                      borderBottom: idx < rest.length - 1 ? '1px solid #f3ece4' : 'none',
                      color: 'inherit',
                      flexWrap: 'wrap',
                    }}
                  >
                    <span style={{ fontWeight: 800, fontSize: '1.05rem', minWidth: 34, color: '#9a3412' }}>
                      #{idx + 4}
                    </span>
                    <RecipeImage
                      src={r.imageUrl}
                      alt={r.title}
                      category={r.category}
                      style={{ width: 54, height: 54, borderRadius: 10, objectFit: 'cover', flexShrink: 0, minHeight: 54 }}
                    />
                    <div style={{ flex: 1, minWidth: 160 }}>
                      <div style={{ fontWeight: 700 }}>{r.title}</div>
                      <div className="muted" style={{ fontSize: '0.82rem', display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                        <span>{cat.label}</span>
                        <span>· {r.prepTimeMinutes + r.cookTimeMinutes} min</span>
                        <span>· by {r.authorName}</span>
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                        <CuteStarIcon size={15} />
                        <strong>{r.averageRating > 0 ? r.averageRating.toFixed(1) : 'New'}</strong>
                        <span className="muted" style={{ fontSize: '0.82rem' }}>
                          {r.ratingCount > 0 ? `based on ${r.ratingCount} ${r.ratingCount === 1 ? 'review' : 'reviews'}` : '(0 reviews)'}
                        </span>
                      </span>
                      <span className="muted" style={{ fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                        <CuteHeartIcon filled size={13} /> {r.favoriteCount || 0}
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
