import { useState } from 'react';
import { CategoryIcon } from '../data/CuteIcons';

export default function RecipeImage({ src, alt, category = 'PLATS', className = '', style = {} }) {
  const [erroredSrc, setErroredSrc] = useState(null);
  const failed = !src || erroredSrc === src;

  if (failed) {
    return (
      <div
        className={`recipe-placeholder ${className}`}
        style={style}
        aria-label={alt || 'Recipe category placeholder'}
      >
        <CategoryIcon categoryKey={category} size={36} />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className={className}
      style={style}
      loading="lazy"
      onError={() => setErroredSrc(src)}
    />
  );
}
