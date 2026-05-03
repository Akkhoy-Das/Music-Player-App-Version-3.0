import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class LoginFrame extends JFrame {
    private UserManager userManager = new UserManager();
    private JTextField    contactField;
    private JPasswordField passwordField;
    private JButton loginBtn;

    private static final Color BG      = new Color(18, 18, 24);
    private static final Color CARD    = new Color(28, 28, 38);
    private static final Color ACCENT  = new Color(99, 102, 241);
    private static final Color AHOVER  = new Color(79, 82, 221);
    private static final Color TEXT1   = new Color(240, 240, 245);
    private static final Color TEXT2   = new Color(150, 150, 170);
    private static final Color FBG     = new Color(38, 38, 52);
    private static final Color BORDER  = new Color(55, 55, 75);

    public LoginFrame() {
        setTitle("MusicWave – Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 490);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0,0,420,490,20,20));
        buildUI();
    }

    private void buildUI() {
        JPanel root = darkPanel(new BorderLayout());
        root.add(titleBar(), BorderLayout.NORTH);

        // Card
        JPanel card = darkPanel(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28,35,28,35));
        card.setBackground(CARD);

        addLogo(card, "Sign in to your account");
        card.add(Box.createVerticalStrut(24));

        card.add(lbl("Email or Phone Number"));
        card.add(Box.createVerticalStrut(6));
        contactField = textField("e.g. user@email.com or 01XXXXXXXXX");
        card.add(contactField);
        card.add(Box.createVerticalStrut(14));

        card.add(lbl("Password"));
        card.add(Box.createVerticalStrut(6));
        passwordField = passField();
        card.add(passwordField);
        card.add(Box.createVerticalStrut(26));

        loginBtn = accentBtn("Login");
        loginBtn.addActionListener(e -> doLogin());
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(14));

        card.add(linkRow("Don't have an account?", "Register", () -> {
            dispose(); new RegisterFrame().setVisible(true);
        }));

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG);
        center.add(card);
        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        getRootPane().setDefaultButton(loginBtn);
        addDragSupport(root);
    }

    private void doLogin() {
        String contact = contactField.getText().trim();
        String pass    = new String(passwordField.getPassword());
        String ph      = "e.g. user@email.com or 01XXXXXXXXX";
        if (contact.isEmpty() || contact.equals(ph)) { err("Enter your email or phone."); return; }
        if (pass.isEmpty())                           { err("Enter your password.");       return; }
        String displayName = userManager.login(contact, pass);
        if (displayName != null) {
            dispose();
            new MusicPlayerFrame(displayName).setVisible(true);
        } else {
            err("Invalid email/phone or password.");
        }
    }

    // ── Shared widget builders ────────────────────────────────

    void addLogo(JPanel p, String sub) {
        JLabel logo = new JLabel("🎵 MusicWave", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(ACCENT);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        p.add(logo);
        p.add(Box.createVerticalStrut(6));
        JLabel s = new JLabel(sub, SwingConstants.CENTER);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        s.setForeground(TEXT2);
        s.setAlignmentX(CENTER_ALIGNMENT);
        p.add(s);
    }

    JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT2);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    JTextField textField(String ph) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FBG); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                super.paintComponent(g); g2.dispose();
            }
        };
        styleField(f, ph);
        return f;
    }

    JPasswordField passField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FBG); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                super.paintComponent(g); g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(TEXT1); f.setCaretColor(ACCENT); f.setEchoChar('●');
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER,10), BorderFactory.createEmptyBorder(10,14,10,14)));
        f.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    void styleField(JTextField f, String ph) {
        f.setOpaque(false);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(TEXT2); f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER,10), BorderFactory.createEmptyBorder(10,14,10,14)));
        f.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setText(ph);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(ph)) { f.setText(""); f.setForeground(TEXT1); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(TEXT2); }
            }
        });
    }

    JButton accentBtn(String text) {
        JButton b = new JButton(text) {
            private Color cur = ACCENT;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { cur = AHOVER; repaint(); }
                public void mouseExited(MouseEvent e)  { cur = ACCENT; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cur); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 14)); b.setForeground(TEXT1);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    JPanel linkRow(String prompt, String linkText, Runnable action) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setOpaque(false);
        JLabel p = new JLabel(prompt); p.setForeground(TEXT2); p.setFont(new Font("Segoe UI",Font.PLAIN,13));
        row.add(p);
        JButton b = new JButton(linkText);
        b.setFont(new Font("Segoe UI",Font.BOLD,13)); b.setForeground(ACCENT);
        b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        row.add(b);
        return row;
    }

    JPanel titleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(420,42));
        bar.setBorder(BorderFactory.createEmptyBorder(8,16,0,16));
        JButton x = new JButton("✕");
        x.setFont(new Font("Segoe UI",Font.PLAIN,13)); x.setForeground(TEXT2);
        x.setContentAreaFilled(false); x.setBorderPainted(false);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.addActionListener(e -> System.exit(0));
        x.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { x.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e)  { x.setForeground(TEXT2); }
        });
        bar.add(x, BorderLayout.EAST);
        return bar;
    }

    JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
            }
        };
        p.setOpaque(false); p.setBackground(BG);
        return p;
    }

    void addDragSupport(JPanel panel) {
        final Point[] pt = {null};
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { pt[0] = e.getPoint(); }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (pt[0] != null) {
                    Point loc = getLocation();
                    setLocation(loc.x + e.getX() - pt[0].x, loc.y + e.getY() - pt[0].y);
                }
            }
        });
    }

    void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Login Error", JOptionPane.ERROR_MESSAGE);
    }
}
