import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import http from '../api/http';

const emptyForm = {
  title: '',
  description: '',
  imageUrl: '',
  prepTimeMinutes: 0,
  cookTimeMinutes: 0,
  servings: 1,
};

export default function AddRecipePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);
  const [ingredients, setIngredients] = useState([{ name: '', quantity: '' }]);
  const [steps, setSteps] = useState(['']);
  const [fieldErrors, setFieldErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const updateIngredient = (index, field, value) => {
    setIngredients(ingredients.map((ing, i) => (i === index ? { ...ing, [field]: value } : ing)));
  };
  const addIngredient = () => setIngredients([...ingredients, { name: '', quantity: '' }]);
  const removeIngredient = (index) => setIngredients(ingredients.filter((_, i) => i !== index));

  const updateStep = (index, value) => {
    setSteps(steps.map((s, i) => (i === index ? value : s)));
  };
  const addStep = () => setSteps([...steps, '']);
  const removeStep = (index) => setSteps(steps.filter((_, i) => i !== index));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setGlobalError('');
    setSubmitting(true);

    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      imageUrl: form.imageUrl.trim(),
      prepTimeMinutes: Number(form.prepTimeMinutes),
      cookTimeMinutes: Number(form.cookTimeMinutes),
      servings: Number(form.servings),
      ingredients: ingredients.filter((i) => i.name.trim() !== ''),
      steps: steps.filter((s) => s.trim() !== ''),
    };

    try {
      const res = await http.post('/recipes', payload);
      navigate(`/recipes/${res.data.id}`);
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;
      if (status === 400 && data?.errors) {
        setFieldErrors(data.errors);
        setGlobalError('Please fix the errors below.');
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
    fieldErrors[name] ? <div style={{ color: 'crimson', fontSize: '0.85rem' }}>{fieldErrors[name]}</div> : null;

  const box = { display: 'flex', flexDirection: 'column', gap: '0.25rem', marginBottom: '1rem' };

  return (
    <form onSubmit={handleSubmit} style={{ maxWidth: 600, margin: '0 auto' }}>
      <h2>Add a recipe</h2>

      {globalError && <div style={{ color: 'crimson', marginBottom: '1rem' }}>{globalError}</div>}

      <div style={box}>
        <label htmlFor="title">Title *</label>
        <input id="title" name="title" value={form.title} onChange={handleChange} />
        {errorText('title')}
      </div>

      <div style={box}>
        <label htmlFor="description">Description</label>
        <textarea id="description" name="description" rows={3} value={form.description} onChange={handleChange} />
        {errorText('description')}
      </div>

      <div style={box}>
        <label htmlFor="imageUrl">Image URL</label>
        <input id="imageUrl" name="imageUrl" value={form.imageUrl} onChange={handleChange} />
        {errorText('imageUrl')}
      </div>

      <div style={{ display: 'flex', gap: '1rem' }}>
        <div style={{ ...box, flex: 1 }}>
          <label htmlFor="prepTimeMinutes">Prep time (min)</label>
          <input id="prepTimeMinutes" name="prepTimeMinutes" type="number" min="0"
                 value={form.prepTimeMinutes} onChange={handleChange} />
          {errorText('prepTimeMinutes')}
        </div>
        <div style={{ ...box, flex: 1 }}>
          <label htmlFor="cookTimeMinutes">Cook time (min)</label>
          <input id="cookTimeMinutes" name="cookTimeMinutes" type="number" min="0"
                 value={form.cookTimeMinutes} onChange={handleChange} />
          {errorText('cookTimeMinutes')}
        </div>
        <div style={{ ...box, flex: 1 }}>
          <label htmlFor="servings">Servings</label>
          <input id="servings" name="servings" type="number" min="1"
                 value={form.servings} onChange={handleChange} />
          {errorText('servings')}
        </div>
      </div>

      <h3>Ingredients *</h3>
      {ingredients.map((ing, index) => (
        <div key={index} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <input placeholder="Name (e.g. Eggs)" value={ing.name}
                 onChange={(e) => updateIngredient(index, 'name', e.target.value)} style={{ flex: 2 }} />
          <input placeholder="Quantity (e.g. 4)" value={ing.quantity}
                 onChange={(e) => updateIngredient(index, 'quantity', e.target.value)} style={{ flex: 1 }} />
          <button type="button" onClick={() => removeIngredient(index)} disabled={ingredients.length === 1}>✕</button>
        </div>
      ))}
      {errorText('ingredients')}
      <button type="button" onClick={addIngredient}>+ Add ingredient</button>

      <h3>Steps *</h3>
      {steps.map((step, index) => (
        <div key={index} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <span>{index + 1}.</span>
          <textarea rows={2} placeholder="Describe this step" value={step}
                    onChange={(e) => updateStep(index, e.target.value)} style={{ flex: 1 }} />
          <button type="button" onClick={() => removeStep(index)} disabled={steps.length === 1}>✕</button>
        </div>
      ))}
      {errorText('steps')}
      <button type="button" onClick={addStep}>+ Add step</button>

      <div style={{ marginTop: '1.5rem' }}>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save recipe'}
        </button>
      </div>
    </form>
  );
}