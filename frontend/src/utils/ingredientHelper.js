const TYPO_MAP = {
  egf: 'Eggs',
  egy: 'Eggs',
  eg: 'Egg',
  egs: 'Eggs',
  egss: 'Eggs',
  egsg: 'Eggs',
  eegg: 'Egg',
  oeuf: 'Eggs',
  oeufs: 'Eggs',
  tomto: 'Tomato',
  tmato: 'Tomato',
  tomtao: 'Tomato',
  tomatoe: 'Tomato',
  tomatos: 'Tomatoes',
  tomate: 'Tomato',
  tomates: 'Tomatoes',
  chiken: 'Chicken',
  chikn: 'Chicken',
  chickn: 'Chicken',
  poulet: 'Chicken',
  courgette: 'Zucchini',
  courgettes: 'Zucchini',
  zuccini: 'Zucchini',
  aubergine: 'Eggplant',
  aubergines: 'Eggplant',
  'pois chiches': 'Chickpeas',
  'pois chiche': 'Chickpeas',
  chickpea: 'Chickpeas',
  thon: 'Tuna',
  agneau: 'Lamb',
  boeuf: 'Beef',
  crevette: 'Shrimp',
  crevettes: 'Shrimp',
  'pomme de terre': 'Potatoes',
  'pommes de terre': 'Potatoes',
  carotte: 'Carrots',
  carottes: 'Carrots',
  poivron: 'Bell pepper',
  poivrons: 'Bell pepper',
  persil: 'Parsley',
  coriandre: 'Coriander',
  menthe: 'Mint',
  amande: 'Almonds',
  amandes: 'Almonds',
  datte: 'Dates',
  dattes: 'Dates',
  semoule: 'Semolina',
  harisa: 'Harissa',
  bsisa: 'Bsissa',
  tabil: 'Tabil',
  merguez: 'Merguez',
  zgougou: 'Zgougou',
  flr: 'Flour',
  flor: 'Flour',
  flourr: 'Flour',
  farine: 'Flour',
  sugr: 'Sugar',
  suger: 'Sugar',
  sucre: 'Sugar',
  buter: 'Butter',
  buttr: 'Butter',
  beurre: 'Butter',
  mlk: 'Milk',
  mkl: 'Milk',
  lait: 'Milk',
  garlc: 'Garlic',
  garlik: 'Garlic',
  ail: 'Garlic',
  onin: 'Onion',
  onionn: 'Onion',
  oignon: 'Onion',
  chese: 'Cheese',
  cheeze: 'Cheese',
  fromage: 'Cheese',
  chocolte: 'Chocolate',
  choclate: 'Chocolate',
  chocolat: 'Chocolate',
  vanila: 'Vanilla',
  vanille: 'Vanilla',
  slat: 'Salt',
  saltt: 'Salt',
  sel: 'Salt',
  peper: 'Pepper',
  pepr: 'Pepper',
  poivre: 'Pepper',
  'oliv oil': 'Olive oil',
  'olive oill': 'Olive oil',
  spagetti: 'Spaghetti',
  spagheti: 'Spaghetti',
  psta: 'Pasta',
  ricee: 'Rice',
  riz: 'Rice',
  potto: 'Potato',
  potao: 'Potato',
  lemonn: 'Lemon',
  citron: 'Lemon',
  stwb: 'Strawberry',
  strawbery: 'Strawberry',
  fraise: 'Strawberry',
  bananna: 'Banana',
  banan: 'Banana',
  hony: 'Honey',
  miel: 'Honey',
  crem: 'Cream',
  creme: 'Cream',
  avocdo: 'Avocado',
  avocat: 'Avocado',
  salmn: 'Salmon',
  saumon: 'Salmon',
};

