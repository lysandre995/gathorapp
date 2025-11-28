import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

// Angular Material
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';

import { OutingService } from '../../services/outing.service';
import { OutingCardComponent } from '../../components/outing-card/outing-card.component';

@Component({
  selector: 'app-outing-list',
  standalone: true,
  imports: [
    FormsModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    OutingCardComponent,
  ],
  template: `
    <div class="outing-list-container">
      <header class="page-header">
        <h1>Available Outings</h1>
        <p class="subtitle">Join outings and meet new people</p>
        @if (eventIdFilter()) {
          <p class="filter-info">
            <mat-icon>filter_alt</mat-icon>
            Showing outings for selected event
          </p>
        }
      </header>

      <div class="filters">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search outings</mat-label>
          <input matInput [(ngModel)]="searchQuery" (ngModelChange)="onSearch()" placeholder="Title, location, organizer..." />
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

      @if (outingService.loading()) {
      <div class="loading">
        <mat-spinner></mat-spinner>
        <p>Loading outings...</p>
      </div>
      }

      @if (outingService.error()) {
      <div class="error">
        <mat-icon>error_outline</mat-icon>
        <h3>Loading Error</h3>
        <p>{{ outingService.error() }}</p>
        <button mat-raised-button color="primary" (click)="loadOutings()">Retry</button>
      </div>
      }

      @if (!outingService.loading() && !outingService.error()) {
        @if (filteredOutings().length > 0) {
      <div class="outings-grid">
        @for (outing of filteredOutings(); track outing.id) {
        <app-outing-card [outing]="outing" />
        }
      </div>
        } @else {
      <div class="empty-state">
        <mat-icon>explore_off</mat-icon>
        <h3>No outings found</h3>
        <p>Try modifying your search filters</p>
      </div>
        }
      }

      <button mat-fab color="primary" class="fab-button" (click)="onCreateOuting()" aria-label="Create new outing">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .outing-list-container { max-width: 1400px; margin: 0 auto; padding: 24px; }
    .page-header { margin-bottom: 32px; text-align: center; }
    .page-header h1 { margin: 0 0 8px 0; font-size: 32px; font-weight: 600; color: #333; }
    .page-header .subtitle { margin: 0; color: #666; font-size: 16px; }
    .page-header .filter-info { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 12px; color: #1976d2; font-weight: 500; }
    .filters { display: flex; gap: 16px; margin-bottom: 32px; flex-wrap: wrap; }
    .filters .search-field { flex: 1; min-width: 300px; }
    .filters mat-form-field { min-width: 200px; }
    .outings-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 24px; margin-bottom: 32px; }
    .loading, .error, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 64px 24px; text-align: center; }
    .loading mat-icon, .error mat-icon, .empty-state mat-icon { font-size: 64px; width: 64px; height: 64px; color: #999; margin-bottom: 16px; }
    .error mat-icon { color: #f44336; }
    .fab-button { position: fixed; right: 32px; bottom: 32px; z-index: 1000; }
    @media (max-width: 768px) {
      .outing-list-container { padding: 16px; }
      .outings-grid { grid-template-columns: 1fr; }
      .filters { flex-direction: column; }
      .filters .search-field, .filters mat-form-field { width: 100%; }
      .fab-button { right: 16px; bottom: 16px; }
    }
  `],
})
export class OutingListComponent implements OnInit {
  outingService = inject(OutingService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  searchQuery = '';
  sortBy = 'date';
  filteredOutings = signal(this.outingService.outings());
  eventIdFilter = signal<string | null>(null);

  ngOnInit() {
    // Check for eventId query parameter
    this.route.queryParams.subscribe((params) => {
      this.eventIdFilter.set(params['eventId'] || null);
      this.loadOutings();
    });
  }

  loadOutings() {
    this.outingService.getUpcomingOutings().subscribe(() => {
      let outings = this.outingService.outings();

      // Filter by eventId if present
      if (this.eventIdFilter()) {
        outings = outings.filter((o) => o.event?.id === this.eventIdFilter());
      }

      this.filteredOutings.set(outings);
    });
  }

  onSearch() {
    const query = this.searchQuery.toLowerCase();
    let outings = this.outingService.outings();

    // Apply eventId filter first
    if (this.eventIdFilter()) {
      outings = outings.filter((o) => o.event?.id === this.eventIdFilter());
    }

    // Then apply search query
    const filtered = outings.filter(
      (outing) =>
        outing.title?.toLowerCase().includes(query) ||
        outing.location?.toLowerCase().includes(query) ||
        outing.organizer?.name?.toLowerCase().includes(query)
    );
    this.filteredOutings.set(filtered);
  }

  onSortChange() {
    const sorted = [...this.filteredOutings()].sort((a, b) => {
      switch (this.sortBy) {
        case 'title':
          return (a.title || '').localeCompare(b.title || '');
        case 'date':
        default:
          return new Date(a.outingDate || 0).getTime() - new Date(b.outingDate || 0).getTime();
      }
    });
    this.filteredOutings.set(sorted);
  }

  onCreateOuting() {
    this.router.navigate(['/outings/create']);
  }
}
