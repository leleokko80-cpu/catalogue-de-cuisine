export default function SkeletonCard({ horizontal = false }) {
  return (
    <div className={`skeleton-card ${horizontal ? 'skeleton-card--row' : ''}`} aria-hidden="true">
      <div className="skeleton-line skeleton-header" />
      <div className="skeleton-media" />
      <div className="skeleton-body">
        <div className="skeleton-line skeleton-title" />
        <div className="skeleton-line skeleton-meta" />
        <div className="skeleton-line skeleton-tags" />
        <div className="skeleton-line skeleton-footer" />
      </div>
    </div>
  );
}
