package com.team04.domain.workspace.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.milestone.repository.FundUsageRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.workspace.dto.request.WorkspaceMessageRequest;
import com.team04.domain.workspace.dto.response.WorkspaceMessageResponse;
import com.team04.domain.workspace.dto.response.WorkspaceResponse;
import com.team04.domain.workspace.entity.WorkspaceMessage;
import com.team04.domain.workspace.repository.WorkspaceMessageRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceParticipantValidator participantValidator;
    private final WorkspaceMessageRepository workspaceMessageRepository;
    private final FundUsageRepository fundUsageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(Long workspaceId, Long userId, Role role) {
        participantValidator.validateParticipant(workspaceId, userId, role);

        Idea idea = participantValidator.getIdeaOrThrow(workspaceId);
        User creator = getUserOrThrow(idea.getUserId());

        return WorkspaceResponse.of(idea, creator, userId);
    }

    @Transactional
    public WorkspaceMessageResponse sendMessage(Long workspaceId, Long userId, Role role, WorkspaceMessageRequest request) {
        participantValidator.validateParticipant(workspaceId, userId, role);

        WorkspaceMessage message = workspaceMessageRepository.save(
                WorkspaceMessage.create(workspaceId, userId, request.content())
        );
        User author = getUserOrThrow(userId);

        return WorkspaceMessageResponse.from(message, author);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMessageResponse> getMessages(Long workspaceId, Long userId, Role role) {
        participantValidator.validateParticipant(workspaceId, userId, role);

        List<WorkspaceMessage> messages = workspaceMessageRepository.findByIdeaIdOrderByCreatedAtAsc(workspaceId);
        Map<Long, User> authors = loadAuthors(messages);

        return messages.stream()
                .map(message -> WorkspaceMessageResponse.from(message, authors.get(message.getAuthorId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FundUsageResponse> getFundUsages(Long workspaceId, Long userId, Role role) {
        participantValidator.validateParticipant(workspaceId, userId, role);

        return fundUsageRepository.findByIdeaIdOrderByUsedAtDesc(workspaceId).stream()
                .map(FundUsageResponse::from)
                .toList();
    }

    private Map<Long, User> loadAuthors(List<WorkspaceMessage> messages) {
        Set<Long> authorIds = messages.stream()
                .map(WorkspaceMessage::getAuthorId)
                .collect(Collectors.toSet());

        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
