import { useState, useRef, useEffect, Fragment } from 'react';
import { Link } from 'react-router-dom';
import http from '../api/http';
import RecipeImage from './RecipeImage';
import { CuteChefHatIcon, CuteSparkleIcon, CuteStarIcon } from '../data/CuteIcons';
import './MrMayoChat.css';

const QUICK_PROMPTS = [
  { label: 'Date Night Menu', text: 'Plan a romantic date night menu from the site' },
  { label: 'Budget Meals', text: 'What can I cook if I am on a tight budget?' },
  { label: 'Fridge Rescue', text: 'I have eggs and tomatoes, what can I cook?' },
  { label: 'Nut & Dairy Free', text: 'What can I cook with a nut and dairy allergy?' },
];

function renderInlineBold(line) {
  const parts = line.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={i}>{part.slice(2, -2)}</strong>;
    }
    return <Fragment key={i}>{part}</Fragment>;
  });
}

function renderMarkdown(text) {
  if (!text) return null;
  const lines = text.split('\n');
  const blocks = [];
  let listItems = [];

  const flushList = (keyPrefix) => {
    if (listItems.length > 0) {
      blocks.push(
        <ul key={`ul-${keyPrefix}`} style={{ margin: '4px 0', paddingLeft: '18px' }}>
          {listItems.map((item, idx) => (
            <li key={idx} style={{ marginBottom: '2px' }}>
              {renderInlineBold(item)}
            </li>
          ))}
        </ul>
      );
      listItems = [];
    }
  };

  lines.forEach((rawLine, idx) => {
    const trimmed = rawLine.trim();
    const bulletMatch = trimmed.match(/^(?:[-*•]|\d+\.)\s+(.*)$/);
    if (bulletMatch) {
      listItems.push(bulletMatch[1]);
    } else {
      flushList(idx);
      if (trimmed === '') {
        blocks.push(<div key={`br-${idx}`} style={{ height: 6 }} />);
      } else {
        blocks.push(
          <div key={`p-${idx}`} style={{ marginBottom: 2 }}>
            {renderInlineBold(rawLine)}
          </div>
        );
      }
    }
  });
  flushList('end');
  return blocks;
}

