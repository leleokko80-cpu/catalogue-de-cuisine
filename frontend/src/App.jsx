import { Routes, Route, Navigate, Link, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import MrMayoChat from './components/MrMayoChat';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import RecipeListPage from './pages/RecipeListPage';
import RecipeDetailPage from './pages/RecipeDetailPage';
import AddRecipePage from './pages/AddRecipePage';
import EditRecipePage from './pages/EditRecipePage';
import MyRecipesPage from './pages/MyRecipesPage';
import RankingPage from './pages/RankingPage';
import FavoritesPage from './pages/FavoritesPage';
import { CuteChefHatIcon } from './data/CuteIcons';

export default function App() {
  const location = useLocation();

  return (
    <div className="app-shell">
      <Navbar />
      <main key={location.pathname} className="container page-fade">
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/recipes" element={<RecipeListPage />} />
          <Route path="/ranking" element={<RankingPage />} />
          <Route
            path="/favorites"
            element={
              <ProtectedRoute>
                <FavoritesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/recipes/new"
            element={
              <ProtectedRoute>
                <AddRecipePage />
              </ProtectedRoute>
            }
          />
          <Route path="/recipes/:id" element={<RecipeDetailPage />} />
          <Route
            path="/recipes/:id/edit"
            element={
              <ProtectedRoute>
                <EditRecipePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-recipes"
            element={
              <ProtectedRoute>
                <MyRecipesPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <footer className="site-footer">
        <div className="site-footer-inner">
          <div>
            <Link to="/" className="navbar-brand" style={{ fontSize: '1.08rem' }}>
              <CuteChefHatIcon size={20} />
              Catalogue<span> de Cuisine</span>
            </Link>
            <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.84rem' }}>
              Cook with joy, share honest notes, and save your kitchen favorites.
            </p>
          </div>

          <div className="site-footer-links">
            <Link to="/">Dashboard</Link>
            <Link to="/recipes">Recettes</Link>
            <Link to="/ranking">Ranking</Link>
            <Link to="/favorites">Favorites</Link>
            <Link to="/recipes/new">Add Recipe</Link>
          </div>
        </div>
      </footer>

      <MrMayoChat />
    </div>
  );
}
