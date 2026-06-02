/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model;

public interface Transactable {
    void deposit(double amount);
    void withdraw(double amount) throws IllegalArgumentException;
    void transfer(double amount, Account target) throws IllegalArgumentException;
}