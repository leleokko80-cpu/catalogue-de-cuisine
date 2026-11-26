import { getCategoryMeta } from './categoryList';

export function CategoryIcon({ categoryKey, size = 24 }) {
  const cat = getCategoryMeta(categoryKey);
  const s = size;

  switch (cat.key) {
    case 'ALL':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#fff3eb" stroke="#c2410c" strokeWidth="1.8" />
          <path d="M12 10v16M10 10v5a2 2 0 0 0 4 0v-5M22 10c2.2 0 3.5 1.8 3.5 4.2 0 1.8-1 3.3-2.5 3.7V26" fill="none" stroke="#c2410c" strokeWidth="1.9" strokeLinecap="round" />
        </svg>
      );
    case 'ENTREES':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#ecfdf3" stroke="#15803d" strokeWidth="1.8" />
          <path d="M9 20c0 4.5 4 7 9 7s9-2.5 9-7H9z" fill="#bbf7d0" stroke="#15803d" strokeWidth="1.7" />
          <path d="M13 18c0-3 2-5 5-5s5 2 5 5" fill="#86efac" stroke="#15803d" strokeWidth="1.6" />
          <circle cx="15" cy="16" r="1.4" fill="#ef4444" />
          <circle cx="20.5" cy="16.5" r="1.4" fill="#f97316" />
        </svg>
      );
    case 'DESSERTS':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#fce7f3" stroke="#be185d" strokeWidth="1.8" />
          <path d="M10 25h16l-1.5-8h-13L10 25z" fill="#fbcfe8" stroke="#be185d" strokeWidth="1.6" strokeLinejoin="round" />
          <path d="M11 17c1.5-2.5 4-3 7-3s5.5.5 7 3" fill="#f9a8d4" stroke="#be185d" strokeWidth="1.6" />
          <circle cx="18" cy="11.5" r="2.2" fill="#e11d48" />
        </svg>
      );
    case 'SWEETS':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#f3e8ff" stroke="#9333ea" strokeWidth="1.8" />
          <circle cx="18" cy="19" r="7" fill="#e9d5ff" stroke="#9333ea" strokeWidth="1.7" />
          <circle cx="18" cy="19" r="2.3" fill="#f3e8ff" stroke="#9333ea" strokeWidth="1.5" />
          <path d="M15 15l1 1M21 16l-1 1M16 22l1-1" stroke="#ec4899" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
      );
    case 'BREAKFAST':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#fef9c3" stroke="#ca8a04" strokeWidth="1.8" />
          <path d="M10 19c0 3.5 3.5 6 8 6s8-2.5 8-6c0-4-3.5-7-8-7s-8 3-8 7z" fill="#ffffff" stroke="#ca8a04" strokeWidth="1.6" />
          <circle cx="18" cy="19" r="3.4" fill="#facc15" />
        </svg>
      );
    case 'APERITIFS':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#fee2e2" stroke="#dc2626" strokeWidth="1.8" />
          <path d="M11 13h14l-7 8-7-8z" fill="#fecaca" stroke="#dc2626" strokeWidth="1.6" strokeLinejoin="round" />
          <path d="M18 21v6M14 27h8" stroke="#dc2626" strokeWidth="1.7" strokeLinecap="round" />
          <circle cx="20" cy="15.5" r="1.6" fill="#16a34a" />
        </svg>
      );
    case 'BOULANGE':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#ffedd5" stroke="#9a3412" strokeWidth="1.8" />
          <path d="M9 21c1-5 5-8 9-8s8 3 9 8c-2 2-16 2-18 0z" fill="#fed7aa" stroke="#9a3412" strokeWidth="1.6" />
          <path d="M14 15l2 3M19 14.5l1.5 3.5" stroke="#9a3412" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      );
    case 'BOISSONS':
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#e0f2fe" stroke="#0284c7" strokeWidth="1.8" />
          <path d="M13 13h10l-1.2 13h-7.6L13 13z" fill="#bae6fd" stroke="#0284c7" strokeWidth="1.6" strokeLinejoin="round" />
          <path d="M20 13l2.5-4" stroke="#0284c7" strokeWidth="1.7" strokeLinecap="round" />
        </svg>
      );
    default:
      return (
        <svg viewBox="0 0 36 36" width={s} height={s} aria-hidden="true" style={{ flexShrink: 0 }}>
          <circle cx="18" cy="18" r="16" fill="#fef3c7" stroke="#b45309" strokeWidth="1.8" />
          <ellipse cx="18" cy="22" rx="9" ry="4" fill="#fde68a" stroke="#b45309" strokeWidth="1.6" />
          <path d="M11 21c0-4 3-7 7-7s7 3 7 7" fill="#fed7aa" stroke="#b45309" strokeWidth="1.6" />
          <path d="M15 10c0-1.5 1-2 1-3M19 10c0-1.5 1-2 1-3" stroke="#d97706" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
      );
  }
}

