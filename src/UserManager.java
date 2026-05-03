import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

public class UserManager {
    private static final String USERS_FILE = "users.dat";

    // Key = normalized email or phone  →  Value = {displayName, password}
    private Map<String, String[]> users;  // [0]=displayName, [1]=password

    public UserManager() {
        users = new HashMap<>();
        loadUsers();
    }

    /** Register a new user.
     *  @param displayName  shown in player header
     *  @param contact      email OR phone number
     *  @param password     plain text (≥4 chars)
     *  @return error message, or null on success
     */
    public String register(String displayName, String contact, String password) {
        if (displayName == null || displayName.trim().isEmpty())
            return "Name cannot be empty.";
        if (contact == null || contact.trim().isEmpty())
            return "Email or phone cannot be empty.";
        if (password == null || password.length() < 4)
            return "Password must be at least 4 characters.";

        String key = normalizeContact(contact.trim());
        if (key == null)
            return "Enter a valid email address or phone number (digits only, 7–15 chars).";
        if (users.containsKey(key))
            return "This email/phone is already registered.";

        users.put(key, new String[]{displayName.trim(), password});
        saveUsers();
        return null; // success
    }

    /** @return displayName on success, null on failure */
    public String login(String contact, String password) {
        if (contact == null || password == null) return null;
        String key = normalizeContact(contact.trim());
        if (key == null) return null;
        String[] data = users.get(key);
        if (data != null && data[1].equals(password)) return data[0];
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────

    /** Returns a canonical key for an email or phone, or null if invalid. */
    private String normalizeContact(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        // Email pattern
        if (raw.contains("@")) {
            String email = raw.toLowerCase();
            if (Pattern.matches("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$", email)) return email;
            return null;
        }
        // Phone: strip spaces, dashes, plus
        String digits = raw.replaceAll("[\\s\\-+()]", "");
        if (digits.matches("\\d{7,15}")) return "phone:" + digits;
        return null;
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            users = (Map<String, String[]>) ois.readObject();
        } catch (Exception e) {
            users = new HashMap<>();
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
