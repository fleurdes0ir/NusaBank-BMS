/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package banking.model.enums;

public enum LoanStatus {
    PENDING("Menunggu Persetujuan"),
    ACTIVE("Aktif"),
    REJECTED("Ditolak"),
    PAID("Lunas");

    private final String displayName;

    LoanStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}