import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: string;
  };
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';

  private tokenSignal = signal<string | null>(localStorage.getItem('access_token'));
  private userSignal = signal<AuthResponse['user'] | null>(
    JSON.parse(localStorage.getItem('current_user') || 'null')
  );

  isAuthenticated = computed(() => !!this.tokenSignal());
  currentUser = computed(() => this.userSignal());

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest) {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/login`, credentials)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  register(data: RegisterRequest) {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/register`, data)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('current_user');
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  private handleAuthSuccess(response: AuthResponse) {
    localStorage.setItem('access_token', response.accessToken);
    localStorage.setItem('refresh_token', response.refreshToken);
    localStorage.setItem('current_user', JSON.stringify(response.user));

    this.tokenSignal.set(response.accessToken);
    this.userSignal.set(response.user);

    this.router.navigate(['/events']);
  }
}
