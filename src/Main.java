import annotations.Log;

class User {
    int id;
    String name;
    String email;

    User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    public Result<User, String> findById(int id) {
        if (id == 1) {
            return Result.ok(new User(1, "Alice", "alice@example.com"));
        }
        return Result.err("User not found");
    }

    public Result<User, String> findByEmail(String email) {
        if (email == null) {
            return Result.err("User not found");
        }
        return Result.ok(new User(1, "Alice", "alice@example.com"));
    }

    @Override
    public String toString() {
        return "{id = " + id + ", name = " + name + ", email = " + email + "}";
    }
}

public class Main {
    @Log
    public static void validateUser() {
        var user = new User(1, "bob", "bob@example.com");
        ff(user);
        user.findByEmail(null);
    }

    private static void ff(User user) {
        user.findById(1);
    }
    
    public static void main(String[] args) throws Exception {
        validateUser();
    }
}