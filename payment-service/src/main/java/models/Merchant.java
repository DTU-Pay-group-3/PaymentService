package models;

public class Merchant {
    private String id;
    private int balance;

    public Merchant(String id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public Merchant() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
}
