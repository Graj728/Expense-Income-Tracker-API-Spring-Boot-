package com.expensetracker.service;

import com.expensetracker.dto.GroupRequest;
import com.expensetracker.dto.GroupResponse;
import com.expensetracker.entity.Group;
import com.expensetracker.entity.GroupMember;
import com.expensetracker.entity.User;
import com.expensetracker.entity.enums.GroupRole;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ForbiddenException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.GroupMemberRepository;
import com.expensetracker.repository.GroupRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse create(Long creatorUserId, GroupRequest request) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Group group = Group.builder()
                .name(request.name())
                .description(request.description())
                .createdBy(creator)
                .build();
        group = groupRepository.save(group);

        GroupMember creatorMembership = GroupMember.builder()
                .group(group)
                .user(creator)
                .role(GroupRole.ADMIN)
                .build();
        groupMemberRepository.save(creatorMembership);

        if (request.memberUserIds() != null) {
            for (Long memberId : request.memberUserIds()) {
                if (memberId.equals(creatorUserId)) continue;
                addMemberInternal(group, memberId);
            }
        }

        return toResponse(groupRepository.findById(group.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listForUser(Long userId) {
        return groupRepository.findAllByMemberUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long userId, Long groupId) {
        Group group = requireGroup(groupId);
        requireMember(groupId, userId);
        return toResponse(group);
    }

    @Transactional
    public GroupResponse addMember(Long requesterId, Long groupId, Long newUserId) {
        Group group = requireGroup(groupId);
        requireMember(groupId, requesterId);

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, newUserId)) {
            throw new BadRequestException("User is already a member of this group");
        }

        addMemberInternal(group, newUserId);
        return toResponse(groupRepository.findById(groupId).orElseThrow());
    }

    @Transactional
    public void removeMember(Long requesterId, Long groupId, Long userIdToRemove) {
        requireGroup(groupId);
        GroupMember requesterMembership = requireMember(groupId, requesterId);

        if (!requesterMembership.getRole().equals(GroupRole.ADMIN) && !requesterId.equals(userIdToRemove)) {
            throw new ForbiddenException("Only a group admin can remove other members");
        }

        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userIdToRemove);
    }

    private void addMemberInternal(Group group, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupRole.MEMBER)
                .build();
        groupMemberRepository.save(member);
    }

    Group requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    GroupMember requireMember(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this group"));
    }

    public boolean isMemberSilent(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }


    private GroupResponse toResponse(Group group) {
        List<GroupResponse.MemberResponse> members = groupMemberRepository.findByGroupId(group.getId()).stream()
                .map(m -> new GroupResponse.MemberResponse(
                        m.getUser().getId(), m.getUser().getName(), m.getUser().getEmail(), m.getRole().name()))
                .toList();

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCreatedBy().getId(),
                members
        );
    }
}
