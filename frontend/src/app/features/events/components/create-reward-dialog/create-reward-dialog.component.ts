import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';

// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { RewardsService } from '../../../../generated/api/rewards.service';
import { CreateRewardRequest } from '../../../../generated/model/createRewardRequest';
import { RewardResponse } from '../../../../generated/model/rewardResponse';

export interface CreateRewardDialogData {
  eventId: string;
  eventTitle: string;
}

@Component({
  selector: 'app-create-reward-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>card_giftcard</mat-icon>
      Create Reward for {{ data.eventTitle }}
    </h2>

    <mat-dialog-content>
      <form [formGroup]="rewardForm">
        <mat-form-field appearance="outline">
          <mat-label>Reward Title</mat-label>
          <input
            matInput
            formControlName="title"
            placeholder="e.g., Free Pizza Margherita"
            maxlength="100"
          />
          <mat-icon matPrefix>title</mat-icon>
          @if (rewardForm.get('title')?.hasError('required') &&
          rewardForm.get('title')?.touched) {
          <mat-error>Title is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea
            matInput
            formControlName="description"
            placeholder="Describe the reward..."
            rows="4"
            maxlength="500"
          ></textarea>
          <mat-icon matPrefix>description</mat-icon>
          @if (rewardForm.get('description')?.hasError('required') &&
          rewardForm.get('description')?.touched) {
          <mat-error>Description is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Required Participants</mat-label>
          <input
            matInput
            type="number"
            formControlName="requiredParticipants"
            placeholder="3"
            min="1"
          />
          <mat-icon matPrefix>groups</mat-icon>
          <mat-hint
            >How many participants need to join to earn this reward?</mat-hint
          >
          @if (rewardForm.get('requiredParticipants')?.hasError('required') &&
          rewardForm.get('requiredParticipants')?.touched) {
          <mat-error>Required participants is required</mat-error>
          } @if (rewardForm.get('requiredParticipants')?.hasError('min') &&
          rewardForm.get('requiredParticipants')?.touched) {
          <mat-error>Must be at least 1</mat-error>
          }
        </mat-form-field>

        @if (error()) {
        <div class="error-message">
          <mat-icon color="warn">error</mat-icon>
          <span>{{ error() }}</span>
        </div>
        }
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="submitting()">
        Cancel
      </button>
      <button
        mat-raised-button
        color="primary"
        (click)="onSubmit()"
        [disabled]="!rewardForm.valid || submitting()"
      >
        @if (submitting()) {
        <mat-spinner diameter="20"></mat-spinner>
        } @else {
        <mat-icon>add_circle</mat-icon>
        } Create Reward
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      h2[mat-dialog-title] {
        display: flex;
        align-items: center;
        gap: 12px;
        margin: 0;
        padding: 24px 24px 16px;

        mat-icon {
          color: #ff9800;
        }
      }

      mat-dialog-content {
        padding: 0 24px;
        min-width: 500px;

        form {
          display: flex;
          flex-direction: column;
          gap: 16px;
          padding: 16px 0;
        }

        mat-form-field {
          width: 100%;
        }

        .error-message {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px;
          background-color: #ffebee;
          border-radius: 4px;
          color: #c62828;
          font-size: 14px;

          mat-icon {
            font-size: 20px;
            width: 20px;
            height: 20px;
          }
        }
      }

      mat-dialog-actions {
        padding: 16px 24px 24px;

        button {
          mat-spinner {
            display: inline-block;
            margin-right: 8px;
          }
        }
      }

      @media (max-width: 600px) {
        mat-dialog-content {
          min-width: auto;
          width: 100%;
        }
      }
    `,
  ],
})
export class CreateRewardDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<CreateRewardDialogComponent>);
  private rewardsService = inject(RewardsService);
  public data = inject<CreateRewardDialogData>(MAT_DIALOG_DATA);

  submitting = signal<boolean>(false);
  error = signal<string | null>(null);

  rewardForm: FormGroup;

  constructor() {
    this.rewardForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      requiredParticipants: [3, [Validators.required, Validators.min(1)]],
    });
  }

  onSubmit() {
    if (this.rewardForm.valid) {
      this.submitting.set(true);
      this.error.set(null);

      const request: CreateRewardRequest = {
        ...this.rewardForm.value,
        eventId: this.data.eventId,
      };

      this.rewardsService.createReward(request).subscribe({
        next: (reward: RewardResponse) => {
          console.log('Reward created:', reward);
          this.submitting.set(false);
          this.dialogRef.close(reward); // Return the created reward
        },
        error: (err) => {
          console.error('Error creating reward:', err);
          this.error.set(
            err.error?.message || 'Failed to create reward. Please try again.'
          );
          this.submitting.set(false);
        },
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
