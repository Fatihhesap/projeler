import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException; 


public class Main extends JFrame {

    private JLabel saatTarihLabel;
    private JPanel butonPanel;
    private JPanel northPanel, southPanel;
    private JScrollPane centerScrollPane;
    private JButton restoreButton;

    private final List<Shortcut> shortcuts = new ArrayList<>();
    private final List<Reminder> reminders = new ArrayList<>();

    private static final String CONFIG_FILE = "shortcuts.config";
    private static final String REMINDER_FILE = "reminders.config";
    private static final String SEPARATOR = "|";
    private static final String REGISTRY_KEY = "Kısayol Merkezi";
    private static final String STARTUP_PREF_KEY = "isStartupSet";


    private boolean isMinimized = false;
    private Dimension originalSize;
    private Point originalLocation;

    private final DataFlavor shortcutFlavor = new DataFlavor(Shortcut.class, "Shortcut");
    private final TransferHandler shortcutTransferHandler = new ShortcutTransferHandler();
    private final MouseListener dragListener = new DragMouseAdapter();

    public Main() {
        setTitle("Kısayol Yöneticisi");
        setResizable(true);
        setUndecorated(true);
        setOpacity(0.95f);
        setAlwaysOnTop(true);
        getContentPane().setBackground(new Color(25, 25, 25));
        setLayout(new BorderLayout());

        originalSize = new Dimension(300, 450);
        setSize(originalSize);
        Dimension ekran = Toolkit.getDefaultToolkit().getScreenSize();
        int x = ekran.width - getWidth() - 20;
        int y = ekran.height - getHeight() - 60;
        setLocation(x, y);
        originalLocation = getLocation();

        initComponents();
        loadAndDisplayShortcuts();
        loadReminders();
        startClockTimer();
        startReminderTimer();
        setupExitMenu();
        makeDraggable();

        setRunOnStartup();

        setVisible(true);
    }

    private void setRunOnStartup() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        boolean isAlreadySet = prefs.getBoolean(STARTUP_PREF_KEY, false);

        if (isAlreadySet) {
            return; 
        }

        String appPath = "";
        try {
            appPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

            if (appPath.contains(" ")) {
                appPath = "\"" + appPath + "\"";
            }

        } catch (URISyntaxException e) {
            hataMesaji("Uygulama yolu bulunamadı, otomatik başlatma ayarı yapılamadı.");
            return;
        }

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                String command = "REG ADD \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v \""
                        + REGISTRY_KEY + "\" /t REG_SZ /d " + appPath + " /f";

                Runtime.getRuntime().exec(command);

                prefs.putBoolean(STARTUP_PREF_KEY, true);
                prefs.flush();

