import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';
import { ChatMessageResponse } from '../../../generated/model/chatMessageResponse';

/**
 * WebSocket service for real-time chat using STOMP protocol
 * Connects to Spring Boot WebSocket endpoint
 */
@Injectable({
  providedIn: 'root',
})
export class WebSocketChatService {
  private stompClient: Client | null = null;
  private messageSubject = new BehaviorSubject<ChatMessageResponse | null>(null);
  private typingSubject = new BehaviorSubject<{ userId: string; isTyping: boolean } | null>(null);
  private connectionReadySubject = new BehaviorSubject<boolean>(false);

  // Signals for reactive state management
  connected = signal(false);
  connecting = signal(false);
  error = signal<string | null>(null);

  // Observables for message streams
  messages$: Observable<ChatMessageResponse | null> = this.messageSubject.asObservable();
  typing$: Observable<{ userId: string; isTyping: boolean } | null> =
    this.typingSubject.asObservable();

  // Observable for connection state - emit when WebSocket is ready for subscriptions
  connectionReady$: Observable<boolean> = this.connectionReadySubject.asObservable();

  /**
   * Connect to WebSocket server
   * @param token JWT token for authentication
   */
  connect(token: string): void {
    if (this.connected() || this.connecting()) {
      console.log('WebSocket already connected or connecting');
      return;
    }

    this.connecting.set(true);
    this.error.set(null);

    const socketFactory = () => {
      return new SockJS('http://localhost:8080/ws');
    };

    const stompConfig: StompConfig = {
      webSocketFactory: socketFactory,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str: string) => {
        console.log('STOMP Debug:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    };

    this.stompClient = new Client(stompConfig);

    this.stompClient.onConnect = (frame) => {
      console.log('WebSocket connected:', frame);
      this.connected.set(true);
      this.connecting.set(false);
      this.error.set(null);

      // Emit that connection is ready for subscriptions
      this.connectionReadySubject.next(true);
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      this.error.set('WebSocket connection error');
      this.connected.set(false);
      this.connecting.set(false);
    };

    this.stompClient.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
      this.error.set('WebSocket connection failed');
      this.connected.set(false);
      this.connecting.set(false);
    };

    this.stompClient.onDisconnect = () => {
      console.log('WebSocket disconnected');
      this.connected.set(false);
      this.connecting.set(false);

      // Emit that connection is no longer ready
      this.connectionReadySubject.next(false);
    };

    this.stompClient.activate();
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connected.set(false);
      this.connecting.set(false);
      this.connectionReadySubject.next(false);
    }
  }

  /**
   * Subscribe to chat messages for a specific outing
   * @param chatId Chat ID to subscribe to
   */
  subscribeToChat(chatId: string): void {
    if (!this.stompClient || !this.connected()) {
      console.error('Cannot subscribe: WebSocket not connected');
      return;
    }

    console.log(`[WebSocket] Subscribing to /topic/chat/${chatId}`);

    // Subscribe to chat messages
    const subscription = this.stompClient.subscribe(`/topic/chat/${chatId}`, (message: IMessage) => {
      console.log('[WebSocket] Message received on topic:', message);
      const chatMessage: ChatMessageResponse = JSON.parse(message.body);
      console.log('[WebSocket] Parsed message:', chatMessage);
      this.messageSubject.next(chatMessage);
    });

    console.log('[WebSocket] Subscription created:', subscription);

    // Subscribe to typing indicators
    this.stompClient.subscribe(`/topic/chat/${chatId}/typing`, (message: IMessage) => {
      console.log('[WebSocket] Typing indicator received:', message);
      const typingData = JSON.parse(message.body);
      this.typingSubject.next(typingData);
    });

    console.log(`[WebSocket] Successfully subscribed to chat ${chatId}`);
  }

  /**
   * Unsubscribe from a chat
   * @param chatId Chat ID to unsubscribe from
   */
  unsubscribeFromChat(chatId: string): void {
    if (!this.stompClient) {
      return;
    }
    // STOMP client handles unsubscribe automatically when subscription is disposed
    console.log(`Unsubscribed from chat ${chatId}`);
  }

  /**
   * Send a message to the chat
   * @param outingId Outing ID
   * @param content Message content
   */
  sendMessage(outingId: string, content: string): void {
    if (!this.stompClient || !this.connected()) {
      console.error('Cannot send message: WebSocket not connected');
      this.error.set('WebSocket not connected');
      return;
    }

    const messagePayload = {
      content: content,
    };

    console.log(`[WebSocket] Sending message to /app/chat/${outingId}/send:`, messagePayload);

    this.stompClient.publish({
      destination: `/app/chat/${outingId}/send`,
      body: JSON.stringify(messagePayload),
    });

    console.log('[WebSocket] Message sent successfully');
  }

  /**
   * Send typing indicator
   * @param outingId Outing ID
   * @param isTyping Whether user is typing
   */
  sendTypingIndicator(outingId: string, isTyping: boolean): void {
    if (!this.stompClient || !this.connected()) {
      return;
    }

    const typingPayload = {
      isTyping: isTyping,
    };

    this.stompClient.publish({
      destination: `/app/chat/${outingId}/typing`,
      body: JSON.stringify(typingPayload),
    });
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.connected();
  }

  /**
   * Get connection error if any
   */
  getError(): string | null {
    return this.error();
  }
}
