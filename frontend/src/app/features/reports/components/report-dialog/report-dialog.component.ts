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
import { MatSelectModule } from '@angular/material/select';

import { ReportControllerService } from '../../../../generated/api/reportController.service';
import { CreateReportRequest } from '../../../../generated/model/createReportRequest';
import { ReportResponse } from '../../../../generated/model/reportResponse';

export interface ReportDialogData {
  relatedEntityId: string;
  relatedEntityType: 'EVENT' | 'OUTING';
  // For events: only the creator
  eventCreator?: { id: string; name: string };
  // For outings: organizer + participants
  outingOrganizer?: { id: string; name: string };
  outingParticipants?: Array<{ id: string; name: string }>;
}

@Component({
  selector: 'app-report-dialog',
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
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>flag</mat-icon>
      Report {{ isEvent() ? 'Event' : 'Outing' }}
    </h2>

    <mat-dialog-content>
      <form [formGroup]="reportForm">
        <!-- For outings: show user selection -->
        @if (!isEvent()) {
        <mat-form-field appearance="outline">
          <mat-label>Report User</mat-label>
          <mat-select formControlName="reportedUserId" placeholder="Select user to report">
            @if (data.outingOrganizer) {
            <mat-option [value]="data.outingOrganizer.id">
              {{ data.outingOrganizer.name }} (Organizer)
            </mat-option>
            }
            @for (participant of data.outingParticipants; track participant.id) {
            <mat-option [value]="participant.id">
              {{ participant.name }}
            </mat-option>
            }
          </mat-select>
          <mat-icon matPrefix>person</mat-icon>
          @if (reportForm.get('reportedUserId')?.hasError('required') &&
          reportForm.get('reportedUserId')?.touched) {
          <mat-error>Please select a user to report</mat-error>
          }
        </mat-form-field>
        }

        <mat-form-field appearance="outline">
          <mat-label>Report Reason</mat-label>
          <mat-select formControlName="type" placeholder="Select a reason">
            @if (isEvent()) {
            <mat-option value="MISLEADING_AD">Misleading Advertising</mat-option>
            <mat-option value="FAKE_IDENTITY">Event Does Not Exist / Organizer Is Not Who They Say</mat-option>
            <mat-option value="INAPPROPRIATE_BEHAVIOR">Inappropriate Behavior</mat-option>
            <mat-option value="OTHER">Scam / Other</mat-option>
            } @else {
            <mat-option value="FAKE_IDENTITY">User Is Not Who They Say</mat-option>
            <mat-option value="INAPPROPRIATE_BEHAVIOR">Inappropriate Behavior</mat-option>
            <mat-option value="OTHER">Other</mat-option>
            }
          </mat-select>
          <mat-icon matPrefix>category</mat-icon>
          @if (reportForm.get('type')?.hasError('required') &&
          reportForm.get('type')?.touched) {
          <mat-error>Please select a reason</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea
            matInput
            formControlName="reason"
            placeholder="Provide more details about this report..."
            rows="5"
            maxlength="1000"
          ></textarea>
          <mat-icon matPrefix>description</mat-icon>
          <mat-hint>{{ reportForm.get('reason')?.value?.length || 0 }}/1000</mat-hint>
        </mat-form-field>

        <div class="info-box">
          <mat-icon>info</mat-icon>
          <p>
            Your report will be reviewed by our administrators. False reports may result in
            account restrictions.
          </p>
        </div>

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
        color="warn"
        (click)="onSubmit()"
        [disabled]="!reportForm.valid || submitting()"
      >
        @if (submitting()) {
        <mat-spinner diameter="20"></mat-spinner>
        } @else {
        <mat-icon>flag</mat-icon>
        } Submit Report
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
          color: #f44336;
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

        .info-box {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 12px;
          background-color: #e3f2fd;
          border-radius: 4px;
          border-left: 4px solid #2196f3;

          mat-icon {
            color: #2196f3;
            font-size: 20px;
            width: 20px;
            height: 20px;
            margin-top: 2px;
          }

          p {
            margin: 0;
            font-size: 14px;
            color: #1565c0;
            line-height: 1.5;
          }
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
export class ReportDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<ReportDialogComponent>);
  private reportService = inject(ReportControllerService);
  public data = inject<ReportDialogData>(MAT_DIALOG_DATA);

  submitting = signal<boolean>(false);
  error = signal<string | null>(null);

  reportForm: FormGroup;

  constructor() {
    // For events, reportedUserId is not needed in form (always the creator)
    // For outings, it's required
    this.reportForm = this.fb.group({
      reportedUserId: [
        this.isEvent() ? null : '',
        this.isEvent() ? [] : [Validators.required]
      ],
      type: ['', [Validators.required]],
      reason: ['', [Validators.maxLength(1000)]],
    });
  }

  isEvent(): boolean {
    return this.data.relatedEntityType === 'EVENT';
  }

  onSubmit() {
    if (this.reportForm.valid) {
      this.submitting.set(true);
      this.error.set(null);

      // For events, use the creator ID; for outings, use the selected user ID
      const reportedUserId = this.isEvent()
        ? this.data.eventCreator!.id
        : this.reportForm.value.reportedUserId;

      const request: CreateReportRequest = {
        reportedUserId: reportedUserId,
        type: this.reportForm.value.type,
        reason: this.reportForm.value.reason || undefined,
        relatedEntityId: this.data.relatedEntityId,
        relatedEntityType: this.data.relatedEntityType,
      };

      this.reportService.createReport(request).subscribe({
        next: (report: ReportResponse) => {
          console.log('Report submitted:', report);
          this.submitting.set(false);
          this.dialogRef.close(report);
        },
        error: (err) => {
          console.error('Error submitting report:', err);
          this.error.set(
            err.error?.message || 'Failed to submit report. Please try again.'
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