                bilgiMesaji("Kısayol Merkezi, bilgisayar açıldığında otomatik başlayacak şekilde ayarlandı.", "Otomatik Başlangıç Ayarı");

            } catch (IOException | BackingStoreException e) { 
                hataMesaji("Otomatik başlatma ayarı yapılamadı. Yönetici izni gerekebilir veya sistem tercihleri kaydedilemedi. Hata: " + e.getMessage());
            }
        } else {
            
        }
    }


    private void initComponents() {
        northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(35, 35, 35));

        JPanel leftHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftHeaderPanel.setOpaque(false);
        JButton minimizeButton = createHeaderButton("-");
        minimizeButton.addActionListener(e -> toggleMinimize());
        leftHeaderPanel.add(minimizeButton);

        JButton reminderButton = createHeaderButton("Hatırlatıcı");
        reminderButton.addActionListener(e -> showAddReminderDialog());
        leftHeaderPanel.add(reminderButton);

        northPanel.add(leftHeaderPanel, BorderLayout.WEST);

        JLabel baslik = new JLabel("Kısayollar", SwingConstants.CENTER);
        baslik.setFont(new Font("Segoe UI", Font.BOLD, 16));
        baslik.setForeground(Color.WHITE);
        northPanel.add(baslik, BorderLayout.CENTER);

        JButton addButton = createHeaderButton("+");
        addButton.addActionListener(e -> showAddShortcutDialog());
        northPanel.add(addButton, BorderLayout.EAST);

        add(northPanel, BorderLayout.NORTH);

        butonPanel = new JPanel();
        butonPanel.setLayout(new GridLayout(0, 1, 5, 5));
        butonPanel.setBackground(new Color(25, 25, 25));
        butonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        butonPanel.setTransferHandler(shortcutTransferHandler);

        centerScrollPane = new JScrollPane(butonPanel);
        centerScrollPane.setBorder(null);
        centerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerScrollPane.setBackground(new Color(25, 25, 25));
        centerScrollPane.getViewport().setBackground(new Color(25, 25, 25));
        add(centerScrollPane, BorderLayout.CENTER);

        southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.setBackground(new Color(35, 35, 35));
        saatTarihLabel = new JLabel("", SwingConstants.CENTER);
        saatTarihLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        saatTarihLabel.setForeground(Color.LIGHT_GRAY);
        southPanel.add(saatTarihLabel);
        add(southPanel, BorderLayout.SOUTH);

        restoreButton = new JButton("O");
        restoreButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        restoreButton.setBackground(new Color(60, 60, 60));
        restoreButton.setForeground(Color.WHITE);
        restoreButton.setFocusPainted(false);
        restoreButton.setBorder(BorderFactory.createLineBorder(Color.CYAN));
        restoreButton.addActionListener(e -> toggleMinimize());
        restoreButton.setVisible(false);
    }

    private JButton createHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(new Color(35, 35, 35));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(2, 10, 2, 10));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(new Color(70, 70, 70)); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(new Color(35, 35, 35)); }
        });
        return button;
    }

    private void rebuildButtonPanel() {
        butonPanel.removeAll();
        for (Shortcut sc : shortcuts) {
            createAndAddShortcutCard(sc);
        }
        butonPanel.revalidate();
        butonPanel.repaint();
    }

    private void createAndAddShortcutCard(Shortcut shortcut) {
        ShortcutCardPanel card = new ShortcutCardPanel(new BorderLayout(3, 0));
        card.setBackground(new Color(40, 40, 40));
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        card.putClientProperty("shortcut", shortcut);
        card.setTransferHandler(shortcutTransferHandler);
        card.addMouseListener(dragListener);

        JButton actionButton = createActionButton(shortcut.name, () -> islemGerceklestir(shortcut.action));
        card.add(actionButton, BorderLayout.CENTER);

        JButton optionsButton = createOptionsButton(shortcut);
        card.add(optionsButton, BorderLayout.EAST);

        butonPanel.add(card);
    }


    private JButton createActionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        button.setBackground(new Color(40, 40, 40));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 12, 8, 12));
        button.setOpaque(true);

        button.addActionListener(e -> action.run());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(60, 60, 60));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(40, 40, 40));
            }
        });
        return button;
    }

    private JButton createOptionsButton(Shortcut shortcut) {
        JButton button = new JButton("…");
        button.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        button.setBackground(new Color(40, 40, 40));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 8, 8, 8));
        button.setOpaque(true);

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Düzenle");
        editItem.addActionListener(e -> showEditShortcutDialog(shortcut));

        JMenuItem deleteItem = new JMenuItem("Sil");
        deleteItem.addActionListener(e -> deleteShortcut(shortcut));

        popupMenu.add(editItem);
        popupMenu.add(deleteItem);

        button.addActionListener(e -> {
            popupMenu.show(button, 0, button.getHeight());
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 70, 70));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(40, 40, 40));
            }
        });
        return button;
    }


    private void showAddShortcutDialog() {
        JTextField nameField = new JTextField();
        JTextField actionField = new JTextField();
        Object[] fields = { "Kısayol İsmi:", nameField, "İşlem (URL, Program Yolu veya Komut):", actionField };

        int result = JOptionPane.showConfirmDialog(this, fields, "Yeni Kısayol Ekle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String action = actionField.getText().trim();
            if (!name.isEmpty() && !action.isEmpty()) {
                Shortcut sc = new Shortcut(name, action);
                shortcuts.add(sc);
                saveShortcuts();
                rebuildButtonPanel();
            }
        }
    }

    private void showEditShortcutDialog(Shortcut oldShortcut) {
        JTextField nameField = new JTextField(oldShortcut.name);
        JTextField actionField = new JTextField(oldShortcut.action);
        Object[] fields = { "Kısayol İsmi:", nameField, "İşlem (URL, Program Yolu veya Komut):", actionField };

        int result = JOptionPane.showConfirmDialog(this, fields, "Kısayolu Düzenle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newAction = actionField.getText().trim();

            if (!newName.isEmpty() && !newAction.isEmpty()) {
                Shortcut newShortcut = new Shortcut(newName, newAction);
                int index = shortcuts.indexOf(oldShortcut);
                if (index != -1) {
                    shortcuts.set(index, newShortcut);
                    saveShortcuts();
                    rebuildButtonPanel();
                } else {
                    hataMesaji("Kısayol güncellenirken hata oluştu.");
                }
            }
        }
    }

    private void deleteShortcut(Shortcut shortcut) {
        int onay = JOptionPane.showConfirmDialog(this,
                "'" + shortcut.name + "' kısayolunu silmek istiyor musun?",
                "Silmeyi Onayla", JOptionPane.YES_NO_OPTION);

        if (onay == JOptionPane.YES_OPTION) {
            shortcuts.remove(shortcut);
            saveShortcuts();
            rebuildButtonPanel();
        }
    }

    private void showAddReminderDialog() {
        JTextField nameField = new JTextField();
        JTextField dateField = new JTextField(LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        Object[] fields = {
                "Hatırlatıcı Notu:", nameField,
                "Tarih/Saat (gg.AA.yyyy SS:dd):", dateField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Yeni Hatırlatıcı Ekle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String dateTimeStr = dateField.getText().trim();

            if (!name.isEmpty() && !dateTimeStr.isEmpty()) {
                try {
                    LocalDateTime reminderTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                    if (reminderTime.isAfter(LocalDateTime.now())) {
                        Reminder r = new Reminder(name, reminderTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        reminders.add(r);
                        saveReminders();
                        bilgiMesaji("'" + name + "' için hatırlatıcı başarıyla eklendi!", "Başarılı");
                    } else {
                        hataMesaji("Geçmiş bir tarih/saat seçilemez.");
                    }
                } catch (Exception e) {
                    hataMesaji("Geçersiz tarih/saat formatı. Lütfen 'gg.AA.yyyy SS:dd' formatını kullanın.");
                }
            }
        }
    }

    private void startReminderTimer() {
        Timer timer = new Timer(5000, e -> checkReminders());

        checkRemindersOnStartup();

        timer.start();
    }

    private void checkRemindersOnStartup() {
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> finishedReminders = new ArrayList<>();

        for (Reminder r : reminders) {
            try {
                LocalDateTime reminderTime = LocalDateTime.parse(r.dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (reminderTime.isBefore(now)) {
                    finishedReminders.add(r);
                }
            } catch (Exception ex) {
            }
        }

        if (!finishedReminders.isEmpty()) {
            showReminderNotification(finishedReminders, "Uygulama kapalıyken kaçırdığınız hatırlatıcılar:");
        }

        reminders.removeAll(finishedReminders);
        saveReminders();
    }

    private void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> finishedReminders = new ArrayList<>();

        for (Reminder r : reminders) {
            try {
                LocalDateTime reminderTime = LocalDateTime.parse(r.dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (reminderTime.isBefore(now.plusSeconds(5))) {
                    finishedReminders.add(r);
                }
            } catch (Exception ex) {
            }
        }

        if (!finishedReminders.isEmpty()) {
            showReminderNotification(finishedReminders, "Zamanı Gelen Hatırlatıcılar:");
        }

        reminders.removeAll(finishedReminders);
        saveReminders();
    }

    private void showReminderNotification(List<Reminder> finishedReminders, String title) {
        StringBuilder sb = new StringBuilder("<html><b>" + title + "</b><ul>");

        for (Reminder r : finishedReminders) {
            try {
                LocalDateTime reminderTime = LocalDateTime.parse(r.dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String formattedTime = reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                sb.append("<li>[").append(formattedTime).append("] ").append(r.note).append("</li>");
            } catch (Exception e) {
                sb.append("<li>").append(r.note).append(" (Zaman hatası)</li>");
            }
        }
        sb.append("</ul></html>");

        JOptionPane.showMessageDialog(this, sb.toString(), "HATIRLATICI BİLDİRİMİ", JOptionPane.INFORMATION_MESSAGE);
    }


    private void loadAndDisplayShortcuts() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] parts = line.split("\\" + SEPARATOR, 2);
                    if (parts.length == 2) {
                        shortcuts.add(new Shortcut(parts[0], parts[1]));
                    }
                }
            } catch (IOException e) {
                hataMesaji("Kısayol yapılandırması okunamadı: " + e.getMessage());
            }
        }

        if (shortcuts.isEmpty()) {
            shortcuts.add(new Shortcut("Kapat", "SYSTEM_SHUTDOWN"));
            shortcuts.add(new Shortcut("Yeniden B.", "SYSTEM_RESTART"));
            saveShortcuts();
        }

        rebuildButtonPanel();
    }

    private void saveShortcuts() {
        Path path = Paths.get(CONFIG_FILE);
        try {
            List<String> lines = shortcuts.stream()
                    .map(sc -> sc.name + SEPARATOR + sc.action)
                    .collect(Collectors.toList());
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            hataMesaji("Kısayollar kaydedilemedi: " + e.getMessage());
        }
    }

    private void saveReminders() {
        Path path = Paths.get(REMINDER_FILE);
        try {
            List<String> lines = reminders.stream()
                    .map(r -> r.note + SEPARATOR + r.dateTime)
                    .collect(Collectors.toList());
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            hataMesaji("Hatırlatıcılar kaydedilemedi: " + e.getMessage());
        }
    }

    private void loadReminders() {
        Path path = Paths.get(REMINDER_FILE);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] parts = line.split("\\" + SEPARATOR, 2);
                    if (parts.length == 2) {
                        reminders.add(new Reminder(parts[0], parts[1]));
                    }
                }
            } catch (IOException e) {
                hataMesaji("Hatırlatıcı yapılandırması okunamadı: " + e.getMessage());
            }
        }
    }



    private void toggleMinimize() {
        isMinimized = !isMinimized;
        if (isMinimized) {
            originalLocation = getLocation();
            northPanel.setVisible(false);
            centerScrollPane.setVisible(false);
            southPanel.setVisible(false);
            getContentPane().add(restoreButton, BorderLayout.CENTER);
            restoreButton.setVisible(true);
            setSize(40, 40);
            setLocation(originalLocation.x + originalSize.width - 40,
                    originalLocation.y + originalSize.height - 40);
        } else {
            restoreButton.setVisible(false);
            getContentPane().remove(restoreButton);
            northPanel.setVisible(true);
            centerScrollPane.setVisible(true);
            southPanel.setVisible(true);
            setSize(originalSize);
            setLocation(originalLocation);
        }
        revalidate();
        repaint();
    }

    private void islemGerceklestir(String action) {
        if (action.equals("SYSTEM_SHUTDOWN")) sistemKapat();
        else if (action.equals("SYSTEM_RESTART")) sistemYenidenBaslat();
        else if (action.startsWith("http://") || action.startsWith("https://") || action.startsWith("www.")) siteAc(action);
        else if (new File(action).isDirectory()) klasorAc(action);
        else programAc(action);
    }
    private void programAc(String path) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win") && path.contains(" ")) {
                new ProcessBuilder("cmd", "/c", "start", "\"\"", path).start();
            } else { new ProcessBuilder(path).start(); }
        } catch (IOException e) { hataMesaji("Program/Komut çalıştırılamadı:\n" + path + "\nHata: " + e.getMessage()); }
    }
    private void siteAc(String url) {
        try {
            if (url.startsWith("www.")) { url = "http://" + url; }
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) { hataMesaji("Site açılamadı:\n" + url); }
    }
    private void klasorAc(String path) {
        try { Desktop.getDesktop().open(new File(path)); }
        catch (IOException e) { hataMesaji("Klasör bulunamadı:\n" + path); }
    }
    private void sistemKapat() {
        int onay = JOptionPane.showConfirmDialog(this, "Bilgisayarı kapatmak istiyor musun?", "Onayla", JOptionPane.YES_NO_OPTION);
        if (onay == JOptionPane.YES_OPTION) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) Runtime.getRuntime().exec("shutdown -s -t 3");
                else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) Runtime.getRuntime().exec("shutdown -h now");
            } catch (IOException e) { hataMesaji("Kapatma komutu başarısız!"); }
        }
    }
    private void sistemYenidenBaslat() {
        int onay = JOptionPane.showConfirmDialog(this, "Bilgisayarı yeniden başlatmak istiyor musun?", "Onayla", JOptionPane.YES_NO_OPTION);
        if (onay == JOptionPane.YES_OPTION) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) Runtime.getRuntime().exec("shutdown -r -t 3");
                else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) Runtime.getRuntime().exec("reboot");
            } catch (IOException e) { hataMesaji("Yeniden başlatma komutu başarısız!"); }
        }
    }
    private void bilgiMesaji(String msg, String title) { JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE); }
    private void hataMesaji(String msg) { JOptionPane.showMessageDialog(this, msg, "Hata", JOptionPane.ERROR_MESSAGE); }
    private void startClockTimer() {
        Timer timer = new Timer(1000, e -> {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss | dd.MM.yyyy");
            saatTarihLabel.setText(LocalDateTime.now().format(dtf));
        });
        timer.start();
    }
    private void setupExitMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem cikis = new JMenuItem("Uygulamayı Kapat");
        cikis.addActionListener(e -> System.exit(0));
        menu.add(cikis);
        MouseAdapter popupListener = new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY());
            }
        };
        northPanel.addMouseListener(popupListener);
        southPanel.addMouseListener(popupListener);
        saatTarihLabel.addMouseListener(popupListener);
    }
    private void makeDraggable() {
        final Point[] initialClick = new Point[1];
        MouseAdapter dragAdapter = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isMinimized) {
                    initialClick[0] = e.getPoint();
                    getComponentAt(initialClick[0]);
                }
            }
            public void mouseDragged(MouseEvent e) {
                if (!isMinimized) {
                    int thisX = getLocation().x;
                    int thisY = getLocation().y;
                    int xMoved = e.getX() - initialClick[0].x;
                    int yMoved = e.getY() - initialClick[0].y;
                    int X = thisX + xMoved;
                    int Y = thisY + yMoved;
                    setLocation(X, Y);
                    originalLocation = getLocation();
                }
            }
        };
        northPanel.addMouseListener(dragAdapter);
        northPanel.addMouseMotionListener(dragAdapter);
    }

    private record Shortcut(String name, String action) {}

    private record Reminder(String note, String dateTime) {}

    private static class ShortcutCardPanel extends JPanel {
        public ShortcutCardPanel(LayoutManager layout) {
            super(layout);
        }
    }


    private class DragMouseAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            JComponent card = (JComponent) e.getSource();
            TransferHandler handler = card.getTransferHandler();
            handler.exportAsDrag(card, e, TransferHandler.MOVE);
        }
    }

    private class ShortcutTransferable implements Transferable {
        private final Shortcut shortcut;
        public ShortcutTransferable(Shortcut shortcut) { this.shortcut = shortcut; }
        @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{shortcutFlavor}; }
        @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return flavor.equals(shortcutFlavor); }
        @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(shortcutFlavor)) return shortcut;
            else throw new UnsupportedFlavorException(flavor);
        }
    }

    private class ShortcutTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            ShortcutCardPanel card = (ShortcutCardPanel) c;
            Shortcut sc = (Shortcut) card.getClientProperty("shortcut");
            return new ShortcutTransferable(sc);
        }

        @Override
        public int getSourceActions(JComponent c) { return MOVE; }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(shortcutFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                Shortcut draggedShortcut = (Shortcut) support.getTransferable().getTransferData(shortcutFlavor);
                DropLocation dl = support.getDropLocation();
                Point dropPoint = dl.getDropPoint();
                Component targetComponent = butonPanel.getComponentAt(dropPoint);
                ShortcutCardPanel targetCard = null;
                if (targetComponent instanceof ShortcutCardPanel) {
                    targetCard = (ShortcutCardPanel) targetComponent;
                } else if (targetComponent != null && targetComponent.getParent() instanceof ShortcutCardPanel) {
                    targetCard = (ShortcutCardPanel) targetComponent.getParent();
                }

                int draggedIndex = shortcuts.indexOf(draggedShortcut);
                int targetIndex;

                if (targetCard != null) {
                    Shortcut targetShortcut = (Shortcut) targetCard.getClientProperty("shortcut");
                    targetIndex = shortcuts.indexOf(targetShortcut);
                } else {
                    targetIndex = shortcuts.size();
                }

                if (draggedIndex == targetIndex) {
                    return false;
                }

                Shortcut item = shortcuts.remove(draggedIndex);
                if (draggedIndex < targetIndex) {
                    shortcuts.add(targetIndex - 1, item);
                } else {
                    shortcuts.add(targetIndex, item);
                }

                saveShortcuts();
                rebuildButtonPanel();

                return true;

            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
