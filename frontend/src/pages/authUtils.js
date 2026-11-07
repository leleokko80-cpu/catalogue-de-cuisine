// Transforme une erreur axios en message lisible (français) pour l'utilisateur.
export function apiErrorMessage(err, fallback = "Une erreur est survenue. Réessayez.") {
  const status = err?.response?.status;
  if (!err?.response) return "Impossible de joindre le serveur. Vérifiez que le backend est lancé.";
  if (status === 401) return "Email ou mot de passe incorrect.";
  if (status === 409) return "Cet email est déjà utilisé.";
  if (status === 400) return "Informations invalides. Vérifiez les champs du formulaire.";
  return err.response.data?.message || fallback;
}

export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
