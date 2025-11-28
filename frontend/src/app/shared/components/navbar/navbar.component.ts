import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/services/auth.service';

// Angular Material
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDivider } from '@angular/material/divider';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatDivider,
  ],
  template: `
    <mat-toolbar color="primary" class="navbar">
      <div class="navbar-content">
        <!-- Logo -->
        <a routerLink="/" class="logo">
          <mat-icon>event</mat-icon>
          <span>GathorApp</span>
        </a>

        <!-- Desktop Navigation -->
        @if (isAuthenticated()) {
        <nav class="desktop-nav">
          <a mat-button routerLink="/events" routerLinkActive="active">
            <mat-icon>calendar_today</mat-icon>
            <span>Eventi</span>
          </a>
          <a mat-button routerLink="/outings" routerLinkActive="active">
            <mat-icon>directions_walk</mat-icon>
            <span>Uscite</span>
          </a>
          <a mat-button routerLink="/map" routerLinkActive="active">
            <mat-icon>map</mat-icon>
            <span>Mappa</span>
          </a>
          <a mat-button routerLink="/vouchers" routerLinkActive="active">
            <mat-icon>card_giftcard</mat-icon>
            <span>Voucher</span>
          </a>
          <a mat-button routerLink="/notifications" routerLinkActive="active">
            <mat-icon
              [matBadge]="unreadCount()"
              [matBadgeHidden]="unreadCount() === 0"
              matBadgeColor="warn"
            >
              notifications
            </mat-icon>
            <span>Notifiche</span>
          </a>
          @if (isAdmin()) {
          <a mat-button routerLink="/admin" routerLinkActive="active">
            <mat-icon>admin_panel_settings</mat-icon>
            <span>Admin</span>
          </a>
          }
        </nav>
        }

        <span class="spacer"></span>

        <!-- Desktop User Menu -->
        @if (isAuthenticated()) {
        <div class="desktop-user">
          <button mat-button [matMenuTriggerFor]="menu">
            <mat-icon>account_circle</mat-icon>
            <span>{{ currentUser()?.name }}</span>
            <mat-icon>arrow_drop_down</mat-icon>
          </button>
          <mat-menu #menu="matMenu">
            <button mat-menu-item routerLink="/profile">
              <mat-icon>person</mat-icon>
              <span>Profilo</span>
            </button>
            <button mat-menu-item (click)="onLogout()">
              <mat-icon>logout</mat-icon>
              <span>Logout</span>
            </button>
          </mat-menu>
        </div>
        } @else {
        <div class="desktop-auth">
          <a mat-button routerLink="/auth/login">Login</a>
          <a mat-raised-button color="accent" routerLink="/auth/register">Registrati</a>
        </div>
        }

        <!-- Mobile Menu Button -->
        @if (isAuthenticated()) {
        <button mat-icon-button class="mobile-toggle" [matMenuTriggerFor]="mobileMenu">
          <mat-icon>menu</mat-icon>
        </button>
        <mat-menu #mobileMenu="matMenu" class="mobile-menu">
          <button mat-menu-item routerLink="/events">
            <mat-icon>calendar_today</mat-icon>
            <span>Eventi</span>
          </button>
          <button mat-menu-item routerLink="/outings">
            <mat-icon>directions_walk</mat-icon>
            <span>Uscite</span>
          </button>
          <button mat-menu-item routerLink="/map">
            <mat-icon>map</mat-icon>
            <span>Mappa</span>
          </button>
          <button mat-menu-item routerLink="/vouchers">
            <mat-icon>card_giftcard</mat-icon>
            <span>Voucher</span>
          </button>
          <button mat-menu-item routerLink="/notifications">
            <mat-icon
              [matBadge]="unreadCount()"
              [matBadgeHidden]="unreadCount() === 0"
              matBadgeColor="warn"
            >
              notifications
            </mat-icon>
            <span>Notifiche</span>
          </button>
          @if (isAdmin()) {
          <button mat-menu-item routerLink="/admin">
            <mat-icon>admin_panel_settings</mat-icon>
            <span>Admin</span>
          </button>
          }
          <button mat-menu-item routerLink="/profile">
            <mat-icon>person</mat-icon>
            <span>Profilo</span>
          </button>
          <mat-divider></mat-divider>
          <button mat-menu-item (click)="onLogout()">
            <mat-icon color="warn">logout</mat-icon>
            <span>Logout</span>
          </button>
        </mat-menu>
        }
      </div>
    </mat-toolbar>
  `,
  styles: [
    `
      .navbar {
        position: sticky;
        top: 0;
        z-index: 1000;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .navbar-content {
        display: flex;
        align-items: center;
        width: 100%;
        max-width: 1400px;
        margin: 0 auto;
        padding: 0 16px;
      }

      .logo {
        display: flex;
        align-items: center;
        gap: 8px;
        color: white;
        text-decoration: none;
        font-size: 20px;
        font-weight: 600;
        margin-right: 24px;

        mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
        }

        span {
          @media (max-width: 600px) {
            display: none;
          }
        }
      }

      .desktop-nav {
        display: flex;
        gap: 4px;

        @media (max-width: 960px) {
          display: none;
        }

        a {
          display: flex;
          align-items: center;
          gap: 4px;

          &.active {
            background-color: rgba(255, 255, 255, 0.15);
          }

          mat-icon {
            font-size: 20px;
            width: 20px;
            height: 20px;
          }
        }
      }

      .spacer {
        flex: 1;
      }

      .desktop-user {
        @media (max-width: 960px) {
          display: none;
        }

        button {
          display: flex;
          align-items: center;
          gap: 8px;

          span {
            max-width: 150px;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }

      .desktop-auth {
        display: flex;
        gap: 12px;

        @media (max-width: 960px) {
          display: none;
        }
      }

      .mobile-toggle {
        display: none;

        @media (max-width: 960px) {
          display: inline-flex;
        }
      }
    `,
  ],
})
export class NavbarComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  isAuthenticated = this.authService.isAuthenticated;
  currentUser = this.authService.currentUser;

  unreadCount = computed(() => 0); // TODO: implementare con notifiche reali

  isAdmin = computed(() => {
    const user = this.currentUser();
    return user?.role === 'ADMIN';
  });

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
