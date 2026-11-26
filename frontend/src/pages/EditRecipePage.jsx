import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import http from '../api/http';
import { useToast } from '../context/ToastContext';
import Alert from '../components/Alert';
import { CATEGORIES, getCategoryMeta } from '../data/categoryList';
import { CategoryIcon } from '../data/CuteIcons';
import { KNOWN_INGREDIENTS, normalizeIngredientClient } from '../utils/ingredientHelper';

const BUDGET_LEVELS = ['BUDGET', 'MODERATE', 'GOURMET'];

export default function EditRecipePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [form, setForm] = useState(null);
  const [ingredients, setIngredients] = useState([]);
  const [steps, setSteps] = useState([]);
  const [fieldErrors, setFieldErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    http.get(`/recipes/${id}`)
      .then((res) => {
        const r = res.data;
        setForm({
          title: r.title || '',
          description: r.description || '',
          imageUrl: r.imageUrl || '',
          prepTimeMinutes: r.prepTimeMinutes ?? 10,
          cookTimeMinutes: r.cookTimeMinutes ?? 15,
          servings: r.servings ?? 2,
          category: r.category || 'PLATS',
          budgetLevel: r.budgetLevel || 'MODERATE',
          estimatedCostTnd: r.estimatedCostTnd ?? '',
          funNote: r.funNote || '',
          allergens: r.allergens || '',
        });
        setIngredients(
          (r.ingredients || []).map((i) => ({ name: i.name, quantity: i.quantity || '', fixedFrom: '', error: '' }))
        );
        setSteps(r.steps || ['']);
      })
      .catch(() => setGlobalError('Could not load recipe. Please try again.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <div className="form-card" style={{ marginTop: 'var(--space-5)', textAlign: 'center' }}>Loading recipe…</div>;
  }

  if (!form) {
    return <Alert type="error">{globalError || 'Recipe not found.'}</Alert>;
  }

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

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
        if (!res.valid) return { ...ing, error: res.reason };
        return { ...ing, name: res.normalized, fixedFrom: res.corrected ? ing.name.trim() : '', error: '' };
      })
    );
  };

  const addIngredient = () => setIngredients([...ingredients, { name: '', quantity: '', fixedFrom: '', error: '' }]);
  const removeIngredient = (index) => setIngredients(ingredients.filter((_, i) => i !== index));
  const updateStep = (index, value) => setSteps(steps.map((s, i) => (i === index ? value : s)));
  const addStep = () => setSteps([...steps, '']);
  const removeStep = (index) => setSteps(steps.filter((_, i) => i !== index));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setGlobalError('');

    const validated = [];
    let hasInvalid = false;
    const nextIngs = ingredients.map((ing) => {
      if (!ing.name.trim()) return ing;
      const res = normalizeIngredientClient(ing.name);
      if (!res.valid) { hasInvalid = true; return { ...ing, error: res.reason }; }
      validated.push({ name: res.normalized, quantity: ing.quantity.trim() });
      return { ...ing, name: res.normalized, fixedFrom: res.corrected ? ing.name.trim() : ing.fixedFrom, error: '' };
    });
    setIngredients(nextIngs);
    if (hasInvalid) { setGlobalError('Please fix the highlighted invalid ingredient(s).'); return; }
    if (validated.length === 0) { setGlobalError('Add at least one ingredient.'); return; }
    const cleanSteps = steps.filter((s) => s.trim() !== '');
    if (cleanSteps.length === 0) { setGlobalError('Add at least one step.'); return; }

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
      steps: cleanSteps,
    };

    try {
      await http.put(`/recipes/${id}`, payload);
      showToast('Recipe updated successfully!', 'success');
      navigate(`/recipes/${id}`);
    } catch (err) {
      const status = err.response?.status;
      if (status === 403) setGlobalError('You can only edit your own recipes.');
      else if (status === 400) setGlobalError(err.response?.data?.message || 'Invalid data. Check the form.');
      else setGlobalError('Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const cat = getCategoryMeta(form.category);
  void KNOWN_INGREDIENTS;

  return (
    <div className="form-card" style={{ maxWidth: 760 }}>
      <div style={{ marginBottom: 'var(--space-4)' }}>
        <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
          ✏️ Edit Recipe
        </h2>
        <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.9rem' }}>
          Update your recipe — all changes go through the same validation.
        </p>
      </div>

      {globalError && <Alert type="error" style={{ marginBottom: 'var(--space-3)' }}>{globalError}</Alert>}

      <form onSubmit={handleSubmit} noValidate>
        {/* Title */}
        <div className="field">
          <label htmlFor="title">Recipe Title *</label>
          <input
            id="title"
            name="title"
            type="text"
            value={form.title}
            onChange={handleChange}
            required
            maxLength={150}
            placeholder="e.g. Couscous au poulet"
          />
          {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
        </div>

        {/* Description */}
        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            name="description"
            rows={3}
            value={form.description}
            onChange={handleChange}
            maxLength={2000}
            placeholder="A short, appetising description…"
          />
        </div>

        {/* Image URL + preview */}
        <div className="field">
          <label htmlFor="imageUrl">Photo URL (Unsplash recommended)</label>
          <input
            id="imageUrl"
            name="imageUrl"
            type="url"
            value={form.imageUrl}
            onChange={handleChange}
            placeholder="https://images.unsplash.com/…"
          />
          {form.imageUrl && (
            <img
              src={form.imageUrl}
              alt="Preview"
              style={{ marginTop: 8, width: '100%', maxHeight: 180, objectFit: 'cover', borderRadius: 'var(--radius)' }}
              onError={(e) => { e.currentTarget.style.display = 'none'; }}
            />
          )}
        </div>

        {/* Times + Servings */}
        <div className="field-row">
          <div className="field">
            <label htmlFor="prepTimeMinutes">Prep Time (min)</label>
            <input id="prepTimeMinutes" name="prepTimeMinutes" type="number" min="0" value={form.prepTimeMinutes} onChange={handleChange} />
          </div>
          <div className="field">
            <label htmlFor="cookTimeMinutes">Cook Time (min)</label>
            <input id="cookTimeMinutes" name="cookTimeMinutes" type="number" min="0" value={form.cookTimeMinutes} onChange={handleChange} />
          </div>
          <div className="field">
            <label htmlFor="servings">Servings</label>
            <input id="servings" name="servings" type="number" min="1" value={form.servings} onChange={handleChange} />
          </div>
        </div>

        {/* Category */}
        <div className="field">
          <label>Category</label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {CATEGORIES.filter((c) => c.key !== 'ALL').map((c) => (
              <button
                key={c.key}
                type="button"
                onClick={() => setForm({ ...form, category: c.key })}
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: 5,
                  padding: '6px 12px', borderRadius: 999,
                  border: `1.5px solid ${form.category === c.key ? c.accent : 'var(--border)'}`,
                  background: form.category === c.key ? c.bg : 'var(--surface)',
                  color: form.category === c.key ? c.accent : 'var(--muted)',
                  fontWeight: 600, fontSize: '0.84rem', cursor: 'pointer',
                }}
              >
                <CategoryIcon categoryKey={c.key} size={18} />
                {c.label}
              </button>
            ))}
          </div>
          <p className="muted" style={{ fontSize: '0.8rem', margin: '4px 0 0' }}>Selected: <strong>{cat.label}</strong></p>
        </div>

        {/* Budget */}
        <div className="field-row">
          <div className="field">
            <label>Budget Level</label>
            <div style={{ display: 'flex', gap: 8 }}>
              {BUDGET_LEVELS.map((b) => (
                <button
                  key={b}
                  type="button"
                  onClick={() => setForm({ ...form, budgetLevel: b })}
                  className={`btn btn-sm ${form.budgetLevel === b ? 'btn-primary' : ''}`}
                >
                  {b === 'BUDGET' ? '💰 Budget' : b === 'MODERATE' ? '💳 Moderate' : '✨ Gourmet'}
                </button>
              ))}
            </div>
          </div>
          <div className="field">
            <label htmlFor="estimatedCostTnd">Est. Cost (TND)</label>
            <input
              id="estimatedCostTnd"
              name="estimatedCostTnd"
              type="number"
              min="0"
              step="0.5"
              value={form.estimatedCostTnd}
              onChange={handleChange}
              placeholder="e.g. 12"
            />
          </div>
        </div>

        {/* Allergens */}
        <div className="field">
          <label htmlFor="allergens">Allergens (comma-separated)</label>
          <input id="allergens" name="allergens" type="text" value={form.allergens} onChange={handleChange} placeholder="e.g. gluten, dairy, nuts" />
        </div>

        {/* Fun Note */}
        <div className="field">
          <label htmlFor="funNote">Chef&apos;s Fun Note (shown on card back)</label>
          <input id="funNote" name="funNote" type="text" value={form.funNote} onChange={handleChange} maxLength={500} placeholder="A fun tip or personal story…" />
        </div>

        {/* Ingredients */}
        <div className="field">
          <label>Ingredients *</label>
          {ingredients.map((ing, i) => (
            <div key={i} className="list-row" style={{ alignItems: 'flex-start' }}>
              <div style={{ flex: 2, minWidth: 0 }}>
                <input
                  value={ing.name}
                  onChange={(e) => updateIngredient(i, 'name', e.target.value)}
                  onBlur={() => handleIngredientBlur(i)}
                  placeholder="Ingredient name"
                  style={{ borderColor: ing.error ? 'var(--danger)' : undefined }}
                />
                {ing.fixedFrom && <p style={{ margin: '2px 0 0', fontSize: '0.78rem', color: 'var(--success)' }}>✔ Auto-corrected from &quot;{ing.fixedFrom}&quot;</p>}
                {ing.error && <p className="field-error" style={{ margin: '2px 0 0' }}>{ing.error}</p>}
              </div>
              <div style={{ flex: 1, minWidth: 80 }}>
                <input value={ing.quantity} onChange={(e) => updateIngredient(i, 'quantity', e.target.value)} placeholder="Qty" />
              </div>
              {ingredients.length > 1 && (
                <button type="button" className="btn btn-sm btn-danger" onClick={() => removeIngredient(i)} style={{ flexShrink: 0 }}>✕</button>
              )}
            </div>
          ))}
          <button type="button" className="btn btn-sm" onClick={addIngredient} style={{ marginTop: 4 }}>+ Add Ingredient</button>
        </div>

        {/* Steps */}
        <div className="field">
          <label>Steps *</label>
          {steps.map((step, i) => (
            <div key={i} className="list-row">
              <span className="step-number">{i + 1}.</span>
              <textarea
                rows={2}
                value={step}
                onChange={(e) => updateStep(i, e.target.value)}
                placeholder={`Step ${i + 1}…`}
                style={{ flex: 1 }}
              />
              {steps.length > 1 && (
                <button type="button" className="btn btn-sm btn-danger" onClick={() => removeStep(i)}>✕</button>
              )}
            </div>
          ))}
          <button type="button" className="btn btn-sm" onClick={addStep} style={{ marginTop: 4 }}>+ Add Step</button>
        </div>

        <div className="form-actions" style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap' }}>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : '✔ Save Changes'}
          </button>
          <button type="button" className="btn" onClick={() => navigate(-1)} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
