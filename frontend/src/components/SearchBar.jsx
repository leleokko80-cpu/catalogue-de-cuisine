import { useEffect, useState } from 'react';
import { normalizeIngredientClient, KNOWN_INGREDIENTS } from '../utils/ingredientHelper';

const ALLERGEN_OPTIONS = [
  { key: 'nuts', label: 'No Nuts' },
  { key: 'dairy', label: 'No Dairy' },
  { key: 'gluten', label: 'Gluten-Free' },
  { key: 'eggs', label: 'Egg-Free' },
  { key: 'pork', label: 'No Pork' },
  { key: 'fish', label: 'No Fish' },
];

export default function SearchBar({
  value,
  onChange,
  ingredients,
  onIngredientsChange,
  liveIngredient,
  onLiveIngredientChange,
  maxTime,
  onMaxTimeChange,
  budget,
  onBudgetChange,
  excludedAllergens,
  onExcludedAllergensChange,
  sort,
  onSortChange,
  onResetAll,
}) {
  const [ingDraft, setIngDraft] = useState(liveIngredient || '');
  const [showAdvanced, setShowAdvanced] = useState(true);

  const normalizedPreview = (() => {
    const raw = ingDraft.trim();
    if (raw.length < 2) return null;
    const res = normalizeIngredientClient(raw);
    if (res.valid && res.corrected && res.normalized.toLowerCase() !== raw.toLowerCase()) {
      return res.normalized;
    }
    return null;
  })();

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const raw = ingDraft.trim();
      if (!raw) {
        if (liveIngredient !== '') onLiveIngredientChange('');
        return;
      }
      const res = normalizeIngredientClient(raw);
      const nextTerm = res.valid && res.corrected ? res.normalized : raw;
      if (nextTerm !== liveIngredient) {
        onLiveIngredientChange(nextTerm);
      }
    }, 250);
    return () => window.clearTimeout(timer);
  }, [ingDraft, liveIngredient, onLiveIngredientChange]);

  function commitIngredientChip(rawText) {
    const raw = (rawText ?? ingDraft).replace(/,/g, '').trim();
    if (!raw) return;
    const res = normalizeIngredientClient(raw);
    const term = res.valid ? res.normalized : raw;
    if (!ingredients.some((i) => i.toLowerCase() === term.toLowerCase())) {
      onIngredientsChange([...ingredients, term]);
    }
    setIngDraft('');
    onLiveIngredientChange('');
  }

  function handleIngredientKeyDown(e) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      commitIngredientChip(ingDraft);
    }
  }

  function handleIngredientChange(e) {
    const val = e.target.value;
    if (val.includes(',')) {
      const parts = val.split(',');
      parts.slice(0, -1).forEach((p) => commitIngredientChip(p));
      setIngDraft(parts[parts.length - 1]);
    } else {
      setIngDraft(val);
    }
  }

  function removeIngredient(name) {
    onIngredientsChange(ingredients.filter((i) => i !== name));
  }

  function toggleAllergen(key) {
    if (excludedAllergens.includes(key)) {
      onExcludedAllergensChange(excludedAllergens.filter((a) => a !== key));
    } else {
      onExcludedAllergensChange([...excludedAllergens, key]);
    }
  }

  function handleClearAll() {
    setIngDraft('');
    onResetAll();
  }

  const hasFilters =
    Boolean(value) ||
    ingredients.length > 0 ||
    Boolean(ingDraft.trim()) ||
    Boolean(maxTime) ||
    Boolean(budget) ||
    excludedAllergens.length > 0;

  return (
    <div className="adv-search-box">
      <div className="adv-search-main">
        <input
          type="search"
          placeholder="Search recipes by name, flavor or ingredient"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          style={{ flex: '2 1 240px' }}
        />

        <div className="chip-input-row" style={{ flex: '1.5 1 230px' }}>
          <input
            type="text"
            list="search-ing-suggestions"
            placeholder="Search by ingredient you have..."
            value={ingDraft}
            onChange={handleIngredientChange}
            onKeyDown={handleIngredientKeyDown}
            aria-label="Search by ingredient you have"
          />
          <datalist id="search-ing-suggestions">
            {KNOWN_INGREDIENTS.map((item) => (
              <option key={item} value={item} />
            ))}
          </datalist>
        </div>

        <button
          type="button"
          className="btn btn-sm"
          onClick={() => setShowAdvanced((s) => !s)}
        >
          {showAdvanced ? 'Hide Filters' : 'More Filters'}
        </button>

        {hasFilters && (
          <button type="button" className="btn btn-sm" onClick={handleClearAll}>
            Clear All
          </button>
        )}
      </div>

      {normalizedPreview && (
        <div style={{ marginTop: 6, fontSize: '0.82rem', color: 'var(--primary)', fontWeight: 600 }}>
          Showing results for <strong>{normalizedPreview}</strong>
        </div>
      )}

      {showAdvanced && (
        <div className="adv-search-filters">
          <div>
            <label htmlFor="filter-max-time" style={{ fontSize: '0.8rem' }}>Max Total Time</label>
            <select
              id="filter-max-time"
              value={maxTime}
              onChange={(e) => onMaxTimeChange(e.target.value)}
            >
              <option value="">Any time</option>
              <option value="15">Quick (15 min or less)</option>
              <option value="30">Weeknight (30 min or less)</option>
              <option value="60">Under 1 hour</option>
            </select>
          </div>

          <div>
            <label htmlFor="filter-budget" style={{ fontSize: '0.8rem' }}>Budget</label>
            <select
              id="filter-budget"
              value={budget}
              onChange={(e) => onBudgetChange(e.target.value)}
            >
              <option value="">Any budget</option>
              <option value="BUDGET">Budget-Friendly</option>
              <option value="MODERATE">Moderate</option>
              <option value="GOURMET">Gourmet</option>
            </select>
          </div>

          <div>
            <label htmlFor="filter-sort" style={{ fontSize: '0.8rem' }}>Sort By</label>
            <select
              id="filter-sort"
              value={sort}
              onChange={(e) => onSortChange(e.target.value)}
            >
              <option value="newest">Newest First</option>
              <option value="ranking">Top Rated / Ranking</option>
              <option value="popular">Most Favorited</option>
              <option value="fastest">Fastest to Cook</option>
            </select>
          </div>

          <div>
            <span style={{ display: 'block', fontWeight: 600, fontSize: '0.8rem', marginBottom: '6px' }}>
              Allergies / Exclude
            </span>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {ALLERGEN_OPTIONS.map((opt) => {
                const active = excludedAllergens.includes(opt.key);
                return (
                  <button
                    key={opt.key}
                    type="button"
                    onClick={() => toggleAllergen(opt.key)}
                    className="btn btn-sm"
                    style={{
                      padding: '4px 9px',
                      fontSize: '0.76rem',
                      background: active ? '#fee2e2' : '#fff',
                      borderColor: active ? '#dc2626' : 'var(--border)',
                      color: active ? '#991b1b' : 'var(--text)',
                    }}
                  >
                    {active ? `Excluded: ${opt.label}` : opt.label}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {ingredients.length > 0 && (
        <div className="active-chips">
          <span className="muted" style={{ fontSize: '0.8rem', alignSelf: 'center' }}>
            Contains all:
          </span>
          {ingredients.map((ing) => (
            <span key={ing} className="filter-chip">
              <span>{ing}</span>
              <button type="button" onClick={() => removeIngredient(ing)} aria-label={`Remove ${ing}`}>
                ×
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
