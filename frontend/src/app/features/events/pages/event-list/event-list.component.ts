import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

// Angular Material
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';

import { EventService } from '../../services/event.service';
import { EventCardComponent } from '../../components/event-card/event-card.component';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [
    FormsModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EventCardComponent,
  ],
  template: `
    <div class="event-list-container">
      <header class="page-header">
        <h1>Available Events</h1>
        <p class="subtitle">Discover and participate in events near you</p>
      </header>

      <!-- Search & Filters -->
      <div class="filters">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search events</mat-label>
          <input
            matInput
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearch()"
            placeholder="Title, location, organizer..."
          />
          <mat-icon matPrefix>search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Sort by</mat-label>
          <mat-select [(ngModel)]="sortBy" (ngModelChange)="onSortChange()">
            <mat-option value="date">Date</mat-option>
            <mat-option value="title">Title</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <!-- Loading State -->
      @if (eventService.loading()) {
      <div class="loading">
        <mat-spinner></mat-spinner>
        <p>Loading events...</p>
      </div>
      }

      <!-- Error State -->
      @if (eventService.error()) {
      <div class="error">
        <mat-icon>error_outline</mat-icon>
        <h3>Loading Error</h3>
        <p>{{ eventService.error() }}</p>
        <button mat-raised-button color="primary" (click)="loadEvents()">Retry</button>
      </div>
      }

      <!-- Events Grid -->
      @if (!eventService.loading() && !eventService.error()) { @if (filteredEvents().length > 0) {
      <div class="events-grid">
        @for (event of filteredEvents(); track event.id) {
        <app-event-card [event]="event" />
        }
      </div>
      } @else {
      <div class="empty-state">
        <mat-icon>event_busy</mat-icon>
        <h3>No events found</h3>
        <p>Try modifying your search filters</p>
      </div>
      } }

      <!-- FAB for creating new event -->
      <button
        mat-fab
        color="primary"
        class="fab-button"
        (click)="onCreateEvent()"
        aria-label="Create new event"
      >
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
  styles: [
    `
      .event-list-container {
        max-width: 1400px;
        margin: 0 auto;
        padding: 24px;
      }

      .page-header {
        margin-bottom: 32px;
        text-align: center;

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
      }

      .filters {
        display: flex;
        gap: 16px;
        margin-bottom: 32px;
        flex-wrap: wrap;

        .search-field {
          flex: 1;
          min-width: 300px;
        }

        mat-form-field {
          min-width: 200px;
        }
      }

      .events-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 24px;
        margin-bottom: 32px;
      }

      .loading,
      .error,
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

        h3 {
          margin: 0 0 8px 0;
          font-size: 24px;
          color: #333;
        }

        p {
          margin: 0 0 16px 0;
          color: #666;
        }
      }

      .error mat-icon {
        color: #f44336;
      }

      @media (max-width: 768px) {
        .event-list-container {
          padding: 16px;
        }

        .events-grid {
          grid-template-columns: 1fr;
        }

        .filters {
          flex-direction: column;

          .search-field,
          mat-form-field {
            width: 100%;
          }
        }
      }

      .fab-button {
        position: fixed;
        right: 32px;
        bottom: 32px;
        z-index: 1000;
      }

      @media (max-width: 768px) {
        .fab-button {
          right: 16px;
          bottom: 16px;
        }
      }
    `,
  ],
})
export class EventListComponent implements OnInit {
  eventService = inject(EventService);
  router = inject(Router);

  searchQuery = '';
  sortBy = 'date';
  filteredEvents = signal<any[]>([]);

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.eventService.getUpcomingEvents().subscribe({
      next: (events) => {
        console.log('Events received in component:', events);
        console.log('Service events signal:', this.eventService.events());
        this.filteredEvents.set(this.eventService.events());
      },
      error: (err) => {
        console.error('Error in component subscribe:', err);
      }
    });
  }

  onSearch() {
    const query = this.searchQuery.toLowerCase();
    const filtered = this.eventService
      .events()
      .filter(
        (event) =>
          event.title?.toLowerCase().includes(query) ||
          event.location?.toLowerCase().includes(query) ||
          event.creator?.name?.toLowerCase().includes(query)
      );
    this.filteredEvents.set(filtered);
  }

  onSortChange() {
    const sorted = [...this.filteredEvents()].sort((a, b) => {
      switch (this.sortBy) {
        case 'title':
          return (a.title || '').localeCompare(b.title || '');
        case 'date':
        default:
          return (
            new Date(a.eventDate || 0).getTime() - new Date(b.eventDate || 0).getTime()
          );
      }
    });
    this.filteredEvents.set(sorted);
  }

  onCreateEvent() {
    this.router.navigate(['/events/create']);
  }
}
