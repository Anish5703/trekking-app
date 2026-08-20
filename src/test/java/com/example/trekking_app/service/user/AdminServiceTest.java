package com.example.trekking_app.service.user;

import com.example.trekking_app.dto.admin.AccountStatusResetRequest;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.EmailNotVerifiedException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.exception.user.DeleteUserFailedException;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.repository.TokenRepository;
import com.example.trekking_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @InjectMocks
    private  AdminService adminService;

    @Mock
    private UserRepository userRepo;
    @Mock
    private TokenRepository tokenRepo;

    private User adminUser;
    private
     User customerUser;

    @BeforeEach()
    void setUp() {
         customerUser= new User();
         customerUser.setId(1);
         customerUser.setEmail("customer@email.com");
         customerUser.setRole(Role.CUSTOMER);
         customerUser.setEmailVerified(true);
         customerUser.setActive(true);

        adminUser = new User();
        adminUser.setId(2);
        adminUser.setEmail("admin@email.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setEmailVerified(true);
        adminUser.setActive(false);

    }


    //getUsers, deleteUser,getInactiveUsers,getActiveUsers,updateAccountStatus

    @Test
    void getUsers_shouldReturnPagedUserDetails(){
       List<User> userList = Collections.singletonList(customerUser);
       Page<User> userPage = new PageImpl<>(userList);

       when(userRepo.findByRoleOrderByTimeStampDesc(eq(Role.CUSTOMER),any(Pageable.class))).thenReturn(userPage);


        ApiResponse<Page<UserDetails>> response = adminService.getUsers(Role.CUSTOMER,0,10);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("List of users retrieved", response.getMessage());
        assertEquals(1, response.getData().getTotalElements());
        assertEquals("customer@email.com", response.getData().getContent().get(0).getEmail());
    }

    @Test
    void getInactiveUsersSuccess(){
        List<User> userList = Collections.singletonList(adminUser);
        Page<User> userPage = new PageImpl<>(userList);

        when(userRepo.findByRoleAndIsActiveFalseOrderByTimeStampDesc(eq(Role.ADMIN),any(Pageable.class))).thenReturn(userPage);

        ApiResponse<Page<UserDetails>> response = adminService.getInactiveUsers(Role.ADMIN,0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("inactive ADMIN fetched",response.getMessage());

    }

    @Test
    void testActiveUsers_Valid() {
        List<User> userList = Collections.singletonList(customerUser);
        Page<User> userPage = new PageImpl<>(userList);

        when(userRepo.findByRoleAndIsActiveTrueOrderByTimeStampDesc(eq(Role.CUSTOMER),any(Pageable.class))).thenReturn(userPage);

        ApiResponse<Page<UserDetails>> response = adminService.getActiveUsers(Role.CUSTOMER,0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("active CUSTOMER fetched",response.getMessage());

        }

    @Test
    void deleteUser_shouldDeleteUserAndToken() {
        Integer userId = 1;

        User user = new User();
        user.setId(userId);

        Token token = new Token();

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepo.findByUser_Id(userId)).thenReturn(Optional.of(token));

        ApiResponse<UserDetails> response = adminService.deleteUser(userId);

        assertEquals(200, response.getStatus());
        assertEquals("User removed", response.getMessage());

        verify(tokenRepo).delete(token);
        verify(userRepo).delete(user);
    }

    @Test
    void shouldThrowDeleteUserFailedException(){
        when(userRepo.findById(1)).thenReturn(Optional.empty());
        assertThrows(DeleteUserFailedException.class,()->adminService.deleteUser(1));
    }


    @Test
    void testUpdateAccountStatus_SuperAdminBlock(){
        //should throw resourceupdatefailed exception
        User superAdmin = new User();
        superAdmin.setId(1);
        superAdmin.setEmail("admin@admin.com");

        AccountStatusResetRequest request = new AccountStatusResetRequest();
        request.setUserId(superAdmin.getId());
        request.setActive(true);

        when(userRepo.findById(request.getUserId())).thenReturn(Optional.of(superAdmin));

        assertThrows(ResourceUpdateFailedException.class
                ,()->adminService.updateAccountStatus(request));

    }

    @Test
    void testUpdateAccountStatus_EmailNotVerified(){
        User admin = new User();
        admin.setId(23);
        admin.setEmail("admin@email.com");
        admin.setEmailVerified(false);

        AccountStatusResetRequest request = new AccountStatusResetRequest();
        request.setUserId(admin.getId());
        request.setActive(false);

        when(userRepo.findById(request.getUserId())).thenReturn(Optional.of(admin));
        assertThrows(EmailNotVerifiedException.class,()->adminService.updateAccountStatus(request));

    }

    @Test
    void testUpdateAccountStatus_Success(){
        User admin = new User();
        admin.setId(21);
        admin.setEmail("admin@email.com");
        admin.setEmailVerified(true);
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        AccountStatusResetRequest request = new AccountStatusResetRequest();
        request.setUserId(admin.getId());
        request.setActive(true);

        when(userRepo.findById(request.getUserId())).thenReturn(Optional.of(admin));
        when(userRepo.save(any(User.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<Void> response = adminService.updateAccountStatus(request);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("admin account is activated",response.getMessage());
    }
}
