import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import http from '../api/http';
import { useToast } from '../context/ToastContext';
import Alert from '../components/Alert';
import RecipeImage from '../components/RecipeImage';
import { CATEGORIES, getCategoryMeta } from '../data/categoryList';
import { CategoryIcon, CuteClockIcon, CuteSparkleIcon, CuteUtensilsIcon } from '../data/CuteIcons';
import { KNOWN_INGREDIENTS, normalizeIngredientClient } from '../utils/ingredientHelper';

const BG_FOOD_PHOTOS = [
  'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1600&q=80',
  'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1600&q=80',
  'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=1600&q=80',
  'https://images.unsplash.com/photo-1551024601-bec78aea704b?auto=format&fit=crop&w=1600&q=80',
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1600&q=80',
];

const QUICK_IMAGE_PRESETS = [
  { label: 'Pasta', url: 'https://images.unsplash.com/photo-1612874742237-6526221588e3?auto=format&fit=crop&w=900&q=80' },
  { label: 'Dessert', url: 'https://images.unsplash.com/photo-1624353365286-3f8d62daad51?auto=format&fit=crop&w=900&q=80' },
  { label: 'Salad / Bowl', url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80' },
  { label: 'Bakery', url: 'https://images.unsplash.com/photo-1509365465985-25d11c17e812?auto=format&fit=crop&w=900&q=80' },
];

const emptyForm = {
  title: '',
  description: '',
  imageUrl: '',
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  servings: 2,
  category: 'PLATS',
  budgetLevel: 'MODERATE',
  estimatedCostTnd: '',
  funNote: '',
  allergens: '',
};

export default function AddRecipePage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [bgIndex, setBgIndex] = useState(0);
  const [form, setForm] = useState(emptyForm);
  const [ingredients, setIngredients] = useState([
    { name: '', quantity: '', fixedFrom: '', error: '' },
  ]);
  const [steps, setSteps] = useState(['']);
  const [fieldErrors, setFieldErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const id = window.setInterval(() => {
      setBgIndex((i) => (i + 1) % BG_FOOD_PHOTOS.length);
    }, 4500);
    return () => window.clearInterval(id);
  }, []);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const updateIngredient = (index, field, value) => {
    setIngredients((prev) =>
      prev.map((ing, i) =>
        i === index ? { ...ing, [field]: value, error: field === 'name' ? '' : ing.error } : ing
      )
    );
  };

  const handleIngredientBlur = (index) => {
    setIngredients((prev) =>
      prev.map((ing, i) => {
        if (i !== index || !ing.name.trim()) return ing;
        const res = normalizeIngredientClient(ing.name);
        if (!res.valid) {
          return { ...ing, error: res.reason };
        }
        return {
          ...ing,
          name: res.normalized,
          fixedFrom: res.corrected ? ing.name.trim() : '',
          error: '',
        };
      })
    );
  };

  const addIngredient = () =>
    setIngredients([...ingredients, { name: '', quantity: '', fixedFrom: '', error: '' }]);
  const removeIngredient = (index) =>
    setIngredients(ingredients.filter((_, i) => i !== index));

  const updateStep = (index, value) => {
    setSteps(steps.map((s, i) => (i === index ? value : s)));
  };
  const addStep = () => setSteps([...steps, '']);
  const removeStep = (index) => setSteps(steps.filter((_, i) => i !== index));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setGlobalError('');

    const validated = [];
    let hasInvalidIng = false;
    const nextIngredients = ingredients.map((ing) => {
      if (!ing.name.trim()) return ing;
      const res = normalizeIngredientClient(ing.name);
      if (!res.valid) {
        hasInvalidIng = true;
        return { ...ing, error: res.reason };
      }
      validated.push({ name: res.normalized, quantity: ing.quantity.trim() });
      return {
        ...ing,
        name: res.normalized,
        fixedFrom: res.corrected ? ing.name.trim() : ing.fixedFrom,
        error: '',
      };
    });
    setIngredients(nextIngredients);

    if (hasInvalidIng) {
      setGlobalError('Please fix the highlighted invalid ingredient(s).');
      return;
    }

    setSubmitting(true);
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      imageUrl: form.imageUrl.trim(),
      prepTimeMinutes: Number(form.prepTimeMinutes),
      cookTimeMinutes: Number(form.cookTimeMinutes),
      servings: Number(form.servings),
      category: form.category,
      budgetLevel: form.budgetLevel,
      estimatedCostTnd: form.estimatedCostTnd === '' ? null : Number(form.estimatedCostTnd),
      funNote: form.funNote.trim(),
      allergens: form.allergens.trim(),
      ingredients: validated,
      steps: steps.filter((s) => s.trim() !== ''),
    };

    try {
      const res = await http.post('/recipes', payload);
      showToast('Recipe published to the catalogue!', 'success');
      navigate(`/recipes/${res.data.id}`);
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;
      if (status === 400 && data?.errors) {
        setFieldErrors(data.errors);
        setGlobalError('Please fix the highlighted fields.');
      } else if (status === 401 || status === 403) {
        setGlobalError('You must be logged in to add a recipe.');
      } else {
        setGlobalError(data?.message || 'Something went wrong. Is the backend running?');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const errorText = (name) =>
    fieldErrors[name] ? <div className="field-error">{fieldErrors[name]}</div> : null;

  const activeCat = getCategoryMeta(form.category);

  return (
    <div style={{ position: 'relative', borderRadius: 24, overflow: 'hidden', padding: 'var(--space-4) var(--space-3)' }}>
      <div
        aria-hidden="true"
        style={{
          position: 'fixed',
          inset: 0,
          zIndex: -2,
          overflow: 'hidden',
          background: '#1c120c',
        }}
      >
        {BG_FOOD_PHOTOS.map((src, idx) => (
          <img
            key={src}
            src={src}
            alt=""
            style={{
              position: 'absolute',
              inset: 0,
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              opacity: idx === bgIndex ? 1 : 0,
              transform: idx === bgIndex ? 'scale(1.06)' : 'scale(1)',
              transition: 'opacity 1.4s ease-in-out, transform 6s ease-out',
            }}
          />
        ))}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'linear-gradient(135deg, rgba(28, 18, 12, 0.72), rgba(124, 45, 18, 0.45))',
            backdropFilter: 'blur(3px)',
          }}
        />
      </div>

      <div className="studio-grid">
        <form
          onSubmit={handleSubmit}
          className="form-card"
          style={{
            maxWidth: '100%',
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(14px)',
            boxShadow: '0 24px 60px rgba(0,0,0,0.28)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
            <CategoryIcon categoryKey={activeCat.key} />
            <h2 style={{ margin: 0 }}>Recipe Creation Studio</h2>
          </div>
          <p className="muted" style={{ marginTop: 0, marginBottom: 'var(--space-3)', fontSize: '0.92rem' }}>
            Share your favorite dish with clear ingredients, timings, and cooking steps.
          </p>

          {globalError && (
            <Alert type="error" onClose={() => setGlobalError('')}>
              {globalError}
            </Alert>
          )}

          <div className="field">
            <label htmlFor="title">Recipe Title *</label>
            <input
              id="title"
              name="title"
              placeholder="e.g. Caramelized Peach Tart"
              value={form.title}
              onChange={handleChange}
            />
            {errorText('title')}
          </div>

          <div className="field-row">
            <div className="field">
              <label htmlFor="category">Section / Category</label>
              <select id="category" name="category" value={form.category} onChange={handleChange}>
                {CATEGORIES.filter((c) => c.key !== 'ALL').map((c) => (
                  <option key={c.key} value={c.key}>
                    {c.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <label htmlFor="budgetLevel">Budget Level</label>
              <select id="budgetLevel" name="budgetLevel" value={form.budgetLevel} onChange={handleChange}>
                <option value="BUDGET">Budget-Friendly</option>
                <option value="MODERATE">Moderate</option>
                <option value="GOURMET">Gourmet</option>
              </select>
            </div>

            <div className="field">
              <label htmlFor="estimatedCostTnd">Est. Cost (TND)</label>
              <input
                id="estimatedCostTnd"
                name="estimatedCostTnd"
                type="number"
                min="0"
                step="0.5"
                placeholder="e.g. 12"
                value={form.estimatedCostTnd}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="field">
            <label htmlFor="description">Short Pitch / Description</label>
            <textarea
              id="description"
              name="description"
              rows={2}
              placeholder="Tell fellow cooks what makes this dish irresistible..."
              value={form.description}
              onChange={handleChange}
            />
            {errorText('description')}
          </div>

          <div className="field">
            <label htmlFor="imageUrl">Dish Photo URL</label>
            <input
              id="imageUrl"
              name="imageUrl"
              placeholder="https://..."
              value={form.imageUrl}
              onChange={handleChange}
            />
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 6 }}>
              <span className="muted" style={{ fontSize: '0.78rem', alignSelf: 'center' }}>Quick photos:</span>
              {QUICK_IMAGE_PRESETS.map((p) => (
                <button
                  key={p.label}
                  type="button"
                  className="btn btn-sm"
                  style={{ fontSize: '0.75rem', padding: '2px 8px' }}
                  onClick={() => setForm({ ...form, imageUrl: p.url })}
                >
                  {p.label}
                </button>
              ))}
            </div>
            {errorText('imageUrl')}
          </div>

          <div className="field-row">
            <div className="field">
              <label htmlFor="prepTimeMinutes">Prep (min)</label>
              <input
                id="prepTimeMinutes"
                name="prepTimeMinutes"
                type="number"
                min="0"
                value={form.prepTimeMinutes}
                onChange={handleChange}
              />
              {errorText('prepTimeMinutes')}
            </div>
            <div className="field">
              <label htmlFor="cookTimeMinutes">Cook (min)</label>
              <input
                id="cookTimeMinutes"
                name="cookTimeMinutes"
                type="number"
                min="0"
                value={form.cookTimeMinutes}
                onChange={handleChange}
              />
              {errorText('cookTimeMinutes')}
            </div>
            <div className="field">
              <label htmlFor="servings">Servings</label>
              <input
                id="servings"
                name="servings"
                type="number"
                min="1"
                value={form.servings}
                onChange={handleChange}
              />
              {errorText('servings')}
            </div>
          </div>

          <div className="field">
            <label htmlFor="funNote">Card-Flip Fun / Chill Note</label>
            <input
              id="funNote"
              name="funNote"
              placeholder="e.g. Play cozy jazz while whisking — tastes 10x better!"
              value={form.funNote}
              onChange={handleChange}
            />
          </div>

          <div className="field">
            <label htmlFor="allergens">Allergens (optional, comma-separated)</label>
            <input
              id="allergens"
              name="allergens"
              placeholder="e.g. eggs, dairy, gluten, nuts"
              value={form.allergens}
              onChange={handleChange}
            />
          </div>

          <h3>Ingredients *</h3>
          <datalist id="studio-ingredients">
            {KNOWN_INGREDIENTS.map((item) => (
              <option key={item} value={item} />
            ))}
          </datalist>

          {ingredients.map((ing, index) => (
            <div key={index} style={{ marginBottom: '10px' }}>
              <div className="list-row" style={{ marginBottom: 2 }}>
                <input
                  list="studio-ingredients"
                  placeholder="Ingredient name"
                  value={ing.name}
                  onChange={(e) => updateIngredient(index, 'name', e.target.value)}
                  onBlur={() => handleIngredientBlur(index)}
                  style={{
                    flex: 2,
                    borderColor: ing.error ? 'var(--danger)' : undefined,
                  }}
                />
                <input
                  placeholder="Quantity (e.g. 200 g)"
                  value={ing.quantity}
                  onChange={(e) => updateIngredient(index, 'quantity', e.target.value)}
                  style={{ flex: 1 }}
                />
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => removeIngredient(index)}
                  disabled={ingredients.length === 1}
                >
                  Remove
                </button>
              </div>
              {ing.fixedFrom && !ing.error && (
                <div style={{ fontSize: '0.78rem', color: 'var(--success)', fontWeight: 600 }}>
                  Auto-corrected: &ldquo;{ing.fixedFrom}&rdquo; → {ing.name}
                </div>
              )}
              {ing.error && <div className="field-error">{ing.error}</div>}
            </div>
          ))}
          {errorText('ingredients')}
          <button type="button" className="btn btn-sm" onClick={addIngredient}>
            + Add Ingredient
          </button>

          <h3 style={{ marginTop: 'var(--space-4)' }}>Preparation Steps *</h3>
          {steps.map((step, index) => (
            <div key={index} className="list-row">
              <span className="step-number">{index + 1}.</span>
              <textarea
                rows={2}
                placeholder="Describe this step clearly..."
                value={step}
                onChange={(e) => updateStep(index, e.target.value)}
                style={{ flex: 1 }}
              />
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => removeStep(index)}
                disabled={steps.length === 1}
              >
                Remove
              </button>
            </div>
          ))}
          {errorText('steps')}
          <button type="button" className="btn btn-sm" onClick={addStep}>
            + Add Step
          </button>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting} style={{ width: '100%' }}>
              <CuteSparkleIcon size={16} />
              <span>{submitting ? 'Publishing Recipe...' : 'Publish Recipe to Catalogue'}</span>
            </button>
          </div>
        </form>

        <aside
          style={{
            position: 'sticky',
            top: 90,
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(14px)',
            borderRadius: 'var(--card-radius)',
            border: '1px solid rgba(255,255,255,0.6)',
            overflow: 'hidden',
            boxShadow: '0 20px 45px rgba(0,0,0,0.24)',
          }}
        >
          <div style={{ padding: '10px 14px', background: '#fff7ed', fontWeight: 700, fontSize: '0.84rem', color: '#9a3412', display: 'flex', alignItems: 'center', gap: 6 }}>
            <CuteSparkleIcon size={15} />
            <span>Live Card Preview</span>
          </div>
          <div style={{ height: 170, overflow: 'hidden' }}>
            <RecipeImage
              src={form.imageUrl}
              alt="Preview"
              style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
            />
          </div>
          <div style={{ padding: 'var(--space-3)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
              <CategoryIcon categoryKey={activeCat.key} />
              <span style={{ fontSize: '0.78rem', fontWeight: 700, color: activeCat.accent }}>
                {activeCat.label}
              </span>
            </div>
            <h3 style={{ margin: '0 0 6px' }}>{form.title.trim() || 'Your Delicious Recipe'}</h3>
            <p className="muted" style={{ fontSize: '0.84rem', margin: '0 0 10px' }}>
              {form.description.trim() || 'Your description will appear here.'}
            </p>
            <div className="muted" style={{ fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <CuteClockIcon size={14} /> {Number(form.prepTimeMinutes) + Number(form.cookTimeMinutes)} min
              </span>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <CuteUtensilsIcon size={14} /> {form.servings} servings
              </span>
            </div>
            {form.funNote.trim() && (
              <div
                style={{
                  marginTop: '10px',
                  padding: '8px 10px',
                  borderRadius: 10,
                  background: '#fff7ed',
                  fontSize: '0.8rem',
                  color: '#7c2d12',
                }}
              >
                Flip note: {form.funNote}
              </div>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
