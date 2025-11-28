import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

import { AdminService } from '../../services/admin.service';
import { UserResponse } from '../../../../generated/model/userResponse';

/**
 * Admin dashboard component
 * Only accessible by ADMIN role
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTabsModule,
    MatTooltipModule,
    MatMenuModule,
  ],
  template: `
    <div class="admin-container">
      <header class="page-header">
        <div class="header-content">
          <div>
            <h1>Admin Dashboard</h1>
            <p class="subtitle">Manage users and view system statistics</p>
          </div>
          <mat-icon class="admin-icon">admin_panel_settings</mat-icon>
        </div>
      </header>

      <mat-tab-group>
        <!-- Statistics Tab -->
        <mat-tab label="Statistics">
          <div class="tab-content">
            @if (adminService.loading() && !adminService.statistics()) {
            <div class="loading">
              <mat-spinner></mat-spinner>
              <p>Loading statistics...</p>
            </div>
            } @else if (adminService.statistics()) {
            <div class="stats-grid">
              <mat-card class="stat-card">
                <mat-card-header>
                  <mat-icon>people</mat-icon>
                  <mat-card-title>Total Users</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="stat-value">{{ adminService.statistics()?.users?.total || 0 }}</div>
                </mat-card-content>
              </mat-card>

              <mat-card class="stat-card">
                <mat-card-header>
                  <mat-icon>event</mat-icon>
                  <mat-card-title>Total Events</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="stat-value">{{ adminService.statistics()?.events?.total || 0 }}</div>
                </mat-card-content>
              </mat-card>

              <mat-card class="stat-card">
                <mat-card-header>
                  <mat-icon>explore</mat-icon>
                  <mat-card-title>Total Outings</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="stat-value">{{ adminService.statistics()?.outings?.total || 0 }}</div>
                </mat-card-content>
              </mat-card>

              <mat-card class="stat-card">
                <mat-card-header>
                  <mat-icon>confirmation_number</mat-icon>
                  <mat-card-title>Active Vouchers</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="stat-value">
                    {{ adminService.statistics()?.vouchers?.active || 0 }}
                  </div>
                </mat-card-content>
              </mat-card>

              <mat-card class="stat-card">
                <mat-card-header>
                  <mat-icon>workspace_premium</mat-icon>
                  <mat-card-title>Premium Users</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="stat-value">
                    {{ adminService.statistics()?.users?.premium || 0 }}
                  </div>
                </mat-card-content>
              </mat-card>

              <mat-card class="stat-card">
                <mat-card-header>
                  <mat-icon>business</mat-icon>
                  <mat-card-title>Business Users</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="stat-value">
                    {{ adminService.statistics()?.users?.business || 0 }}
                  </div>
                </mat-card-content>
              </mat-card>
            </div>

            <div class="admin-actions">
              <button
                mat-raised-button
                color="warn"
                (click)="cleanupExpiredData()"
                matTooltip="Clean up expired chats and vouchers"
              >
                <mat-icon>cleaning_services</mat-icon>
                Cleanup Expired Data
              </button>
              <button mat-raised-button color="primary" (click)="refreshStats()">
                <mat-icon>refresh</mat-icon>
                Refresh Statistics
              </button>
            </div>
            }
          </div>
        </mat-tab>

        <!-- User Management Tab -->
        <mat-tab label="User Management">
          <div class="tab-content">
            @if (adminService.loading() && adminService.users().length === 0) {
            <div class="loading">
              <mat-spinner></mat-spinner>
              <p>Loading users...</p>
            </div>
            } @else if (adminService.users().length > 0) {
            <mat-card>
              <table mat-table [dataSource]="adminService.users()" class="users-table">
                <!-- Name Column -->
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>Name</th>
                  <td mat-cell *matCellDef="let user">{{ user.name }}</td>
                </ng-container>

                <!-- Email Column -->
                <ng-container matColumnDef="email">
                  <th mat-header-cell *matHeaderCellDef>Email</th>
                  <td mat-cell *matCellDef="let user">{{ user.email }}</td>
                </ng-container>

                <!-- Role Column -->
                <ng-container matColumnDef="role">
                  <th mat-header-cell *matHeaderCellDef>Role</th>
                  <td mat-cell *matCellDef="let user">
                    <mat-chip [class]="'role-' + user.role?.toLowerCase()">
                      {{ user.role }}
                    </mat-chip>
                  </td>
                </ng-container>

                <!-- Actions Column -->
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let user">
                    <button mat-icon-button [matMenuTriggerFor]="roleMenu" matTooltip="Change role">
                      <mat-icon>manage_accounts</mat-icon>
                    </button>
                    <mat-menu #roleMenu="matMenu">
                      <button mat-menu-item (click)="changeRole(user.id!, 'USER')">
                        <mat-icon>person</mat-icon>
                        User
                      </button>
                      <button mat-menu-item (click)="changeRole(user.id!, 'PREMIUM')">
                        <mat-icon>workspace_premium</mat-icon>
                        Premium
                      </button>
                      <button mat-menu-item (click)="changeRole(user.id!, 'BUSINESS')">
                        <mat-icon>business</mat-icon>
                        Business
                      </button>
                      <button mat-menu-item (click)="changeRole(user.id!, 'ADMIN')">
                        <mat-icon>admin_panel_settings</mat-icon>
                        Admin
                      </button>
                    </mat-menu>

                    <button
                      mat-icon-button
                      [matMenuTriggerFor]="userMenu"
                      matTooltip="More actions"
                    >
                      <mat-icon>more_vert</mat-icon>
                    </button>
                    <mat-menu #userMenu="matMenu">
                      <button mat-menu-item (click)="banUser(user.id!)">
                        <mat-icon>block</mat-icon>
                        Ban User
                      </button>
                      <button mat-menu-item (click)="unbanUser(user.id!)">
                        <mat-icon>check_circle</mat-icon>
                        Unban User
                      </button>
                    </mat-menu>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </mat-card>
            } @else {
            <div class="empty-state">
              <mat-icon>people_outline</mat-icon>
              <p>No users found</p>
            </div>
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [
    `
      .admin-container {
        max-width: 1400px;
        margin: 24px auto;
        padding: 24px;
      }

      .page-header {
        margin-bottom: 24px;

        .header-content {
          display: flex;
          justify-content: space-between;
          align-items: center;

          h1 {
            margin: 0 0 8px 0;
            font-size: 32px;
            font-weight: 600;
            color: #333;
          }

          .subtitle {
            margin: 0;
            color: #666;
            font-size: 16px;
          }

          .admin-icon {
            font-size: 64px;
            width: 64px;
            height: 64px;
            color: #f44336;
          }
        }
      }

      .tab-content {
        padding: 24px 0;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 24px;
        margin-bottom: 24px;
      }

      .stat-card {
        text-align: center;

        mat-card-header {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          padding: 16px;

          mat-icon {
            font-size: 48px;
            width: 48px;
            height: 48px;
            color: #1976d2;
          }

          mat-card-title {
            font-size: 14px;
            font-weight: 500;
            color: #666;
          }
        }

        mat-card-content {
          padding: 0 16px 16px;

          .stat-value {
            font-size: 36px;
            font-weight: 700;
            color: #333;
          }
        }
      }

      .admin-actions {
        display: flex;
        gap: 16px;
        justify-content: center;
        margin-top: 24px;
      }

      .users-table {
        width: 100%;

        .role-basic {
          background-color: #9e9e9e;
          color: white;
        }

        .role-premium {
          background-color: #ff9800;
          color: white;
        }

        .role-business {
          background-color: #2196f3;
          color: white;
        }

        .role-admin {
          background-color: #f44336;
          color: white;
        }

        .status-active {
          background-color: #4caf50;
          color: white;
        }

        .status-inactive {
          background-color: #f44336;
          color: white;
        }
      }

      .loading,
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px 24px;
        text-align: center;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: #999;
          margin-bottom: 16px;
        }
      }

      @media (max-width: 768px) {
        .admin-container {
          padding: 16px;
        }

        .stats-grid {
          grid-template-columns: 1fr;
        }

        .admin-actions {
          flex-direction: column;
        }
      }
    `,
  ],
})
export class AdminDashboardComponent implements OnInit {
  adminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);

  displayedColumns: string[] = ['name', 'email', 'role', 'actions'];

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.adminService.getAllUsers().subscribe();
    this.adminService.getStatistics().subscribe();
  }

  refreshStats() {
    this.adminService.getStatistics().subscribe({
      next: () => {
        this.snackBar.open('Statistics refreshed', 'Close', { duration: 2000 });
      },
    });
  }

  changeRole(userId: string, newRole: 'USER' | 'PREMIUM' | 'BUSINESS' | 'ADMIN') {
    this.adminService.changeUserRole(userId, newRole).subscribe({
      next: () => {
        this.snackBar.open(`User role changed to ${newRole}`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Error changing user role', 'Close', { duration: 3000 });
      },
    });
  }

  banUser(userId: string) {
    this.adminService.banUser(userId).subscribe({
      next: () => {
        this.snackBar.open('User banned', 'Close', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Error banning user', 'Close', { duration: 3000 });
      },
    });
  }

  unbanUser(userId: string) {
    this.adminService.unbanUser(userId).subscribe({
      next: () => {
        this.snackBar.open('User unbanned', 'Close', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Error unbanning user', 'Close', { duration: 3000 });
      },
    });
  }

  cleanupExpiredData() {
    // Run both cleanup operations in parallel
    forkJoin({
      chats: this.adminService.cleanupExpiredChats(),
      vouchers: this.adminService.cleanupExpiredVouchers(),
    }).subscribe({
      next: (results) => {
        const totalDeleted =
          (results.chats['cleaned'] || 0) + (results.vouchers['cleaned'] || 0);
        this.snackBar.open(
          `Cleanup completed: ${totalDeleted} items removed`,
          'Close',
          { duration: 3000 }
        );
        this.refreshStats();
      },
      error: (err) => {
        console.error('Cleanup error:', err);
        this.snackBar.open('Error during cleanup', 'Close', { duration: 3000 });
      },
    });
  }
}
