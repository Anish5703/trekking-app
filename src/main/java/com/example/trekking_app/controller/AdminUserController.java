package com.example.trekking_app.controller;

import com.example.trekking_app.dto.admin.AccountStatusResetRequest;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.service.AdminService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<UserDetails>>> handleGetUserList(@RequestParam @NonNull Role role,
                                                                            @RequestParam @NonNull Integer page ,
                                                                            @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<UserDetails>> response = adminService.getUsers(role,page,size);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
    }
    @PutMapping("/status/update")
    public ResponseEntity<ApiResponse<Void>> handleUpdateAccountStatus(@Valid @RequestBody AccountStatusResetRequest approveRequest)
    {
        ApiResponse<Void> response = adminService.updateAccountStatus(approveRequest);
        return ResponseEntity.status(200).body(response);

    }
    @GetMapping("/inactive/list")
    public ResponseEntity<ApiResponse<Page<UserDetails>>> handleGetInactiveUsers(@RequestParam @NonNull Role role,
                                                                                 @RequestParam @NonNull Integer page ,
                                                                                 @RequestParam @NonNull Integer size
                                                                                       )
    {
        ApiResponse<Page<UserDetails>> response = adminService.getInactiveUsers(role,page,size);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<Page<UserDetails>>> handleGetActiveUsers(@RequestParam @NonNull Role role,
                                                                               @RequestParam @NonNull Integer page,
                                                                               @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<UserDetails>> response = adminService.getActiveUsers(role,page,size);
        return ResponseEntity.status(200).body(response);
    }

    /*
    @DeleteMapping("/delete/user")
    public ResponseEntity<ApiResponse<UserDetails>> handleDeleteUser(@RequestParam(name = "id") Integer id) {
        ApiResponse<UserDetails> response = adminService.deleteUser(id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);

    }
     */



}
