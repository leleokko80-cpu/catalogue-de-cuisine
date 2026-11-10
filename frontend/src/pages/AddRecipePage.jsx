import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import http from '../api/http';
import Alert from '../components/Alert';

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
    fieldErrors[name] ? <div className="field-error">{fieldErrors[name]}</div> : null;

  return (
    <form onSubmit={handleSubmit} className="form-card">
      <h2>Add a recipe</h2>

      {globalError && (
        <Alert type="error" onClose={() => setGlobalError('')}>
          {globalError}
        </Alert>
      )}

      <div className="field">
        <label htmlFor="title">Title *</label>
        <input id="title" name="title" value={form.title} onChange={handleChange} />
        {errorText('title')}
      </div>

      <div className="field">
        <label htmlFor="description">Description</label>
        <textarea id="description" name="description" rows={3} value={form.description} onChange={handleChange} />
        {errorText('description')}
      </div>

      <div className="field">
        <label htmlFor="imageUrl">Image URL</label>
        <input id="imageUrl" name="imageUrl" value={form.imageUrl} onChange={handleChange} />
        {errorText('imageUrl')}
      </div>

      <div className="field-row">
        <div className="field">
          <label htmlFor="prepTimeMinutes">Prep time (min)</label>
          <input id="prepTimeMinutes" name="prepTimeMinutes" type="number" min="0"
                 value={form.prepTimeMinutes} onChange={handleChange} />
          {errorText('prepTimeMinutes')}
        </div>
        <div className="field">
          <label htmlFor="cookTimeMinutes">Cook time (min)</label>
          <input id="cookTimeMinutes" name="cookTimeMinutes" type="number" min="0"
                 value={form.cookTimeMinutes} onChange={handleChange} />
          {errorText('cookTimeMinutes')}
        </div>
        <div className="field">
          <label htmlFor="servings">Servings</label>
          <input id="servings" name="servings" type="number" min="1"
                 value={form.servings} onChange={handleChange} />
          {errorText('servings')}
        </div>
      </div>

      <h3>Ingredients *</h3>
      {ingredients.map((ing, index) => (
        <div key={index} className="list-row">
          <input placeholder="Name (e.g. Eggs)" value={ing.name}
                 onChange={(e) => updateIngredient(index, 'name', e.target.value)} style={{ flex: 2 }} />
          <input placeholder="Quantity (e.g. 4)" value={ing.quantity}
                 onChange={(e) => updateIngredient(index, 'quantity', e.target.value)} style={{ flex: 1 }} />
          <button type="button" className="btn btn-sm" onClick={() => removeIngredient(index)}
                  disabled={ingredients.length === 1}>
            Remove
          </button>
        </div>
      ))}
      {errorText('ingredients')}
      <button type="button" className="btn btn-sm" onClick={addIngredient}>Add ingredient</button>

      <h3>Steps *</h3>
      {steps.map((step, index) => (
        <div key={index} className="list-row">
          <span className="step-number">{index + 1}.</span>
          <textarea rows={2} placeholder="Describe this step" value={step}
                    onChange={(e) => updateStep(index, e.target.value)} style={{ flex: 1 }} />
          <button type="button" className="btn btn-sm" onClick={() => removeStep(index)}
                  disabled={steps.length === 1}>
            Remove
          </button>
        </div>
      ))}
      {errorText('steps')}
      <button type="button" className="btn btn-sm" onClick={addStep}>Add step</button>

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save recipe'}
        </button>
      </div>
    </form>
  );
}