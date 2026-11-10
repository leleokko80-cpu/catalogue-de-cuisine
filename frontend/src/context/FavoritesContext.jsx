import { createContext, useContext, useEffect, useState } from "react";
import http from "../api/http";
import { useAuth } from "./AuthContext";

const FavoritesContext = createContext(null);

export function FavoritesProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [ids, setIds] = useState([]);

  // Charge les favoris à la connexion ; vide la liste à la déconnexion (cleanup).
  useEffect(() => {
    if (!isAuthenticated) return undefined;
    let cancelled = false;
    http
      .get("/favorites/ids")
      .then(({ data }) => {
        if (!cancelled) setIds(data);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
      setIds([]);
    };
  }, [isAuthenticated]);

  // useParams() donne des chaînes, l'API des nombres : on normalise avec Number()
  function isFavorite(recipeId) {
    return isAuthenticated && ids.includes(Number(recipeId));
  }

  // Mise à jour optimiste ; en cas d'erreur on annule et on relance l'erreur.
  async function toggle(recipeId) {
    const id = Number(recipeId);
    const wasFavorite = ids.includes(id);

    setIds((prev) => {
      const without = prev.filter((x) => x !== id);
      return wasFavorite ? without : [...without, id];
    });

    try {
      if (wasFavorite) {
        await http.delete(`/recipes/${id}/favorite`);
      } else {
        await http.post(`/recipes/${id}/favorite`);
      }
    } catch (err) {
      setIds((prev) => {
        const without = prev.filter((x) => x !== id);
        return wasFavorite ? [...without, id] : without;
      });
      throw err;
    }
  }

  return (
    <FavoritesContext.Provider value={{ isFavorite, toggle }}>
      {children}
    </FavoritesContext.Provider>
  );
}

export function useFavorites() {
  const ctx = useContext(FavoritesContext);
  if (!ctx) {
    throw new Error("useFavorites doit être utilisé dans <FavoritesProvider>");
  }
  return ctx;
}
