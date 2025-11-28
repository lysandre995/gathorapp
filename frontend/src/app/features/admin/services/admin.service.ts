import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of } from 'rxjs';
import { AdminService as AdminApiService } from '../../../generated/api/admin.service';
import { UserResponse } from '../../../generated/model/userResponse';

/**
 * Platform statistics structure returned by the backend
 */
export interface PlatformStatistics {
  users: {
    total: number;
    base: number;
    premium: number;
    business: number;
  };
  events: {
    total: number;
    upcoming: number;
  };
  outings: {
    total: number;
    upcoming: number;
  };
  participations: {
    total: number;
  };
  chats: {
    total: number;
    active: number;
  };
  vouchers: {
    total: number;
    active: number;
  };
  generated_at: string;
}

/**
 * Service for admin operations
 * Wraps the generated OpenAPI AdminService
 */
@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private adminApi = inject(AdminApiService);

  users = signal<UserResponse[]>([]);
  statistics = signal<PlatformStatistics | null>(null);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  /**
   * Get all users (admin only)
   */
  getAllUsers(): Observable<UserResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.adminApi.getAllUsers1().pipe(
      tap({
        next: (users) => {
          this.users.set(users);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading users');
          this.loading.set(false);
          console.error('Error loading users:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading users');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Change user role (admin only)
   */
  changeUserRole(userId: string, newRole: 'USER' | 'PREMIUM' | 'BUSINESS' | 'ADMIN'): Observable<UserResponse> {
    return this.adminApi.changeUserRole(userId, newRole).pipe(
      tap({
        next: (updatedUser) => {
          // Update user in list
          this.users.update((users) =>
            users.map((u) => (u.id === userId ? updatedUser : u))
          );
        },
        error: (err) => {
          console.error('Error changing user role:', err);
        },
      })
    );
  }

  /**
   * Ban user (admin/maintainer only)
   */
  banUser(userId: string, reason?: string): Observable<UserResponse> {
    return this.adminApi.banUser(userId, reason).pipe(
      tap({
        next: (updatedUser) => {
          // Update user in list
          this.users.update((users) =>
            users.map((u) => (u.id === userId ? updatedUser : u))
          );
        },
        error: (err) => {
          console.error('Error banning user:', err);
        },
      })
    );
  }

  /**
   * Unban user (admin/maintainer only)
   */
  unbanUser(userId: string): Observable<UserResponse> {
    return this.adminApi.unbanUser(userId).pipe(
      tap({
        next: (updatedUser) => {
          // Update user in list
          this.users.update((users) =>
            users.map((u) => (u.id === userId ? updatedUser : u))
          );
        },
        error: (err) => {
          console.error('Error unbanning user:', err);
        },
      })
    );
  }

  /**
   * Get system statistics (admin/maintainer only)
   */
  getStatistics(): Observable<PlatformStatistics | null> {
    this.loading.set(true);
    this.error.set(null);

    return this.adminApi.getStats().pipe(
      tap({
        next: (stats) => {
          this.statistics.set(stats as unknown as PlatformStatistics);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading statistics');
          this.loading.set(false);
          console.error('Error loading statistics:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading statistics');
        this.loading.set(false);
        return of(null);
      })
    ) as Observable<PlatformStatistics | null>;
  }

  /**
   * Clean up expired chats (admin/maintainer only)
   */
  cleanupExpiredChats(): Observable<{ [key: string]: number }> {
    return this.adminApi.cleanupExpiredChats().pipe(
      tap({
        error: (err) => {
          console.error('Error cleaning up expired chats:', err);
        },
      })
    );
  }

  /**
   * Clean up expired vouchers (admin/maintainer only)
   */
  cleanupExpiredVouchers(): Observable<{ [key: string]: number }> {
    return this.adminApi.cleanupExpiredVouchers().pipe(
      tap({
        error: (err) => {
          console.error('Error cleaning up expired vouchers:', err);
        },
      })
    );
  }
}
