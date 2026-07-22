package com.expensetracker.service;

import com.expensetracker.dto.SettlementRequest;
import com.expensetracker.dto.SettlementResponse;
import com.expensetracker.entity.Group;
import com.expensetracker.entity.Settlement;
import com.expensetracker.entity.User;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.SettlementRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    @Transactional
    public SettlementResponse recordSettlement(Long requesterId, Long groupId, SettlementRequest request) {
        Group group = groupService.requireGroup(groupId);
        groupService.requireMember(groupId, requesterId);

        if (request.paidByUserId().equals(request.paidToUserId())) {
            throw new BadRequestException("paidByUserId and paidToUserId must be different users");
        }
        if (!groupService.isMemberSilent(groupId, request.paidByUserId())
                || !groupService.isMemberSilent(groupId, request.paidToUserId())) {
            throw new BadRequestException("Both users must be members of this group");
        }

        User paidBy = userRepository.findById(request.paidByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User paidTo = userRepository.findById(request.paidToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Settlement settlement = Settlement.builder()
                .group(group)
                .paidBy(paidBy)
                .paidTo(paidTo)
                .amount(request.amount())
                .note(request.note())
                .build();

        settlement = settlementRepository.save(settlement);
        return toResponse(settlement);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> listForGroup(Long requesterId, Long groupId) {
        groupService.requireMember(groupId, requesterId);
        return settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SettlementResponse toResponse(Settlement s) {
        return new SettlementResponse(
                s.getId(), s.getGroup().getId(),
                s.getPaidBy().getId(), s.getPaidBy().getName(),
                s.getPaidTo().getId(), s.getPaidTo().getName(),
                s.getAmount(), s.getSettledAt(), s.getNote()
        );
    }
}
