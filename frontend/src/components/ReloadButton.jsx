function ReloadButton({ onClick, loading = false, label = "Reload", className = "" }) {
  return (
    <button
      type="button"
      className={`btn btn-outline-secondary d-inline-flex align-items-center gap-2 ${className}`}
      onClick={onClick}
      disabled={loading}
      aria-busy={loading}
    >
      <span className={loading ? "reload-icon is-spinning" : "reload-icon"} aria-hidden="true">↻</span>
      <span>{loading ? "Refreshing..." : label}</span>
    </button>
  );
}

export default ReloadButton;
