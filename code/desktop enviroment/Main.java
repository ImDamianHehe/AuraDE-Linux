import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {

    private static final Font APP_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font CLOCK_FONT = new Font("Arial", Font.BOLD, 14);
    private static final int BUTTON_CORNER_RADIUS = 12;

    // Cache CLI availability once on startup to prevent repeated process creation
    private static final boolean HAS_RSVG = isCommandAvailable("rsvg-convert");
    private static final boolean HAS_MAGICK = isCommandAvailable("magick");
    private static final boolean HAS_INKSCAPE = isCommandAvailable("inkscape");

    private static DateTimeFormatter clockFormatter;

    public static class Settings {
        public String theme = "dark";
        public String wallpaper = "";
        public boolean clock24h = true;
    }

    public static class RoundedButton extends JButton {
        private final int cornerRadius;

        public RoundedButton(String text, int radius) {
            super(text);
            this.cornerRadius = radius;
            init();
        }

        public RoundedButton(int radius) {
            super();
            this.cornerRadius = radius;
            init();
        }

        private void init() {
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class WallpaperPanel extends JPanel {
        private Image wallpaperImage;

        public WallpaperPanel(String imagePath) {
            setLayout(null);
            setWallpaper(imagePath);
        }

        public void setWallpaper(String imagePath) {
            if (imagePath != null && !imagePath.isBlank()) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    this.wallpaperImage = new ImageIcon(imagePath).getImage();
                } else {
                    this.wallpaperImage = null;
                }
            } else {
                this.wallpaperImage = null;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (wallpaperImage != null) {
                g.drawImage(wallpaperImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // Path Definitions
            String home = System.getProperty("user.home");
            Path configDir = Paths.get(home, ".local", "aurade", "config");
            Path configFile = configDir.resolve("settings.json");

            // Load Configuration
            Settings settings = loadSettings();

            // Setup Theme Colors
            boolean isLight = "light".equalsIgnoreCase(settings.theme);

            Color DESKTOP_BG   = isLight ? new Color(230, 235, 245) : new Color(12, 14, 20);
            Color PANEL_BG     = isLight ? new Color(220, 225, 235) : new Color(10, 12, 18);
            Color BUTTON_BG    = isLight ? new Color(200, 205, 220) : new Color(20, 24, 34);
            Color BUTTON_HOVER = isLight ? new Color(180, 188, 205) : new Color(30, 36, 50);
            Color MENU_BG      = isLight ? new Color(240, 242, 248) : new Color(16, 18, 26);
            Color MENU_HOVER   = isLight ? new Color(210, 218, 232) : new Color(28, 32, 44);
            Color TEXT         = isLight ? new Color(20, 24, 35)    : new Color(235, 240, 255);

            // Desktop Frame & Screen Setup
            JFrame desktop = new JFrame("desktop");
            WallpaperPanel screen = new WallpaperPanel(settings.wallpaper);
            screen.setBackground(DESKTOP_BG);

            // Top Bar Navigation Buttons & Popup Menus
            JButton settingsButton = createIconButton(null, "settingsButton.png", 16);
            JButton appsButton = createIconButton("Apps", "appsButton.png", 16);
            JButton systemButton = createIconButton("System", "systemButton.png", 16);
            JButton toolsButton = createIconButton("Tools", "toolsButton.png", 16);

            JDialog appsDialog = new JDialog(desktop);
            appsDialog.setUndecorated(true);
            appsDialog.setFocusableWindowState(false);
            appsDialog.setType(Window.Type.POPUP);

            settingsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        ProcessBuilder pb = new ProcessBuilder("aura-settings");
                        pb.start();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            appsDialog.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
                @Override public void windowGainedFocus(java.awt.event.WindowEvent e) {}

                @Override
                public void windowLostFocus(java.awt.event.WindowEvent e) {
                    appsDialog.setFocusableWindowState(false);
                    appsDialog.setVisible(false);
                }
            });

            JPopupMenu systemMenu = new JPopupMenu();
            JPopupMenu toolsMenu = new JPopupMenu();

            // Load app menu asynchronously to keep startup instant
            new Thread(() -> buildApplicationsMenu(appsDialog, MENU_BG, MENU_HOVER, TEXT)).start();

            // Tools Menu Items
            JMenuItem terminalItem = new JMenuItem("Terminal (konsole)");
            JMenuItem auraMassItem = new JMenuItem("Terminal (Built-In)");
            JMenuItem fileItem = new JMenuItem("File Manager");

            JMenuItem[] toolsItems = { terminalItem, auraMassItem, fileItem };

            for (JMenuItem item : toolsItems) {
                item.setFont(APP_FONT);
                item.setBackground(MENU_BG);
                item.setForeground(TEXT);
                item.setOpaque(true);
                item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

                item.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { item.setBackground(MENU_HOVER); }
                    @Override public void mouseExited(MouseEvent e) { item.setBackground(MENU_BG); }
                });
            }

            terminalItem.addActionListener(e -> launchProcess("konsole"));
            auraMassItem.addActionListener(e -> launchProcess("auramass"));
            fileItem.addActionListener(e -> launchProcess("dolphin"));

            toolsMenu.setBackground(BUTTON_BG);
            toolsMenu.setForeground(TEXT);
            toolsMenu.setBorderPainted(false);
            toolsMenu.setOpaque(true);
            toolsMenu.add(terminalItem);
            toolsMenu.add(auraMassItem);
            toolsMenu.add(fileItem);

            // System Menu Items
            JMenuItem restartItem = new JMenuItem("Restart");
            JMenuItem powerItem = new JMenuItem("Power Off");
            JMenuItem logoutItem = new JMenuItem("Log Out");

            JMenuItem[] systemItems = { restartItem, powerItem, logoutItem };

            for (JMenuItem item : systemItems) {
                item.setFont(APP_FONT);
                item.setBackground(MENU_BG);
                item.setForeground(TEXT);
                item.setOpaque(true);
                item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

                item.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { item.setBackground(MENU_HOVER); }
                    @Override public void mouseExited(MouseEvent e) { item.setBackground(MENU_BG); }
                });
            }

            restartItem.addActionListener(e -> launchProcess("systemctl", "reboot"));
            powerItem.addActionListener(e -> launchProcess("systemctl", "poweroff"));
            logoutItem.addActionListener(e -> System.exit(0));

            systemMenu.setBackground(MENU_BG);
            systemMenu.setBorder(BorderFactory.createLineBorder(new Color(36, 42, 58)));
            systemMenu.add(restartItem);
            systemMenu.add(powerItem);
            systemMenu.add(logoutItem);

            // Left side layout positioning
            settingsButton.setBounds(5, 3, 28, 24);
            appsButton.setBounds(38, 3, 72, 24);
            systemButton.setBounds(115, 3, 80, 24);
            toolsButton.setBounds(200, 3, 75, 24);

            JButton[] topButtons = { settingsButton, appsButton, systemButton, toolsButton };

            for (JButton button : topButtons) {
                button.setFont(APP_FONT);
                button.setBackground(BUTTON_BG);
                button.setForeground(TEXT);
                button.setMargin(new Insets(2, 4, 2, 4));

                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        button.setBackground(BUTTON_HOVER);
                        button.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        button.setBackground(BUTTON_BG);
                        button.repaint();
                    }
                });
            }

            appsButton.addActionListener(e -> {
                if (appsDialog.isVisible()) {
                    appsDialog.setFocusableWindowState(false);
                    appsDialog.setVisible(false);
                } else {
                    Point location = appsButton.getLocationOnScreen();
                    appsDialog.setLocation(location.x, location.y + appsButton.getHeight());
                    appsDialog.pack();
                    appsDialog.setFocusableWindowState(true);
                    appsDialog.setVisible(true);
                    appsDialog.requestFocus();
                }
            });

            systemButton.addActionListener(e -> {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                systemMenu.show(systemButton, 0, systemButton.getHeight());
            });

            toolsButton.addActionListener(e -> {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                toolsMenu.show(toolsButton, 0, toolsButton.getHeight());
            });

            // Taskbar Panel
            JPanel taskbarPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            taskbarPanel.setBackground(PANEL_BG);
            taskbarPanel.setBounds(Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 450, 0, 900, 30);

            javax.swing.Timer taskbarTimer = new javax.swing.Timer(1000, e ->
                    refreshTaskbar(taskbarPanel, BUTTON_BG, BUTTON_HOVER, TEXT)
            );
            taskbarTimer.setInitialDelay(0);
            taskbarTimer.start();

            // System Controls
            JButton volDown = createIconButton(null, "volDown.png", 14);
            JButton volUp   = createIconButton(null, "volUp.png", 14);
            JButton mute    = createIconButton(null, "Mute_Volume.png", 14);
            JButton briDown = createIconButton(null, "briDown.png", 14);
            JButton briUp   = createIconButton(null, "BriUp.png", 14);

            JLabel volLabel = new JLabel(getVolume());
            JLabel briLabel = new JLabel(getBrightness());

            volDown.addActionListener(e -> {
                launchProcess("amixer", "-q", "set", "Master", "5%-");
                volLabel.setText(getVolume());
            });
            volUp.addActionListener(e -> {
                launchProcess("amixer", "-q", "set", "Master", "5%+");
                volLabel.setText(getVolume());
            });
            mute.addActionListener(e -> {
                launchProcess("amixer", "-q", "set", "Master", "toggle");
                volLabel.setText(getVolume());
            });

            briDown.addActionListener(e -> {
                launchProcess("brightnessctl", "set", "5%-");
                briLabel.setText(getBrightness());
            });
            briUp.addActionListener(e -> {
                launchProcess("brightnessctl", "set", "5%+");
                briLabel.setText(getBrightness());
            });

            volLabel.setFont(APP_FONT);
            briLabel.setFont(APP_FONT);
            volLabel.setForeground(TEXT);
            briLabel.setForeground(TEXT);

            new javax.swing.Timer(1000, e -> {
                volLabel.setText(getVolume());
                briLabel.setText(getBrightness());
            }).start();

            int screenW = Toolkit.getDefaultToolkit().getScreenSize().width;
            int startX = screenW - 420;

            volDown.setBounds(startX, 3, 32, 24);
            volUp.setBounds(startX + 35, 3, 32, 24);
            mute.setBounds(startX + 70, 3, 32, 24);
            volLabel.setBounds(startX + 105, 6, 40, 18);

            briDown.setBounds(startX + 148, 3, 32, 24);
            briUp.setBounds(startX + 183, 3, 32, 24);
            briLabel.setBounds(startX + 218, 6, 40, 18);

            // Wi-Fi Control
            JButton wifiButton = createIconButton("WiFi", "wifiButton.png", 16);
            JPopupMenu wifiMenu = new JPopupMenu();

            wifiButton.setBounds(startX + 260, 3, 60, 24);
            wifiButton.setFont(APP_FONT);
            wifiButton.setBackground(BUTTON_BG);
            wifiButton.setForeground(TEXT);
            wifiButton.setMargin(new Insets(2, 4, 2, 4));

            wifiButton.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { wifiButton.setBackground(BUTTON_HOVER); wifiButton.repaint(); }
                @Override public void mouseExited(MouseEvent e) { wifiButton.setBackground(BUTTON_BG); wifiButton.repaint(); }
            });

            wifiButton.addActionListener(e -> {
                rebuildWifiMenu(wifiMenu, MENU_BG, MENU_HOVER, TEXT);
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                wifiMenu.show(wifiButton, 0, wifiButton.getHeight());
            });

            JButton[] controlButtons = { volDown, volUp, mute, briDown, briUp };

            for (JButton b : controlButtons) {
                b.setFont(APP_FONT);
                b.setBackground(BUTTON_BG);
                b.setForeground(TEXT);

                b.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { b.setBackground(BUTTON_HOVER); b.repaint(); }
                    @Override public void mouseExited(MouseEvent e) { b.setBackground(BUTTON_BG); b.repaint(); }
                });
            }

            // Clock Widget
            JLabel clockLabel = new JLabel();
            clockLabel.setFont(CLOCK_FONT);
            clockLabel.setForeground(TEXT);
            clockLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            try (InputStream is = Main.class.getResourceAsStream("/assets/icons/Clock.png")) {
                if (is != null) {
                    ImageIcon original = new ImageIcon(is.readAllBytes());
                    Image scaled = original.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
                    clockLabel.setIcon(new ImageIcon(scaled));
                    clockLabel.setIconTextGap(4);
                } else {
                    File localFile = new File("assets/icons/Clock.png");
                    if (localFile.exists()) {
                        ImageIcon original = new ImageIcon(localFile.getAbsolutePath());
                        Image scaled = original.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
                        clockLabel.setIcon(new ImageIcon(scaled));
                        clockLabel.setIconTextGap(4);
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not load clock icon: " + e.getMessage());
            }

            clockLabel.setBounds(screenW - 95, 5, 90, 20);

            clockFormatter = DateTimeFormatter.ofPattern(settings.clock24h ? "HH:mm" : "hh:mm a");

            javax.swing.Timer clockTimer = new javax.swing.Timer(1000, e -> clockLabel.setText(LocalTime.now().format(clockFormatter)));
            clockTimer.setInitialDelay(0);
            clockTimer.start();

            // Top Panel Assembly
            JPanel panel = new JPanel(null);
            panel.setBounds(0, 0, Toolkit.getDefaultToolkit().getScreenSize().width, 30);
            panel.setBackground(PANEL_BG);

            panel.add(settingsButton);
            panel.add(appsButton);
            panel.add(systemButton);
            panel.add(toolsButton);
            panel.add(taskbarPanel);
            panel.add(volDown);
            panel.add(volUp);
            panel.add(mute);
            panel.add(volLabel);
            panel.add(briDown);
            panel.add(briUp);
            panel.add(briLabel);
            panel.add(wifiButton);
            panel.add(clockLabel);

            // Frame Assembly
            desktop.setUndecorated(true);
            desktop.setFocusableWindowState(false);
            desktop.setAutoRequestFocus(false);
            desktop.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            desktop.setResizable(false);

            screen.setLayout(null);

            screen.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    launchProcess("wmctrl", "-r", "desktop", "-b", "add,below,skip_taskbar,skip_pager");
                }
            });

            desktop.setContentPane(screen);
            screen.add(panel);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            Rectangle screenBounds = gd.getDefaultConfiguration().getBounds();

            desktop.setBounds(screenBounds);
            desktop.setVisible(true);

            launchProcess("wmctrl", "-r", "desktop", "-b", "add,below,skip_taskbar,skip_pager");

            // Real-Time File Watcher for Instant Config Updates
            startSettingsWatcher(configDir, configFile, () -> {
                Settings newSettings = loadSettings();

                boolean light = "light".equalsIgnoreCase(newSettings.theme);
                Color newDeskBg   = light ? new Color(230, 235, 245) : new Color(12, 14, 20);
                Color newPanelBg  = light ? new Color(220, 225, 235) : new Color(10, 12, 18);
                Color newBtnBg    = light ? new Color(200, 205, 220) : new Color(20, 24, 34);
                Color newMenuBg   = light ? new Color(240, 242, 248) : new Color(16, 18, 26);
                Color newMenuHov  = light ? new Color(210, 218, 232) : new Color(28, 32, 44);
                Color newTextColor = light ? new Color(20, 24, 35)    : new Color(235, 240, 255);

                // Update Screen & Wallpaper
                screen.setBackground(newDeskBg);
                screen.setWallpaper(newSettings.wallpaper);

                // Update Panel & Buttons
                panel.setBackground(newPanelBg);
                taskbarPanel.setBackground(newPanelBg);

                for (JButton btn : new JButton[]{settingsButton, appsButton, systemButton, toolsButton, wifiButton, volDown, volUp, mute, briDown, briUp}) {
                    btn.setBackground(newBtnBg);
                    btn.setForeground(newTextColor);
                }

                volLabel.setForeground(newTextColor);
                briLabel.setForeground(newTextColor);
                clockLabel.setForeground(newTextColor);

                // Update Clock Format
                clockFormatter = DateTimeFormatter.ofPattern(newSettings.clock24h ? "HH:mm" : "hh:mm a");
                clockLabel.setText(LocalTime.now().format(clockFormatter));

                // Reload App Menu Palette
                new Thread(() -> buildApplicationsMenu(appsDialog, newMenuBg, newMenuHov, newTextColor)).start();

                panel.revalidate();
                panel.repaint();
                screen.revalidate();
                screen.repaint();
            });
        });
    }

    // ==========================================
    // REAL-TIME FILE WATCHER
    // ==========================================
    private static void startSettingsWatcher(Path configDir, Path configFile, Runnable onConfigChanged) {
        Thread watcherThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                if (Files.exists(configDir)) {
                    configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
                }

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changedFile = (Path) event.context();
                        if (changedFile.equals(configFile.getFileName())) {
                            Thread.sleep(100); // Small buffer to allow file writes to finish
                            SwingUtilities.invokeLater(onConfigChanged);
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) break;
                }
            } catch (Exception e) {
                System.err.println("Settings watcher stopped: " + e.getMessage());
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    // ==========================================
    // JSON CONFIG LOADER & PARSER
    // ==========================================
    private static Settings loadSettings() {
        Settings settings = new Settings();
        String home = System.getProperty("user.home");
        Path configDir = Paths.get(home, ".local", "aurade", "config");
        Path configFile = configDir.resolve("settings.json");

        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configDir);
                String defaultConfig = "{\n" +
                        "  \"theme\": \"dark\",\n" +
                        "  \"wallpaper\": \"" + home + "/Pictures/wallpaper.jpg\",\n" +
                        "  \"clock24h\": true\n" +
                        "}";
                Files.writeString(configFile, defaultConfig);
            }

            String content = Files.readString(configFile);
            settings.theme = parseJsonString(content, "theme", "dark");
            settings.wallpaper = parseJsonString(content, "wallpaper", home + "/Pictures/wallpaper.jpg");
            settings.clock24h = parseJsonBoolean(content, "clock24h", true);

        } catch (Exception e) {
            System.err.println("Could not load settings.json, falling back to defaults: " + e.getMessage());
        }

        return settings;
    }

    private static String parseJsonString(String json, String key, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : defaultValue;
    }

    private static boolean parseJsonBoolean(String json, String key, boolean defaultValue) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : defaultValue;
    }

    // Helpers
    private static String getVolume() {
        try {
            Process p = new ProcessBuilder("amixer", "get", "Master").start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("[off]")) return "MUT";
                    Pattern pattern = Pattern.compile("\\[(\\d+%\\])");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) return matcher.group(1).replace("]", "");
                }
            }
        } catch (Exception ignored) {}
        return "--%";
    }

    private static String getBrightness() {
        try {
            Process p = new ProcessBuilder("brightnessctl", "g").start();
            Process pMax = new ProcessBuilder("brightnessctl", "m").start();

            int curr = 0, max = 1;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l = br.readLine();
                if (l != null) curr = Integer.parseInt(l.trim());
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(pMax.getInputStream()))) {
                String l = br.readLine();
                if (l != null) max = Integer.parseInt(l.trim());
            }
            return Math.round((curr * 100.0) / max) + "%";
        } catch (Exception ignored) {}
        return "--%";
    }

    private static void launchProcess(String... command) {
        try {
            new ProcessBuilder(command).start();
        } catch (Exception e) {
            System.err.println("Failed to run command " + Arrays.toString(command) + ": " + e.getMessage());
        }
    }

    private static void refreshTaskbar(JPanel panel, Color bg, Color hover, Color fg) {
        try {
            Process process = new ProcessBuilder("wmctrl", "-l").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> windows = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+", 5);
                    if (parts.length >= 5 && !parts[4].equalsIgnoreCase("desktop")) {
                        windows.add(parts[4]);
                    }
                }

                panel.removeAll();
                for (String title : windows) {
                    String displayTitle = title.length() > 20 ? title.substring(0, 17) + "..." : title;
                    JButton btn = new RoundedButton(displayTitle, 8);
                    btn.setFont(APP_FONT);
                    btn.setBackground(bg);
                    btn.setForeground(fg);
                    btn.addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); btn.repaint(); }
                        @Override public void mouseExited(MouseEvent e) { btn.setBackground(bg); btn.repaint(); }
                    });
                    btn.addActionListener(e -> launchProcess("wmctrl", "-a", title));
                    panel.add(btn);
                }
                panel.revalidate();
                panel.repaint();
            }
        } catch (Exception e) {
            System.err.println("Error updating taskbar: " + e.getMessage());
        }
    }

    private static JButton createIconButton(String text, String iconFileName, int iconSize) {
        JButton button = (text != null)
                ? new RoundedButton(text, BUTTON_CORNER_RADIUS)
                : new RoundedButton(BUTTON_CORNER_RADIUS);
        button.setFont(APP_FONT);

        try (InputStream is = Main.class.getResourceAsStream("/assets/icons/" + iconFileName)) {
            if (is != null) {
                ImageIcon original = new ImageIcon(is.readAllBytes());
                Image scaled = original.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaled));
                if (text != null) button.setIconTextGap(4);
            } else {
                File localFile = new File("assets/icons/" + iconFileName);
                if (localFile.exists()) {
                    ImageIcon original = new ImageIcon(localFile.getAbsolutePath());
                    Image scaled = original.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
                    button.setIcon(new ImageIcon(scaled));
                    if (text != null) button.setIconTextGap(4);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon " + iconFileName + ": " + e.getMessage());
        }

        return button;
    }

    private static void buildApplicationsMenu(JDialog dialog, Color bg, Color hover, Color text) {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBackground(bg);
        containerPanel.setBorder(BorderFactory.createLineBorder(new Color(36, 42, 58)));

        JTextField searchField = new JTextField();
        searchField.setFont(APP_FONT);
        searchField.setBackground(bg.brighter());
        searchField.setForeground(text);
        searchField.setCaretColor(text);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(36, 42, 58)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JPanel appsPanel = new JPanel();
        appsPanel.setLayout(new BoxLayout(appsPanel, BoxLayout.Y_AXIS));
        appsPanel.setBackground(bg);

        List<Path> dirs = List.of(
                Paths.get(System.getProperty("user.home"), ".local/share/applications"),
                Paths.get("/usr/share/applications"),
                Paths.get("/usr/local/share/applications"),
                Paths.get("/var/lib/flatpak/exports/share/applications"),
                Paths.get(System.getProperty("user.home"), ".local/share/flatpak/exports/share/applications")
        );

        List<JButton> allButtons = new ArrayList<>();
        Set<String> addedExecs = new HashSet<>();

        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) continue;

            try (Stream<Path> stream = Files.walk(dir, 3)) {
                stream.filter(p -> p.toString().endsWith(".desktop"))
                        .forEach(p -> addDesktopEntry(appsPanel, p, allButtons, addedExecs, bg, hover, text, dialog));
            } catch (IOException e) {
                System.err.println("Error reading desktop files: " + e.getMessage());
            }
        }

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String query = searchField.getText().toLowerCase().trim();
                appsPanel.removeAll();
                for (JButton button : allButtons) {
                    if (query.isEmpty() || button.getText().toLowerCase().contains(query)) {
                        appsPanel.add(button);
                    }
                }
                appsPanel.revalidate();
                appsPanel.repaint();
            }

            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
        });

        JScrollPane scrollPane = new JScrollPane(appsPanel);
        scrollPane.setPreferredSize(new Dimension(300, 500));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(bg);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        containerPanel.add(searchField, BorderLayout.NORTH);
        containerPanel.add(scrollPane, BorderLayout.CENTER);

        // Apply component changes safely on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            dialog.getContentPane().removeAll();
            dialog.getContentPane().add(containerPanel);
        });
    }

    private static void addDesktopEntry(JPanel panel, Path file, List<JButton> allButtons, Set<String> addedExecs, Color bg, Color hover, Color text, JDialog dialog) {
        String name = null;
        String exec = null;
        String iconName = null;
        boolean noDisplay = false;

        try {
            for (String line : Files.readAllLines(file)) {
                if (line.startsWith("Name=") && name == null) name = line.substring(5).trim();
                else if (line.startsWith("Exec=") && exec == null) exec = line.substring(5).trim();
                else if (line.startsWith("Icon=") && iconName == null) iconName = line.substring(5).trim();
                else if (line.equalsIgnoreCase("NoDisplay=true")) noDisplay = true;
            }
        } catch (IOException e) {
            return;
        }

        if (name == null || exec == null || noDisplay) return;

        final String command = sanitizeExec(exec);

        if (addedExecs.contains(command)) return;
        addedExecs.add(command);

        JButton item = new JButton(name);
        item.setFont(APP_FONT);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setBackground(bg);
        item.setForeground(text);
        item.setFocusPainted(false);
        item.setBorderPainted(false);
        item.setOpaque(true);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Load Icon
        ImageIcon icon = loadIcon(iconName, 24);
        if (icon != null) {
            item.setIcon(icon);
        } else {
            item.setIcon(createDefaultAppIcon(text));
        }
        item.setIconTextGap(10);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { item.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { item.setBackground(bg); }
        });

        item.addActionListener(e -> {
            launchProcess("sh", "-c", command);
            dialog.setFocusableWindowState(false);
            dialog.setVisible(false);
        });

        allButtons.add(item);
        panel.add(item);
    }

    private static String sanitizeExec(String exec) {
        return exec.replaceAll("%[fFuUdDnNickvm]", "").trim();
    }

    // Optimized Icon Resolver
    private static ImageIcon loadIcon(String iconName, int targetSize) {
        if (iconName == null || iconName.isBlank()) return null;

        if (iconName.startsWith("/")) {
            File f = new File(iconName);
            if (f.exists()) return renderFileToIcon(f, targetSize);
        }

        String home = System.getProperty("user.home");
        String[] iconDirs = {
                home + "/.local/share/icons",
                home + "/.local/share/flatpak/exports/share/icons",
                "/var/lib/flatpak/exports/share/icons",
                "/usr/share/icons/hicolor",
                "/usr/share/pixmaps"
        };

        // Quick path check first (bypasses directory tree walking)
        for (String baseDir : iconDirs) {
            File directPng = new File(baseDir, iconName + ".png");
            if (directPng.exists()) return scaleIcon(new ImageIcon(directPng.getAbsolutePath()), targetSize);
        }

        // Search directory trees for PNGs
        for (String baseDir : iconDirs) {
            File dir = new File(baseDir);
            if (!dir.exists()) continue;

            try (Stream<Path> stream = Files.walk(dir.toPath(), 4)) {
                Optional<Path> foundPng = stream
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase(iconName + ".png"))
                        .findFirst();

                if (foundPng.isPresent()) {
                    return scaleIcon(new ImageIcon(foundPng.get().toString()), targetSize);
                }
            } catch (IOException ignored) {}
        }

        // Fallback search for SVGs and render/rasterize
        for (String baseDir : iconDirs) {
            File dir = new File(baseDir);
            if (!dir.exists()) continue;

            try (Stream<Path> stream = Files.walk(dir.toPath(), 4)) {
                Optional<Path> foundSvg = stream
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase(iconName + ".svg"))
                        .findFirst();

                if (foundSvg.isPresent()) {
                    return renderSvgToIcon(foundSvg.get().toFile(), targetSize);
                }
            } catch (IOException ignored) {}
        }

        return null;
    }

    private static ImageIcon renderFileToIcon(File file, int size) {
        if (file.getName().endsWith(".svg")) {
            return renderSvgToIcon(file, size);
        }
        return scaleIcon(new ImageIcon(file.getAbsolutePath()), size);
    }

    private static ImageIcon scaleIcon(ImageIcon icon, int size) {
        if (icon == null || icon.getIconWidth() <= 0) return null;
        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    // Converts SVGs using cached CLI detection
    private static ImageIcon renderSvgToIcon(File svgFile, int size) {
        try {
            Path tempPng = Files.createTempFile("aura_icon_", ".png");
            tempPng.toFile().deleteOnExit();

            Process p = null;
            if (HAS_RSVG) {
                p = new ProcessBuilder("rsvg-convert", "-w", String.valueOf(size), "-h", String.valueOf(size), svgFile.getAbsolutePath(), "-o", tempPng.toString()).start();
            } else if (HAS_MAGICK) {
                p = new ProcessBuilder("magick", "-background", "none", svgFile.getAbsolutePath(), "-resize", size + "x" + size, tempPng.toString()).start();
            } else if (HAS_INKSCAPE) {
                p = new ProcessBuilder("inkscape", "-w", String.valueOf(size), "-h", String.valueOf(size), svgFile.getAbsolutePath(), "-o", tempPng.toString()).start();
            }

            if (p != null) {
                p.waitFor();
                if (Files.exists(tempPng) && Files.size(tempPng) > 0) {
                    return new ImageIcon(tempPng.toString());
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static boolean isCommandAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder("which", cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Dynamic Fallback Icon
    private static ImageIcon createDefaultAppIcon(Color textColor) {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(textColor);
        g2.drawRoundRect(2, 2, 15, 15, 4, 4);
        g2.fillOval(7, 7, 5, 5);
        g2.dispose();
        return new ImageIcon(img);
    }

    private static void rebuildWifiMenu(JPopupMenu menu, Color bg, Color hover, Color text) {
        menu.removeAll();
        menu.setBackground(bg);
        menu.setBorder(BorderFactory.createLineBorder(new Color(36, 42, 58)));

        JMenuItem onItem = createMenuItem("Wi-Fi On", bg, hover, text);
        onItem.addActionListener(e -> launchProcess("nmcli", "radio", "wifi", "on"));

        JMenuItem offItem = createMenuItem("Wi-Fi Off", bg, hover, text);
        offItem.addActionListener(e -> launchProcess("nmcli", "radio", "wifi", "off"));

        menu.add(onItem);
        menu.add(offItem);
        menu.addSeparator();

        try {
            Process p = new ProcessBuilder("nmcli", "-t", "-f", "SSID", "dev", "wifi").start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                Set<String> ssids = new LinkedHashSet<>();
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) ssids.add(line);
                }

                if (ssids.isEmpty()) {
                    JMenuItem none = createMenuItem("No networks found", bg, hover, text);
                    none.setEnabled(false);
                    menu.add(none);
                } else {
                    for (String ssid : ssids) {
                        JMenuItem net = createMenuItem(ssid, bg, hover, text);
                        net.addActionListener(e ->
                                JOptionPane.showMessageDialog(
                                        null,
                                        "Connect using terminal:\nnmcli dev wifi connect " + ssid,
                                        "WiFi",
                                        JOptionPane.INFORMATION_MESSAGE
                                ));
                        menu.add(net);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error fetching WiFi networks: " + ex.getMessage());
        }
    }

    private static JMenuItem createMenuItem(String text, Color bg, Color hover, Color fg) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(APP_FONT);
        item.setBackground(bg);
        item.setForeground(fg);
        item.setOpaque(true);
        item.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { item.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { item.setBackground(bg); }
        });

        return item;
    }
}
