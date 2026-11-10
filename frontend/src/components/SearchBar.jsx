export default function SearchBar({ value, onChange }) {
    return (
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <input
          type="search"
          placeholder="Search a recipe by name..."
          value={value}
          onChange={(e) => onChange(e.target.value)}
          style={{ flex: 1, padding: '0.5rem' }}
        />
        {value && (
          <button type="button" onClick={() => onChange('')}>
            Clear
          </button>
        )}
      </div>
    );
  }