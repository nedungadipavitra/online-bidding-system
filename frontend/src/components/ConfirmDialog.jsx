import { useCallback, useState } from "react";
import { ConfirmContext } from "./confirmContext";

export function ConfirmProvider({ children }) {
  const [dialog, setDialog] = useState(null);

  const confirm = useCallback((options) => new Promise((resolve) => {
    setDialog({
      title: "Please confirm",
      message: "Are you sure you want to continue?",
      confirmLabel: "Confirm",
      cancelLabel: "Cancel",
      danger: false,
      ...options,
      resolve
    });
  }), []);

  const close = (result) => {
    dialog?.resolve(result);
    setDialog(null);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {dialog && (
        <div className="confirm-backdrop" role="presentation">
          <div
            className="modal d-block"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-dialog-title"
            onClick={(event) => {
              if (event.target === event.currentTarget) close(false);
            }}
          >
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content border-0 shadow">
                <div className="modal-header">
                  <h5 id="confirm-dialog-title" className="modal-title fw-bold">{dialog.title}</h5>
                  <button type="button" className="btn-close" aria-label="Close" onClick={() => close(false)} />
                </div>
                <div className="modal-body text-start">
                  <p className="mb-0 text-secondary">{dialog.message}</p>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-outline-secondary" onClick={() => close(false)}>
                    {dialog.cancelLabel}
                  </button>
                  <button
                    type="button"
                    className={`btn ${dialog.danger ? "btn-danger" : "btn-primary"}`}
                    onClick={() => close(true)}
                  >
                    {dialog.confirmLabel}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  );
}
