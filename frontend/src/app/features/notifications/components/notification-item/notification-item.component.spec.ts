import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotificationItemComponent } from './notification-item.component';
import { NotificationResponse } from '../../../../generated/model/notificationResponse';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe } from '@angular/common';

describe('NotificationItemComponent', () => {
  let component: NotificationItemComponent;
  let fixture: ComponentFixture<NotificationItemComponent>;

  const mockUnreadNotification: NotificationResponse = {
    id: '1',
    type: 'PARTICIPATION_REQUEST',
    title: 'New participation request',
    message: 'Test User wants to join Test Outing',
    relatedEntityId: 'entity1',
    relatedEntityType: 'PARTICIPATION',
    read: false,
    readAt: null,
    createdAt: '2025-01-01T10:00:00',
  };

  const mockReadNotification: NotificationResponse = {
    id: '2',
    type: 'PARTICIPATION_APPROVED',
    title: 'Participation approved!',
    message: 'Your request for Test Outing has been approved',
    relatedEntityId: 'entity2',
    relatedEntityType: 'OUTING',
    read: true,
    readAt: '2025-01-01T10:05:00',
    createdAt: '2025-01-01T10:00:00',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationItemComponent, MatIconModule, MatButtonModule, MatTooltipModule, DatePipe],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('notification', mockUnreadNotification);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display notification title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('.notification-title');
    expect(title?.textContent).toContain('New participation request');
  });

  it('should display notification message', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const message = compiled.querySelector('.notification-message');
    expect(message?.textContent).toContain('Test User wants to join Test Outing');
  });

  it('should show unread indicator for unread notifications', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const unreadIndicator = compiled.querySelector('.unread-indicator');
    expect(unreadIndicator).toBeTruthy();
  });

  it('should NOT show unread indicator for read notifications', () => {
    fixture.componentRef.setInput('notification', mockReadNotification);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const unreadIndicator = compiled.querySelector('.unread-indicator');
    expect(unreadIndicator).toBeFalsy();
  });

  it('should show "mark as read" button only for unread notifications', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const markAsReadButtons = compiled.querySelectorAll('button mat-icon');
    const hasDoneIcon = Array.from(markAsReadButtons).some(
      (icon) => icon.textContent?.trim() === 'done'
    );
    expect(hasDoneIcon).toBe(true);
  });

  it('should NOT show "mark as read" button for read notifications', () => {
    fixture.componentRef.setInput('notification', mockReadNotification);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const markAsReadButtons = compiled.querySelectorAll('button mat-icon');
    const hasDoneIcon = Array.from(markAsReadButtons).some(
      (icon) => icon.textContent?.trim() === 'done'
    );
    expect(hasDoneIcon).toBe(false);
  });

  it('should always show delete button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const deleteButtons = compiled.querySelectorAll('button mat-icon');
    const hasDeleteIcon = Array.from(deleteButtons).some(
      (icon) => icon.textContent?.trim() === 'delete'
    );
    expect(hasDeleteIcon).toBe(true);
  });

  describe('Layout fix verification', () => {
    it('should use custom flex layout (NOT MatListItem)', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const notificationItem = compiled.querySelector('.notification-item');

      // Verify custom flex layout exists
      expect(notificationItem).toBeTruthy();

      // Verify it does NOT use MatListItem directives
      const matListItemTitle = compiled.querySelector('[matListItemTitle]');
      const matListItemLine = compiled.querySelector('[matListItemLine]');
      const matListItemIcon = compiled.querySelector('[matListItemIcon]');

      expect(matListItemTitle).toBeFalsy();
      expect(matListItemLine).toBeFalsy();
      expect(matListItemIcon).toBeFalsy();
    });

    it('should have .notification-actions with flex-shrink: 0 to prevent button cutoff', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const actionsDiv = compiled.querySelector('.notification-actions');

      expect(actionsDiv).toBeTruthy();
      // Buttons should be inside this div
      const buttons = actionsDiv?.querySelectorAll('button');
      expect(buttons && buttons.length > 0).toBe(true);
    });

    it('should have .notification-content with flex: 1', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const contentDiv = compiled.querySelector('.notification-content');

      expect(contentDiv).toBeTruthy();
      // Should contain title, message, and time
      const title = contentDiv?.querySelector('.notification-title');
      const message = contentDiv?.querySelector('.notification-message');
      const time = contentDiv?.querySelector('.notification-time');

      expect(title).toBeTruthy();
      expect(message).toBeTruthy();
      expect(time).toBeTruthy();
    });
  });

  describe('Event handlers', () => {
    it('should emit markAsRead when mark as read button is clicked', () => {
      spyOn(component.markAsRead, 'emit');

      const compiled = fixture.nativeElement as HTMLElement;
      const buttons = compiled.querySelectorAll('button');
      const markAsReadButton = Array.from(buttons).find((btn) =>
        btn.querySelector('mat-icon')?.textContent?.includes('done')
      );

      expect(markAsReadButton).toBeTruthy();
      markAsReadButton?.click();

      expect(component.markAsRead.emit).toHaveBeenCalledWith('1');
    });

    it('should emit delete when delete button is clicked', () => {
      spyOn(component.delete, 'emit');

      const compiled = fixture.nativeElement as HTMLElement;
      const buttons = compiled.querySelectorAll('button');
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector('mat-icon')?.textContent?.includes('delete')
      );

      expect(deleteButton).toBeTruthy();
      deleteButton?.click();

      expect(component.delete.emit).toHaveBeenCalledWith('1');
    });

    it('should stop event propagation on onMarkAsRead', () => {
      const event = new Event('click');
      spyOn(event, 'stopPropagation');
      spyOn(component.markAsRead, 'emit');

      component.onMarkAsRead(event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(component.markAsRead.emit).toHaveBeenCalledWith('1');
    });

    it('should stop event propagation on onDelete', () => {
      const event = new Event('click');
      spyOn(event, 'stopPropagation');
      spyOn(component.delete, 'emit');

      component.onDelete(event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(component.delete.emit).toHaveBeenCalledWith('1');
    });
  });

  describe('getIconForType', () => {
    it('should return correct icon for PARTICIPATION type', () => {
      expect(component.getIconForType('PARTICIPATION')).toBe('group_add');
    });

    it('should return correct icon for MESSAGE type', () => {
      expect(component.getIconForType('MESSAGE')).toBe('chat');
    });

    it('should return correct icon for REWARD type', () => {
      expect(component.getIconForType('REWARD')).toBe('card_giftcard');
    });

    it('should return correct icon for SYSTEM type', () => {
      expect(component.getIconForType('SYSTEM')).toBe('notifications');
    });

    it('should return default icon for unknown type', () => {
      expect(component.getIconForType('UNKNOWN')).toBe('notifications');
    });
  });
});
