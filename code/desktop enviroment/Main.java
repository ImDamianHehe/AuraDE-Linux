import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;

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

            JButton menuButton = new JButton("Menu");
            JPopupMenu menu = new JPopupMenu();

            JMenuItem terminalItem = new JMenuItem("Terminal");
            JMenuItem browserItem = new JMenuItem("Browser");
            JMenuItem fileItem = new JMenuItem("File Manager");
            JMenuItem logoutItem = new JMenuItem("Logout");

            JPanel panel = new JPanel();

            JLabel clockLabel = new JLabel();
            clockLabel.setFont(new Font("Arial", Font.BOLD, 16));
            clockLabel.setForeground(TEXT);
            clockLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            clockLabel.setBounds(
                    Toolkit.getDefaultToolkit().getScreenSize().width - 120,
                    0,
                    110,
                    30
            );

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            Timer clockTimer = new Timer(1000, e -> {
                LocalTime now = LocalTime.now();
                clockLabel.setText(now.format(formatter));
            });

            clockTimer.setInitialDelay(0);
            clockTimer.start();

            menuButton.setBounds(5, 0, 80, 30);

            menuButton.setBackground(BUTTON_BG);
            menuButton.setForeground(TEXT);
            menuButton.setFocusPainted(false);
            menuButton.setBorderPainted(false);
            menuButton.setOpaque(true);

            menuButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    menuButton.setBackground(BUTTON_HOVER);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    menuButton.setBackground(BUTTON_BG);
                }
            });

            panel.setBounds(
                    0,
                    0,
                    Toolkit.getDefaultToolkit().getScreenSize().width,
                    30
            );

            panel.setLayout(null);
            panel.setBackground(PANEL_BG);

            menu.setBackground(MENU_BG);
            menu.setBorder(BorderFactory.createLineBorder(new Color(36, 42, 58)));

            JMenuItem[] items = {
                    terminalItem,
                    browserItem,
                    fileItem,
                    logoutItem
            };

            for (JMenuItem item : items) {
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

            desktop.setUndecorated(true);
            desktop.setFocusableWindowState(false);
            desktop.setAutoRequestFocus(false);
            desktop.setDefaultCloseOperation(EXIT_ON_CLOSE);
            desktop.setResizable(false);

            screen.setLayout(null);
            screen.setBackground(DESKTOP_BG);

            menuButton.addActionListener(e -> {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                menu.show(menuButton, 0, menuButton.getHeight());
            });

            terminalItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("auramass").start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            browserItem.addActionListener(e -> {
                try {
                    new ProcessBuilder("firefox").start();
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

            logoutItem.addActionListener(e -> System.exit(0));

            desktop.setContentPane(screen);

            screen.add(panel);

            panel.add(menuButton);
            panel.add(clockLabel);

            menu.add(terminalItem);
            menu.add(browserItem);
            menu.add(fileItem);
            menu.add(logoutItem);

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
                        "add,below"
                ).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
