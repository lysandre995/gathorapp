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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { OutingsService } from '../../../../generated/api/outings.service';
import { EventsService } from '../../../../generated/api/events.service';
import { CreateOutingRequest } from '../../../../generated/model/createOutingRequest';
import { OutingResponse } from '../../../../generated/model/outingResponse';
import { EventResponse } from '../../../../generated/model/eventResponse';
import { MapService } from '../../../map/services/map.service';

/**
 * Component for creating and editing outings
 * Premium users can create outings, optionally linked to events
 */
@Component({
  selector: 'app-outing-create',
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
    MatSelectModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="outing-create-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>
            <mat-icon>{{ isEditMode() ? 'edit' : 'add' }}</mat-icon>
            {{ isEditMode() ? 'Edit Outing' : 'Create New Outing' }}
          </mat-card-title>
        </mat-card-header>

        <mat-card-content>
          @if (loading()) {
          <div class="loading">
            <mat-spinner></mat-spinner>
            <p>{{ isEditMode() ? 'Loading outing...' : 'Creating outing...' }}</p>
          </div>
          } @else {
          <form [formGroup]="outingForm" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <!-- Title -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Outing Title</mat-label>
                <input matInput formControlName="title" placeholder="Enter outing title" />
                <mat-icon matPrefix>title</mat-icon>
                @if (outingForm.get('title')?.hasError('required')) {
                <mat-error>Title is required</mat-error>
                }
              </mat-form-field>

              <!-- Description -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Description</mat-label>
                <textarea
                  matInput
                  formControlName="description"
                  placeholder="Describe your outing"
                  rows="4"
                ></textarea>
                <mat-icon matPrefix>description</mat-icon>
                @if (outingForm.get('description')?.hasError('required')) {
                <mat-error>Description is required</mat-error>
                }
              </mat-form-field>

              <!-- Optional: Link to Event -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Link to Event (Optional)</mat-label>
                <mat-select formControlName="eventId">
                  <mat-option [value]="null">No event</mat-option>
                  @for (event of availableEvents(); track event.id) {
                  <mat-option [value]="event.id">{{ event.title }}</mat-option>
                  }
                </mat-select>
                <mat-icon matPrefix>event</mat-icon>
                <mat-hint>Link this outing to an existing event to earn rewards</mat-hint>
              </mat-form-field>

              <!-- Location -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Location</mat-label>
                <input matInput formControlName="location" placeholder="Outing location" />
                <mat-icon matPrefix>location_on</mat-icon>
                @if (outingForm.get('location')?.hasError('required')) {
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
                  [disabled]="!outingForm.get('location')?.value || geocoding()"
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
                  @if (outingForm.get('latitude')?.hasError('required')) {
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
                  @if (outingForm.get('longitude')?.hasError('required')) {
                  <mat-error>Longitude is required</mat-error>
                  }
                </mat-form-field>
              </div>

              <!-- Outing Date -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Outing Date & Time</mat-label>
                <input
                  matInput
                  type="datetime-local"
                  formControlName="outingDate"
                  placeholder="Select outing date and time"
                />
                <mat-icon matPrefix>calendar_today</mat-icon>
                @if (outingForm.get('outingDate')?.hasError('required')) {
                <mat-error>Outing date is required</mat-error>
                }
              </mat-form-field>

              <!-- Max Participants -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Max Participants</mat-label>
                <input
                  matInput
                  type="number"
                  formControlName="maxParticipants"
                  placeholder="Maximum number of participants"
                  min="2"
                />
                <mat-icon matPrefix>group</mat-icon>
                @if (outingForm.get('maxParticipants')?.hasError('required')) {
                <mat-error>Max participants is required</mat-error>
                } @if (outingForm.get('maxParticipants')?.hasError('min')) {
                <mat-error>At least 2 participants required</mat-error>
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
            [disabled]="!outingForm.valid || loading()"
          >
            <mat-icon>{{ isEditMode() ? 'save' : 'add' }}</mat-icon>
            {{ isEditMode() ? 'Update Outing' : 'Create Outing' }}
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .outing-create-container {
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
        .outing-create-container {
          padding: 16px;
        }

        .coordinates-row {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class OutingCreateComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private outingsApi = inject(OutingsService);
  private eventsApi = inject(EventsService);
  private snackBar = inject(MatSnackBar);
  private mapService = inject(MapService);

  loading = signal(false);
  isEditMode = signal(false);
  outingId = signal<string | null>(null);
  availableEvents = signal<EventResponse[]>([]);
  geocoding = signal(false);

  outingForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    location: ['', [Validators.required]],
    latitude: [null, [Validators.required]],
    longitude: [null, [Validators.required]],
    outingDate: ['', [Validators.required]],
    maxParticipants: [10, [Validators.required, Validators.min(2)]],
    eventId: [null], // Optional
  });

  ngOnInit() {
    // Load available events for linking
    this.loadAvailableEvents();

    // Check if we're in edit mode
    this.route.params.subscribe((params) => {
      if (params['id']) {
        this.isEditMode.set(true);
        this.outingId.set(params['id']);
        this.loadOuting(params['id']);
      }
    });
  }

  /**
   * Load available events for linking
   */
  loadAvailableEvents() {
    this.eventsApi.getUpcomingEvents().subscribe({
      next: (events) => {
        this.availableEvents.set(events);
      },
      error: (error) => {
        console.error('Error loading events:', error);
      },
    });
  }

  /**
   * Load outing data for editing
   */
  loadOuting(id: string) {
    this.loading.set(true);
    this.outingsApi.getOutingById(id).subscribe({
      next: (outing: OutingResponse) => {
        this.outingForm.patchValue({
          title: outing.title,
          description: outing.description,
          location: outing.location,
          latitude: outing.latitude,
          longitude: outing.longitude,
          outingDate: outing.outingDate ? this.formatDateForInput(outing.outingDate) : '',
          maxParticipants: outing.maxParticipants,
          eventId: outing.event?.id || null,
        });
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading outing:', error);
        this.snackBar.open('Error loading outing', 'Close', { duration: 3000 });
        this.loading.set(false);
        this.router.navigate(['/outings']);
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
    if (this.outingForm.invalid) {
      this.snackBar.open('Please fill in all required fields', 'Close', { duration: 3000 });
      return;
    }

    this.loading.set(true);

    const formValue = this.outingForm.value;
    const outingDate = new Date(formValue.outingDate).toISOString();

    if (this.isEditMode() && this.outingId()) {
      // Update existing outing
      // TODO: Update API endpoint not available yet
      // For now, we'll just show a message that edit is not supported
      this.snackBar.open(
        'Update functionality not yet available. Please delete and recreate the outing.',
        'Close',
        { duration: 5000 }
      );
      this.loading.set(false);
      this.router.navigate(['/outings']);
    } else {
      // Create new outing
      const createRequest: CreateOutingRequest = {
        title: formValue.title,
        description: formValue.description,
        location: formValue.location,
        latitude: parseFloat(formValue.latitude),
        longitude: parseFloat(formValue.longitude),
        outingDate: outingDate,
        maxParticipants: parseInt(formValue.maxParticipants),
        eventId: formValue.eventId || undefined,
      };

      this.outingsApi.createOuting(createRequest).subscribe({
        next: (response) => {
          this.snackBar.open('Outing created successfully!', 'Close', { duration: 3000 });
          this.loading.set(false);
          this.router.navigate(['/outings', response.id]);
        },
        error: (error) => {
          console.error('Error creating outing:', error);
          this.snackBar.open(
            'Error creating outing. Make sure you have PREMIUM role.',
            'Close',
            { duration: 5000 }
          );
          this.loading.set(false);
        },
      });
    }
  }

  /**
   * Geocode location address to get coordinates
   */
  geocodeLocation() {
    const locationValue = this.outingForm.get('location')?.value;
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
            this.outingForm.patchValue({
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
    this.router.navigate(['/outings']);
  }
}