export const KNOWN_INGREDIENTS = [
  'Eggs', 'Egg', 'Tomato', 'Tomatoes', 'Chicken', 'Beef', 'Pork', 'Salmon', 'Tuna', 'Shrimp',
  'Flour', 'Sugar', 'Brown sugar', 'Powdered sugar', 'Butter', 'Milk', 'Heavy cream', 'Cream',
  'Yogurt', 'Cheese', 'Mozzarella', 'Parmesan', 'Pecorino', 'Cheddar', 'Feta', 'Ricotta',
  'Garlic', 'Onion', 'Red onion', 'Shallot', 'Ginger', 'Olive oil', 'Vegetable oil', 'Sesame oil',
  'Salt', 'Pepper', 'Black pepper', 'Paprika', 'Cumin', 'Harissa', 'Cinnamon', 'Nutmeg', 'Oregano',
  'Basil', 'Parsley', 'Cilantro', 'Thyme', 'Rosemary', 'Mint', 'Vanilla', 'Vanilla extract',
  'Chocolate', 'Dark chocolate', 'Cocoa powder', 'Honey', 'Maple syrup', 'Baking powder',
  'Baking soda', 'Yeast', 'Spaghetti', 'Pasta', 'Rice', 'Couscous', 'Quinoa', 'Bread',
  'Potato', 'Potatoes', 'Sweet potato', 'Carrot', 'Carrots', 'Bell pepper', 'Peppers',
  'Zucchini', 'Eggplant', 'Spinach', 'Mushroom', 'Mushrooms', 'Broccoli', 'Avocado',
  'Lemon', 'Lime', 'Orange', 'Apple', 'Banana', 'Strawberry', 'Strawberries', 'Blueberries',
  'Almonds', 'Walnuts', 'Hazelnuts', 'Pistachios', 'Peanuts', 'Coconut milk', 'Soy sauce',
  'Vinegar', 'Balsamic vinegar', 'Mustard', 'Ketchup', 'Mayonnaise', 'Guanciale', 'Bacon',
  'Chickpeas', 'Lentils', 'Black beans', 'Water', 'Coffee', 'Espresso', 'Matcha', 'Green tea',
];

function levenshtein(a, b) {
  const prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  const curr = new Array(b.length + 1).fill(0);
  for (let i = 1; i <= a.length; i++) {
    curr[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      curr[j] = Math.min(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    for (let j = 0; j <= b.length; j++) prev[j] = curr[j];
  }
  return prev[b.length];
}

export function normalizeIngredientClient(raw) {
  if (!raw || !raw.trim()) {
    return { normalized: '', valid: false, corrected: false, reason: 'Ingredient cannot be empty' };
  }
  const cleaned = raw.trim().replace(/\s+/g, ' ');
  if (cleaned.length < 2 || cleaned.length > 80) {
    return { normalized: cleaned, valid: false, corrected: false, reason: 'Use 2 to 80 characters' };
  }
  if (/^[0-9\s\p{P}]+$/u.test(cleaned) || !/^[\p{L}0-9\s'&.,()-]+$/u.test(cleaned)) {
    return { normalized: cleaned, valid: false, corrected: false, reason: 'Please enter a valid food ingredient' };
  }
  const lettersOnly = cleaned.toLowerCase().replace(/[^\p{L}]/gu, '');
  if (lettersOnly.length < 2 || /(.)\1\1\1/.test(lettersOnly)) {
    return { normalized: cleaned, valid: false, corrected: false, reason: 'That does not look like a real ingredient' };
  }

  const lower = cleaned.toLowerCase();
  if (TYPO_MAP[lower]) {
    const fixed = TYPO_MAP[lower];
    return { normalized: fixed, valid: true, corrected: fixed.toLowerCase() !== lower, reason: '' };
  }

  const exact = KNOWN_INGREDIENTS.find((k) => k.toLowerCase() === lower);
  if (exact) {
    return { normalized: exact, valid: true, corrected: exact !== cleaned, reason: '' };
  }

  let best = null;
  let bestDist = Infinity;
  for (const known of KNOWN_INGREDIENTS) {
    const maxAllowed = known.length <= 4 ? 1 : 2;
    const d = levenshtein(lower, known.toLowerCase());
    if (d <= maxAllowed && d < bestDist) {
      bestDist = d;
      best = known;
    }
  }
  if (best) {
    return { normalized: best, valid: true, corrected: true, reason: '' };
  }

  const cap = cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
  return { normalized: cap, valid: true, corrected: false, reason: '' };
}
