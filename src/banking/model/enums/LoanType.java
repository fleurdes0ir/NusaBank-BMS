/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author abinbin0s
 */
package banking.model.enums;

/**
 * LoanType mendefinisikan metode perhitungan bunga pinjaman.
 *
 * FLAT    — bunga dihitung dari pokok awal, cicilan tetap sama setiap bulan.
 *           Rumus: cicilan = (pokok + pokok*rate*tenor) / tenor
 *           Umum dipakai: KTA tanpa agunan, kredit multiguna.
 *
 * ANNUITY — bunga dihitung dari sisa pokok, cicilan tetap tapi
 *           komposisi bunga/pokok berubah tiap bulan.
 *           Rumus: cicilan = pokok * (rate*(1+rate)^tenor) / ((1+rate)^tenor - 1)
 *           Umum dipakai: KPR, KKB, kredit modal kerja.
 */
public enum LoanType {
    FLAT("Flat Rate"),
    ANNUITY("Anuitas");

    private final String displayName;

    LoanType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}