function LoadingState({ message = "Loading...", compact = false }) {
  return (
    <div className={`d-flex flex-column align-items-center justify-content-center text-muted ${compact ? "py-3" : "py-5"}`}>
      <span className="spinner-border text-primary mb-3" role="status" aria-label={message} />
      <span>{message}</span>
    </div>
  );
}

export default LoadingState;
