import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  CuteChefHatIcon,
  CuteHeartIcon,
  CuteMenuIcon,
  CuteMusicIcon,
  CuteTrophyIcon,
} from '../data/CuteIcons';
import { musicEngine } from '../utils/musicEngine';

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const [muted, setMuted] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);


  useEffect(() => {
    let cancelled = false;
    const tryStart = async () => {
      if (!cancelled) await musicEngine.start();
    };
    tryStart();

    const unlock = async () => {
      if (!musicEngine.isMuted) {
        const ok = await musicEngine.start();
        if (ok) {
          window.removeEventListener('pointerdown', unlock);
          window.removeEventListener('keydown', unlock);
        }
      }
    };
    window.addEventListener('pointerdown', unlock, { passive: true });
    window.addEventListener('keydown', unlock, { passive: true });

    return () => {
      cancelled = true;
      window.removeEventListener('pointerdown', unlock);
      window.removeEventListener('keydown', unlock);
    };
  }, []);

  useEffect(() => {
    const handleOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        setDropdownOpen(false);
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  function handleToggleMusic() {
    const nextMuted = musicEngine.toggleMute();
    setMuted(nextMuted);
  }

  function handleLogout() {
    setDropdownOpen(false);
    setMenuOpen(false);
    logout();
    navigate('/login');
  }

  const initial = (user?.name || '?').trim().charAt(0).toUpperCase();
  const musicTooltip = muted
    ? 'Unmute relaxing kitchen music'
    : 'Mute relaxing kitchen music';

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand" onClick={() => setMenuOpen(false)}>
          <CuteChefHatIcon size={24} />
          Catalogue<span> de Cuisine</span>
        </Link>

        <nav className="navbar-links" aria-label="Main navigation">
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/recipes" end>Recettes</NavLink>
          <NavLink to="/ranking">
            <CuteTrophyIcon size={15} />
            Ranking
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/favorites">
              <CuteHeartIcon filled size={14} />
              Favorites
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/my-recipes">My Recipes</NavLink>
          )}
          <NavLink to="/recipes/new">+ Add Recipe</NavLink>
        </nav>

        <div className="navbar-right">
          <button
            type="button"
            className={`music-icon-btn ${muted ? 'music-icon-btn--muted' : ''}`}
            onClick={handleToggleMusic}
            title={musicTooltip}
            aria-label={musicTooltip}
          >
            <CuteMusicIcon muted={muted} size={18} />
          </button>

          {isAuthenticated ? (
            <div className="user-menu-wrap" ref={dropdownRef}>
              <button
                type="button"
                className="user-menu-trigger"
                onClick={() => setDropdownOpen((o) => !o)}
                aria-expanded={dropdownOpen}
                aria-haspopup="menu"
              >
                <span className="navbar-avatar" aria-hidden="true">{initial}</span>
                <span className="navbar-user-name">{user?.name}</span>
              </button>

              {dropdownOpen && (
                <div className="user-dropdown" role="menu">
                  <div className="user-dropdown-header">
                    <strong>{user?.name}</strong>
                    <div className="muted" style={{ fontSize: '0.78rem' }}>{user?.email}</div>
                  </div>
                  <Link
                    to="/favorites"
                    className="user-dropdown-item"
                    role="menuitem"
                    onClick={() => setDropdownOpen(false)}
                  >
                    <CuteHeartIcon filled size={15} />
                    My Favorites
                  </Link>
                  <Link
                    to="/my-recipes"
                    className="user-dropdown-item"
                    role="menuitem"
                    onClick={() => setDropdownOpen(false)}
                  >
                    <CuteChefHatIcon size={15} />
                    My Recipes
                  </Link>
                  <Link
                    to="/recipes/new"
                    className="user-dropdown-item"
                    role="menuitem"
                    onClick={() => setDropdownOpen(false)}
                  >
                    <CuteChefHatIcon size={15} />
                    Add Recipe
                  </Link>
                  <button
                    type="button"
                    className="user-dropdown-item"
                    role="menuitem"
                    style={{ color: 'var(--danger)' }}
                    onClick={handleLogout}
                  >
                    Se déconnecter
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '6px' }}>
              <Link to="/login" className="btn btn-sm">Login</Link>
              <Link to="/register" className="btn btn-sm btn-primary">Register</Link>
            </div>
          )}

          <button
            type="button"
            className="hamburger-btn"
            onClick={() => setMenuOpen((o) => !o)}
            aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={menuOpen}
          >
            <CuteMenuIcon open={menuOpen} size={20} />
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav className="mobile-nav-drawer" aria-label="Mobile navigation">
          <NavLink to="/" end onClick={() => setMenuOpen(false)}>Dashboard</NavLink>
          <NavLink to="/recipes" end onClick={() => setMenuOpen(false)}>Recettes</NavLink>
          <NavLink to="/ranking" onClick={() => setMenuOpen(false)}>
            <CuteTrophyIcon size={15} /> Ranking
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/favorites" onClick={() => setMenuOpen(false)}>
              <CuteHeartIcon filled size={14} /> Favorites
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/my-recipes" onClick={() => setMenuOpen(false)}>
              <CuteChefHatIcon size={14} /> My Recipes
            </NavLink>
          )}
          <NavLink to="/recipes/new" onClick={() => setMenuOpen(false)}>+ Add Recipe</NavLink>
        </nav>
      )}

    </header>
  );
}
