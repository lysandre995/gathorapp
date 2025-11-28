import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of } from 'rxjs';
import { OutingsService } from '../../../generated/api/outings.service';
import { ParticipationsService } from '../../../generated/api/participations.service';
import { OutingResponse } from '../../../generated/model/outingResponse';

/**
 * Service for managing outings with state management
 * Wraps the generated OpenAPI OutingsService
 */
@Injectable({
  providedIn: 'root',
})
export class OutingService {
  private outingsApi = inject(OutingsService);
  private participationsApi = inject(ParticipationsService);

  outings = signal<OutingResponse[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  /**
   * Get all outings
   */
  getOutings(): Observable<OutingResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.outingsApi.getAllOutings().pipe(
      tap({
        next: (outings) => {
          this.outings.set(outings);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading outings');
          this.loading.set(false);
          console.error('Error loading outings:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading outings');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Get outing by ID
   */
  getOutingById(id: string): Observable<OutingResponse> {
    this.loading.set(true);
    this.error.set(null);

    return this.outingsApi.getOutingById(id).pipe(
      tap({
        next: () => this.loading.set(false),
        error: (err) => {
          this.error.set('Error loading outing details');
          this.loading.set(false);
          console.error('Error loading outing:', err);
        },
      })
    );
  }

  /**
   * Get upcoming outings
   */
  getUpcomingOutings(): Observable<OutingResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.outingsApi.getUpcomingOutings().pipe(
      tap({
        next: (outings) => {
          this.outings.set(outings);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading upcoming outings');
          this.loading.set(false);
          console.error('Error loading upcoming outings:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading upcoming outings');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Get my outings (organized by authenticated user)
   */
  getMyOutings(): Observable<OutingResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.outingsApi.getMyOutings().pipe(
      tap({
        next: (outings) => {
          this.outings.set(outings);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading my outings');
          this.loading.set(false);
          console.error('Error loading my outings:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading my outings');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Join an outing - creates a PENDING participation request
   */
  joinOuting(id: string): Observable<any> {
    this.loading.set(true);
    this.error.set(null);

    return this.participationsApi.joinOuting(id).pipe(
      tap({
        next: () => this.loading.set(false),
        error: (err) => {
          this.error.set('Error joining outing');
          this.loading.set(false);
          console.error('Error joining outing:', err);
        },
      })
    );
  }

  /**
   * Leave an outing - removes participation
   */
  leaveOuting(participationId: string): Observable<any> {
    this.loading.set(true);
    this.error.set(null);

    return this.participationsApi.leaveOuting1(participationId).pipe(
      tap({
        next: () => this.loading.set(false),
        error: (err) => {
          this.error.set('Error leaving outing');
          this.loading.set(false);
          console.error('Error leaving outing:', err);
        },
      })
    );
  }
}
