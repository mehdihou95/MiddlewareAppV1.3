package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.Optional;

/**
 * Service for managing User entities.
 */
public interface UserService extends UserDetailsService {
    /**
     * Get all users with pagination.
     *
     * @param pageable The pagination information
     * @return Page of users
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Get a user by ID.
     *
     * @param id The ID of the user
     * @return Optional containing the user if found
     */
    Optional<User> getUserById(Long id);

    /**
     * Get a user by username.
     *
     * @param username The username
     * @return Optional containing the user if found
     */
    Optional<User> getUserByUsername(String username);

    /**
     * Create a new user.
     *
     * @param user The user to create
     * @return The created user
     */
    User createUser(User user);

    /**
     * Update an existing user.
     *
     * @param id The ID of the user to update
     * @param user The updated user data
     * @return The updated user
     */
    Optional<User> updateUser(Long id, User user);

    /**
     * Delete a user.
     *
     * @param id The ID of the user to delete
     */
    void deleteUser(Long id);

    /**
     * Change a user's password.
     *
     * @param id The ID of the user
     * @param oldPassword The old password
     * @param newPassword The new password
     * @return true if password was changed successfully
     */
    boolean changePassword(Long id, String oldPassword, String newPassword);

    /**
     * Reset a user's password.
     *
     * @param id The ID of the user
     * @param newPassword The new password
     */
    void resetPassword(Long id, String newPassword);

    /**
     * Check if a username is available.
     *
     * @param username The username to check
     * @return true if username is available
     */
    boolean isUsernameAvailable(String username);

    Page<User> getUsers(Pageable pageable, String searchTerm, Boolean enabled);
} 
