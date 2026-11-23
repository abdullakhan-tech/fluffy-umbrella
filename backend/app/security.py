"""Firebase ID token and App Check verification for protected API routes."""
from dataclasses import dataclass
import logging
from typing import Any, Dict

import firebase_admin
from firebase_admin import app_check, auth, credentials
from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import settings
from app.services.rate_limit import rate_limiter

logger = logging.getLogger("zaythar_backend.security")
bearer_scheme = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class AuthenticatedUser:
    uid: str


def _admin_app():
    try:
        return firebase_admin.get_app()
    except ValueError:
        options: Dict[str, str] = {}
        if settings.firebase_project_id:
            options["projectId"] = settings.firebase_project_id
        # Uses Application Default Credentials. In production, prefer the
        # workload identity attached to the runtime instead of a key file.
        return firebase_admin.initialize_app(
            credentials.ApplicationDefault(),
            options=options,
        )


def verify_firebase_id_token(token: str) -> Dict[str, Any]:
    return auth.verify_id_token(token, app=_admin_app(), check_revoked=True)


def verify_app_check_token(token: str) -> Dict[str, Any]:
    return app_check.verify_token(token, app=_admin_app())


async def require_authenticated_user(
    request: Request,
    credentials_header: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> AuthenticatedUser:
    if credentials_header is None or credentials_header.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        claims = verify_firebase_id_token(credentials_header.credentials)
    except Exception as exc:
        logger.warning("Firebase ID token verification failed (%s)", type(exc).__name__)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        ) from None

    uid = claims.get("uid") or claims.get("sub")
    if not isinstance(uid, str) or not uid.strip():
        raise HTTPException(status_code=401, detail="Invalid authentication identity.")

    if settings.app_check_required:
        app_check_token = request.headers.get("X-Firebase-AppCheck", "").strip()
        if not app_check_token:
            raise HTTPException(status_code=403, detail="App Check attestation required.")
        try:
            app_check_claims = verify_app_check_token(app_check_token)
        except Exception as exc:
            logger.warning("Firebase App Check verification failed (%s)", type(exc).__name__)
            raise HTTPException(status_code=403, detail="App Check attestation rejected.") from None

        allowed_app_ids = settings.app_check_allowed_app_ids_list
        if allowed_app_ids:
            verified_app_id = app_check_claims.get("app_id") or app_check_claims.get("sub")
            if not isinstance(verified_app_id, str) or verified_app_id not in allowed_app_ids:
                logger.warning("Firebase App Check token belongs to an unapproved app")
                raise HTTPException(status_code=403, detail="App Check application is not allowed.")

    client_ip = request.client.host if request.client else "unknown"
    await rate_limiter.enforce(uid, client_ip)
    return AuthenticatedUser(uid=uid)
