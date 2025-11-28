import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { EventsService } from '../../../../generated/api/events.service';
import { CreateEventRequest } from '../../../../generated/model/createEventRequest';
import { UpdateEventRequest } from '../../../../generated/model/updateEventRequest';
import { EventResponse } from '../../../../generated/model/eventResponse';
import { MapService } from '../../../map/services/map.service';

/**
 * Component for creating and editing events
 * Only BUSINESS users can create events
 */
@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="event-create-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>
            <mat-icon>{{ isEditMode() ? 'edit' : 'add' }}</mat-icon>
            {{ isEditMode() ? 'Edit Event' : 'Create New Event' }}
          </mat-card-title>
        </mat-card-header>

        <mat-card-content>
          @if (loading()) {
          <div class="loading">
            <mat-spinner></mat-spinner>
            <p>{{ isEditMode() ? 'Loading event...' : 'Creating event...' }}</p>
          </div>
          } @else {
          <form [formGroup]="eventForm" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <!-- Title -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Event Title</mat-label>
                <input matInput formControlName="title" placeholder="Enter event title" />
                <mat-icon matPrefix>title</mat-icon>
                @if (eventForm.get('title')?.hasError('required')) {
                <mat-error>Title is required</mat-error>
                }
              </mat-form-field>

              <!-- Description -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Description</mat-label>
                <textarea
                  matInput
                  formControlName="description"
                  placeholder="Describe your event"
                  rows="4"
                ></textarea>
                <mat-icon matPrefix>description</mat-icon>
                @if (eventForm.get('description')?.hasError('required')) {
                <mat-error>Description is required</mat-error>
                }
              </mat-form-field>

              <!-- Location -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Location</mat-label>
                <input matInput formControlName="location" placeholder="Event location" />
                <mat-icon matPrefix>location_on</mat-icon>
                @if (eventForm.get('location')?.hasError('required')) {
                <mat-error>Location is required</mat-error>
                }
              </mat-form-field>

              <!-- Geocode Button -->
              <div class="geocode-section">
                <button
                  mat-raised-button
                  color="accent"
                  type="button"
                  (click)="geocodeLocation()"
                  [disabled]="!eventForm.get('location')?.value || geocoding()"
                >
                  @if (geocoding()) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    <mat-icon>search</mat-icon>
                  }
                  Get Coordinates from Address
                </button>
                <p class="hint-text">Click to automatically fill coordinates from the location address above</p>
              </div>

              <!-- Latitude and Longitude -->
              <div class="coordinates-row">
                <mat-form-field appearance="outline">
                  <mat-label>Latitude</mat-label>
                  <input
                    matInput
                    type="number"
                    formControlName="latitude"
                    placeholder="e.g., 44.4949"
                  />
                  <mat-icon matPrefix>my_location</mat-icon>
                  @if (eventForm.get('latitude')?.hasError('required')) {
                  <mat-error>Latitude is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Longitude</mat-label>
                  <input
                    matInput
                    type="number"
                    formControlName="longitude"
                    placeholder="e.g., 11.3426"
                  />
                  <mat-icon matPrefix>place</mat-icon>
                  @if (eventForm.get('longitude')?.hasError('required')) {
                  <mat-error>Longitude is required</mat-error>
                  }
                </mat-form-field>
              </div>

              <!-- Event Date -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Event Date & Time</mat-label>
                <input
                  matInput
                  type="datetime-local"
                  formControlName="eventDate"
                  placeholder="Select event date and time"
                />
                <mat-icon matPrefix>event</mat-icon>
                @if (eventForm.get('eventDate')?.hasError('required')) {
                <mat-error>Event date is required</mat-error>
                }
              </mat-form-field>
            </div>
          </form>
          }
        </mat-card-content>

        <mat-card-actions>
          <button mat-button (click)="onCancel()" [disabled]="loading()">
            <mat-icon>cancel</mat-icon>
            Cancel
          </button>
          <button
            mat-raised-button
            color="primary"
            (click)="onSubmit()"
            [disabled]="!eventForm.valid || loading()"
          >
            <mat-icon>{{ isEditMode() ? 'save' : 'add' }}</mat-icon>
            {{ isEditMode() ? 'Update Event' : 'Create Event' }}
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .event-create-container {
        max-width: 800px;
        margin: 24px auto;
        padding: 24px;
      }

      mat-card {
        mat-card-header {
          margin-bottom: 24px;

          mat-card-title {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 24px;
            font-weight: 600;

            mat-icon {
              font-size: 32px;
              width: 32px;
              height: 32px;
            }
          }
        }

        mat-card-content {
          padding: 0 16px;
        }

        mat-card-actions {
          display: flex;
          justify-content: flex-end;
          gap: 12px;
          padding: 16px;
          border-top: 1px solid rgba(0, 0, 0, 0.12);
        }
      }

      .loading {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px 24px;
        text-align: center;

        mat-spinner {
          margin-bottom: 16px;
        }
      }

      .form-grid {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .full-width {
        width: 100%;
      }

      .coordinates-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
      }

      .geocode-section {
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding: 16px;
        background-color: #f5f5f5;
        border-radius: 8px;
        border-left: 4px solid #1976d2;

        button {
          display: flex;
          align-items: center;
          gap: 8px;
          justify-content: center;
        }

        .hint-text {
          margin: 0;
          font-size: 13px;
          color: #666;
          text-align: center;
        }
      }

      @media (max-width: 768px) {
        .event-create-container {
          padding: 16px;
        }

        .coordinates-row {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class EventCreateComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private eventsApi = inject(EventsService);
  private snackBar = inject(MatSnackBar);
  private mapService = inject(MapService);

  loading = signal(false);
  isEditMode = signal(false);
  eventId = signal<string | null>(null);
  geocoding = signal(false);

  eventForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    location: ['', [Validators.required]],
    latitude: [null, [Validators.required]],
    longitude: [null, [Validators.required]],
    eventDate: ['', [Validators.required]],
  });

  ngOnInit() {
    // Check if we're in edit mode by looking at route params
    this.route.params.subscribe((params) => {
      if (params['id']) {
        this.isEditMode.set(true);
        this.eventId.set(params['id']);
        this.loadEvent(params['id']);
      }
    });
  }

  /**
   * Load event data for editing
   */
  loadEvent(id: string) {
    this.loading.set(true);
    this.eventsApi.getEventById(id).subscribe({
      next: (event: EventResponse) => {
        // Populate form with event data
        this.eventForm.patchValue({
          title: event.title,
          description: event.description,
          location: event.location,
          latitude: event.latitude,
          longitude: event.longitude,
          eventDate: event.eventDate ? this.formatDateForInput(event.eventDate) : '',
        });
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading event:', error);
        this.snackBar.open('Error loading event', 'Close', { duration: 3000 });
        this.loading.set(false);
        this.router.navigate(['/events']);
      },
    });
  }

  /**
   * Format ISO date string for datetime-local input
   */
  private formatDateForInput(isoDate: string): string {
    const date = new Date(isoDate);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  /**
   * Submit form
   */
  onSubmit() {
    if (this.eventForm.invalid) {
      this.snackBar.open('Please fill in all required fields', 'Close', { duration: 3000 });
      return;
    }

    this.loading.set(true);

    const formValue = this.eventForm.value;
    const eventDate = new Date(formValue.eventDate).toISOString();

    if (this.isEditMode() && this.eventId()) {
      // Update existing event
      const updateRequest: UpdateEventRequest = {
        title: formValue.title,
        description: formValue.description,
        location: formValue.location,
        latitude: parseFloat(formValue.latitude),
        longitude: parseFloat(formValue.longitude),
        eventDate: eventDate,
      };

      this.eventsApi.updateEvent(this.eventId()!, updateRequest).subscribe({
        next: () => {
          this.snackBar.open('Event updated successfully!', 'Close', { duration: 3000 });
          this.loading.set(false);
          this.router.navigate(['/events', this.eventId()]);
        },
        error: (error) => {
          console.error('Error updating event:', error);
          this.snackBar.open('Error updating event', 'Close', { duration: 3000 });
          this.loading.set(false);
        },
      });
    } else {
      // Create new event
      const createRequest: CreateEventRequest = {
        title: formValue.title,
        description: formValue.description,
        location: formValue.location,
        latitude: parseFloat(formValue.latitude),
        longitude: parseFloat(formValue.longitude),
        eventDate: eventDate,
      };

      this.eventsApi.createEvent(createRequest).subscribe({
        next: (response) => {
          this.snackBar.open('Event created successfully!', 'Close', { duration: 3000 });
          this.loading.set(false);
          this.router.navigate(['/events', response.id]);
        },
        error: (error) => {
          console.error('Error creating event:', error);
          this.snackBar.open('Error creating event. Make sure you have BUSINESS role.', 'Close', {
            duration: 5000,
          });
          this.loading.set(false);
        },
      });
    }
  }

  /**
   * Geocode location address to get coordinates
   */
  geocodeLocation() {
    const locationValue = this.eventForm.get('location')?.value;
    if (!locationValue) {
      this.snackBar.open('Please enter a location first', 'Close', { duration: 3000 });
      return;
    }

    this.geocoding.set(true);
    this.mapService.geocodeAddress(locationValue).subscribe({
      next: (suggestions) => {
        if (suggestions && suggestions.length > 0) {
          const firstResult = suggestions[0];
          const lat = firstResult.latitudeAsDouble;
          const lon = firstResult.longitudeAsDouble;

          if (lat !== undefined && lon !== undefined) {
            this.eventForm.patchValue({
              latitude: lat,
              longitude: lon
            });
            this.snackBar.open('Coordinates found successfully!', 'Close', { duration: 3000 });
          } else {
            this.snackBar.open('Could not extract coordinates from result', 'Close', { duration: 5000 });
          }
        } else {
          this.snackBar.open('No results found for this address. Try a different format.', 'Close', { duration: 5000 });
        }
        this.geocoding.set(false);
      },
      error: (error) => {
        console.error('Geocoding error:', error);
        this.snackBar.open('Error geocoding address. Please try again.', 'Close', { duration: 5000 });
        this.geocoding.set(false);
      }
    });
  }

  /**
   * Cancel and go back
   */
  onCancel() {
    this.router.navigate(['/events']);
  }
}
