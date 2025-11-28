import { Component, OnInit, OnDestroy, inject, signal, effect } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';

import { WebSocketChatService } from '../../services/websocket-chat.service';
import { ChatService } from '../../../../generated/api/chat.service';
import { ChatMessageResponse } from '../../../../generated/model/chatMessageResponse';
import { ChatInfoResponse } from '../../../../generated/model/chatInfoResponse';
import { AuthService } from '../../../../core/auth/services/auth.service';

/**
 * Chat component for real-time messaging in outings
 * Uses WebSocket for real-time updates and REST API for message history
 */
@Component({
  selector: 'app-chat-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  template: `
    <div class="chat-container">
      <mat-card class="chat-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>chat</mat-icon>
            Outing Chat
          </mat-card-title>
          <mat-card-subtitle>
            @if (wsService.connected()) {
            <mat-chip class="status-chip connected">
              <mat-icon>wifi</mat-icon>
              Connected
            </mat-chip>
            } @else if (wsService.connecting()) {
            <mat-chip class="status-chip connecting">
              <mat-icon>sync</mat-icon>
              Connecting...
            </mat-chip>
            } @else {
            <mat-chip class="status-chip disconnected">
              <mat-icon>wifi_off</mat-icon>
              Disconnected
            </mat-chip>
            }
          </mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <!-- Loading state -->
          @if (loading()) {
          <div class="loading-container">
            <mat-spinner></mat-spinner>
            <p>Loading messages...</p>
          </div>
          }

          <!-- Error state -->
          @if (error()) {
          <div class="error-container">
            <mat-icon>error_outline</mat-icon>
            <p>{{ error() }}</p>
          </div>
          }

          <!-- Messages list -->
          @if (!loading() && !error()) {
          <div class="messages-container" #messagesContainer>
            @if (messages().length === 0) {
            <div class="empty-state">
              <mat-icon>chat_bubble_outline</mat-icon>
              <p>No messages yet. Start the conversation!</p>
            </div>
            } @else { @for (message of messages(); track message.id) {
            <div
              class="message"
              [class.own-message]="message.sender?.id === currentUserId()"
              [class.other-message]="message.sender?.id !== currentUserId()"
            >
              <div class="message-header">
                <span class="sender-name">{{ message.sender?.name || 'Unknown' }}</span>
                <span class="message-time">{{ message.timestamp | date : 'short' }}</span>
              </div>
              <div class="message-content">{{ message.content }}</div>
            </div>
            } }

            <!-- Typing indicator -->
            @if (someoneTyping()) {
            <div class="typing-indicator">
              <mat-icon>edit</mat-icon>
              <span>Someone is typing...</span>
            </div>
            }
          </div>
          }
        </mat-card-content>

        <mat-card-actions class="message-input-container">
          <mat-form-field appearance="outline" class="message-input">
            <mat-label>Type a message</mat-label>
            <input
              matInput
              [(ngModel)]="newMessage"
              (keyup.enter)="sendMessage()"
              (input)="onTyping()"
              placeholder="Type your message..."
              [disabled]="!wsService.connected()"
            />
          </mat-form-field>
          <button
            mat-fab
            color="primary"
            (click)="sendMessage()"
            [disabled]="!newMessage.trim() || !wsService.connected()"
            aria-label="Send message"
          >
            <mat-icon>send</mat-icon>
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .chat-container {
        max-width: 900px;
        margin: 24px auto;
        padding: 24px;
        height: calc(100vh - 100px);
      }

      .chat-card {
        height: 100%;
        display: flex;
        flex-direction: column;

        mat-card-header {
          mat-card-title {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 24px;
          }

          mat-card-subtitle {
            margin-top: 8px;
          }
        }

        mat-card-content {
          flex: 1;
          overflow: hidden;
          padding: 16px;
        }

        mat-card-actions {
          border-top: 1px solid rgba(0, 0, 0, 0.12);
          padding: 16px;
        }
      }

      .status-chip {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;

        &.connected {
          background-color: #4caf50;
          color: white;
        }

        &.connecting {
          background-color: #ff9800;
          color: white;
        }

        &.disconnected {
          background-color: #f44336;
          color: white;
        }

        mat-icon {
          font-size: 16px;
          width: 16px;
          height: 16px;
        }
      }

      .loading-container,
      .error-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        gap: 16px;
      }

      .messages-container {
        height: 100%;
        overflow-y: auto;
        padding: 16px;
        background-color: #f5f5f5;
        border-radius: 8px;
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: #999;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          margin-bottom: 16px;
        }
      }

      .message {
        margin-bottom: 16px;
        padding: 12px;
        border-radius: 12px;
        max-width: 70%;
        animation: slideIn 0.3s ease-out;

        &.own-message {
          margin-left: auto;
          background-color: #1976d2;
          color: white;

          .message-header {
            .sender-name {
              color: rgba(255, 255, 255, 0.9);
            }

            .message-time {
              color: rgba(255, 255, 255, 0.7);
            }
          }
        }

        &.other-message {
          margin-right: auto;
          background-color: white;
          color: #333;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

          .message-header {
            .sender-name {
              color: #1976d2;
            }

            .message-time {
              color: #999;
            }
          }
        }
      }

      .message-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        font-size: 12px;

        .sender-name {
          font-weight: 600;
        }

        .message-time {
          font-size: 11px;
        }
      }

      .message-content {
        word-wrap: break-word;
        line-height: 1.5;
      }

      .typing-indicator {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #666;
        font-style: italic;
        font-size: 14px;
        animation: fadeIn 0.3s ease-in;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .message-input-container {
        display: flex;
        align-items: center;
        gap: 12px;

        .message-input {
          flex: 1;
        }
      }

      @keyframes slideIn {
        from {
          opacity: 0;
          transform: translateY(10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      @media (max-width: 768px) {
        .chat-container {
          padding: 16px;
        }

        .message {
          max-width: 85%;
        }
      }
    `,
  ],
})
export class ChatViewComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  wsService = inject(WebSocketChatService);
  private chatApi = inject(ChatService);
  private authService = inject(AuthService);

  outingId = signal<string>('');
  chatId = signal<string>('');
  currentUserId = signal<string>(this.authService.currentUser()?.id || '');
  messages = signal<ChatMessageResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  someoneTyping = signal(false);

  newMessage = '';
  private typingTimeout: any;
  private connectionSubscription: any;

  constructor() {
    // Effect to scroll to bottom when new messages arrive
    effect(() => {
      const msgList = this.messages();
      if (msgList.length > 0) {
        setTimeout(() => this.scrollToBottom(), 100);
      }
    });
  }

  ngOnInit() {
    // Get outing ID from route
    this.route.params.subscribe((params) => {
      if (params['outingId']) {
        this.outingId.set(params['outingId']);
        this.initializeChat();
      }
    });

    // Subscribe to WebSocket messages
    this.wsService.messages$.subscribe((message) => {
      if (message) {
        this.messages.update((msgs) => [...msgs, message]);
      }
    });

    // Subscribe to typing indicators
    this.wsService.typing$.subscribe((typingData) => {
      if (typingData && typingData.userId !== this.currentUserId()) {
        this.someoneTyping.set(typingData.isTyping);
      }
    });
  }

  ngOnDestroy() {
    // Unsubscribe from connection events
    if (this.connectionSubscription) {
      this.connectionSubscription.unsubscribe();
    }

    // Unsubscribe from chat and disconnect WebSocket
    if (this.chatId()) {
      this.wsService.unsubscribeFromChat(this.chatId());
    }
    this.wsService.disconnect();
  }

  /**
   * Initialize chat: load messages and connect WebSocket
   * Uses event-driven approach - subscribes when connection is ready
   */
  private initializeChat() {
    this.loading.set(true);
    this.error.set(null);

    // First, get the chat info to obtain the correct chatId
    this.chatApi.getChatInfo(this.outingId()).subscribe({
      next: (chatInfo: ChatInfoResponse) => {
        console.log('Chat info received:', chatInfo);

        // Set the chatId from the server response
        if (chatInfo.chatId) {
          this.chatId.set(chatInfo.chatId);
          console.log('Chat ID set to:', chatInfo.chatId);
        }

        // EVENT-DRIVEN: Subscribe to connection ready event
        const token = this.getAuthToken();
        if (token) {
          // Listen for connection ready event
          this.connectionSubscription = this.wsService.connectionReady$.subscribe((isReady) => {
            if (isReady && this.chatId()) {
              console.log('Connection ready! Subscribing to chat with ID:', this.chatId());
              this.wsService.subscribeToChat(this.chatId());
            }
          });

          // Connect after setting up the subscription listener
          this.wsService.connect(token);
        }

        // Load message history
        this.chatApi.getMessages(this.outingId()).subscribe({
          next: (messages: ChatMessageResponse[]) => {
            console.log('Messages loaded:', messages.length);
            this.messages.set(messages);
            this.loading.set(false);
          },
          error: (err: any) => {
            console.error('Error loading messages:', err);
            this.error.set('Failed to load messages');
            this.loading.set(false);
          },
        });
      },
      error: (err: any) => {
        console.error('Error getting chat info:', err);
        this.error.set('Failed to initialize chat');
        this.loading.set(false);
      },
    });
  }

  /**
   * Send a message
   */
  sendMessage() {
    if (!this.newMessage.trim() || !this.wsService.isConnected()) {
      return;
    }

    this.wsService.sendMessage(this.outingId(), this.newMessage);
    this.newMessage = '';
    this.wsService.sendTypingIndicator(this.outingId(), false);
  }

  /**
   * Handle typing event
   */
  onTyping() {
    if (!this.wsService.isConnected()) {
      return;
    }

    this.wsService.sendTypingIndicator(this.outingId(), true);

    // Clear previous timeout
    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }

    // Set timeout to stop typing indicator after 2 seconds of inactivity
    this.typingTimeout = setTimeout(() => {
      this.wsService.sendTypingIndicator(this.outingId(), false);
    }, 2000);
  }

  /**
   * Scroll to bottom of messages
   */
  private scrollToBottom() {
    const container = document.querySelector('.messages-container');
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  }

  /**
   * Get JWT token from storage
   * TODO: Replace with actual auth service
   */
  private getAuthToken(): string | null {
    return localStorage.getItem('access_token');
  }
}
