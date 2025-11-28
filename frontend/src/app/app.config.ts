import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/auth/interceptors/jwt.interceptor';
import { Configuration } from './generated/configuration';

// Custom Configuration that treats */* as JSON
class CustomConfiguration extends Configuration {
  override isJsonMime(mime: string): boolean {
    // Treat */* as JSON to fix OpenAPI generator blob issue
    if (mime === '*/*') {
      return true;
    }
    return super.isJsonMime(mime);
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    { provide: Configuration, useValue: new CustomConfiguration() },
  ],
};
