import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { EventService } from '../../services/event.service';
import { EventResponse } from '../../../../generated/model/eventResponse';
import { RewardResponse } from '../../../../generated/model/rewardResponse';
import { RewardsService } from '../../../../generated/api/rewards.service';
import { RewardCardComponent } from '../../components/reward-card/reward-card.component';
import {
  CreateRewardDialogComponent,
  CreateRewardDialogData,
} from '../../components/create-reward-dialog/create-reward-dialog.component';
import { AuthService } from '../../../../core/auth/services/auth.service';
import {
  ReportDialogComponent,
  ReportDialogData,
} from '../../../reports/components/report-dialog/report-dialog.component';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    MatDialogModule,
    RewardCardComponent,
  ],
  template: `
    <div class="container">
      <!-- Loading State -->
      @if (loading()) {
      <div class="loading-container">
        <mat-spinner></mat-spinner>
        <p>Loading event details...</p>
      </div>
      }

      <!-- Error State -->
      @if (error()) {
      <mat-card class="error-card">
        <mat-card-content>
          <div class="error-content">
            <mat-icon color="warn">error_outline</mat-icon>
            <h2>Error Loading Event</h2>
            <p>{{ error() }}</p>
            <button mat-raised-button color="primary" (click)="loadEvent()">
              Retry
            </button>
            <button mat-button [routerLink]="['/events']">
              Back to Events
            </button>
          </div>
        </mat-card-content>
      </mat-card>
      }

      <!-- Event Details -->
      @if (event() && !loading() && !error()) {
      <div class="event-detail">
        <!-- Header with Back Button -->
        <div class="header-actions">
          <button mat-button [routerLink]="['/events']">
            <mat-icon>arrow_back</mat-icon>
            Back to Events
          </button>
          <div class="actions-right">
            @if (isCreator()) {
            <div class="creator-actions">
              <button mat-button color="primary" [routerLink]="['/events', event()!.id, 'edit']">
                <mat-icon>edit</mat-icon>
                Edit
              </button>
              <button mat-button color="warn" (click)="onDelete()">
                <mat-icon>delete</mat-icon>
                Delete
              </button>
            </div>
            } @else {
            <button mat-button color="warn" (click)="openReportDialog()">
              <mat-icon>flag</mat-icon>
              Report Event
            </button>
            }
          </div>
        </div>

        <!-- Main Card -->
        <mat-card class="main-card">
          <!-- Hero Image/Placeholder -->
          <div class="hero-image">
            <mat-icon>event</mat-icon>
          </div>

          <mat-card-header>
            <mat-card-title class="event-title">{{ event()!.title }}</mat-card-title>
            <mat-card-subtitle>
              <div class="subtitle-info">
                <div class="organizer">
                  <mat-icon>business</mat-icon>
                  <span>Organized by: {{ event()!.creator?.name || 'Unknown' }}</span>
                </div>
                @if (event()!.creator?.email) {
                <div class="contact">
                  <mat-icon>email</mat-icon>
                  <a [href]="'mailto:' + event()!.creator?.email">{{ event()!.creator?.email }}</a>
                </div>
                }
              </div>
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <!-- Description -->
            <section class="section">
              <h3>
                <mat-icon>description</mat-icon>
                Description
              </h3>
              <p class="description">{{ event()!.description }}</p>
            </section>

            <mat-divider></mat-divider>

            <!-- Event Information -->
            <section class="section">
              <h3>
                <mat-icon>info</mat-icon>
                Event Information
              </h3>
              <div class="info-grid">
                <div class="info-item">
                  <mat-icon>calendar_today</mat-icon>
                  <div>
                    <strong>Date & Time</strong>
                    <p>{{ event()!.eventDate | date: 'EEEE, MMMM d, y, h:mm a' }}</p>
                  </div>
                </div>

                <div class="info-item">
                  <mat-icon>location_on</mat-icon>
                  <div>
                    <strong>Location</strong>
                    <p>{{ event()!.location }}</p>
                    @if (event()!.latitude && event()!.longitude) {
                    <p class="coordinates">
                      Coordinates: {{ event()!.latitude?.toFixed(6) }}, {{ event()!.longitude?.toFixed(6) }}
                    </p>
                    }
                  </div>
                </div>
              </div>
            </section>

            <mat-divider></mat-divider>

            <!-- Rewards Section -->
            <section class="section">
              <div class="section-header">
                <h3>
                  <mat-icon>card_giftcard</mat-icon>
                  Rewards
                </h3>
                @if (isCreator()) {
                <button mat-raised-button color="accent" (click)="openCreateRewardDialog()">
                  <mat-icon>add</mat-icon>
                  Add Reward
                </button>
                }
              </div>

              @if (loadingRewards()) {
              <div class="rewards-loading">
                <mat-spinner diameter="40"></mat-spinner>
                <p>Loading rewards...</p>
              </div>
              } @else if (rewards().length === 0) {
              <div class="no-rewards">
                <mat-icon>card_giftcard</mat-icon>
                <p>No rewards available for this event yet.</p>
                @if (isCreator()) {
                <p class="hint-text">
                  Add a reward to incentivize Premium users to organize outings for your event!
                </p>
                }
              </div>
              } @else {
              <div class="rewards-grid">
                @for (reward of rewards(); track reward.id) {
                <app-reward-card [reward]="reward" />
                }
              </div>
              }
            </section>

            <mat-divider></mat-divider>

            <!-- Related Outings Section -->
            <section class="section">
              <h3>
                <mat-icon>groups</mat-icon>
                Join or Create an Outing
              </h3>
              <p class="hint-text">
                Want to participate in this event? Browse existing outings or create your own!
              </p>
              <div class="action-buttons">
                <button mat-raised-button color="primary" [routerLink]="['/outings']" [queryParams]="{eventId: event()!.id}">
                  <mat-icon>search</mat-icon>
                  Browse Outings for this Event
                </button>
                <button mat-raised-button color="accent" [routerLink]="['/outings/create']" [queryParams]="{eventId: event()!.id}">
                  <mat-icon>add_circle</mat-icon>
                  Create New Outing
                </button>
              </div>
            </section>

            @if (event()!.latitude && event()!.longitude) {
            <mat-divider></mat-divider>

            <!-- Map Section -->
            <section class="section">
              <h3>
                <mat-icon>map</mat-icon>
                Location on Map
              </h3>
              <button mat-raised-button color="primary" [routerLink]="['/map']" [queryParams]="{lat: event()!.latitude, lng: event()!.longitude, zoom: 15}">
                <mat-icon>open_in_new</mat-icon>
                View on Map
              </button>
            </section>
            }
          </mat-card-content>
        </mat-card>

        <!-- Metadata Card -->
        <mat-card class="metadata-card">
          <mat-card-header>
            <mat-card-title>Details</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="metadata-item">
              <mat-icon>fingerprint</mat-icon>
              <div>
                <strong>Event ID</strong>
                <p class="id-text">{{ event()!.id }}</p>
              </div>
            </div>
            @if (event()!.createdAt) {
            <div class="metadata-item">
              <mat-icon>schedule</mat-icon>
              <div>
                <strong>Created</strong>
                <p>{{ event()!.createdAt | date: 'short' }}</p>
              </div>
            </div>
            }
          </mat-card-content>
        </mat-card>
      </div>
      }
    </div>
  `,
  styles: [
    `
      .container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 24px;
      }

      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        gap: 16px;
      }

      .error-card {
        margin-top: 24px;
      }

      .error-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
        padding: 32px;
        text-align: center;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
        }

        h2 {
          margin: 0;
        }

        p {
          color: #666;
        }
      }

      .event-detail {
        display: grid;
        grid-template-columns: 1fr 300px;
        gap: 24px;
      }

      .header-actions {
        grid-column: 1 / -1;
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .creator-actions {
          display: flex;
          gap: 8px;
        }
      }

      .main-card {
        grid-column: 1;
      }

      .hero-image {
        height: 300px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;

        mat-icon {
          font-size: 120px;
          width: 120px;
          height: 120px;
          color: white;
        }
      }

      .event-title {
        font-size: 32px;
        font-weight: 600;
        margin: 24px 0 8px;
      }

      .subtitle-info {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-top: 8px;

        .organizer,
        .contact {
          display: flex;
          align-items: center;
          gap: 8px;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }

          a {
            color: #1976d2;
            text-decoration: none;

            &:hover {
              text-decoration: underline;
            }
          }
        }
      }

      .section {
        margin: 24px 0;

        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;

          h3 {
            margin: 0;
          }
        }

        h3 {
          display: flex;
          align-items: center;
          gap: 8px;
          margin: 0 0 16px 0;
          font-size: 20px;
          font-weight: 500;

          mat-icon {
            color: #1976d2;
          }
        }
      }

      .rewards-loading {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
        padding: 32px;
        color: #666;
      }

      .no-rewards {
        text-align: center;
        padding: 48px 24px;
        background-color: #f5f5f5;
        border-radius: 8px;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          color: #ccc;
          margin-bottom: 16px;
        }

        p {
          margin: 8px 0;
          color: #666;
        }
      }

      .rewards-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 16px;
      }

      .description {
        line-height: 1.8;
        color: #333;
        white-space: pre-wrap;
      }

      .info-grid {
        display: grid;
        gap: 24px;
      }

      .info-item {
        display: flex;
        gap: 16px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
          color: #1976d2;
          flex-shrink: 0;
        }

        strong {
          display: block;
          margin-bottom: 4px;
          font-size: 14px;
          text-transform: uppercase;
          color: #666;
        }

        p {
          margin: 0;
          font-size: 16px;
          color: #333;
        }

        .coordinates {
          font-size: 14px;
          color: #666;
          margin-top: 4px;
        }
      }

      .hint-text {
        color: #666;
        margin: 0 0 16px;
      }

      .action-buttons {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
      }

      .metadata-card {
        grid-column: 2;
        height: fit-content;
        position: sticky;
        top: 24px;
      }

      .metadata-item {
        display: flex;
        gap: 12px;
        margin-bottom: 20px;

        &:last-child {
          margin-bottom: 0;
        }

        mat-icon {
          color: #666;
          flex-shrink: 0;
        }

        strong {
          display: block;
          margin-bottom: 4px;
          font-size: 12px;
          text-transform: uppercase;
          color: #666;
        }

        p {
          margin: 0;
          font-size: 14px;
          color: #333;
          word-break: break-all;
        }

        .id-text {
          font-family: monospace;
          font-size: 12px;
        }
      }

      mat-divider {
        margin: 24px 0;
      }

      @media (max-width: 968px) {
        .event-detail {
          grid-template-columns: 1fr;
        }

        .main-card,
        .metadata-card {
          grid-column: 1;
        }

        .metadata-card {
          position: static;
        }

        .header-actions {
          flex-direction: column;
          align-items: stretch;
          gap: 12px;

          .creator-actions {
            justify-content: stretch;

            button {
              flex: 1;
            }
          }
        }
      }

      @media (max-width: 600px) {
        .container {
          padding: 16px;
        }

        .event-title {
          font-size: 24px;
        }

        .action-buttons {
          flex-direction: column;

          button {
            width: 100%;
          }
        }
      }
    `,
  ],
})
export class EventDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private eventService = inject(EventService);
  private rewardsService = inject(RewardsService);
  private dialog = inject(MatDialog);
  private authService = inject(AuthService);

  event = signal<EventResponse | null>(null);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  isCreator = signal<boolean>(false);

  // Rewards
  rewards = signal<RewardResponse[]>([]);
  loadingRewards = signal<boolean>(false);

  ngOnInit() {
    const eventId = this.route.snapshot.paramMap.get('id');
    if (eventId) {
      this.loadEvent();
    } else {
      this.error.set('No event ID provided');
      this.loading.set(false);
    }
  }

  loadEvent() {
    const eventId = this.route.snapshot.paramMap.get('id');
    if (!eventId) return;

    this.loading.set(true);
    this.error.set(null);

    this.eventService.getEventById(eventId).subscribe({
      next: (event) => {
        this.event.set(event);
        this.loading.set(false);

        // Check if current user is the creator
        const currentUser = this.authService.currentUser();
        if (currentUser && event.creator?.id) {
          this.isCreator.set(currentUser.id === event.creator.id);
        }

        // Load rewards for this event
        this.loadRewards(eventId);
      },
      error: (err) => {
        console.error('Error loading event:', err);
        this.error.set('Failed to load event details. The event may not exist.');
        this.loading.set(false);
      },
    });
  }

  loadRewards(eventId: string) {
    this.loadingRewards.set(true);

    this.rewardsService.getRewardsByEvent(eventId).subscribe({
      next: (rewards: RewardResponse[]) => {
        this.rewards.set(rewards);
        this.loadingRewards.set(false);
      },
      error: (err: any) => {
        console.error('Error loading rewards:', err);
        this.loadingRewards.set(false);
        // Don't show error to user, just log it
      },
    });
  }

  openCreateRewardDialog() {
    const event = this.event();
    if (!event || !event.id || !event.title) return;

    const dialogData: CreateRewardDialogData = {
      eventId: event.id,
      eventTitle: event.title,
    };

    const dialogRef = this.dialog.open(CreateRewardDialogComponent, {
      width: '600px',
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe((reward: RewardResponse | undefined) => {
      if (reward) {
        // Add the new reward to the list
        this.rewards.update((rewards) => [...rewards, reward]);
      }
    });
  }

  onDelete() {
    if (confirm('Are you sure you want to delete this event?')) {
      // TODO: Implement delete functionality
      console.log('Delete event:', this.event()?.id);
      // After deletion, navigate back to events list
      // this.router.navigate(['/events']);
    }
  }

  openReportDialog() {
    const event = this.event();
    if (!event || !event.id || !event.creator?.id) return;

    const dialogData: ReportDialogData = {
      relatedEntityId: event.id,
      relatedEntityType: 'EVENT',
      eventCreator: {
        id: event.creator.id,
        name: event.creator.name || 'Unknown',
      },
    };

    const dialogRef = this.dialog.open(ReportDialogComponent, {
      width: '600px',
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe((report) => {
      if (report) {
        console.log('Report submitted:', report);
        // Show success message (optional)
        alert('Report submitted successfully. Administrators will review it.');
      }
    });
  }
}
