import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';

import { OutingService } from '../../services/outing.service';
import { OutingResponse } from '../../../../generated/model/outingResponse';
import { ParticipationService } from '../../../participations/services/participation.service';
import { ParticipationResponse } from '../../../../generated/model/participationResponse';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { ReviewControllerService } from '../../../../generated/api/reviewController.service';
import { ReviewResponse } from '../../../../generated/model/reviewResponse';
import { CreateReviewRequest } from '../../../../generated/model/createReviewRequest';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSliderModule } from '@angular/material/slider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  ReportDialogComponent,
  ReportDialogData,
} from '../../../reports/components/report-dialog/report-dialog.component';

@Component({
  selector: 'app-outing-detail',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatListModule,
    MatDividerModule,
    MatExpansionModule,
    MatBadgeModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSliderModule,
    MatDialogModule,
  ],
  template: `
    <div class="outing-detail-container">
      @if (outingService.loading()) {
        <div class="loading">
          <mat-spinner></mat-spinner>
        </div>
      } @else if (outing()) {
        <mat-card class="outing-card">
          <mat-card-header>
            <mat-card-title>{{ outing()?.title }}</mat-card-title>
            <mat-card-subtitle>
              <mat-icon>person</mat-icon>
              Organized by {{ outing()?.organizer?.name }}
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <!-- Description -->
            <div class="section">
              <h3><mat-icon>description</mat-icon> Description</h3>
              <p>{{ outing()?.description }}</p>
            </div>

            <!-- Location -->
            <div class="section">
              <h3><mat-icon>location_on</mat-icon> Location</h3>
              <p>{{ outing()?.location }}</p>
            </div>

            <!-- Date & Time -->
            <div class="section">
              <h3><mat-icon>event</mat-icon> Date & Time</h3>
              <p>{{ outing()?.outingDate | date:'full' }}</p>
            </div>

            <!-- Participants -->
            <div class="section">
              <h3>
                <mat-icon>group</mat-icon>
                Participants ({{ outing()?.currentParticipants }}/{{ outing()?.maxParticipants }})
              </h3>

              @if (outing()?.isFull) {
                <mat-chip class="full-chip">
                  <mat-icon>block</mat-icon>
                  Full
                </mat-chip>
              } @else {
                <mat-chip class="available-chip">
                  <mat-icon>check_circle</mat-icon>
                  {{ outing()?.maxParticipants! - outing()?.currentParticipants! }} spots available
                </mat-chip>
              }

              @if (outing()?.participants && outing()?.participants!.length > 0) {
                <mat-list>
                  @for (participant of outing()?.participants; track participant.id) {
                    <mat-list-item>
                      <mat-icon matListItemIcon>person</mat-icon>
                      <span matListItemTitle>{{ participant.name }}</span>
                      <span matListItemLine>{{ participant.email }}</span>
                    </mat-list-item>
                    <mat-divider></mat-divider>
                  }
                </mat-list>
              } @else {
                <p class="no-participants">No participants yet. Be the first to join!</p>
              }
            </div>

            <!-- Linked Event -->
            @if (outing()?.event) {
              <div class="section">
                <h3><mat-icon>celebration</mat-icon> Linked Event</h3>
                <mat-card class="event-card">
                  <mat-card-content>
                    <h4>{{ outing()?.event?.title }}</h4>
                    <p><mat-icon>event</mat-icon> {{ outing()?.event?.eventDate | date:'short' }}</p>
                  </mat-card-content>
                </mat-card>
              </div>
            }

            <!-- Participation Requests (Organizer Only) -->
            @if (isOrganizer()) {
              <div class="section">
                <mat-expansion-panel [expanded]="pendingParticipations().length > 0">
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon [matBadge]="pendingParticipations().length" matBadgeColor="warn" [matBadgeHidden]="pendingParticipations().length === 0">pending_actions</mat-icon>
                      Participation Requests
                    </mat-panel-title>
                    <mat-panel-description>
                      {{ pendingParticipations().length }} pending request{{ pendingParticipations().length !== 1 ? 's' : '' }}
                    </mat-panel-description>
                  </mat-expansion-panel-header>

                  @if (participationService.loading()) {
                    <div class="loading-small">
                      <mat-spinner diameter="30"></mat-spinner>
                      <p>Loading participation requests...</p>
                    </div>
                  } @else if (pendingParticipations().length > 0) {
                    <mat-list>
                      @for (participation of pendingParticipations(); track participation.id) {
                        <mat-list-item>
                          <mat-icon matListItemIcon>person_outline</mat-icon>
                          <div matListItemTitle>{{ participation.user?.name }}</div>
                          <div matListItemLine>{{ participation.user?.email }}</div>
                          <div matListItemLine class="request-date">
                            <mat-icon>schedule</mat-icon>
                            Requested: {{ participation.createdAt | date:'short' }}
                          </div>
                          <div matListItemMeta class="action-buttons">
                            <button
                              mat-mini-fab
                              color="primary"
                              (click)="approveParticipation(participation.id!)"
                              [disabled]="actionLoading()"
                              matTooltip="Approve">
                              <mat-icon>check</mat-icon>
                            </button>
                            <button
                              mat-mini-fab
                              color="warn"
                              (click)="rejectParticipation(participation.id!)"
                              [disabled]="actionLoading()"
                              matTooltip="Reject">
                              <mat-icon>close</mat-icon>
                            </button>
                          </div>
                        </mat-list-item>
                        <mat-divider></mat-divider>
                      }
                    </mat-list>
                  } @else {
                    <p class="no-requests">No pending participation requests</p>
                  }
                </mat-expansion-panel>
              </div>
            }

            <!-- Reviews Section -->
            <div class="section">
              <h3><mat-icon>star_rate</mat-icon> Reviews</h3>

              <!-- Create Review Form (for participants only) -->
              @if (outing()?.isParticipant && !hasUserReviewed()) {
                <mat-card class="review-form-card">
                  <mat-card-content>
                    <h4>Leave a Review</h4>
                    <form [formGroup]="reviewForm" (ngSubmit)="submitReview()">
                      <div class="rating-field">
                        <label>Rating: {{ reviewForm.get('rating')?.value }}/5</label>
                        <mat-slider min="1" max="5" step="1" discrete showTickMarks>
                          <input matSliderThumb formControlName="rating">
                        </mat-slider>
                      </div>

                      <mat-form-field appearance="outline" class="full-width">
                        <mat-label>Comment (optional)</mat-label>
                        <textarea
                          matInput
                          formControlName="comment"
                          placeholder="Share your experience..."
                          rows="3"
                        ></textarea>
                        <mat-icon matPrefix>comment</mat-icon>
                      </mat-form-field>

                      <div class="form-actions">
                        <button
                          mat-raised-button
                          color="primary"
                          type="submit"
                          [disabled]="!reviewForm.valid || reviewLoading()">
                          @if (reviewLoading()) {
                            <mat-spinner diameter="20"></mat-spinner>
                          } @else {
                            <mat-icon>send</mat-icon>
                          }
                          Submit Review
                        </button>
                      </div>
                    </form>
                  </mat-card-content>
                </mat-card>
              }

              <!-- Reviews List -->
              @if (reviews().length > 0) {
                <div class="reviews-list">
                  @for (review of reviews(); track review.id) {
                    <mat-card class="review-card">
                      <mat-card-content>
                        <div class="review-header">
                          <div class="reviewer-info">
                            <mat-icon>account_circle</mat-icon>
                            <span class="reviewer-name">{{ review.reviewer?.name }}</span>
                          </div>
                          <div class="rating">
                            @for (star of [1,2,3,4,5]; track star) {
                              <mat-icon [class.filled]="star <= (review.rating || 0)">
                                {{ star <= (review.rating || 0) ? 'star' : 'star_border' }}
                              </mat-icon>
                            }
                          </div>
                        </div>
                        @if (review.comment) {
                          <p class="review-comment">{{ review.comment }}</p>
                        }
                        <p class="review-date">{{ review.createdAt | date:'medium' }}</p>
                      </mat-card-content>
                    </mat-card>
                  }
                </div>
              } @else {
                <p class="no-reviews">No reviews yet. Be the first to review!</p>
              }
            </div>
          </mat-card-content>

          <mat-card-actions>
            <!-- Chat button (only for participants) -->
            @if (outing()?.isParticipant || isOrganizer()) {
              <button
                mat-raised-button
                color="accent"
                (click)="goToChat()">
                <mat-icon>chat</mat-icon>
                Open Chat
              </button>
            }

            <!-- Join/Leave buttons -->
            @if (outing()?.isParticipant) {
              <button
                mat-raised-button
                color="warn"
                (click)="leaveOuting()"
                [disabled]="actionLoading()">
                <mat-icon>exit_to_app</mat-icon>
                Leave Outing
              </button>
            } @else if (!outing()?.isFull) {
              <button
                mat-raised-button
                color="primary"
                (click)="joinOuting()"
                [disabled]="actionLoading()">
                <mat-icon>group_add</mat-icon>
                Join Outing
              </button>
            }

            <button mat-button (click)="goBack()">
              <mat-icon>arrow_back</mat-icon>
              Back
            </button>

            <!-- Report button (for non-organizers) -->
            @if (!isOrganizer()) {
              <button
                mat-button
                color="warn"
                (click)="openReportDialog()">
                <mat-icon>flag</mat-icon>
                Report
              </button>
            }
          </mat-card-actions>
        </mat-card>
      } @else {
        <mat-card class="error-card">
          <mat-card-content>
            <mat-icon>error_outline</mat-icon>
            <h3>Outing not found</h3>
            <button mat-raised-button color="primary" (click)="goBack()">
              Go Back
            </button>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .outing-detail-container {
      max-width: 900px;
      margin: 24px auto;
      padding: 24px;
    }

    .loading {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 400px;
    }

    .outing-card {
      mat-card-header {
        margin-bottom: 24px;

        mat-card-title {
          font-size: 28px;
          font-weight: 600;
        }

        mat-card-subtitle {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 8px;
          font-size: 16px;
        }
      }
    }

    .section {
      margin-bottom: 32px;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 20px;
        font-weight: 600;
        margin-bottom: 16px;
        color: #333;
      }

      p {
        font-size: 16px;
        line-height: 1.6;
        color: #666;
      }
    }

    .full-chip {
      background-color: #f44336;
      color: white;
      margin-bottom: 16px;
    }

    .available-chip {
      background-color: #4caf50;
      color: white;
      margin-bottom: 16px;
    }

    .no-participants {
      color: #999;
      font-style: italic;
      padding: 16px;
      background-color: #f5f5f5;
      border-radius: 8px;
    }

    .loading-small {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 24px;
      color: #666;

      p {
        margin: 0;
        font-size: 14px;
      }
    }

    .no-requests {
      color: #999;
      font-style: italic;
      padding: 16px;
      text-align: center;
    }

    .request-date {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #999;
      font-size: 12px;

      mat-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
      }
    }

    .action-buttons {
      display: flex;
      gap: 8px;

      button {
        width: 36px;
        height: 36px;
      }
    }

    mat-expansion-panel {
      mat-panel-title {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .event-card {
      margin-top: 8px;
      background-color: #f5f5f5;

      h4 {
        margin: 0 0 8px 0;
        font-size: 18px;
      }

      p {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0;
        color: #666;
      }
    }

    .review-form-card {
      margin-bottom: 24px;
      background-color: #f0f7ff;

      h4 {
        margin: 0 0 16px 0;
        font-size: 18px;
        color: #1976d2;
      }

      .rating-field {
        margin-bottom: 16px;

        label {
          display: block;
          margin-bottom: 8px;
          font-weight: 500;
          color: #333;
        }

        mat-slider {
          width: 100%;
        }
      }

      .form-actions {
        display: flex;
        justify-content: flex-end;
        margin-top: 16px;

        button {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }

    .reviews-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .review-card {
      background-color: #fafafa;

      .review-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .reviewer-info {
          display: flex;
          align-items: center;
          gap: 8px;

          mat-icon {
            color: #666;
          }

          .reviewer-name {
            font-weight: 500;
            color: #333;
          }
        }

        .rating {
          display: flex;
          gap: 2px;

          mat-icon {
            font-size: 20px;
            width: 20px;
            height: 20px;
            color: #ccc;

            &.filled {
              color: #ffc107;
            }
          }
        }
      }

      .review-comment {
        margin: 0 0 12px 0;
        color: #666;
        line-height: 1.6;
      }

      .review-date {
        margin: 0;
        font-size: 12px;
        color: #999;
      }
    }

    .no-reviews {
      color: #999;
      font-style: italic;
      padding: 24px;
      text-align: center;
      background-color: #f5f5f5;
      border-radius: 8px;
    }

    .full-width {
      width: 100%;
    }

    mat-card-actions {
      display: flex;
      gap: 12px;
      padding: 16px;
      border-top: 1px solid #e0e0e0;

      button {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .error-card {
      text-align: center;
      padding: 48px;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #f44336;
        margin-bottom: 16px;
      }

      h3 {
        margin-bottom: 24px;
      }
    }

    @media (max-width: 768px) {
      .outing-detail-container {
        padding: 16px;
      }

      .outing-card mat-card-header mat-card-title {
        font-size: 24px;
      }

      mat-card-actions {
        flex-direction: column;

        button {
          width: 100%;
        }
      }
    }
  `],
})
export class OutingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private reviewService = inject(ReviewControllerService);
  private dialog = inject(MatDialog);
  outingService = inject(OutingService);
  participationService = inject(ParticipationService);

  outing = signal<OutingResponse | null>(null);
  actionLoading = signal(false);
  allParticipations = signal<ParticipationResponse[]>([]);
  reviews = signal<ReviewResponse[]>([]);
  reviewLoading = signal(false);

  // Computed signals
  isOrganizer = signal(false);
  pendingParticipations = signal<ParticipationResponse[]>([]);

  reviewForm: FormGroup = this.fb.group({
    rating: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: [''],
  });

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadOuting(id);
    }
  }

  loadOuting(id: string) {
    this.outingService.getOutingById(id).subscribe({
      next: (outing) => {
        this.outing.set(outing);

        // Check if current user is the organizer
        const currentUserId = this.authService.currentUser()?.id;
        const isOrg = currentUserId === outing.organizer?.id;
        this.isOrganizer.set(isOrg);

        // Load participations if organizer
        if (isOrg) {
          this.loadParticipations(id);
        }

        // Load reviews
        this.loadReviews(id);
      },
      error: (err) => {
        console.error('Error loading outing:', err);
        this.snackBar.open('Error loading outing', 'Close', { duration: 3000 });
      },
    });
  }

  loadReviews(outingId: string) {
    this.reviewService.getReviewsByOuting(outingId).subscribe({
      next: (reviews) => {
        this.reviews.set(reviews);
      },
      error: (err) => {
        console.error('Error loading reviews:', err);
      },
    });
  }

  hasUserReviewed(): boolean {
    const currentUserId = this.authService.currentUser()?.id;
    return this.reviews().some(review => review.reviewer?.id === currentUserId);
  }

  submitReview() {
    if (this.reviewForm.invalid) return;

    const outingId = this.outing()?.id;
    if (!outingId) return;

    this.reviewLoading.set(true);

    const reviewRequest: CreateReviewRequest = {
      outingId: outingId,
      rating: this.reviewForm.value.rating,
      comment: this.reviewForm.value.comment || undefined,
    };

    this.reviewService.createReview(reviewRequest).subscribe({
      next: () => {
        this.reviewLoading.set(false);
        this.snackBar.open('Review submitted successfully!', 'Close', { duration: 3000 });
        this.reviewForm.reset({ rating: 3, comment: '' });
        this.loadReviews(outingId);
      },
      error: (err) => {
        this.reviewLoading.set(false);
        console.error('Error submitting review:', err);
        const message = err.error?.message || 'Error submitting review';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }

  loadParticipations(outingId: string) {
    this.participationService.getParticipationsByOuting(outingId).subscribe({
      next: (participations) => {
        this.allParticipations.set(participations);

        // Filter pending participations
        const pending = participations.filter(
          p => p.status === 'PENDING'
        );
        this.pendingParticipations.set(pending);
      },
      error: (err) => {
        console.error('Error loading participations:', err);
      },
    });
  }

  joinOuting() {
    const id = this.outing()?.id;
    if (!id) return;

    this.actionLoading.set(true);
    this.outingService.joinOuting(id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Join request sent! Waiting for organizer approval.', 'Close', { duration: 4000 });

        // Reload outing to update participant count
        this.loadOuting(id);
      },
      error: (err) => {
        this.actionLoading.set(false);
        console.error('Error joining outing:', err);
        const message = err.error?.message || 'Error joining outing';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }

  leaveOuting() {
    const outingId = this.outing()?.id;
    if (!outingId) return;

    // Find current user's participation
    const currentUserId = this.authService.currentUser()?.id;
    const userParticipation = this.allParticipations().find(
      p => p.user?.id === currentUserId && p.status === 'APPROVED'
    );

    if (!userParticipation?.id) {
      this.snackBar.open('Participation not found', 'Close', { duration: 3000 });
      return;
    }

    this.actionLoading.set(true);
    this.participationService.leaveOuting(userParticipation.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Successfully left outing', 'Close', { duration: 3000 });

        // Reload outing and participations
        this.loadOuting(outingId);
        this.loadParticipations(outingId);
      },
      error: (err) => {
        this.actionLoading.set(false);
        console.error('Error leaving outing:', err);
        const message = err.error?.message || 'Error leaving outing';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }

  approveParticipation(participationId: string) {
    this.actionLoading.set(true);
    this.participationService.approveParticipation(participationId).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Participation approved successfully!', 'Close', { duration: 3000 });

        // Reload outing and participations
        const outingId = this.outing()?.id;
        if (outingId) {
          this.loadOuting(outingId);
          this.loadParticipations(outingId);
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        console.error('Error approving participation:', err);
        const message = err.error?.message || 'Error approving participation';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }

  rejectParticipation(participationId: string) {
    this.actionLoading.set(true);
    this.participationService.rejectParticipation(participationId).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Participation rejected', 'Close', { duration: 3000 });

        // Reload participations
        const outingId = this.outing()?.id;
        if (outingId) {
          this.loadParticipations(outingId);
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        console.error('Error rejecting participation:', err);
        const message = err.error?.message || 'Error rejecting participation';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }

  goToChat() {
    const outingId = this.outing()?.id;
    if (outingId) {
      this.router.navigate(['/chat/outing', outingId]);
    }
  }

  goBack() {
    this.router.navigate(['/outings']);
  }

  openReportDialog() {
    const outing = this.outing();
    if (!outing || !outing.id) return;

    // Build list of users to report: organizer + participants
    const users: Array<{ id: string; name: string }> = [];

    // Add organizer
    if (outing.organizer?.id) {
      users.push({
        id: outing.organizer.id,
        name: outing.organizer.name || 'Unknown',
      });
    }

    // Add participants (excluding current user)
    const currentUserId = this.authService.currentUser()?.id;
    if (outing.participants) {
      outing.participants.forEach((participant) => {
        if (participant.id && participant.id !== currentUserId) {
          users.push({
            id: participant.id,
            name: participant.name || 'Unknown',
          });
        }
      });
    }

    const dialogData: ReportDialogData = {
      relatedEntityId: outing.id,
      relatedEntityType: 'OUTING',
      outingOrganizer: outing.organizer?.id
        ? { id: outing.organizer.id, name: outing.organizer.name || 'Unknown' }
        : undefined,
      outingParticipants: outing.participants
        ?.filter((p) => p.id !== currentUserId)
        .map((p) => ({ id: p.id!, name: p.name || 'Unknown' })) || [],
    };

    const dialogRef = this.dialog.open(ReportDialogComponent, {
      width: '600px',
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe((report) => {
      if (report) {
        console.log('Report submitted:', report);
        this.snackBar.open(
          'Report submitted successfully. Administrators will review it.',
          'Close',
          { duration: 5000 }
        );
      }
    });
  }
}