export default function MrMayoChat() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [aiPowered, setAiPowered] = useState(false);
  const [recipeContext, setRecipeContext] = useState(null);
  const [budgetTnd, setBudgetTnd] = useState(() => {
    try {
      return localStorage.getItem('mrmayo-budget-tnd') || '';
    } catch {
      return '';
    }
  });
  const [messages, setMessages] = useState([
    {
      role: 'bot',
      text: "Bonjour! I'm mr.mayo, your personal kitchen chef. Ask me for a date-night menu, allergy-safe dishes, budget recipes, or tell me what's in your fridge!",
      suggestions: [],
    },
  ]);
  const scrollRef = useRef(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, open, loading]);

  useEffect(() => {
    const onAskRecipe = (e) => {
      const detail = e.detail;
      if (!detail?.recipeId) return;
      setRecipeContext({ recipeId: detail.recipeId, recipeTitle: detail.recipeTitle });
      setOpen(true);
    };
    window.addEventListener('mrmayo:ask', onAskRecipe);
    return () => window.removeEventListener('mrmayo:ask', onAskRecipe);
  }, []);

  function handleBudgetChange(e) {
    const val = e.target.value;
    setBudgetTnd(val);
    try {
      localStorage.setItem('mrmayo-budget-tnd', val);
    } catch { /* ignore */ }
  }

  async function sendPrompt(text) {
    const trimmed = text.trim();
    if (!trimmed || loading) return;

    const historySnapshot = messages
      .slice(-6)
      .map((m) => ({ role: m.role, text: m.text }));

    setMessages((prev) => [...prev, { role: 'user', text: trimmed }]);
    setInput('');
    setLoading(true);

    const parsedBudget = budgetTnd.trim() ? Number(budgetTnd) : null;

    try {
      const { data } = await http.post('/chat', {
        message: trimmed,
        recipeId: recipeContext?.recipeId ?? null,
        budgetTnd: parsedBudget && !Number.isNaN(parsedBudget) ? parsedBudget : null,
        history: historySnapshot,
      });
      setAiPowered(Boolean(data.aiPowered));
      setMessages((prev) => [
        ...prev,
        {
          role: 'bot',
          text: data.reply,
          suggestions: data.suggestions || [],
        },
      ]);
    } catch (err) {
      setAiPowered(false);
      const errMsg =
        err.response?.status === 429
          ? 'Whoa, slow down chef! Please wait a minute before sending more messages.'
          : 'Oups! My kitchen walkie-talkie lost connection to the backend. Make sure the server is running and try again!';
      setMessages((prev) => [
        ...prev,
        {
          role: 'bot',
          text: errMsg,
          suggestions: [],
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e) {
    e.preventDefault();
    sendPrompt(input);
  }

  return (
    <div className="mayo-dock">
      {open && (
        <section className="mayo-panel" aria-label="Chat with mr.mayo">
          <header className="mayo-header">
            <div className="mayo-header-id">
              <span className="mayo-avatar-mini" aria-hidden="true">
                <CuteChefHatIcon size={20} />
              </span>
              <div>
                <strong>mr.mayo</strong>
                <div className="mayo-status">
                  {aiPowered ? 'AI Chef' : 'Chef Assistant · Kitchen & Recipes Only'}
                </div>
              </div>
            </div>
            <button
              type="button"
              className="mayo-close"
              onClick={() => setOpen(false)}
              aria-label="Close mr.mayo chat"
            >
              ×
            </button>
          </header>

          <div className="mayo-chips">
            {QUICK_PROMPTS.map((qp) => (
              <button
                key={qp.label}
                type="button"
                className="mayo-chip"
                onClick={() => sendPrompt(qp.text)}
                disabled={loading}
              >
                {qp.label}
              </button>
            ))}
          </div>

          <div className="mayo-context-bar">
            {recipeContext && (
              <span className="mayo-context-pill">
                <CuteSparkleIcon size={12} />
                <span className="mayo-context-title">About: {recipeContext.recipeTitle}</span>
                <button
                  type="button"
                  onClick={() => setRecipeContext(null)}
                  aria-label="Clear recipe context"
                >
                  ×
                </button>
              </span>
            )}
            <label className="mayo-budget-inline">
              <span>Budget:</span>
              <input
                type="number"
                min="1"
                step="1"
                placeholder="TND"
                value={budgetTnd}
                onChange={handleBudgetChange}
                aria-label="My budget in TND"
              />
              <span>TND</span>
            </label>
          </div>

          <div className="mayo-messages" ref={scrollRef}>
            {messages.map((m, idx) => (
              <div key={idx} className={`mayo-msg mayo-msg--${m.role}`}>
                <div className="mayo-bubble">
                  {m.role === 'bot' ? renderMarkdown(m.text) : m.text}
                </div>
                {m.suggestions && m.suggestions.length > 0 && (
                  <div className="mayo-suggestions">
                    {m.suggestions.map((s) => (
                      <Link
                        key={s.id}
                        to={`/recipes/${s.id}`}
                        className="mayo-recipe-pill"
                        onClick={() => setOpen(false)}
                      >
                        <RecipeImage
                          src={s.imageUrl}
                          alt={s.title}
                          category={s.category}
                          style={{ width: 42, height: 42, borderRadius: 8, objectFit: 'cover', flexShrink: 0, minHeight: 42 }}
                        />
                        <div>
                          <div className="mayo-recipe-title">{s.title}</div>
                          <div className="mayo-recipe-meta" style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                            <span>
                              {s.totalMinutes} min ·{' '}
                              {s.estimatedCostTnd != null ? `~${s.estimatedCostTnd} TND` : s.budgetLevel?.toLowerCase()}
                            </span>
                            {s.averageRating > 0 && (
                              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 2 }}>
                                <CuteStarIcon size={12} /> {s.averageRating}
                              </span>
                            )}
                          </div>
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ))}
            {loading && (
              <div className="mayo-msg mayo-msg--bot">
                <div className="mayo-bubble mayo-typing">mr.mayo is whisking ideas...</div>
              </div>
            )}
          </div>

          <form className="mayo-input-row" onSubmit={handleSubmit}>
            <input
              type="text"
              value={input}
              maxLength={500}
              onChange={(e) => setInput(e.target.value)}
              placeholder={
                recipeContext
                  ? `Ask about ${recipeContext.recipeTitle}...`
                  : 'Ask mr.mayo about recipes, allergies, budget...'
              }
              aria-label="Message mr.mayo"
            />
            <button type="submit" disabled={loading || !input.trim()}>
              Send
            </button>
          </form>
        </section>
      )}

      <button
        type="button"
        className={`mayo-fab ${open ? 'mayo-fab--open' : ''}`}
        onClick={() => setOpen((o) => !o)}
        aria-label="Open mr.mayo chef chatbot"
        title="Chat with mr.mayo"
      >
        <span className="mayo-fab-bubble">Ask mr.mayo!</span>
        <span className="mayo-chef-svg" aria-hidden="true">
          <svg viewBox="0 0 64 64" width="44" height="44">
            <path
              d="M17 26c-4-.4-7-3.6-7-7.5 0-4.1 3.3-7.4 7.4-7.4.8 0 1.6.1 2.3.4C21.3 8 26.2 5.5 32 5.5c5.8 0 10.7 2.5 12.3 6 .7-.3 1.5-.4 2.3-.4 4.1 0 7.4 3.3 7.4 7.4 0 3.9-3 7.1-7 7.5v5H17v-5z"
              fill="#ffffff"
              stroke="#7c2d12"
              strokeWidth="2.4"
            />
            <rect x="17" y="27" width="30" height="6" rx="2" fill="#fed7aa" stroke="#7c2d12" strokeWidth="2.2" />
            <circle cx="32" cy="43" r="14" fill="#fde68a" stroke="#7c2d12" strokeWidth="2.2" />
            <circle cx="27" cy="41" r="2" fill="#451a03" />
            <circle cx="37" cy="41" r="2" fill="#451a03" />
            <circle cx="22.5" cy="44.5" r="2.2" fill="#fca5a5" opacity="0.8" />
            <circle cx="41.5" cy="44.5" r="2.2" fill="#fca5a5" opacity="0.8" />
            <path
              d="M32 45.5c-2.5-2-6.5-1.5-8 .8 2 .8 4.8.6 8-.8 3.2 1.4 6 1.6 8 .8-1.5-2.3-5.5-2.8-8-.8z"
              fill="#ffffff"
              stroke="#7c2d12"
              strokeWidth="1.5"
            />
            <path d="M28.5 49.5c2 1.6 5 1.6 7 0" fill="none" stroke="#7c2d12" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
        </span>
      </button>
    </div>
  );
}
