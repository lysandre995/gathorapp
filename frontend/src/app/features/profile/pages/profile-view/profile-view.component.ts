import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { UsersService } from '../../../../generated/api/users.service';
import { UserResponse } from '../../../../generated/model/userResponse';

@Component({
  selector: 'app-profile-view',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDividerModule,
    MatTabsModule,
  ],
  template: `
    <div class="profile-container">
      <mat-card class="profile-card">
        <mat-card-header>
          <div class="avatar">
            <mat-icon>account_circle</mat-icon>
          </div>
          <div class="header-content">
            <mat-card-title>{{ currentUser()?.name }}</mat-card-title>
            <mat-card-subtitle>
              <mat-chip class="role-chip">
                <mat-icon>{{ getRoleIcon(currentUser()?.role) }}</mat-icon>
                {{ currentUser()?.role }}
              </mat-chip>
            </mat-card-subtitle>
          </div>
        </mat-card-header>

        <mat-divider></mat-divider>

        <mat-tab-group class="profile-tabs">
          <!-- Profile Info Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>person</mat-icon>
              Profile Info
            </ng-template>
            <mat-card-content class="tab-content">
              @if (loading()) {
                <div class="loading">
                  <mat-spinner diameter="40"></mat-spinner>
                  <p>Loading profile...</p>
                </div>
              } @else if (userProfile()) {
                <div class="profile-info">
                  <div class="info-item">
                    <mat-icon>fingerprint</mat-icon>
                    <div>
                      <strong>User ID</strong>
                      <p class="id-text">{{ userProfile()?.id }}</p>
                    </div>
                  </div>

                  <div class="info-item">
                    <mat-icon>person</mat-icon>
                    <div>
                      <strong>Name</strong>
                      <p>{{ userProfile()?.name }}</p>
                    </div>
                  </div>

                  <div class="info-item">
                    <mat-icon>email</mat-icon>
                    <div>
                      <strong>Email</strong>
                      <p>{{ userProfile()?.email }}</p>
                    </div>
                  </div>

                  <div class="info-item">
                    <mat-icon>badge</mat-icon>
                    <div>
                      <strong>Role</strong>
                      <p>{{ userProfile()?.role }}</p>
                    </div>
                  </div>


                  @if (userProfile()?.createdAt) {
                    <div class="info-item">
                      <mat-icon>schedule</mat-icon>
                      <div>
                        <strong>Member Since</strong>
                        <p>{{ userProfile()?.createdAt | date:'long' }}</p>
                      </div>
                    </div>
                  }
                </div>
              }
            </mat-card-content>
          </mat-tab>

          <!-- Edit Profile Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>edit</mat-icon>
              Edit Profile
            </ng-template>
            <mat-card-content class="tab-content">
              <form [formGroup]="updateForm" (ngSubmit)="onUpdate()">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Name</mat-label>
                  <input matInput formControlName="name" placeholder="Your name" />
                  <mat-icon matPrefix>person</mat-icon>
                  @if (updateForm.get('name')?.hasError('required')) {
                    <mat-error>Name is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Email</mat-label>
                  <input matInput type="email" formControlName="email" placeholder="your.email@example.com" />
                  <mat-icon matPrefix>email</mat-icon>
                  @if (updateForm.get('email')?.hasError('required')) {
                    <mat-error>Email is required</mat-error>
                  }
                  @if (updateForm.get('email')?.hasError('email')) {
                    <mat-error>Invalid email format</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>New Password (leave empty to keep current)</mat-label>
                  <input matInput type="password" formControlName="password" placeholder="New password" />
                  <mat-icon matPrefix>lock</mat-icon>
                  @if (updateForm.get('password')?.hasError('minlength')) {
                    <mat-error>Password must be at least 6 characters</mat-error>
                  }
                </mat-form-field>

                <div class="form-actions">
                  <button mat-raised-button color="primary" type="submit" [disabled]="!updateForm.valid || saving()">
                    @if (saving()) {
                      <mat-spinner diameter="20"></mat-spinner>
                    } @else {
                      <mat-icon>save</mat-icon>
                    }
                    Save Changes
                  </button>
                  <button mat-button type="button" (click)="resetForm()">
                    <mat-icon>refresh</mat-icon>
                    Reset
                  </button>
                </div>
              </form>
            </mat-card-content>
          </mat-tab>

          <!-- Account Actions Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>settings</mat-icon>
              Settings
            </ng-template>
            <mat-card-content class="tab-content">
              <div class="settings-section">
                <h3>
                  <mat-icon>upgrade</mat-icon>
                  Account Management
                </h3>
                @if (currentUser()?.role === 'USER') {
                  <p>Upgrade your account to access premium features!</p>
                  <div class="upgrade-buttons">
                    <button mat-raised-button color="accent" (click)="changeRole('PREMIUM')" [disabled]="upgrading()">
                      @if (upgrading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>star</mat-icon>
                      }
                      Upgrade to Premium
                    </button>
                    <button mat-raised-button color="primary" (click)="changeRole('BUSINESS')" [disabled]="upgrading()">
                      @if (upgrading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>business</mat-icon>
                      }
                      Upgrade to Business
                    </button>
                  </div>
                } @else if (currentUser()?.role === 'PREMIUM') {
                  <p>You have a Premium account. You can upgrade to Business or downgrade to User.</p>
                  <div class="upgrade-buttons">
                    <button mat-raised-button color="primary" (click)="changeRole('BUSINESS')" [disabled]="upgrading()">
                      @if (upgrading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>business</mat-icon>
                      }
                      Upgrade to Business
                    </button>
                    <button mat-raised-button color="warn" (click)="changeRole('USER')" [disabled]="upgrading()">
                      @if (upgrading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>person</mat-icon>
                      }
                      Downgrade to User
                    </button>
                  </div>
                } @else if (currentUser()?.role === 'BUSINESS') {
                  <p>You have a Business account. You can downgrade to Premium or User.</p>
                  <div class="upgrade-buttons">
                    <button mat-raised-button color="accent" (click)="changeRole('PREMIUM')" [disabled]="upgrading()">
                      @if (upgrading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>star</mat-icon>
                      }
                      Downgrade to Premium
                    </button>
                    <button mat-raised-button color="warn" (click)="changeRole('USER')" [disabled]="upgrading()">
                      @if (upgrading()) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>person</mat-icon>
                      }
                      Downgrade to User
                    </button>
                  </div>
                } @else {
                  <p>Account role management is not available for {{ currentUser()?.role }} accounts.</p>
                }
              </div>

              <mat-divider></mat-divider>

              <div class="settings-section danger-zone">
                <h3>
                  <mat-icon>warning</mat-icon>
                  Danger Zone
                </h3>
                <p>These actions are irreversible. Please be careful.</p>
                <button mat-raised-button color="warn" (click)="onLogout()">
                  <mat-icon>logout</mat-icon>
                  Logout
                </button>
              </div>
            </mat-card-content>
          </mat-tab>
        </mat-tab-group>
      </mat-card>
    </div>
  `,
  styles: [`
    .profile-container {
      max-width: 900px;
      margin: 24px auto;
      padding: 24px;
    }

    .profile-card {
      mat-card-header {
        display: flex;
        align-items: center;
        gap: 24px;
        padding: 24px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        margin: -16px -16px 0 -16px;

        .avatar {
          mat-icon {
            font-size: 80px;
            width: 80px;
            height: 80px;
          }
        }

        .header-content {
          flex: 1;

          mat-card-title {
            font-size: 28px;
            font-weight: 600;
            color: white !important;
          }

          mat-card-subtitle {
            margin-top: 8px;
          }
        }

        .role-chip {
          background-color: rgba(255, 255, 255, 0.2);
          color: white;
          font-weight: 500;

          mat-icon {
            color: white;
          }
        }
      }
    }

    mat-divider {
      margin: 0;
    }

    .profile-tabs {
      margin-top: 0;

      ::ng-deep .mat-mdc-tab-labels {
        padding: 0 16px;
      }
    }

    .tab-content {
      padding: 32px 24px;
      min-height: 400px;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 64px;
      color: #666;
    }

    .profile-info {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .info-item {
      display: flex;
      gap: 16px;
      align-items: start;
      padding: 16px;
      background-color: #f5f5f5;
      border-radius: 8px;
      transition: background-color 0.2s;

      &:hover {
        background-color: #eeeeee;
      }

      &.warning {
        background-color: #ffebee;
        border-left: 4px solid #f44336;

        mat-icon {
          color: #f44336;
        }
      }

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #1976d2;
        flex-shrink: 0;
      }

      strong {
        display: block;
        font-size: 12px;
        text-transform: uppercase;
        color: #666;
        margin-bottom: 4px;
        letter-spacing: 0.5px;
      }

      p {
        margin: 0;
        font-size: 16px;
        color: #333;
        word-break: break-word;
      }

      .id-text {
        font-family: monospace;
        font-size: 14px;
        color: #666;
      }

      .ban-reason {
        margin-top: 8px;
        font-style: italic;
        color: #d32f2f;
      }
    }

    form {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .full-width {
        width: 100%;
      }

      .form-actions {
        display: flex;
        gap: 12px;
        margin-top: 16px;

        button {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }

    .settings-section {
      padding: 24px 0;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 16px 0;
        font-size: 20px;
        font-weight: 600;
        color: #333;
      }

      p {
        color: #666;
        margin-bottom: 16px;
        line-height: 1.6;
      }

      .upgrade-buttons {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;

        button {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }

      .premium-message {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px;
        background-color: #e8f5e9;
        border-radius: 8px;
        color: #2e7d32;
        font-weight: 500;

        mat-icon {
          color: #4caf50;
        }
      }

      &.danger-zone {
        border-top: 2px solid #ffebee;
        padding-top: 24px;

        h3 {
          color: #d32f2f;

          mat-icon {
            color: #d32f2f;
          }
        }

        p {
          color: #c62828;
        }
      }
    }

    @media (max-width: 768px) {
      .profile-container {
        padding: 16px;
      }

      .profile-card mat-card-header {
        flex-direction: column;
        text-align: center;

        .header-content {
          display: flex;
          flex-direction: column;
          align-items: center;
        }
      }

      .settings-section .upgrade-buttons {
        flex-direction: column;

        button {
          width: 100%;
        }
      }

      form .form-actions {
        flex-direction: column;

        button {
          width: 100%;
        }
      }
    }
  `],
})
export class ProfileViewComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private usersService = inject(UsersService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  currentUser = this.authService.currentUser;
  userProfile = signal<UserResponse | null>(null);
  loading = signal<boolean>(true);
  saving = signal<boolean>(false);
  upgrading = signal<boolean>(false);

  updateForm: FormGroup;

  constructor() {
    this.updateForm = this.fb.group({
      name: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.minLength(6)]],
    });
  }

  ngOnInit() {
    this.loadProfile();
  }

  loadProfile() {
    this.loading.set(true);
    this.usersService.getCurrentUser().subscribe({
      next: (profile) => {
        this.userProfile.set(profile);
        this.updateForm.patchValue({
          name: profile.name,
          email: profile.email,
        });
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading profile:', err);
        this.snackBar.open('Error loading profile', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  onUpdate() {
    if (!this.updateForm.valid) return;

    this.saving.set(true);
    const formValue = this.updateForm.value;

    // Don't send password if empty
    const updateData = {
      name: formValue.name,
      email: formValue.email,
      ...(formValue.password ? { password: formValue.password } : {}),
    };

    this.usersService.updateCurrentUser(updateData as any).subscribe({
      next: (updatedProfile) => {
        this.userProfile.set(updatedProfile);
        this.saving.set(false);
        this.snackBar.open('Profile updated successfully!', 'Close', { duration: 3000 });
        // Clear password field after successful update
        this.updateForm.patchValue({ password: '' });
      },
      error: (err) => {
        console.error('Error updating profile:', err);
        const message = err.error?.message || 'Error updating profile';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        this.saving.set(false);
      },
    });
  }

  resetForm() {
    this.updateForm.patchValue({
      name: this.userProfile()?.name,
      email: this.userProfile()?.email,
      password: '',
    });
  }

  changeRole(newRole: 'USER' | 'PREMIUM' | 'BUSINESS') {
    const currentRole = this.currentUser()?.role;

    // Prevent changing if already at target role
    if (currentRole === newRole) {
      this.snackBar.open(`You already have a ${newRole} account!`, 'Close', { duration: 3000 });
      return;
    }

    this.upgrading.set(true);

    // Using the upgrade/downgrade endpoint
    this.usersService.upgradeAccount(newRole).subscribe({
      next: (updatedUser) => {
        this.upgrading.set(false);
        this.userProfile.set(updatedUser);

        const action = this.isUpgrade(currentRole, newRole) ? 'upgraded' : 'downgraded';
        this.snackBar.open(
          `Successfully ${action} to ${newRole}! 🎉 Please log out and log back in to see all changes.`,
          'Close',
          { duration: 5000 }
        );

        // Reload profile after short delay
        setTimeout(() => {
          this.loadProfile();
        }, 1000);
      },
      error: (err) => {
        this.upgrading.set(false);
        console.error('Error changing account role:', err);
        const message = err.error?.message || 'Error changing account role. Please try again.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }

  private isUpgrade(from: string | undefined, to: string): boolean {
    const levels: { [key: string]: number } = {
      'USER': 1,
      'PREMIUM': 2,
      'BUSINESS': 3
    };
    const fromLevel = from ? levels[from] || 0 : 0;
    const toLevel = levels[to] || 0;
    return toLevel > fromLevel;
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  getRoleIcon(role: string | undefined): string {
    switch (role) {
      case 'BUSINESS':
        return 'business';
      case 'PREMIUM':
        return 'star';
      case 'ADMIN':
        return 'admin_panel_settings';
      default:
        return 'person';
    }
  }
}
