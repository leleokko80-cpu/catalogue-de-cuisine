import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import http from '../api/http';
import SearchBar from '../components/SearchBar';
import Alert from '../components/Alert';
import RecipeCard from '../components/RecipeCard';
import SkeletonCard from '../components/SkeletonCard';
import { CATEGORIES, getCategoryMeta } from '../data/categoryList';
import { normalizeIngredientClient } from '../utils/ingredientHelper';

function CategoryLineIcon({ categoryKey, size = 40 }) {
  const s = size;
  switch (categoryKey) {
    case 'ENTREES':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M8 32 C8 20 14 10 20 10 C26 10 32 20 32 32" />
          <path d="M8 32 h24" />
          <circle cx="20" cy="10" r="2.5" />
          <path d="M14 24 q3-4 6 0 q3 4 6 0" />
        </svg>
      );
    case 'PLATS':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <ellipse cx="20" cy="22" rx="13" ry="4" />
          <path d="M7 22 C7 15 13 10 20 10 C27 10 33 15 33 22" />
          <line x1="4" y1="26" x2="36" y2="26" />
          <line x1="20" y1="6" x2="20" y2="10" />
        </svg>
      );
    case 'DESSERTS':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 26 C14 20 10 16 10 16 C10 12 20 10 20 10 C30 10 30 12 30 16 C30 16 26 20 26 26" />
          <rect x="12" y="26" width="16" height="6" rx="2" />
          <path d="M20 10 L20 7" />
          <circle cx="20" cy="6" r="1.5" />
        </svg>
      );
    case 'SWEETS':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="20" cy="22" r="10" />
          <path d="M15 22 q5-6 10 0" />
          <line x1="20" y1="12" x2="20" y2="8" />
          <path d="M17 8 q3-3 6 0" />
        </svg>
      );
    case 'BREAKFAST':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="20" cy="22" r="11" />
          <circle cx="20" cy="22" r="5" />
          <line x1="20" y1="4" x2="20" y2="8" />
          <line x1="32" y1="10" x2="29" y2="13" />
          <line x1="8" y1="10" x2="11" y2="13" />
        </svg>
      );
    case 'APERITIFS':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 10 L20 22 L28 10" />
          <line x1="20" y1="22" x2="20" y2="30" />
          <line x1="13" y1="30" x2="27" y2="30" />
          <path d="M12 10 h16" />
        </svg>
      );
    case 'BOULANGE':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M8 24 C8 16 12 12 20 12 C28 12 32 16 32 24 L30 28 H10 Z" />
          <path d="M12 18 q4-4 8 0 q4 4 8 0" />
          <line x1="10" y1="28" x2="30" y2="28" />
        </svg>
      );
    case 'BOISSONS':
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 10 h12 l-2 20 H16 Z" />
          <path d="M26 14 C30 14 32 17 32 20 C32 23 30 26 26 26" />
          <line x1="13" y1="17" x2="23" y2="17" />
        </svg>
      );
    default:
      return (
        <svg width={s} height={s} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="20" cy="20" r="12" />
          <path d="M14 20 h12 M20 14 v12" />
        </svg>
      );
  }
}

const CAT_DISPLAY_LABELS = {
  ALL: 'Toutes',
  ENTREES: 'Entrées',
  PLATS: 'Plats',
  DESSERTS: 'Desserts',
  SWEETS: 'Sweets',
  BREAKFAST: 'Brunch',
  APERITIFS: 'Apéritifs',
  BOULANGE: 'Boulange',
  BOISSONS: 'Boissons',
};

function computeClosestMatches(allRecipes, terms) {
  if (!allRecipes.length || !terms.length) return [];
  const lowerTerms = terms.map((t) => t.toLowerCase().trim()).filter(Boolean);
  return allRecipes
    .map((r) => {
      const hay = [
        r.title || '',
        r.description || '',
        ...(r.ingredients || []).map((i) => i.name || ''),
      ]
        .join(' ')
        .toLowerCase();
      let score = 0;
      lowerTerms.forEach((t) => {
        if (hay.includes(t)) score += 2;
        else if (t.length > 3 && hay.includes(t.slice(0, -1))) score += 1;
      });
      return { recipe: r, score };
    })
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 6)
    .map((x) => x.recipe);
}

