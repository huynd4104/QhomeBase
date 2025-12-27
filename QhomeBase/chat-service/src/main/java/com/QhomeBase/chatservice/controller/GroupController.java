package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Group Chat", description = "Group chat management APIs")
public class GroupController {

    private final GroupService groupService;
    private final com.QhomeBase.chatservice.service.GroupFileService groupFileService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Create a new group", description = "Create a new chat group with optional initial members")
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        GroupResponse response = groupService.createGroup(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get group by ID", description = "Get detailed information about a specific group including all members. All group members (ADMIN, MODERATOR, MEMBER) can view the member list.")
    public ResponseEntity<GroupResponse> getGroupById(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        GroupResponse response = groupService.getGroupById(groupId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my groups", description = "Get paginated list of groups the user is a member of")
    public ResponseEntity<GroupPagedResponse> getMyGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        GroupPagedResponse response = groupService.getGroupsByResident(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update group", description = "Update group information (name, description, avatar). Only admins can update.")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        GroupResponse response = groupService.updateGroup(groupId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Add members to group", description = "Add new members to the group. Only admins and moderators can add members.")
    public ResponseEntity<Void> addMembers(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMembersRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        groupService.addMembers(groupId, request, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Remove member from group", description = "Remove a member from the group. Admins can remove anyone, members can only remove themselves.")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        groupService.removeMember(groupId, memberId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/leave")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Leave group", description = "Leave the group. Cannot leave if you're the last admin.")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Delete group", description = "Delete the group. Only the group creator can delete the group.")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        groupService.deleteGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/files")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get group files", description = "Get paginated list of all files sent in the group, sorted by createdAt descending")
    public ResponseEntity<com.QhomeBase.chatservice.dto.GroupFilePagedResponse> getGroupFiles(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        com.QhomeBase.chatservice.dto.GroupFilePagedResponse response = 
            groupFileService.getGroupFiles(groupId, userId, page, size);
        return ResponseEntity.ok(response);
    }
}

