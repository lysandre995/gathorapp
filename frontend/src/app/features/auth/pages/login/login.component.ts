import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <h1>Login</h1>
        <p class="subtitle">Accedi a Gathorapp</p>

        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Email</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              placeholder="mario@example.com"
              [class.error]="loginForm.get('email')?.invalid && loginForm.get('email')?.touched"
            />
            @if (loginForm.get('email')?.invalid && loginForm.get('email')?.touched) {
            <span class="error-message">Email non valida</span>
            }
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              placeholder="••••••••"
              [class.error]="
                loginForm.get('password')?.invalid && loginForm.get('password')?.touched
              "
            />
            @if (loginForm.get('password')?.invalid && loginForm.get('password')?.touched) {
            <span class="error-message">Password obbligatoria</span>
            }
          </div>

          @if (errorMessage()) {
          <div class="alert alert-error">
            {{ errorMessage() }}
          </div>
          }

          <button type="submit" class="btn btn-primary" [disabled]="loginForm.invalid || loading()">
            @if (loading()) {
            <span>Accesso in corso...</span>
            } @else {
            <span>Accedi</span>
            }
          </button>
        </form>

        <div class="auth-footer">
          <p>Non hai un account? <a routerLink="/auth/register">Registrati</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .auth-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 2rem;
      }

      .auth-card {
        background: white;
        border-radius: 12px;
        padding: 3rem;
        width: 100%;
        max-width: 400px;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      }

      h1 {
        margin: 0 0 0.5rem 0;
        color: #2c3e50;
        font-size: 2rem;
      }

      .subtitle {
        margin: 0 0 2rem 0;
        color: #7f8c8d;
      }

      .form-group {
        margin-bottom: 1.5rem;
      }

      label {
        display: block;
        margin-bottom: 0.5rem;
        color: #34495e;
        font-weight: 500;
      }

      input {
        width: 100%;
        padding: 0.75rem;
        border: 2px solid #e0e0e0;
        border-radius: 6px;
        font-size: 1rem;
        transition: border-color 0.3s;
        box-sizing: border-box;
      }

      input:focus {
        outline: none;
        border-color: #667eea;
      }

      input.error {
        border-color: #e74c3c;
      }

      .error-message {
        display: block;
        color: #e74c3c;
        font-size: 0.875rem;
        margin-top: 0.25rem;
      }

      .alert {
        padding: 1rem;
        border-radius: 6px;
        margin-bottom: 1rem;
      }

      .alert-error {
        background: #fee;
        color: #c33;
        border: 1px solid #fcc;
      }

      .btn {
        width: 100%;
        padding: 0.75rem;
        border: none;
        border-radius: 6px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s;
      }

      .btn-primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }

      .btn-primary:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
      }

      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .auth-footer {
        margin-top: 2rem;
        text-align: center;
      }

      .auth-footer p {
        color: #7f8c8d;
        margin: 0;
      }

      .auth-footer a {
        color: #667eea;
        text-decoration: none;
        font-weight: 600;
      }

      .auth-footer a:hover {
        text-decoration: underline;
      }
    `,
  ],
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.loading.set(true);
      this.errorMessage.set(null);

      this.authService.login(this.loginForm.value).subscribe({
        next: () => {
          this.loading.set(false);
        },
        error: (error) => {
          this.loading.set(false);
          this.errorMessage.set(error.error?.message || 'Credenziali non valide. Riprova.');
        },
      });
    }
  }
}