export default function RecipeListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get('q') || '';
  const category = (searchParams.get('category') || 'ALL').toUpperCase();

  const [input, setInput] = useState(q);
  const [ingredients, setIngredients] = useState([]);
  const [liveIngredient, setLiveIngredient] = useState('');
  const [maxTime, setMaxTime] = useState('');
  const [budget, setBudget] = useState('');
  const [excludedAllergens, setExcludedAllergens] = useState([]);
  const [sort, setSort] = useState('newest');

  const combinedIngredients = [
    ...ingredients,
    ...(liveIngredient && !ingredients.some((i) => i.toLowerCase() === liveIngredient.toLowerCase())
      ? [liveIngredient]
      : []),
  ];

  const queryKey = JSON.stringify({
    q,
    category,
    combinedIngredients,
    maxTime,
    budget,
    excludedAllergens,
    sort,
  });

  const [fetchState, setFetchState] = useState({
    loadedKey: null,
    recipes: [],
    widenedFrom: null,
    closestMatches: [],
    didYouMean: '',
    error: '',
  });

  useEffect(() => {
    const timer = setTimeout(() => {
      if (input.trim() !== q) {
        const next = new URLSearchParams(searchParams);
        if (input.trim()) next.set('q', input.trim());
        else next.delete('q');
        setSearchParams(next, { replace: true });
      }
    }, 280);
    return () => clearTimeout(timer);
  }, [input, q, searchParams, setSearchParams]);

  function selectCategory(catKey) {
    const next = new URLSearchParams(searchParams);
    if (catKey === 'ALL') next.delete('category');
    else next.set('category', catKey);
    setSearchParams(next, { replace: true });
  }

  function handleResetAll() {
    setInput('');
    setIngredients([]);
    setLiveIngredient('');
    setMaxTime('');
    setBudget('');
    setExcludedAllergens([]);
    setSort('newest');
    selectCategory('ALL');
  }

  useEffect(() => {
    let cancelled = false;
    const baseParams = {};
    if (q) baseParams.q = q;
    if (combinedIngredients.length > 0) baseParams.ingredients = combinedIngredients.join(',');
    if (maxTime) baseParams.maxTime = maxTime;
    if (budget) baseParams.budget = budget;
    if (excludedAllergens.length > 0) baseParams.excludeAllergen = excludedAllergens.join(',');
    if (sort) baseParams.sort = sort;

    const paramsWithCat = { ...baseParams };
    if (category && category !== 'ALL') paramsWithCat.category = category;

    async function load() {
      try {
        const res = await http.get('/recipes', { params: paramsWithCat });
        let data = res.data || [];
        let widenedFrom = null;

        // If searching by ingredient in a specific category yields 0 results, widen to ALL categories
        if (data.length === 0 && category !== 'ALL' && combinedIngredients.length > 0) {
          const allCatRes = await http.get('/recipes', { params: baseParams });
          if ((allCatRes.data || []).length > 0) {
            data = allCatRes.data;
            widenedFrom = getCategoryMeta(category).label;
          }
        }

        let closestMatches = [];
        let didYouMean = '';
        if (data.length === 0) {
          const allRes = await http.get('/recipes', { params: { sort: 'ranking' } });
          const terms = [...combinedIngredients, ...q.split(/\s+/).filter(Boolean)];
          closestMatches = computeClosestMatches(allRes.data || [], terms);
          if (q.trim()) {
            const normQ = normalizeIngredientClient(q.trim());
            if (normQ.valid && normQ.corrected) didYouMean = normQ.normalized;
          } else if (liveIngredient) {
            const normIng = normalizeIngredientClient(liveIngredient);
            if (normIng.valid && normIng.corrected) didYouMean = normIng.normalized;
          }
        }

        if (!cancelled) {
          setFetchState({
            loadedKey: queryKey,
            recipes: data,
            widenedFrom,
            closestMatches,
            didYouMean,
            error: '',
          });
        }
      } catch {
        if (!cancelled) {
          setFetchState({
            loadedKey: queryKey,
            recipes: [],
            widenedFrom: null,
            closestMatches: [],
            didYouMean: '',
            error: 'Could not load recipes. Is the backend running?',
          });
        }
      }
    }

    load();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryKey]);

  const loading = fetchState.loadedKey !== queryKey;
  const { recipes, widenedFrom, closestMatches, didYouMean, error } = fetchState;
  const displayCats = CATEGORIES.filter((c) => c.key !== 'ALL');

  return (
    <div>
      <div className="recipes-banner">
        <img
          src="https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=70"
          alt=""
          aria-hidden="true"
          className="recipes-banner__photo"
        />
        <img
          src="https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=600&q=70"
          alt=""
          aria-hidden="true"
          className="recipes-banner__photo"
        />
        <img
          src="https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=70"
          alt=""
          aria-hidden="true"
          className="recipes-banner__photo"
        />
        <div className="recipes-banner__overlay" />
        <h1 className="recipes-banner__title">Recettes à savourer sans modération</h1>
      </div>

      <div className="recipes-cat-cards">
        <button
          type="button"
          className={`recipes-cat-card ${category === 'ALL' ? 'recipes-cat-card--active' : ''}`}
          onClick={() => selectCategory('ALL')}
        >
          <div className="recipes-cat-card__icon" style={{ color: '#b85c38' }}>
            <svg width={40} height={40} viewBox="0 0 40 40" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="6" y="6" width="12" height="12" rx="2" />
              <rect x="22" y="6" width="12" height="12" rx="2" />
              <rect x="6" y="22" width="12" height="12" rx="2" />
              <rect x="22" y="22" width="12" height="12" rx="2" />
            </svg>
          </div>
          <span className="recipes-cat-card__name">Toutes</span>
        </button>

        {displayCats.map((cat) => (
          <button
            key={cat.key}
            type="button"
            className={`recipes-cat-card ${category === cat.key ? 'recipes-cat-card--active' : ''}`}
            onClick={() => selectCategory(cat.key)}
          >
            <div className="recipes-cat-card__icon" style={{ color: '#b85c38' }}>
              <CategoryLineIcon categoryKey={cat.key} size={40} />
            </div>
            <span className="recipes-cat-card__name">
              {CAT_DISPLAY_LABELS[cat.key] || cat.label}
            </span>
          </button>
        ))}
      </div>

      <div style={{ marginTop: 'var(--space-3)' }}>
        <SearchBar
          value={input}
          onChange={setInput}
          ingredients={ingredients}
          onIngredientsChange={setIngredients}
          liveIngredient={liveIngredient}
          onLiveIngredientChange={setLiveIngredient}
          maxTime={maxTime}
          onMaxTimeChange={setMaxTime}
          budget={budget}
          onBudgetChange={setBudget}
          excludedAllergens={excludedAllergens}
          onExcludedAllergensChange={setExcludedAllergens}
          sort={sort}
          onSortChange={setSort}
          onResetAll={handleResetAll}
        />
      </div>

      {widenedFrom && (
        <p className="muted" style={{ margin: '0 0 var(--space-3)', fontSize: '0.9rem', fontWeight: 600 }}>
          No match in {widenedFrom}, showing results from all categories.
        </p>
      )}

      {loading && (
        <div className="recipes-grid" style={{ marginTop: 'var(--space-3)' }}>
          {Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      )}

      {error && <Alert type="error">{error}</Alert>}

      {!loading && !error && recipes.length === 0 && (
        <div style={{ marginTop: 'var(--space-3)' }}>
          <div className="form-card" style={{ textAlign: 'center', marginBottom: 'var(--space-4)' }}>
            {didYouMean && (
              <p style={{ marginBottom: 'var(--space-2)' }}>
                Did you mean{' '}
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => {
                    setInput(didYouMean);
                    setLiveIngredient('');
                  }}
                >
                  {didYouMean}
                </button>
                ?
              </p>
            )}
            <p style={{ marginBottom: 'var(--space-3)' }}>
              No exact recipes matched all filters. Try clearing a filter or{' '}
              <Link to="/recipes/new">create a new recipe</Link>!
            </p>
            <button type="button" className="btn btn-primary btn-sm" onClick={handleResetAll}>
              Reset All Filters
            </button>
          </div>

          {closestMatches.length > 0 && (
            <div>
              <h3 style={{ marginBottom: 'var(--space-3)' }}>Closest matches</h3>
              <div className="recipes-grid">
                {closestMatches.map((r) => <RecipeCard key={r.id} recipe={r} />)}
              </div>
            </div>
          )}
        </div>
      )}

      {!loading && !error && recipes.length > 0 && (
        <div className="recipes-grid" style={{ marginTop: 'var(--space-3)' }}>
          {recipes.map((r) => <RecipeCard key={r.id} recipe={r} />)}
        </div>
      )}
    </div>
  );
}
