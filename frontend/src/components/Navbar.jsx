import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const initial = (user?.name || '?').trim().charAt(0).toUpperCase();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/recipes" className="navbar-brand">
          Catalogue<span> de Cuisine</span>
        </Link>

        <nav className="navbar-links">
          <NavLink to="/recipes" end>Recipes</NavLink>
          <NavLink to="/recipes/new">Add recipe</NavLink>
        </nav>

        <div className="navbar-user">
          {isAuthenticated ? (
            <>
              <span className="navbar-avatar" aria-hidden="true">{initial}</span>
              <span className="navbar-name">{user?.name}</span>
              <button type="button" className="btn btn-sm" onClick={handleLogout}>
                Se déconnecter
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-sm">Login</Link>
              <Link to="/register" className="btn btn-sm btn-primary">Register</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}