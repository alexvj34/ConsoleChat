import java.io.PrintWriter;

public class User {
    private String username;
    private String role;
    private PrintWriter out;

    public User(String username, String role, PrintWriter out) {
        this.username = username;
        this.role = role;
        this.out = out;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void sendMessage(String message) {
        out.println(message);
    }


    public void setRole(String role) {
        this.role = role;
    }
}

