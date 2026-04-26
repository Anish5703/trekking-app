package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.service.AdminService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/user")
    public ResponseEntity<ApiResponse<List<UserDetails>>> handleGetUserList() {
        ApiResponse<List<UserDetails>> response = adminService.getUserList();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/user")
    public ResponseEntity<ApiResponse<UserDetails>> handleDeleteUser(@RequestParam(name = "id") Integer id) {
        ApiResponse<UserDetails> response = adminService.deleteUser(id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);

    }

}
