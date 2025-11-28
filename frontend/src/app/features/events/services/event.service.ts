import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of } from 'rxjs';
import { EventsService } from '../../../generated/api/events.service';
import { EventResponse } from '../../../generated/model/eventResponse';

/**
 * Service for managing events with state management
 * Wraps the generated OpenAPI EventsService
 */
@Injectable({
  providedIn: 'root',
})
export class EventService {
  private eventsApi = inject(EventsService);

  events = signal<EventResponse[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  /**
   * Get all events
   */
  getEvents(): Observable<EventResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.eventsApi.getAllEvents().pipe(
      tap({
        next: (events) => {
          this.events.set(events);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading events');
          this.loading.set(false);
          console.error('Error loading events:', err);
        },
      }),
      catchError((err) => {
        this.error.set('Error loading events');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Get event by ID
   */
  getEventById(id: string): Observable<EventResponse> {
    this.loading.set(true);
    this.error.set(null);

    return this.eventsApi.getEventById(id).pipe(
      tap({
        next: () => this.loading.set(false),
        error: (err) => {
          this.error.set('Error loading event details');
          this.loading.set(false);
          console.error('Error loading event:', err);
        },
      })
    );
  }

  /**
   * Get upcoming events
   */
  getUpcomingEvents(): Observable<EventResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.eventsApi.getUpcomingEvents().pipe(
      tap({
        next: (events) => {
          this.events.set(events);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading upcoming events');
          this.loading.set(false);
          console.error('Error loading upcoming events:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading upcoming events');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Get my events (created by authenticated user)
   */
  getMyEvents(): Observable<EventResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.eventsApi.getMyEvents().pipe(
      tap({
        next: (events) => {
          this.events.set(events);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error loading my events');
          this.loading.set(false);
          console.error('Error loading my events:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error loading my events');
        this.loading.set(false);
        return of([]);
      })
    );
  }
}
