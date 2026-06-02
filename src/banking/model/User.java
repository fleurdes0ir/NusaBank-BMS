/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

import banking.model.enums.UserRole;

public class User {
    private String userId;
    private String username;
    private String passwordHash;
    private UserRole role;
    private String customerId;

    public User(String userId, String username, String passwordHash,
                UserRole role, String customerId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customerId = customerId;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public String getCustomerId() { return customerId; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @Override
    public String toString() {
        return userId + "," + username + "," + passwordHash + ","
             + role.name() + "," + customerId;
    }
}