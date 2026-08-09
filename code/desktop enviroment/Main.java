import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;
import java.io.*;
import java.util.*;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            Color DESKTOP_BG = new Color(12, 14, 20);
            Color PANEL_BG = new Color(10, 12, 18);
            Color BUTTON_BG = new Color(20, 24, 34);
            Color BUTTON_HOVER = new Color(30, 36, 50);
            Color MENU_BG = new Color(16, 18, 26);
            Color MENU_HOVER = new Color(28, 32, 44);
            Color TEXT = new Color(235, 240, 255);

            JFrame desktop = new JFrame("desktop");

            String home = System.getProperty("user.home");
            Path auradeDir = Paths.get(home, ".local", "aurade");

            try {
                Files.createDirectories(auradeDir);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String wallpaperPath = auradeDir.resolve("wallpaper.jpg").toString();

            JPanel screen;

            if (Files.exists(Path.of(wallpaperPath))) {
                screen = new WallpaperPanel(wallpaperPath);
            } else {
                screen = new JPanel(null);
                screen.setBackground(DESKTOP_BG);
            }

            JButton systemButton = new JButton("System");
            JButton appsButton = new JButton("Apps");
            JButton toolsButton = new JButton("Tools");

            JPopupMenu appsMenu = new JPopupMenu();
            JPopupMenu systemMenu = new JPopupMenu();
            JPopupMenu toolsMenu = new JPopupMenu();

            buildApplicationsMenu(appsMenu, MENU_BG, MENU_HOVER, TEXT);

            JMenuItem terminalItem = new JMenuItem("Terminal (konsole)");
            JMenuItem auraMassItem = new JMenuItem("Terminal (Built-In)");
            JMenuItem fileItem = new JMenuItem("File Manager");

            JMenuItem[] toolsItems = {
                    terminalItem,
                    auraMassItem,
                    fileItem
            };

            for (JMenuItem item : toolsItems) {
                item.setBackground(MENU_BG);
                item.setForeground(TEXT);
                item.setOpaque(true);
                item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

                item.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        item.setBackground(MENU_HOVER);
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        item.setBackground(MENU_BG);
                    }
                });
            }


            JMenuItem restartItem = new JMenuItem("Restart");
            JMenuItem powerItem = new JMenuItem("Power Off");
            JMenuItem logoutItem = new JMenuItem("Log Out");

            JMenuItem[] systemItems = {
                    restartItem,
                    powerItem,
                    logoutItem
            };

            for (JMenuItem item : systemItems) {

                item.setBackground(MENU_BG);
                item.setForeground(TEXT);
                item.setOpaque(true);
                item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

                item.addMouseListener(new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        item.setBackground(MENU_HOVER);
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        item.setBackground(MENU_BG);
                    }
                });
            }

            restartItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("systemctl", "reboot").start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            powerItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("systemctl", "poweroff").start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            logoutItem.addActionListener(e -> System.exit(0));

            systemMenu.setBackground(MENU_BG);
            systemMenu.setBorder(
                    BorderFactory.createLineBorder(new Color(36, 42, 58))
            );

            systemMenu.add(restartItem);
            systemMenu.add(powerItem);
            systemMenu.add(logoutItem);

            JPanel panel = new JPanel();

            JPanel taskbarPanel = new JPanel(
                    new FlowLayout(FlowLayout.CENTER, 6, 2)
            );

            taskbarPanel.setBackground(PANEL_BG);

            taskbarPanel.setBounds(
                    Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 450,
                    0,
                    900,
                    30
            );

            terminalItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("konsole").start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            auraMassItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("auramass").start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            fileItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("dolphin").start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            toolsMenu.setBackground(BUTTON_BG);
            toolsMenu.setForeground(TEXT);
            toolsMenu.setBorderPainted(false);
            toolsMenu.setOpaque(true);
            toolsMenu.add(terminalItem);
            toolsMenu.add(auraMassItem);
            toolsMenu.add(fileItem);

            JLabel clockLabel = new JLabel();
            clockLabel.setFont(new Font("Arial", Font.BOLD, 16));
            clockLabel.setForeground(TEXT);
            clockLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            clockLabel.setBounds(
                    Toolkit.getDefaultToolkit().getScreenSize().width - 120,
                    3,
                    100,
                    20
            );

            JButton wifiButton = new JButton("WiFi");
            JPopupMenu wifiMenu = new JPopupMenu();

            wifiButton.setBounds(
                    Toolkit.getDefaultToolkit().getScreenSize().width - 190,
                    0,
                    75,
                    30
            );

            wifiButton.setBackground(BUTTON_BG);
            wifiButton.setForeground(TEXT);
            wifiButton.setFocusPainted(false);
            wifiButton.setBorderPainted(false);
            wifiButton.setOpaque(true);

            wifiButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    wifiButton.setBackground(BUTTON_HOVER);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    wifiButton.setBackground(BUTTON_BG);
                }
            });

            wifiButton.addActionListener(e -> {
                rebuildWifiMenu(wifiMenu, MENU_BG, MENU_HOVER, TEXT);

                JPopupMenu.setDefaultLightWeightPopupEnabled(false);

                wifiMenu.show(wifiButton, 0, wifiButton.getHeight());
            });

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            Timer clockTimer = new Timer(1000, e -> {
                LocalTime now = LocalTime.now();
                clockLabel.setText(now.format(formatter));
            });

            clockTimer.setInitialDelay(0);
            clockTimer.start();

            Timer taskbarTimer = new Timer(1000, e ->
                    refreshTaskbar(
                            taskbarPanel,
                            BUTTON_BG,
                            BUTTON_HOVER,
                            TEXT
                    ));

            taskbarTimer.setInitialDelay(0);
            taskbarTimer.start();

            appsButton.setBounds(5, 0, 80, 30);
            systemButton.setBounds(90, 0, 90, 30);
            toolsButton.setBounds(185, 0, 95, 30);

            JButton[] topButtons = { appsButton, systemButton, toolsButton};

            for (JButton button : topButtons) {

                button.setBackground(BUTTON_BG);
                button.setForeground(TEXT);
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                button.setOpaque(true);

                button.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        button.setBackground(BUTTON_HOVER);
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        button.setBackground(BUTTON_BG);
                    }
                });
            }

            panel.setBounds(
                    0,
                    0,
                    Toolkit.getDefaultToolkit().getScreenSize().width,
                    30
            );

            panel.setLayout(null);
            panel.setBackground(PANEL_BG);

            panel.add(taskbarPanel);

            desktop.setUndecorated(true);
            desktop.setFocusableWindowState(false);
            desktop.setAutoRequestFocus(false);
            desktop.setDefaultCloseOperation(EXIT_ON_CLOSE);
            desktop.setResizable(false);

            screen.setLayout(null);
            screen.setBackground(DESKTOP_BG);

            screen.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    try {
                        new ProcessBuilder(
                                "wmctrl",
                                "-r",
                                "desktop",
                                "-b",
                                "add,below,skip_taskbar,skip_pager"
                        ).start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            appsButton.addActionListener(e -> {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                appsMenu.show(appsButton, 0, appsButton.getHeight());
            });

            systemButton.addActionListener(e -> {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                systemMenu.show(systemButton, 0, systemButton.getHeight());
            });

            toolsButton.addActionListener(e -> {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                toolsMenu.show(toolsButton, 0, toolsButton.getHeight());
            });

            desktop.setContentPane(screen);

            screen.add(panel);

            panel.add(appsButton);
            panel.add(systemButton);
            panel.add(wifiButton);
            panel.add(clockLabel);
            panel.add(toolsButton);

            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();

            GraphicsDevice gd =
                    ge.getDefaultScreenDevice();

            Rectangle screenBounds =
                    gd.getDefaultConfiguration().getBounds();

            desktop.setBounds(screenBounds);

            desktop.setVisible(true);

            try {
                new ProcessBuilder(
                        "wmctrl",
                        "-r",
                        "desktop",
                        "-b",
                        "add,below,skip_taskbar,skip_pager"
                ).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void buildApplicationsMenu(JPopupMenu menu,
                                              Color bg,
                                              Color hover,
                                              Color text) {

        JPanel appsPanel = new JPanel();
        appsPanel.setLayout(new BoxLayout(appsPanel, BoxLayout.Y_AXIS));
        appsPanel.setBackground(bg);

        List<Path> dirs = List.of(
                Paths.get(
                        System.getProperty("user.home"),
                        ".local/share/applications"
                ),
                Paths.get("/usr/share/applications"),
                Paths.get("/usr/local/share/applications")
        );

        Set<String> added = new TreeSet<>();

        for (Path dir : dirs) {

            if (!Files.isDirectory(dir))
                continue;

            try {
                Files.walk(dir, 1)
                        .filter(p -> p.toString().endsWith(".desktop"))
                        .forEach(p ->
                                addDesktopEntry(
                                        appsPanel,
                                        p,
                                        added,
                                        bg,
                                        hover,
                                        text
                                ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JScrollPane scrollPane = new JScrollPane(appsPanel);

        scrollPane.setPreferredSize(new Dimension(300, 500));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(bg);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        menu.removeAll();
        menu.add(scrollPane);
    }

    private static void addDesktopEntry(JPanel panel,
                                        Path file,
                                        Set<String> added,
                                        Color bg,
                                        Color hover,
                                        Color text) {

        String name = null;
        String exec = null;
        String iconName = null;
        boolean noDisplay = false;

        try {
            for (String line : Files.readAllLines(file)) {

                if (line.startsWith("Name=") && name == null) {
                    name = line.substring(5).trim();
                } else if (line.startsWith("Exec=") && exec == null) {
                    exec = line.substring(5).trim();
                } else if (line.startsWith("Icon=") && iconName == null) {
                    iconName = line.substring(5).trim();
                } else if (line.equalsIgnoreCase("NoDisplay=true")) {
                    noDisplay = true;
                }
            }
        } catch (IOException e) {
            return;
        }

        if (name == null || exec == null || noDisplay)
            return;

        if (!added.add(name))
            return;

        final String command = sanitizeExec(exec);

        JButton item = new JButton(name);

        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setBackground(bg);
        item.setForeground(text);
        item.setFocusPainted(false);
        item.setBorderPainted(false);
        item.setOpaque(true);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        ImageIcon icon = loadIcon(iconName);

        if (icon != null) {
            Image img = icon.getImage().getScaledInstance(
                    20,
                    20,
                    Image.SCALE_SMOOTH
            );

            item.setIcon(new ImageIcon(img));
            item.setIconTextGap(10);
        }

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(bg);
            }
        });

        item.addActionListener(e -> {
            try {
                new ProcessBuilder("sh", "-c", command).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        panel.add(item);
    }

    private static String sanitizeExec(String exec) {
        return exec.replaceAll("%[fFuUdDnNickvm]", "").trim();
    }

    private static ImageIcon loadIcon(String iconName) {

        if (iconName == null || iconName.isBlank())
            return null;

        if (iconName.startsWith("/")) {
            File f = new File(iconName);

            if (f.exists())
                return new ImageIcon(iconName);
        }

        String[] dirs = {
                System.getProperty("user.home") + "/.local/share/icons",
                "/usr/share/icons/hicolor",
                "/usr/share/pixmaps"
        };

        String[] sizes = { "48x48", "64x64", "128x128", "256x256" };

        for (String dir : dirs) {

            for (String size : sizes) {

                Path p = Paths.get(
                        dir,
                        size,
                        "apps",
                        iconName + ".png"
                );

                if (Files.exists(p))
                    return new ImageIcon(p.toString());
            }

            Path pixmap = Paths.get(dir, iconName + ".png");

            if (Files.exists(pixmap))
                return new ImageIcon(pixmap.toString());
        }

        return null;
    }

    private static void rebuildWifiMenu(JPopupMenu menu,
                                        Color bg,
                                        Color hover,
                                        Color text) {

        menu.removeAll();
        menu.setBackground(bg);
        menu.setBorder(
                BorderFactory.createLineBorder(new Color(36, 42, 58))
        );

        JMenuItem onItem = createMenuItem(
                "Wi-Fi On",
                bg,
                hover,
                text
        );

        onItem.addActionListener(e -> {
            try {
                new ProcessBuilder(
                        "nmcli",
                        "radio",
                        "wifi",
                        "on"
                ).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        JMenuItem offItem = createMenuItem(
                "Wi-Fi Off",
                bg,
                hover,
                text
        );

        offItem.addActionListener(e -> {
            try {
                new ProcessBuilder(
                        "nmcli",
                        "radio",
                        "wifi",
                        "off"
                ).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        menu.add(onItem);
        menu.add(offItem);
        menu.addSeparator();

        try {

            Process p = new ProcessBuilder(
                    "nmcli",
                    "-t",
                    "-f",
                    "SSID",
                    "dev",
                    "wifi"
            ).start();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );

            Set<String> ssids = new LinkedHashSet<>();

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (!line.isEmpty())
                    ssids.add(line);
            }

            if (ssids.isEmpty()) {

                JMenuItem none = createMenuItem(
                        "No networks found",
                        bg,
                        hover,
                        text
                );

                none.setEnabled(false);

                menu.add(none);

            } else {

                for (String ssid : ssids) {

                    JMenuItem net = createMenuItem(
                            ssid,
                            bg,
                            hover,
                            text
                    );

                    net.addActionListener(e ->
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Connect using terminal:\\nnmcli dev wifi connect " + ssid,
                                    "WiFi",
                                    JOptionPane.INFORMATION_MESSAGE
                            ));

                    menu.add(net);
                }
            }

        } catch (Exception ex) {

            JMenuItem err = createMenuItem(
                    "Wi-Fi unavailable",
                    bg,
                    hover,
                    text
            );

            err.setEnabled(false);

            menu.add(err);
        }
    }

    private static JMenuItem createMenuItem(String title,
                                            Color bg,
                                            Color hover,
                                            Color text) {

        JMenuItem item = new JMenuItem(title);

        item.setBackground(bg);
        item.setForeground(text);
        item.setOpaque(true);
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        item.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(bg);
            }
        });

        return item;
    }

    private static void refreshTaskbar(JPanel taskbarPanel,
                                       Color buttonBg,
                                       Color hoverBg,
                                       Color textColor) {

        SwingUtilities.invokeLater(() -> {

            taskbarPanel.removeAll();

            try {

                Process p = new ProcessBuilder(
                        "wmctrl",
                        "-lx"
                ).start();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream())
                );

                String line;

                while ((line = br.readLine()) != null) {

                    String[] parts = line.split("\\s+", 5);

                    if (parts.length < 5)
                        continue;

                    String title = parts[4].trim();

                    if (title.isEmpty())
                        continue;

                    if (title.equalsIgnoreCase("desktop"))
                        continue;

                    JButton appButton =
                            new JButton(shorten(title, 18));

                    appButton.setBackground(buttonBg);
                    appButton.setForeground(textColor);
                    appButton.setFocusPainted(false);
                    appButton.setBorderPainted(false);

                    appButton.addMouseListener(
                            new java.awt.event.MouseAdapter() {

                                @Override
                                public void mouseEntered(
                                        java.awt.event.MouseEvent e
                                ) {
                                    appButton.setBackground(hoverBg);
                                }

                                @Override
                                public void mouseExited(
                                        java.awt.event.MouseEvent e
                                ) {
                                    appButton.setBackground(buttonBg);
                                }
                            });

                    appButton.addActionListener(e -> {
                        try {
                            new ProcessBuilder(
                                    "wmctrl",
                                    "-a",
                                    title
                            ).start();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    taskbarPanel.add(appButton);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            taskbarPanel.revalidate();
            taskbarPanel.repaint();
        });
    }

    private static String shorten(String text, int max) {

        if (text.length() <= max)
            return text;

        return text.substring(0, max - 1) + "…";
    }
}

class WallpaperPanel extends JPanel {

    private final Image wallpaper;

    public WallpaperPanel(String path) {

        wallpaper = new ImageIcon(path).getImage();

        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        g.drawImage(
                wallpaper,
                0,
                0,
                getWidth(),
                getHeight(),
                this
        );
    }
}
