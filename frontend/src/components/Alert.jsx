export default function Alert({ type = 'info', children, onClose }) {
    return (
      <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>
        <span>{children}</span>
        {onClose && (
          <button type="button" className="alert-close" onClick={onClose} aria-label="Dismiss">
            &times;
          </button>
        )}
      </div>
    );
  }