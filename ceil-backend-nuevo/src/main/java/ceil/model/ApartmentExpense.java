package ceil.model;

public class ApartmentExpense {
    public int id;
    public String emoji;
    public String name;
    public double amount;

    // Constructor vacío (necesario para Javalin/JSON)
    public ApartmentExpense() {}

    // Constructor con los 4 argumentos
    public ApartmentExpense(int id, String emoji, String name, double amount) {
        this.id = id;
        this.emoji = emoji;
        this.name = name;
        this.amount = amount;
    }
}