import { Link } from 'react-router-dom';

export default function Navbar() {
  return (
    <nav style={{ display: 'flex', gap: '1rem', padding: '1rem', borderBottom: '1px solid #ddd' }}>
      <Link to="/recipes">Recipes</Link>
      <Link to="/recipes/new">Add recipe</Link>
      <span style={{ marginLeft: 'auto' }} />
      <Link to="/login">Login</Link>
      <Link to="/register">Register</Link>
    </nav>
  );
}