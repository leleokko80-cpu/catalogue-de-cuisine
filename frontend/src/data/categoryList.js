export const CATEGORIES = [
  {
    key: 'ALL',
    label: 'Toutes',
    shortLabel: 'All',
    accent: '#c2410c',
    bg: '#fff3eb',
    thumb: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'ENTREES',
    label: 'Entrées',
    shortLabel: 'Entrées',
    accent: '#15803d',
    bg: '#ecfdf3',
    thumb: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'PLATS',
    label: 'Plats',
    shortLabel: 'Plats',
    accent: '#b45309',
    bg: '#fef3c7',
    thumb: 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'DESSERTS',
    label: 'Desserts',
    shortLabel: 'Desserts',
    accent: '#be185d',
    bg: '#fce7f3',
    thumb: 'https://images.unsplash.com/photo-1551024601-bec78aea704b?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'SWEETS',
    label: 'Sweets',
    shortLabel: 'Sweets',
    accent: '#9333ea',
    bg: '#f3e8ff',
    thumb: 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'BREAKFAST',
    label: 'Brunch',
    shortLabel: 'Brunch',
    accent: '#ca8a04',
    bg: '#fef9c3',
    thumb: 'https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'APERITIFS',
    label: 'Apéritifs',
    shortLabel: 'Apéros',
    accent: '#dc2626',
    bg: '#fee2e2',
    thumb: 'https://images.unsplash.com/photo-1541529086526-db283c563270?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'BOULANGE',
    label: 'Boulange',
    shortLabel: 'Boulange',
    accent: '#9a3412',
    bg: '#ffedd5',
    thumb: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=240&q=70',
  },
  {
    key: 'BOISSONS',
    label: 'Boissons',
    shortLabel: 'Boissons',
    accent: '#0284c7',
    bg: '#e0f2fe',
    thumb: 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=240&q=70',
  },
];

export function getCategoryMeta(key) {
  const upper = (key || 'PLATS').toUpperCase();
  return CATEGORIES.find((c) => c.key === upper) || CATEGORIES[2];
}
