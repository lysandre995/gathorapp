package com.alfano.gathorapp.chat;

import com.alfano.gathorapp.chat.dto.ChatMessageResponse;
import com.alfano.gathorapp.chat.dto.SendMessageRequest;
import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.outing.OutingRepository;
import com.alfano.gathorapp.participation.ParticipationRepository;
import com.alfano.gathorapp.user.User;
import com.alfano.gathorapp.user.UserRepository;
import com.alfano.gathorapp.notification.NotificationService;
import com.alfano.gathorapp.notification.NotificationType;
import com.alfano.gathorapp.participation.Participation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing chats and messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

        private final ChatRepository chatRepository;
        private final ChatMessageRepository chatMessageRepository;
        private final OutingRepository outingRepository;
        private final UserRepository userRepository;
        private final ParticipationRepository participationRepository;
        private final ChatMapper chatMapper;
        private final NotificationService notificationService;

        /**
         * Get or create chat for an outing.
         */
        @Transactional
        public Chat getOrCreateChat(UUID outingId) {
                Outing outing = outingRepository.findById(outingId)
                                .orElseThrow(() -> new RuntimeException("Outing not found with id: " + outingId));

                return chatRepository.findByOuting(outing)
                                .orElseGet(() -> {
                                        Chat chat = Chat.builder()
                                                        .outing(outing)
                                                        .active(true)
                                                        .build();
                                        return chatRepository.save(chat);
                                });
        }

        /**
         * Get all messages for a chat.
         */
        @Transactional(readOnly = true)
        public List<ChatMessageResponse> getMessages(UUID outingId, UUID userId) {
                log.debug("Fetching messages for outing: {}", outingId);

                // Verify user is participant or organizer
                Outing outing = outingRepository.findById(outingId)
                                .orElseThrow(() -> new RuntimeException("Outing not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                boolean isOrganizer = outing.getOrganizer().getId().equals(userId);
                boolean isParticipant = participationRepository.existsByUserAndOuting(user, outing);

                if (!isOrganizer && !isParticipant) {
                        throw new UnauthorizedChatAccessException("Only participants and organizer can view messages");
                }

                // Get or create chat (in case it wasn't created with the outing, e.g., seed
                // data)
                Chat chat = getOrCreateChat(outingId);

                // Get messages
                return chatMessageRepository.findByChatOrderByTimestampAsc(chat)
                                .stream()
                                .map(chatMapper::toMessageResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Send a message to a chat.
         */
        @Transactional
        public ChatMessageResponse sendMessage(UUID outingId, SendMessageRequest request, UUID userId) {
                log.info("User {} sending message to outing {}", userId, outingId);

                // Verify user is participant or organizer
                Outing outing = outingRepository.findById(outingId)
                                .orElseThrow(() -> new RuntimeException("Outing not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                boolean isOrganizer = outing.getOrganizer().getId().equals(userId);
                boolean isParticipant = participationRepository.existsByUserAndOuting(user, outing);

                if (!isOrganizer && !isParticipant) {
                        throw new UnauthorizedChatAccessException("Only participants and organizer can send messages");
                }

                // Get or create chat
                Chat chat = getOrCreateChat(outingId);

                // Check if chat is active
                if (!chat.getActive()) {
                        throw new InactiveChatException("This chat has been deactivated");
                }

                // Create message
                ChatMessage message = ChatMessage.builder()
                                .chat(chat)
                                .sender(user)
                                .content(request.getContent())
                                .build();

                ChatMessage savedMessage = chatMessageRepository.save(message);
                log.info("Message sent successfully");

                // Send notifications to all participants (except sender)
                notifyParticipants(outing, user, savedMessage);

                return chatMapper.toMessageResponse(savedMessage);
        }

        // Add this new private method to the class:
        /**
         * Notify all participants of a new message (except the sender).
         */
        private void notifyParticipants(Outing outing, User sender, ChatMessage message) {
                // Get all approved participants
                List<Participation> participants = participationRepository.findApprovedByOuting(outing);

                // Notify each participant (except sender)
                for (Participation participation : participants) {
                        UUID participantId = participation.getUser().getId();

                        if (!participantId.equals(sender.getId())) {
                                notificationService.createNotification(
                                                participantId,
                                                NotificationType.NEW_MESSAGE,
                                                "New message in " + outing.getTitle(),
                                                sender.getName() + ": " + message.getContent(),
                                                outing.getId(),
                                                "OUTING");
                        }
                }

                // Also notify organizer if they're not the sender
                if (!outing.getOrganizer().getId().equals(sender.getId())) {
                        notificationService.createNotification(
                                        outing.getOrganizer().getId(),
                                        NotificationType.NEW_MESSAGE,
                                        "New message in " + outing.getTitle(),
                                        sender.getName() + ": " + message.getContent(),
                                        outing.getId(),
                                        "OUTING");
                }
        }
}
