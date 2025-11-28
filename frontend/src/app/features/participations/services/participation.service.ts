import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of } from 'rxjs';
import { ParticipationsService } from '../../../generated/api/participations.service';
import { ParticipationResponse } from '../../../generated/model/participationResponse';

/**
 * Service for managing participations with state management
 * Wraps the generated OpenAPI ParticipationsService
 */
@Injectable({
  providedIn: 'root',
})
export class ParticipationService {
  private participationsApi = inject(ParticipationsService);

  participations = signal<ParticipationResponse[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  /**
   * Get participations for an outing
   */
  getParticipationsByOuting(outingId: string): Observable<ParticipationResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.participationsApi.getParticipationsByOuting(outingId).pipe(
      tap({
        next: (participations) => {
          this.participations.set(participations);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading participations');
          this.loading.set(false);
          console.error('Error loading participations:', err);
        },
      }),
      catchError((err) => {
        this.error.set('Error loading participations');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Get my participations
   */
  getMyParticipations(): Observable<ParticipationResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.participationsApi.getMyParticipations().pipe(
      tap({
        next: (participations) => {
          this.participations.set(participations);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading my participations');
          this.loading.set(false);
          console.error('Error loading my participations:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading my participations');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Approve a participation request
   */
  approveParticipation(participationId: string): Observable<ParticipationResponse> {
    this.loading.set(true);
    this.error.set(null);

    return this.participationsApi.approveParticipation(participationId).pipe(
      tap({
        next: () => this.loading.set(false),
        error: (err) => {
          this.error.set('Error approving participation');
          this.loading.set(false);
          console.error('Error approving participation:', err);
        },
      })
    );
  }

  /**
   * Reject a participation request
   */
  rejectParticipation(participationId: string): Observable<ParticipationResponse> {
    this.loading.set(true);
    this.error.set(null);

    return this.participationsApi.rejectParticipation(participationId).pipe(
      tap({
        next: () => this.loading.set(false),
        error: (err) => {
          this.error.set('Error rejecting participation');
          this.loading.set(false);
          console.error('Error rejecting participation:', err);
        },
      })
    );
  }

  /**
   * Leave an outing (cancel participation)
   */
  leaveOuting(participationId: string): Observable<void> {
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
