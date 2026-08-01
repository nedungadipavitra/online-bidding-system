
function ActiveStatus() {
  return (
    <div
      style={{
        backgroundColor: "var(--green-light)",
        color: "var(--green-primary)",
        width: "max-content",
      }}
      className="rounded text-center fw-bold px-3 py-1"
    >
      ACTIVE
    </div>
  );
}

export default ActiveStatus;
