# `aws-deploy-changes` — pending monorepo sync

Commits on **Pavitra’s fork only**:

| Item | Value |
|------|--------|
| Remote | `obs` → `https://github.com/nedungadipavitra/online-bidding-system.git` |
| Branch | `aws-deploy-changes` |
| Monorepo | `origin` → `ayush4u-cpu/online-bidding-system-monorepo` — **do not push until asked** |

Use this file to re-apply the same changes on the monorepo later (cherry-pick the fork SHAs, or re-do the listed file edits).

---

## 2026-08-09 — Product image S3 upload wiring

**Status:** Pending monorepo sync

### Problem
Add Product never uploaded to S3. The UI base64-encoded the image into `imageUrl` on create. Backend already exposed `POST /products/{id}/image` (multipart → S3), but the form never called it. `apiFetch` also set `Content-Type: application/json` for any body, which breaks `FormData`.

### Files
- `frontend/src/sections/AddProductForm.jsx` — keep `File` + preview; create product, then multipart upload to `/products/{productId}/image`
- `frontend/src/api/client.js` — do not default JSON Content-Type when body is `FormData`
- `backend/product-service/src/main/java/com/onlinebidding/product_service/service/S3Service.java` — sanitize filename, normalize base path, clearer S3 errors

### Fork commit
| Date | SHA | Message | Monorepo |
|------|-----|---------|----------|
| 2026-08-09 | _pending_ | Upload product images via multipart S3 endpoint | Pending |

### Re-apply on monorepo later
1. Cherry-pick the SHA above from `obs/aws-deploy-changes` (or copy the three file diffs).
2. Confirm add-product sets `imageUrl` to an `https://…s3…amazonaws.com/…` URL.
3. Mark this entry **Synced** and record the monorepo SHA.

### Deploy (AWS / Jenkins)
Rebuild/redeploy **frontend** and **product-service** after this lands on `obs/aws-deploy-changes`.
