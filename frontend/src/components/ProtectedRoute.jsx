import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

// Utilisation dans App.jsx :
// <Route path="/recipes/new" element={<ProtectedRoute><AddRecipePage /></ProtectedRoute>} />
export default function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return children;
}