export function CuteStarIcon({ filled = true, size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path
        d="M12 2.4l2.9 5.9 6.5.9-4.7 4.6 1.1 6.5L12 17.2l-5.8 3.1 1.1-6.5L2.6 9.2l6.5-.9L12 2.4z"
        fill={filled ? '#f59e0b' : '#e5e7eb'}
        stroke={filled ? '#d97706' : '#cbd5e1'}
        strokeWidth="1.3"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function CuteClockIcon({ size = 15 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <circle cx="12" cy="13" r="8.5" fill="#fff7ed" stroke="#ea580c" strokeWidth="1.8" />
      <path d="M12 8.5V13l3 2M9.5 3h5" fill="none" stroke="#ea580c" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

export function CuteChefHatIcon({ size = 18 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path
        d="M6 14.5C4.2 14.2 3 12.6 3 10.8 3 8.8 4.6 7.2 6.6 7.2c.6-2 2.5-3.4 4.7-3.4 1.8 0 3.4.9 4.2 2.3.5-.2 1-.3 1.5-.3 2.2 0 4 1.8 4 4 0 2-1.4 3.6-3.3 3.9V19a1.5 1.5 0 0 1-1.5 1.5H7.5A1.5 1.5 0 0 1 6 19v-4.5z"
        fill="#fff7ed"
        stroke="#c2410c"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
      <path d="M6 16.5h12" stroke="#c2410c" strokeWidth="1.5" />
    </svg>
  );
}

export function CuteHeartIcon({ filled = true, size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path
        d="M12 20.6l-1.35-1.23C5.8 14.95 2.5 11.96 2.5 8.35 2.5 5.38 4.82 3.1 7.75 3.1c1.66 0 3.25.78 4.25 2.02C13 3.88 14.59 3.1 16.25 3.1c2.93 0 5.25 2.28 5.25 5.25 0 3.61-3.3 6.6-8.15 11.03L12 20.6z"
        fill={filled ? '#e11d48' : 'none'}
        stroke="#e11d48"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function CuteTrophyIcon({ size = 18 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path d="M7 4h10v5a5 5 0 0 1-10 0V4z" fill="#fde68a" stroke="#b45309" strokeWidth="1.7" />
      <path d="M7 5.5H4.5a1.5 1.5 0 0 0-1.5 1.5v1a3 3 0 0 0 3 3H7M17 5.5h2.5A1.5 1.5 0 0 1 21 7v1a3 3 0 0 1-3 3h-1" fill="none" stroke="#b45309" strokeWidth="1.6" />
      <path d="M12 14v5M8.5 19.5h7" stroke="#b45309" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

export function CuteFlameIcon({ size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path
        d="M12.5 2.5c1.5 3.2 4.8 5.3 4.8 9.6A5.3 5.3 0 0 1 12 17.5a5.3 5.3 0 0 1-5.3-5.4c0-2.5 1.2-4.6 2.8-6 .2 1.5 1 2.4 2.1 2.8-.4-2.1.1-4.3.9-6.4z"
        fill="#fed7aa"
        stroke="#ea580c"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function CuteSparkleIcon({ size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path
        d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3z"
        fill="#fde68a"
        stroke="#d97706"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function CuteUtensilsIcon({ size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path d="M7 3v18M4.5 3v5a2.5 2.5 0 0 0 5 0V3M15.5 3c2.2 0 3.5 1.8 3.5 4.2 0 1.8-1 3.3-2.5 3.7V21" fill="none" stroke="#c2410c" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

export function CuteSearchIcon({ size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <circle cx="11" cy="11" r="6.8" fill="none" stroke="currentColor" strokeWidth="2" />
      <path d="M16 16l4.5 4.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function CutePrinterIcon({ size = 16 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path d="M7 8V3h10v5M6 17H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      <rect x="7" y="14" width="10" height="7" rx="1" fill="#fff7ed" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}

export function CuteMusicIcon({ muted = false, size = 18 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <path d="M9 18V6l10-2v12" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="6.5" cy="18" r="2.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
      <circle cx="16.5" cy="16" r="2.5" fill="none" stroke="currentColor" strokeWidth="1.9" />
      {muted && <path d="M3 3l18 18" stroke="#b42318" strokeWidth="2.1" strokeLinecap="round" />}
    </svg>
  );
}

export function CuteMenuIcon({ open = false, size = 20 }) {
  if (open) {
    return (
      <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
        <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
      <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function CuteChevronLeftIcon({ size = 18 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
      <path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function CuteChevronRightIcon({ size = 18 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
      <path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function CuteBudgetIcon({ size = 15 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" style={{ flexShrink: 0 }}>
      <circle cx="12" cy="12" r="9" fill="#ecfdf3" stroke="#15803d" strokeWidth="1.7" />
      <path d="M12 7v10M14.5 9.5c0-1-1.1-1.5-2.5-1.5s-2.5.6-2.5 1.8c0 2.2 5 1.4 5 3.8 0 1.2-1.1 1.9-2.5 1.9s-2.5-.6-2.5-1.6" fill="none" stroke="#15803d" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}
