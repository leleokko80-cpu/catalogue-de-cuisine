import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import http from '../api/http';
import { CATEGORIES } from '../data/categoryList';
import {
  CategoryIcon,
  CuteBudgetIcon,
  CuteChefHatIcon,
  CuteChevronLeftIcon,
  CuteChevronRightIcon,
  CuteClockIcon,
  CuteSearchIcon,
  CuteSparkleIcon,
  CuteStarIcon,
  CuteTrophyIcon,
  CuteUtensilsIcon,
} from '../data/CuteIcons';
import RecipeFlipCard from '../components/RecipeFlipCard';
import HorizontalRecipeRow from '../components/HorizontalRecipeRow';
import RecipeImage from '../components/RecipeImage';
import SkeletonCard from '../components/SkeletonCard';

function isTunisianOrMed(r) {
  const text = `${r.title || ''} ${r.description || ''}`.toLowerCase();
  return /tunis|mediterranean|couscous|chakchouka|ojja|lablabi|mechouia|brik|fricas|mloukhia|makroudh|zgougou|bambalouni|tabouna|mtabga|mlawi|citronnade|bsissa|tajine|slata|hummus|baba ghanoush|feta|za'atar/.test(
    text
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const rowRef = useRef(null);
  const [heroQuery, setHeroQuery] = useState('');
  const [recipes, setRecipes] = useState([]);
  const [ranking, setRanking] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([http.get('/recipes'), http.get('/recipes/ranking')])
      .then(([allRes, rankRes]) => {
        setRecipes(allRes.data || []);
        setRanking(rankRes.data || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const topThree = ranking.slice(0, 3);
  const freshPicks = recipes.slice(0, 10);

  const quickThirty = recipes.filter((r) => r.prepTimeMinutes + r.cookTimeMinutes <= 30);
  const budgetUnderTen = recipes.filter((r) =>
    r.estimatedCostTnd != null ? r.estimatedCostTnd < 10 : r.budgetLevel === 'BUDGET'
  );
  const newestRecipes = [...recipes].sort((a, b) => b.id - a.id);
  const tunisianMed = recipes.filter(isTunisianOrMed);
  const categorySections = CATEGORIES.filter((c) => c.key !== 'ALL');

  function handleHeroSearch(e) {
    e.preventDefault();
    const trimmed = heroQuery.trim();
    navigate(trimmed ? `/recipes?q=${encodeURIComponent(trimmed)}` : '/recipes');
  }

  function scrollFreshRow(direction) {
    if (!rowRef.current) return;
    rowRef.current.scrollBy({ left: direction * 300, behavior: 'smooth' });
  }

  return (
    <div>
      {/* Video Hero Dashboard */}
      <section className="hero-video-card">
        <div className="hero-fallback-bg" aria-hidden="true" />
        <video
          className="hero-video-bg"
          autoPlay
          muted
          loop
          playsInline
          poster="https://images.unsplash.com/photo-1556910103-1c02745aae4d?auto=format&fit=crop&w=1600&q=80"
        >
          <source
            src="https://videos.pexels.com/video-files/3195650/3195650-uhd_2560_1440_25fps.mp4"
            type="video/mp4"
          />
          <source
            src="https://videos.pexels.com/video-files/4253001/4253001-uhd_2560_1440_25fps.mp4"
            type="video/mp4"
          />
        </video>
        <div className="hero-video-overlay" />

        <div className="hero-video-content">
          <div className="hero-kicker">
            <CuteChefHatIcon size={15} />
            <span>Culinary Studio &amp; Cozy Kitchen</span>
          </div>
          <h1>Cook with Soul, Flip for Smiles, Savor Every Bite.</h1>
          <p>
            Discover vibrant home-cooked dishes, honest community notes, and smart kitchen help from mr.mayo.
          </p>

          <form className="hero-search-form" onSubmit={handleHeroSearch}>
            <input
              type="search"
              placeholder="Search recipes by name, flavor or ingredient"
              value={heroQuery}
              onChange={(e) => setHeroQuery(e.target.value)}
              aria-label="Search recipes"
            />
            <button type="submit" className="btn btn-primary">
              <CuteSearchIcon size={16} />
              <span>Search</span>
            </button>
          </form>

          <div className="hero-cta-row">
            <Link to="/recipes" className="btn btn-primary">
              Browse All Recipes →
            </Link>
            <Link
              to="/ranking"
              className="btn"
              style={{ background: 'rgba(255,255,255,0.92)', color: '#7c2d12' }}
            >
              <CuteTrophyIcon size={16} />
              <span>Top Recipe Ranking</span>
            </Link>
          </div>

          <div className="hero-stats-row">
            <div className="hero-stat-pill">
              <CuteUtensilsIcon size={15} />
              <strong>{recipes.length}</strong>
              <span>Curated Recipes</span>
            </div>
            <div className="hero-stat-pill">
              <CuteSparkleIcon size={15} />
              <strong>{CATEGORIES.length - 1}</strong>
              <span>Culinary Sections</span>
            </div>
            {topThree[0] && (
              <div className="hero-stat-pill">
                <CuteTrophyIcon size={15} />
                <span>#1 Ranked:</span>
                <strong>{topThree[0].title}</strong>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Category Picture Explorer */}
      <section>
        <div className="page-header">
          <h2>Explore by Mood &amp; Section</h2>
          <Link to="/recipes" className="page-actions" style={{ fontWeight: 600 }}>
            See full catalog →
          </Link>
        </div>

        <div className="cat-picture-grid">
          {categorySections.map((cat) => (
            <Link
              key={cat.key}
              to={`/recipes?category=${cat.key}`}
              className="cat-picture-card"
            >
              <div className="cat-thumb-wrap">
                <img src={cat.thumb} alt={cat.label} className="cat-thumb-img" />
                <CategoryIcon categoryKey={cat.key} size={22} />
              </div>
              <span>{cat.label}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* Top 3 Leaderboard Preview */}
      {topThree.length > 0 && (
        <section style={{ marginBottom: '3rem' }}>
          <div className="page-header">
            <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <CuteTrophyIcon size={22} />
              <span>Community Top 3</span>
            </h2>
            <Link to="/ranking" className="page-actions" style={{ fontWeight: 600 }}>
              Full Leaderboard →
            </Link>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 'var(--space-3)' }}>
            {topThree.map((r, idx) => (
              <Link
                key={r.id}
                to={`/recipes/${r.id}`}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '12px',
                  borderRadius: 'var(--card-radius)',
                  background: '#fff',
                  border: idx === 0 ? '2px solid #f59e0b' : '1px solid var(--border)',
                  boxShadow: 'var(--shadow)',
                  color: 'inherit',
                }}
              >
                <div
                  style={{
                    fontWeight: 800,
                    fontSize: '1.1rem',
                    minWidth: 32,
                    height: 32,
                    borderRadius: '50%',
                    display: 'grid',
                    placeItems: 'center',
                    background: idx === 0 ? '#fef3c7' : idx === 1 ? '#f1f5f9' : '#ffedd5',
                    color: idx === 0 ? '#b45309' : idx === 1 ? '#475569' : '#9a3412',
                  }}
                >
                  #{idx + 1}
                </div>
                <RecipeImage
                  src={r.imageUrl}
                  alt={r.title}
                  category={r.category}
                  style={{ width: 60, height: 60, borderRadius: 12, objectFit: 'cover', flexShrink: 0, minHeight: 60 }}
                />
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {r.title}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: '0.84rem', marginTop: 2 }}>
                    <CuteStarIcon size={14} />
                    <strong>{r.averageRating > 0 ? r.averageRating.toFixed(1) : 'New'}</strong>
                    <span className="muted">
                      {r.ratingCount > 0 ? `based on ${r.ratingCount} ${r.ratingCount === 1 ? 'review' : 'reviews'}` : '(0 reviews)'}
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Fresh Picks — ONLY Horizontal Flip-Card Row */}
      <section>
        <div className="page-header">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <CuteSparkleIcon size={22} />
            <span>Fresh Picks (Click any card to flip)</span>
          </h2>
          <div className="page-actions fresh-row-controls">
            <button
              type="button"
              className="carousel-arrow-btn"
              onClick={() => scrollFreshRow(-1)}
              aria-label="Scroll Fresh Picks left"
              title="Scroll left"
            >
              <CuteChevronLeftIcon size={18} />
            </button>
            <button
              type="button"
              className="carousel-arrow-btn"
              onClick={() => scrollFreshRow(1)}
              aria-label="Scroll Fresh Picks right"
              title="Scroll right"
            >
              <CuteChevronRightIcon size={18} />
            </button>
          </div>
        </div>

        <div className="fresh-picks-row" ref={rowRef}>
          {loading
            ? Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} horizontal />)
            : freshPicks.map((r) => <RecipeFlipCard key={r.id} recipe={r} />)}
        </div>
      </section>

      {/* Plain RecipeCard Horizontal Rows */}
      <HorizontalRecipeRow
        title="Top Rated by the Community"
        recipes={ranking}
        loading={loading}
        seeAllTo="/ranking"
        icon={<CuteTrophyIcon size={20} />}
      />

      <HorizontalRecipeRow
        title="Ready in 30 Minutes or Less"
        recipes={quickThirty}
        loading={loading}
        seeAllTo="/recipes"
        icon={<CuteClockIcon size={20} />}
      />

      <HorizontalRecipeRow
        title="Budget-Friendly (under 10 TND)"
        recipes={budgetUnderTen}
        loading={loading}
        seeAllTo="/recipes"
        icon={<CuteBudgetIcon size={20} />}
      />

      <HorizontalRecipeRow
        title="New on the Site"
        recipes={newestRecipes}
        loading={loading}
        seeAllTo="/recipes"
        icon={<CuteSparkleIcon size={20} />}
      />

      <HorizontalRecipeRow
        title="Tunisian & Mediterranean Favorites"
        recipes={tunisianMed}
        loading={loading}
        seeAllTo="/recipes"
        icon={<CuteChefHatIcon size={20} />}
      />

      {categorySections.map((cat) => (
        <HorizontalRecipeRow
          key={cat.key}
          title={cat.label}
          recipes={recipes.filter((r) => r.category === cat.key)}
          loading={loading}
          seeAllTo={`/recipes?category=${cat.key}`}
          icon={<CategoryIcon categoryKey={cat.key} size={22} />}
        />
      ))}
    </div>
  );
}
