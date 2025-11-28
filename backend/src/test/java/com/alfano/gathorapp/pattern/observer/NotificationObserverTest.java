package com.alfano.gathorapp.pattern.observer;

import com.alfano.gathorapp.notification.Notification;
import com.alfano.gathorapp.notification.NotificationRepository;
import com.alfano.gathorapp.notification.NotificationType;
import com.alfano.gathorapp.user.Role;
import com.alfano.gathorapp.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the Observer Pattern implementation.
 * Tests NotificationManager with focus on multithreading (parallelStream).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationObserver Pattern Tests")
class NotificationObserverTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationManager notificationManager;
    private PersistenceNotificationObserver persistenceObserver;
    private WebSocketNotificationObserver webSocketObserver;

    private NotificationObserver mockObserver1;
    private NotificationObserver mockObserver2;

    private Notification testNotification;
    private User testUser;

    @BeforeEach
    void setUp() {
        notificationManager = new NotificationManager();
        persistenceObserver = new PersistenceNotificationObserver(notificationRepository);
        webSocketObserver = new WebSocketNotificationObserver(messagingTemplate);

        mockObserver1 = mock(NotificationObserver.class);
        mockObserver2 = mock(NotificationObserver.class);
        lenient().when(mockObserver1.getName()).thenReturn("MockObserver1");
        lenient().when(mockObserver2.getName()).thenReturn("MockObserver2");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        testNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(NotificationType.NEW_MESSAGE)
                .title("Test Notification")
                .message("Test message")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ==================== NotificationManager Basic Tests ====================

    @Test
    @DisplayName("Should register observers successfully")
    void testRegisterObserver() {
        notificationManager.registerObserver(mockObserver1);
        notificationManager.registerObserver(mockObserver2);

        assertEquals(2, notificationManager.getObserverCount());
        assertTrue(notificationManager.getObserverNames().contains("MockObserver1"));
        assertTrue(notificationManager.getObserverNames().contains("MockObserver2"));
    }

    @Test
    @DisplayName("Should not register same observer twice")
    void testRegisterSameObserverTwice() {
        notificationManager.registerObserver(mockObserver1);
        notificationManager.registerObserver(mockObserver1);

        assertEquals(1, notificationManager.getObserverCount());
    }

    @Test
    @DisplayName("Should remove observer successfully")
    void testRemoveObserver() {
        notificationManager.registerObserver(mockObserver1);
        notificationManager.registerObserver(mockObserver2);

        notificationManager.removeObserver(mockObserver1);

        assertEquals(1, notificationManager.getObserverCount());
        assertFalse(notificationManager.getObserverNames().contains("MockObserver1"));
        assertTrue(notificationManager.getObserverNames().contains("MockObserver2"));
    }

    @Test
    @DisplayName("Should notify all registered observers")
    void testNotifyAllObservers() throws InterruptedException {
        notificationManager.registerObserver(mockObserver1);
        notificationManager.registerObserver(mockObserver2);

        notificationManager.notifyObservers(testNotification);

        // Wait for parallel processing to complete
        Thread.sleep(100);

        verify(mockObserver1, times(1)).onNotification(testNotification);
        verify(mockObserver2, times(1)).onNotification(testNotification);
    }

    @Test
    @DisplayName("Should continue notifying other observers when one fails")
    void testObserverFailureIsolation() throws InterruptedException {
        notificationManager.registerObserver(mockObserver1);
        notificationManager.registerObserver(mockObserver2);

        // Make first observer throw exception
        doThrow(new RuntimeException("Observer 1 failed"))
                .when(mockObserver1).onNotification(any());

        // Should not throw exception
        assertDoesNotThrow(() -> notificationManager.notifyObservers(testNotification));

        // Wait for parallel processing
        Thread.sleep(100);

        // Second observer should still be notified
        verify(mockObserver2, times(1)).onNotification(testNotification);
    }

    @Test
    @DisplayName("Should handle notification with no observers gracefully")
    void testNotifyWithNoObservers() {
        // Should not throw exception
        assertDoesNotThrow(() -> notificationManager.notifyObservers(testNotification));
    }

    @Test
    @DisplayName("Should return correct observer count")
    void testGetObserverCount() {
        assertEquals(0, notificationManager.getObserverCount());

        notificationManager.registerObserver(mockObserver1);
        assertEquals(1, notificationManager.getObserverCount());

        notificationManager.registerObserver(mockObserver2);
        assertEquals(2, notificationManager.getObserverCount());

        notificationManager.removeObserver(mockObserver1);
        assertEquals(1, notificationManager.getObserverCount());
    }

    @Test
    @DisplayName("Should handle removing non-existent observer")
    void removeObserver_NotExists() {
        notificationManager.registerObserver(mockObserver1);

        notificationManager.removeObserver(mockObserver2);

        assertThat(notificationManager.getObserverCount()).isEqualTo(1);
    }

    // ==================== Multithreading Tests (parallelStream) ====================

    @Test
    @DisplayName("Should handle concurrent observer notifications with parallelStream")
    void notifyObservers_Concurrent_Success() throws InterruptedException {
        // Create 10 custom observers to test parallelStream
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int observerNum = i;
            notificationManager.registerObserver(new NotificationObserver() {
                @Override
                public void onNotification(Notification notification) {
                    callCount.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public String getName() {
                    return "TestObserver" + observerNum;
                }
            });
        }

        notificationManager.notifyObservers(testNotification);

        // Wait for all observers to be notified (with timeout)
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(callCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle high volume notifications in parallel")
    void notifyObservers_HighVolume_Concurrent() throws InterruptedException {
        AtomicInteger processedCount = new AtomicInteger(0);
        int notificationCount = 100;
        CountDownLatch latch = new CountDownLatch(notificationCount);

        notificationManager.registerObserver(new NotificationObserver() {
            @Override
            public void onNotification(Notification notification) {
                processedCount.incrementAndGet();
                latch.countDown();
            }

            @Override
            public String getName() {
                return "HighVolumeObserver";
            }
        });

        // Send 100 notifications
        for (int i = 0; i < notificationCount; i++) {
            notificationManager.notifyObservers(testNotification);
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(processedCount.get()).isEqualTo(notificationCount);
    }

    @Test
    @DisplayName("Should handle thread-safe observer registration during notifications")
    void notifyObservers_ThreadSafeRegistration() throws InterruptedException {
        AtomicInteger notifyCount = new AtomicInteger(0);
        CountDownLatch registrationLatch = new CountDownLatch(5);
        CountDownLatch notificationLatch = new CountDownLatch(1);

        // Start thread that continuously sends notifications
        Thread notificationThread = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                notificationManager.notifyObservers(testNotification);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
            notificationLatch.countDown();
        });

        // Start threads that register observers while notifications are being sent
        List<Thread> registrationThreads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                NotificationObserver observer = new NotificationObserver() {
                    @Override
                    public void onNotification(Notification notification) {
                        notifyCount.incrementAndGet();
                    }

                    @Override
                    public String getName() {
                        return "ConcurrentObserver" + threadNum;
                    }
                };
                notificationManager.registerObserver(observer);
                registrationLatch.countDown();
            });
            registrationThreads.add(thread);
        }

        // Start all threads
        notificationThread.start();
        registrationThreads.forEach(Thread::start);

        // Wait for completion
        boolean registrationComplete = registrationLatch.await(5, TimeUnit.SECONDS);
        boolean notificationComplete = notificationLatch.await(10, TimeUnit.SECONDS);

        assertThat(registrationComplete).isTrue();
        assertThat(notificationComplete).isTrue();
        assertThat(notificationManager.getObserverCount()).isEqualTo(5);
    }

    // ==================== PersistenceNotificationObserver Tests ====================

    @Test
    @DisplayName("PersistenceObserver - Should persist notification to database")
    void persistenceObserver_SavesNotification() {
        persistenceObserver.onNotification(testNotification);

        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    @DisplayName("PersistenceObserver - Should have correct name")
    void persistenceObserver_CorrectName() {
        assertThat(persistenceObserver.getName()).isEqualTo("PersistenceNotificationObserver");
    }

    @Test
    @DisplayName("PersistenceObserver - Should handle save exception gracefully")
    void persistenceObserver_HandlesException() {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw exception
        assertDoesNotThrow(() -> persistenceObserver.onNotification(testNotification));

        verify(notificationRepository, times(1)).save(testNotification);
    }

    // ==================== WebSocketNotificationObserver Tests ====================

    @Test
    @DisplayName("WebSocketObserver - Should send notification via WebSocket")
    void webSocketObserver_SendsNotification() {
        webSocketObserver.onNotification(testNotification);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(testUser.getId().toString()),
                eq("/queue/notifications"),
                any()
        );
    }

    @Test
    @DisplayName("WebSocketObserver - Should have correct name")
    void webSocketObserver_CorrectName() {
        assertThat(webSocketObserver.getName()).isEqualTo("WebSocketNotificationObserver");
    }

    @Test
    @DisplayName("WebSocketObserver - Should handle send exception gracefully")
    void webSocketObserver_HandlesException() {
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSendToUser(any(), any(), any());

        // Should not throw exception
        assertDoesNotThrow(() -> webSocketObserver.onNotification(testNotification));

        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Integration - Should handle complete notification flow")
    void integration_CompleteNotificationFlow() throws InterruptedException {
        notificationManager.registerObserver(persistenceObserver);
        notificationManager.registerObserver(webSocketObserver);

        notificationManager.notifyObservers(testNotification);

        // Wait for parallel processing
        Thread.sleep(100);

        verify(notificationRepository, times(1)).save(testNotification);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(testUser.getId().toString()),
                eq("/queue/notifications"),
                any()
        );
    }

    @Test
    @DisplayName("Integration - Should maintain observer isolation on failures")
    void integration_ObserverIsolation() throws InterruptedException {
        // Persistence will fail
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        notificationManager.registerObserver(persistenceObserver);
        notificationManager.registerObserver(webSocketObserver);

        notificationManager.notifyObservers(testNotification);

        // Wait for parallel processing
        Thread.sleep(100);

        // Both should be called despite persistence failure
        verify(notificationRepository, times(1)).save(any());
        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("Integration - Should handle empty observer list")
    void integration_EmptyObserverList() {
        // Should not throw exception with no observers
        assertDoesNotThrow(() -> notificationManager.notifyObservers(testNotification));
        assertThat(notificationManager.getObserverCount()).isZero();
    }
}
