import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav style={{ display: 'flex', gap: '1rem', padding: '1rem', borderBottom: '1px solid #ddd', alignItems: 'center' }}>
      <Link to="/recipes">Recipes</Link>
      <Link to="/recipes/new">Add recipe</Link>
      <span style={{ marginLeft: 'auto' }} />
      {isAuthenticated ? (
        <>
          <span>Bonjour, {user?.name}</span>
          <button type="button" onClick={handleLogout}>Se déconnecter</button>
        </>
      ) : (
        <>
          <Link to="/login">Login</Link>
          <Link to="/register">Register</Link>
        </>
      )}
    </nav>
  );
}