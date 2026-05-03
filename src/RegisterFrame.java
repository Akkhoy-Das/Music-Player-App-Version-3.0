import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class RegisterFrame extends JFrame {
    private UserManager   userManager = new UserManager();
    private JTextField    nameField;
    private JTextField    contactField;
    private JPasswordField passField;
    private JPasswordField confirmField;
    private JButton        registerBtn;

    private static final Color BG     = new Color(18, 18, 24);
    private static final Color CARD   = new Color(28, 28, 38);
    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color TEXT2  = new Color(150, 150, 170);

    public RegisterFrame() {
        setTitle("MusicWave – Register");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 590);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0,0,420,590,20,20));
        buildUI();
    }

    private void buildUI() {
        // Reuse helper methods from LoginFrame via composition
        LoginFrame helper = new LoginFrame();
        helper.setVisible(false);

        JPanel root = helper.darkPanel(new BorderLayout());
        root.add(helper.titleBar(), BorderLayout.NORTH);

        JPanel card = helper.darkPanel(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24,35,24,35));
        card.setBackground(CARD);

        helper.addLogo(card, "Create your free account");
        card.add(Box.createVerticalStrut(22));

        card.add(helper.lbl("Full Name"));
        card.add(Box.createVerticalStrut(6));
        nameField = helper.textField("Your display name");
        card.add(nameField);
        card.add(Box.createVerticalStrut(12));

        card.add(helper.lbl("Email or Phone Number"));
        card.add(Box.createVerticalStrut(6));
        contactField = helper.textField("e.g. user@email.com or 01XXXXXXXXX");
        card.add(contactField);
        card.add(Box.createVerticalStrut(12));

        card.add(helper.lbl("Password"));
        card.add(Box.createVerticalStrut(6));
        passField = helper.passField();
        card.add(passField);
        card.add(Box.createVerticalStrut(12));

        card.add(helper.lbl("Confirm Password"));
        card.add(Box.createVerticalStrut(6));
        confirmField = helper.passField();
        card.add(confirmField);
        card.add(Box.createVerticalStrut(22));

        registerBtn = helper.accentBtn("Create Account");
        registerBtn.addActionListener(e -> doRegister());
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(14));

        card.add(helper.linkRow("Already have an account?", "Login", () -> {
            dispose(); new LoginFrame().setVisible(true);
        }));

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG);
        center.add(card);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        getRootPane().setDefaultButton(registerBtn);
        helper.addDragSupport(root);
    }

    private void doRegister() {
        String name    = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String pass    = new String(passField.getPassword());
        String confirm = new String(confirmField.getPassword());

        String namePh    = "Your display name";
        String contactPh = "e.g. user@email.com or 01XXXXXXXXX";

        if (name.equals(namePh))    name    = "";
        if (contact.equals(contactPh)) contact = "";

        if (name.isEmpty())    { err("Please enter your name.");                 return; }
        if (contact.isEmpty()) { err("Please enter your email or phone.");       return; }
        if (pass.isEmpty())    { err("Please enter a password.");                return; }
        if (!pass.equals(confirm)) { err("Passwords do not match.");             return; }

        String error = userManager.register(name, contact, pass);
        if (error == null) {
            JOptionPane.showMessageDialog(this,
                "Account created! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginFrame().setVisible(true);
        } else {
            err(error);
        }
    }

    void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Registration Error", JOptionPane.ERROR_MESSAGE);
    }
}
