import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { OutingService } from './outing.service';
import { OutingsService } from '../../../generated/api/outings.service';
import { ParticipationsService } from '../../../generated/api/participations.service';
import { OutingResponse } from '../../../generated/model/outingResponse';
import { ParticipationResponse } from '../../../generated/model/participationResponse';

describe('OutingService', () => {
  let service: OutingService;
  let outingsApiSpy: jasmine.SpyObj<OutingsService>;
  let participationsApiSpy: jasmine.SpyObj<ParticipationsService>;

  const mockOutings: OutingResponse[] = [
    {
      id: '1',
      title: 'Test Outing 1',
      description: 'Description 1',
      location: 'Location 1',
      latitude: 40.0,
      longitude: -74.0,
      outingDate: '2026-01-01T10:00:00',
      maxParticipants: 10,
      currentParticipants: 2,
      isFull: false,
      organizer: { id: 'org1', name: 'Organizer 1', email: 'org@example.com', role: 'USER' },
      participants: [],
      createdAt: '2025-01-01T10:00:00',
    },
  ];

  const mockParticipation: ParticipationResponse = {
    id: 'part1',
    user: { id: 'user1', name: 'Test User', email: 'test@example.com' },
    outing: { id: '1', title: 'Test Outing 1', outingDate: '2026-01-01T10:00:00', maxParticipants: 10 },
    status: 'PENDING',
    createdAt: '2025-01-01T10:00:00',
  };

  beforeEach(() => {
    const outingsApiSpyObj = jasmine.createSpyObj('OutingsService', [
      'getAllOutings',
      'getUpcomingOutings',
      'getOutingById',
    ]);
    const participationsApiSpyObj = jasmine.createSpyObj('ParticipationsService', [
      'joinOuting',
      'leaveOuting1',
    ]);

    TestBed.configureTestingModule({
      providers: [
        OutingService,
        { provide: OutingsService, useValue: outingsApiSpyObj },
        { provide: ParticipationsService, useValue: participationsApiSpyObj },
      ],
    });

    service = TestBed.inject(OutingService);
    outingsApiSpy = TestBed.inject(OutingsService) as jasmine.SpyObj<OutingsService>;
    participationsApiSpy = TestBed.inject(ParticipationsService) as jasmine.SpyObj<ParticipationsService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getOutings', () => {
    it('should load outings and update signal', (done) => {
      outingsApiSpy.getAllOutings.and.returnValue(of(mockOutings));

      service.getOutings().subscribe({
        next: (outings) => {
          expect(outings).toEqual(mockOutings);
          expect(service.outings()).toEqual(mockOutings);
          expect(service.loading()).toBe(false);
          expect(service.error()).toBeNull();
          done();
        },
      });
    });

    it('should handle errors', (done) => {
      outingsApiSpy.getAllOutings.and.returnValue(throwError(() => new Error('API Error')));

      service.getOutings().subscribe({
        next: (outings) => {
          expect(outings).toEqual([]);
          expect(service.error()).toBe('Error loading outings');
          expect(service.loading()).toBe(false);
          done();
        },
      });
    });
  });

  describe('joinOuting - CRITICAL TEST', () => {
    it('should call participationsApi.joinOuting (NOT outingsApi)', (done) => {
      const outingId = '123';
      participationsApiSpy.joinOuting.and.returnValue(of(mockParticipation));

      service.joinOuting(outingId).subscribe({
        next: () => {
          // CRITICAL: Verify it calls the CORRECT API
          expect(participationsApiSpy.joinOuting).toHaveBeenCalledWith(outingId);
          expect(participationsApiSpy.joinOuting).toHaveBeenCalledTimes(1);

          // Ensure it does NOT call the wrong API
          expect(outingsApiSpy['joinOuting1']).toBeUndefined();

          expect(service.loading()).toBe(false);
          expect(service.error()).toBeNull();
          done();
        },
      });
    });

    it('should handle join errors', (done) => {
      const outingId = '123';
      participationsApiSpy.joinOuting.and.returnValue(throwError(() => new Error('Join Error')));

      service.joinOuting(outingId).subscribe({
        error: (err) => {
          expect(service.error()).toBe('Error joining outing');
          expect(service.loading()).toBe(false);
          done();
        },
      });
    });
  });

  describe('leaveOuting', () => {
    it('should call participationsApi.leaveOuting1 with correct participationId', (done) => {
      const participationId = 'part123';
      participationsApiSpy.leaveOuting1.and.returnValue(of(void 0));

      service.leaveOuting(participationId).subscribe({
        next: () => {
          expect(participationsApiSpy.leaveOuting1).toHaveBeenCalledWith(participationId);
          expect(service.loading()).toBe(false);
          expect(service.error()).toBeNull();
          done();
        },
      });
    });

    it('should handle leave errors', (done) => {
      const participationId = 'part123';
      participationsApiSpy.leaveOuting1.and.returnValue(throwError(() => new Error('Leave Error')));

      service.leaveOuting(participationId).subscribe({
        error: (err) => {
          expect(service.error()).toBe('Error leaving outing');
          expect(service.loading()).toBe(false);
          done();
        },
      });
    });
  });

  describe('getUpcomingOutings', () => {
    it('should load upcoming outings', (done) => {
      outingsApiSpy.getUpcomingOutings.and.returnValue(of(mockOutings));

      service.getUpcomingOutings().subscribe({
        next: (outings) => {
          expect(outings).toEqual(mockOutings);
          expect(service.outings()).toEqual(mockOutings);
          done();
        },
      });
    });
  });
});
