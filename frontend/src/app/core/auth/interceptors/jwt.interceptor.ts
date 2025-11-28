import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  // Try to get token from auth_tokens object first
  const authTokensStr = localStorage.getItem('auth_tokens');
  let token: string | null = null;

  if (authTokensStr) {
    try {
      const authTokens = JSON.parse(authTokensStr);
      token = authTokens.accessToken;
    } catch (e) {
      console.error('Error parsing auth_tokens:', e);
    }
  }

  // Fallback to old access_token key for backwards compatibility
  if (!token) {
    token = localStorage.getItem('access_token');
  }

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(req);
};
