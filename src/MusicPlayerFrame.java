import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerFrame extends JFrame {
    // Colors
    private static final Color BG        = new Color(15, 15, 22);
    private static final Color SIDEBAR   = new Color(22, 22, 32);
    private static final Color CARD      = new Color(30, 30, 44);
    private static final Color ACCENT    = new Color(99, 102, 241);
    private static final Color ACCENT2   = new Color(139, 92, 246);
    private static final Color TEXT1     = new Color(240, 240, 250);
    private static final Color TEXT2     = new Color(140, 140, 165);
    private static final Color SEL_BG    = new Color(99, 102, 241, 50);
    private static final Color HOVER_BG  = new Color(255, 255, 255, 12);
    private static final Color CTRL_BG   = new Color(22, 22, 35);

    private AudioPlayer audioPlayer = new AudioPlayer();
    private List<File>  playlist    = new ArrayList<>();
    private int         currentIdx  = -1;
    private boolean     isDragging  = false;

    // UI refs
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String>            songList;
    private JLabel  songTitleLabel;
    private JLabel  artistLabel;
    private JLabel  timeLabel;
    private JSlider progressSlider;
    private JSlider volumeSlider;
    private JButton playPauseBtn;
    private JLabel  userLabel;

    public MusicPlayerFrame(String username) {
        setTitle("MusicWave");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 560);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);
        buildUI(username);
        setupAudioListener();
    }

    private void buildUI(String username) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        // ── TOP BAR ──────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(SIDEBAR);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0, new Color(50,50,70)),
            BorderFactory.createEmptyBorder(10,20,10,20)
        ));
        JLabel appName = new JLabel("🎵 MusicWave");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appName.setForeground(ACCENT);
        topBar.add(appName, BorderLayout.WEST);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        topRight.setOpaque(false);
        userLabel = new JLabel("👤  " + username);
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setForeground(TEXT2);
        topRight.add(userLabel);

        JButton logoutBtn = makeTextBtn("Logout", new Color(239,68,68));
        logoutBtn.addActionListener(e -> logout());
        topRight.add(logoutBtn);
        topBar.add(topRight, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // ── MAIN SPLIT ──────────────────────────────────────
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(BG);

        // LEFT – playlist
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(SIDEBAR);
        left.setPreferredSize(new Dimension(270, 0));
        left.setBorder(BorderFactory.createMatteBorder(0,0,0,1, new Color(40,40,58)));

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setBackground(SIDEBAR);
        listHeader.setBorder(BorderFactory.createEmptyBorder(14,16,10,16));
        JLabel listTitle = new JLabel("My Playlist");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        listTitle.setForeground(TEXT1);
        listHeader.add(listTitle, BorderLayout.WEST);

        JButton addBtn = makeIconBtn("＋  Add Song");
        addBtn.addActionListener(e -> addSongs());
        listHeader.add(addBtn, BorderLayout.EAST);
        left.add(listHeader, BorderLayout.NORTH);

        songList = new JList<>(listModel);
        songList.setBackground(SIDEBAR);
        songList.setForeground(TEXT1);
        songList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        songList.setFixedCellHeight(44);
        songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songList.setCellRenderer(new SongCellRenderer());
        songList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = songList.locationToIndex(e.getPoint());
                    if (idx >= 0) playSong(idx);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(songList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(SIDEBAR);
        scroll.getViewport().setBackground(SIDEBAR);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = new Color(70, 70, 100);
                trackColor = SIDEBAR;
            }
        });
        left.add(scroll, BorderLayout.CENTER);

        JPanel listFooter = new JPanel(new FlowLayout(FlowLayout.CENTER));
        listFooter.setBackground(SIDEBAR);
        listFooter.setBorder(BorderFactory.createEmptyBorder(6,0,6,0));
        JButton removeBtn = makeTextBtn("Remove Selected", new Color(239,68,68));
        removeBtn.addActionListener(e -> removeSelected());
        listFooter.add(removeBtn);
        left.add(listFooter, BorderLayout.SOUTH);
        main.add(left, BorderLayout.WEST);

        // RIGHT – player
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG);
        right.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        // Album art placeholder
        JPanel artPanel = new JPanel(new GridBagLayout());
        artPanel.setBackground(BG);
        JPanel art = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0, ACCENT, getWidth(), getHeight(), ACCENT2);
                g2.setPaint(gp);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.setColor(new Color(255,255,255,40));
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
                FontMetrics fm = g2.getFontMetrics();
                String emoji = "🎵";
                g2.drawString(emoji,
                    (getWidth()-fm.stringWidth(emoji))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        art.setPreferredSize(new Dimension(180, 180));
        art.setOpaque(false);
        artPanel.add(art);
        right.add(artPanel, BorderLayout.NORTH);

        // Song info
        JPanel info = new JPanel();
        info.setBackground(BG);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        songTitleLabel = new JLabel("No Song Selected", SwingConstants.CENTER);
        songTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        songTitleLabel.setForeground(TEXT1);
        songTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.add(songTitleLabel);
        info.add(Box.createVerticalStrut(6));

        artistLabel = new JLabel("Add songs to get started", SwingConstants.CENTER);
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        artistLabel.setForeground(TEXT2);
        artistLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.add(artistLabel);
        info.add(Box.createVerticalStrut(22));

        // Progress
        progressSlider = new JSlider(0, 1000, 0);
        progressSlider.setBackground(BG);
        progressSlider.setForeground(ACCENT);
        progressSlider.setPaintTicks(false);
        progressSlider.setPaintLabels(false);
        progressSlider.setUI(new SlimSliderUI(progressSlider, ACCENT, new Color(55,55,80)));
        progressSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { isDragging = true; }
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
                long dur = audioPlayer.getDuration();
                if (dur > 0) {
                    long seek = (long)(progressSlider.getValue() / 1000.0 * dur);
                    audioPlayer.seek(seek);
                }
            }
        });
        info.add(progressSlider);
        info.add(Box.createVerticalStrut(4));

        timeLabel = new JLabel("0:00 / 0:00", SwingConstants.RIGHT);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(TEXT2);
        timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        info.add(timeLabel);
        info.add(Box.createVerticalStrut(20));

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        controls.setBackground(BG);

        JButton prevBtn = makeCtrlBtn("⏮");
        prevBtn.addActionListener(e -> playPrev());

        playPauseBtn = makeCtrlBtn("▶");
        playPauseBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        playPauseBtn.setPreferredSize(new Dimension(60,60));
        playPauseBtn.addActionListener(e -> togglePlayPause());

        JButton nextBtn = makeCtrlBtn("⏭");
        nextBtn.addActionListener(e -> playNext());

        controls.add(prevBtn);
        controls.add(playPauseBtn);
        controls.add(nextBtn);
        info.add(controls);
        info.add(Box.createVerticalStrut(16));

        // Volume
        JPanel volPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        volPanel.setBackground(BG);
        JLabel volIcon = new JLabel("🔊");
        volIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        volPanel.add(volIcon);
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setBackground(BG);
        volumeSlider.setPreferredSize(new Dimension(130, 20));
        volumeSlider.setUI(new SlimSliderUI(volumeSlider, ACCENT2, new Color(55,55,80)));
        volumeSlider.addChangeListener(e -> audioPlayer.setVolume(volumeSlider.getValue() / 100f));
        volPanel.add(volumeSlider);
        info.add(volPanel);

        right.add(info, BorderLayout.CENTER);
        main.add(right, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);

        setContentPane(root);
        // Apply initial volume
        audioPlayer.setVolume(0.8f);
    }

    private void setupAudioListener() {
        audioPlayer.setListener(new AudioPlayer.PlayerCallback() {
            public void onFinished() {
                SwingUtilities.invokeLater(() -> {
                    playPauseBtn.setText("▶");
                    progressSlider.setValue(0);
                    timeLabel.setText("0:00 / 0:00");
                    playNext();
                });
            }
            public void onPositionChanged(long pos, long total) {
                if (!isDragging && total > 0) {
                    int val = (int)((pos * 1000.0) / total);
                    SwingUtilities.invokeLater(() -> {
                        progressSlider.setValue(Math.min(val, 1000));
                        timeLabel.setText(formatTime(pos) + " / " + formatTime(total));
                    });
                }
            }
        });
    }

    private void addSongs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Audio Files (*.wav, *.mp3, *.aiff)", "wav", "mp3", "aif", "aiff"));
        chooser.setDialogTitle("Add Songs");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                playlist.add(f);
                listModel.addElement(f.getName());
            }
        }
    }

    private void removeSelected() {
        int idx = songList.getSelectedIndex();
        if (idx < 0) return;
        if (idx == currentIdx) {
            audioPlayer.stop();
            playPauseBtn.setText("▶");
            songTitleLabel.setText("No Song Selected");
            artistLabel.setText("Add songs to get started");
            progressSlider.setValue(0);
            timeLabel.setText("0:00 / 0:00");
            currentIdx = -1;
        } else if (idx < currentIdx) {
            currentIdx--;
        }
        playlist.remove(idx);
        listModel.remove(idx);
    }

    private void playSong(int idx) {
        if (idx < 0 || idx >= playlist.size()) return;
        File f = playlist.get(idx);
        if (!audioPlayer.load(f)) {
            JOptionPane.showMessageDialog(this,
                "Cannot play: " + f.getName() + "\nMake sure it is a valid WAV, AIFF or MP3 file.",
                "Playback Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        audioPlayer.play();
        audioPlayer.setVolume(volumeSlider.getValue() / 100f);
        currentIdx = idx;
        songList.setSelectedIndex(idx);
        String name = f.getName();
        if (name.lastIndexOf('.') > 0) name = name.substring(0, name.lastIndexOf('.'));
        songTitleLabel.setText(name);
        artistLabel.setText(f.getParentFile() != null ? f.getParentFile().getName() : "Local File");
        playPauseBtn.setText("⏸");
    }

    private void togglePlayPause() {
        if (playlist.isEmpty()) { addSongs(); return; }
        if (currentIdx < 0) { playSong(0); return; }
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
            playPauseBtn.setText("▶");
        } else {
            audioPlayer.play();
            playPauseBtn.setText("⏸");
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        int next = (currentIdx + 1) % playlist.size();
        playSong(next);
    }

    private void playPrev() {
        if (playlist.isEmpty()) return;
        int prev = (currentIdx - 1 + playlist.size()) % playlist.size();
        playSong(prev);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            audioPlayer.stop();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private String formatTime(long micros) {
        long secs = micros / 1_000_000;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    // ── Helper button builders ─────────────────────────────

    private JButton makeCtrlBtn(String icon) {
        JButton btn = new JButton(icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CTRL_BG);
                g2.fillOval(0,0,getWidth()-1,getHeight()-1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btn.setForeground(TEXT1);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(48, 48));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(ACCENT); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(TEXT1); }
        });
        return btn;
    }

    private JButton makeIconBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return btn;
    }

    private JButton makeTextBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Song list cell renderer ────────────────────────────
    class SongCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            panel.setOpaque(true);
            panel.setBackground(isSelected ? SEL_BG : (index == currentIdx ? new Color(99,102,241,30) : SIDEBAR));

            JLabel num = new JLabel(String.valueOf(index + 1));
            num.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            num.setForeground(isSelected ? ACCENT : TEXT2);
            num.setPreferredSize(new Dimension(22, 44));
            panel.add(num, BorderLayout.WEST);

            String name = value.toString();
            if (name.lastIndexOf('.') > 0) name = name.substring(0, name.lastIndexOf('.'));
            JLabel title = new JLabel(name);
            title.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            title.setForeground(isSelected || index == currentIdx ? TEXT1 : TEXT1);
            panel.add(title, BorderLayout.CENTER);

            if (index == currentIdx && audioPlayer.isPlaying()) {
                JLabel playing = new JLabel("▶");
                playing.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                playing.setForeground(ACCENT);
                panel.add(playing, BorderLayout.EAST);
            }

            panel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) panel.setBackground(HOVER_BG);
                }
                public void mouseExited(MouseEvent e) {
                    panel.setBackground(isSelected ? SEL_BG : SIDEBAR);
                }
            });
            return panel;
        }
    }
}
