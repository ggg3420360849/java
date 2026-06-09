package contacts.ui;

import contacts.io.AddressBookStorage;
import contacts.io.CsvService;
import contacts.io.VCardService;
import contacts.model.AddressBook;
import contacts.model.Contact;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends JFrame {
    private static final Color APP_BG = new Color(0xF2FBF8);
    private static final Color SURFACE_BG = Color.WHITE;
    private static final Color SURFACE_ALT = new Color(0xF7FCFB);
    private static final Color BORDER_COLOR = new Color(0xD8EAE3);
    private static final Color TEXT_PRIMARY = new Color(0x25423B);
    private static final Color TEXT_MUTED = new Color(0x6D8A82);
    private static final Color ACCENT = new Color(0x4DAF95);
    private static final Color ACCENT_SOFT = new Color(0xE6F6F0);
    private static final Color ACCENT_BORDER = new Color(0xB9E0D3);
    private static final Color DANGER = new Color(0xC96B6B);
    private static final Color DANGER_SOFT = new Color(0xFBEFEF);
    private static final Color GROUP_CARD_BG = new Color(0xEEF8F4);
    private static final Color ROW_ALT = new Color(0xFBFEFD);
    private static final Color SELECTION_BG = new Color(0xE2F4ED);
    private static final Color[] AVATAR_FALLBACK_COLORS = {
            new Color(0x4DAF95),
            new Color(0x5DB7D1),
            new Color(0xF2A65A),
            new Color(0x7EC7A2),
            new Color(0xE58B8B),
            new Color(0x89C2A8)
    };
    private static final Font TITLE_FONT = new Font("Microsoft YaHei", Font.BOLD, 24);
    private static final Font SUBTITLE_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("Microsoft YaHei", Font.PLAIN, 12);
    private static final Font BODY_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final Font SMALL_FONT = new Font("Microsoft YaHei", Font.PLAIN, 11);
    private static final int LIST_AVATAR_SIZE = 28;

    private final AddressBook addressBook;
    private final AddressBookStorage storage;
    private final CsvService csvService = new CsvService();
    private final VCardService vCardService = new VCardService();
    private final ContactTableModel tableModel = new ContactTableModel();
    private final JTable contactTable = new JTable(tableModel);
    private final DefaultListModel<String> groupListModel = new DefaultListModel<>();
    private final JList<String> groupList = new JList<>(groupListModel);
    private final ContactPosterPanel posterPanel = new ContactPosterPanel();
    private final JTextField searchField = new JTextField(18);
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel tableTitleLabel = new JLabel("联系人列表");
    private final TableRowSorter<ContactTableModel> tableSorter;
    private final JPopupMenu tablePopupMenu;
    private final Map<String, Icon> avatarCache = new HashMap<>();
    private JSplitPane mainSplit;
    private JPanel rightPanel;
    private JLabel sideToggleLabel;
    private JCheckBox selectAllBox;
    private boolean detailVisible;
    private int rightPanelWidth = 380;

    public MainFrame(AddressBook addressBook, AddressBookStorage storage) {
        super("通讯录管理");
        this.addressBook = addressBook;
        this.storage = storage;

        applyLookAndFeel();
        tableSorter = new TableRowSorter<>(tableModel);
        contactTable.setRowSorter(tableSorter);
        tablePopupMenu = createCompactTablePopupMenu();

        setTitle("通讯录管理");
        tableTitleLabel.setText("联系人");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BG);
        posterPanel.setOnImportCsv(() -> importContacts("csv"));
        posterPanel.setOnImportVcf(() -> importContacts("vcf"));
        add(createCompactTopBar(), BorderLayout.NORTH);
        add(createCompactMainPanel(), BorderLayout.CENTER);
        add(createCompactStatusBar(), BorderLayout.SOUTH);
        registerKeyboardShortcuts();
        refreshGroups();
        refreshContacts();
    }

    private void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        UIManager.put("control", APP_BG);
        UIManager.put("info", SURFACE_BG);
        UIManager.put("nimbusBase", ACCENT);
        UIManager.put("nimbusFocus", ACCENT_BORDER);
        UIManager.put("text", TEXT_PRIMARY);
        UIManager.put("MenuBar.background", SURFACE_BG);
        UIManager.put("Menu.background", SURFACE_BG);
        UIManager.put("MenuItem.background", SURFACE_BG);
    }

    private JPanel createCompactTopBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(APP_BG);
        wrapper.setBorder(new EmptyBorder(18, 20, 14, 20));
        wrapper.add(createCompactHeroPanel(), BorderLayout.NORTH);
        wrapper.add(createCompactActionPanel(), BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createCompactHeroPanel() {
        JPanel panel = createSurfacePanel(new BorderLayout(18, 0), new EmptyBorder(18, 20, 18, 20));

        JLabel titleLabel = new JLabel("通讯录");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(createCompactSearchPanel(), BorderLayout.EAST);
        return panel;
    }

    private JPanel createCompactSearchPanel() {
        JPanel searchCard = new JPanel(new BorderLayout(8, 0));
        searchCard.setBackground(SURFACE_ALT);
        searchCard.setBorder(compoundBorder(roundedLineBorder(ACCENT_BORDER), new EmptyBorder(8, 12, 8, 12)));
        searchCard.setPreferredSize(new Dimension(300, 48));

        searchField.setFont(BODY_FONT);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(ACCENT);
        searchField.putClientProperty("JTextField.placeholderText", "搜索联系人");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshContacts();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshContacts();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshContacts();
            }
        });

        JButton clearButton = createGhostButton("清空");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });

        searchCard.add(searchField, BorderLayout.CENTER);
        searchCard.add(clearButton, BorderLayout.EAST);
        return searchCard;
    }

    private JPanel createCompactActionPanel() {
        JPanel card = createSurfacePanel(new BorderLayout(), new EmptyBorder(12, 14, 12, 14));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton addButton = createFilledButton("新增联系人", ACCENT, Color.WHITE, ACCENT);
        JButton editButton = createSoftButton("编辑", ACCENT_SOFT, TEXT_PRIMARY, ACCENT_BORDER);
        JButton deleteButton = createSoftButton("删除", DANGER_SOFT, DANGER, blend(DANGER, Color.WHITE, 0.45f));
        JButton addGroupButton = createSoftButton("新建分组", GROUP_CARD_BG, TEXT_PRIMARY, BORDER_COLOR);
        JButton deleteGroupButton = createSoftButton("删除分组", SURFACE_BG, TEXT_MUTED, BORDER_COLOR);
        JButton joinGroupButton = createSoftButton("加入分组", GROUP_CARD_BG, TEXT_PRIMARY, BORDER_COLOR);
        JButton leaveGroupButton = createSoftButton("移出分组", SURFACE_BG, TEXT_MUTED, BORDER_COLOR);

        addButton.addActionListener(e -> openContactDialog(null));
        editButton.addActionListener(e -> editSelectedContact());
        deleteButton.addActionListener(e -> deleteCheckedContacts());
        addGroupButton.addActionListener(e -> addGroup());
        deleteGroupButton.addActionListener(e -> deleteSelectedGroup());
        joinGroupButton.addActionListener(e -> assignGroupToChecked());
        leaveGroupButton.addActionListener(e -> removeGroupFromChecked());

        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(addGroupButton);
        actions.add(deleteGroupButton);
        actions.add(joinGroupButton);
        actions.add(leaveGroupButton);

        card.add(actions, BorderLayout.WEST);
        return card;
    }

    private JPanel createCompactMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(APP_BG);
        mainPanel.setBorder(new EmptyBorder(0, 20, 18, 20));

        JPanel sidebarPanel = createCompactGroupPanel();
        JPanel tablePanel = createCompactTablePanel();
        JPanel contactWithToggle = new JPanel(new BorderLayout());
        contactWithToggle.setOpaque(false);
        contactWithToggle.add(tablePanel, BorderLayout.CENTER);
        contactWithToggle.add(createSideToggle(), BorderLayout.EAST);

        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setMinimumSize(new Dimension(260, 0));
        JPanel detailCard = createSurfacePanel(new BorderLayout(), new EmptyBorder(0, 0, 0, 0));
        detailCard.add(posterPanel, BorderLayout.CENTER);
        rightPanel.add(detailCard, BorderLayout.CENTER);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, contactWithToggle);
        leftSplit.setDividerLocation(230);
        leftSplit.setDividerSize(6);
        leftSplit.setBorder(BorderFactory.createEmptyBorder());
        leftSplit.setOpaque(false);
        leftSplit.setResizeWeight(0.0);
        styleSplitPane(leftSplit);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerSize(8);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setOpaque(false);
        mainSplit.setResizeWeight(1.0);
        styleSplitPane(mainSplit);

        mainPanel.add(mainSplit, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::hideDetailPanel);
        return mainPanel;
    }

    private JPanel createCompactGroupPanel() {
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setFont(BODY_FONT);
        groupList.setFixedCellHeight(46);
        groupList.setCellRenderer(new GroupListRenderer());
        groupList.setBackground(GROUP_CARD_BG);
        groupList.setSelectionBackground(ACCENT_SOFT);
        groupList.setSelectionForeground(TEXT_PRIMARY);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshContacts();
            }
        });

        JScrollPane scrollPane = new JScrollPane(groupList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(GROUP_CARD_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = createSurfacePanel(new BorderLayout(0, 12), new EmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(230, 0));
        panel.setMinimumSize(new Dimension(200, 0));

        JLabel title = new JLabel("分组");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBackground(GROUP_CARD_BG);
        listWrapper.setBorder(compoundBorder(roundedLineBorder(BORDER_COLOR), new EmptyBorder(8, 8, 8, 8)));
        listWrapper.add(scrollPane, BorderLayout.CENTER);

        panel.add(title, BorderLayout.NORTH);
        panel.add(listWrapper, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCompactTablePanel() {
        configureTable();

        JScrollPane scrollPane = new JScrollPane(contactTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(SURFACE_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = createSurfacePanel(new BorderLayout(0, 12), new EmptyBorder(16, 16, 16, 16));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        tableTitleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        tableTitleLabel.setForeground(TEXT_PRIMARY);

        selectAllBox = new JCheckBox("全选");
        selectAllBox.setOpaque(false);
        selectAllBox.setFont(BODY_FONT);
        selectAllBox.setForeground(TEXT_MUTED);
        selectAllBox.addActionListener(e -> {
            tableModel.toggleAll(selectAllBox.isSelected());
            updateStatus();
        });

        // right-click hint
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(tableTitleLabel);
        JLabel headerHint = new JLabel("右键表头选择字段");
        headerHint.setFont(SMALL_FONT);
        headerHint.setForeground(TEXT_MUTED);
        titlePanel.add(headerHint);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(selectAllBox, BorderLayout.EAST);

        contactTable.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) chooseColumns(); }
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) chooseColumns(); }
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCompactStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE_BG);
        bar.setBorder(compoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR), new EmptyBorder(8, 18, 8, 18)));
        statusLabel.setFont(SMALL_FONT);
        statusLabel.setForeground(TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private JPopupMenu createCompactTablePopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(popupItem("编辑联系人", e -> editSelectedContact()));
        menu.add(popupItem("删除联系人", e -> deleteCheckedContacts()));
        menu.addSeparator();
        menu.add(popupItem("加入分组", e -> assignGroupToChecked()));
        menu.add(popupItem("移出分组", e -> removeGroupFromChecked()));
        menu.addSeparator();
        menu.add(popupItem("导出到 CSV", e -> exportContacts("csv")));
        menu.add(popupItem("导出到 vCard", e -> exportContacts("vcf")));
        return menu;
    }

    private JPanel createTopBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(APP_BG);
        wrapper.setBorder(new EmptyBorder(18, 20, 14, 20));
        wrapper.add(createHeroPanel(), BorderLayout.NORTH);
        wrapper.add(createActionPanel(), BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createHeroPanel() {
        JPanel panel = createSurfacePanel(new BorderLayout(18, 0), new EmptyBorder(18, 20, 18, 20));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("通讯录");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("管理联系人、分组、照片和详细资料");
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(createSearchPanel(), BorderLayout.EAST);
        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel searchCard = new JPanel(new BorderLayout(10, 0));
        searchCard.setBackground(SURFACE_ALT);
        searchCard.setBorder(compoundBorder(roundedLineBorder(ACCENT_BORDER), new EmptyBorder(8, 12, 8, 12)));
        searchCard.setPreferredSize(new Dimension(340, 48));

        JLabel iconLabel = new JLabel("搜索");
        iconLabel.setFont(BODY_FONT);
        iconLabel.setForeground(TEXT_MUTED);

        searchField.setFont(BODY_FONT);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(ACCENT);
        searchField.putClientProperty("JTextField.placeholderText", "按姓名、电话、手机或拼音检索");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshContacts();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshContacts();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshContacts();
            }
        });

        JButton clearButton = createGhostButton("清空");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });

        searchCard.add(iconLabel, BorderLayout.WEST);
        searchCard.add(searchField, BorderLayout.CENTER);
        searchCard.add(clearButton, BorderLayout.EAST);
        return searchCard;
    }

    private JPanel createActionPanel() {
        JPanel card = createSurfacePanel(new BorderLayout(), new EmptyBorder(12, 14, 12, 14));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton addButton = createFilledButton("新增联系人", ACCENT, Color.WHITE, ACCENT);
        JButton editButton = createSoftButton("编辑", ACCENT_SOFT, TEXT_PRIMARY, ACCENT_BORDER);
        JButton deleteButton = createSoftButton("删除", DANGER_SOFT, DANGER, blend(DANGER, Color.WHITE, 0.45f));
        JButton addGroupButton = createSoftButton("新建分组", GROUP_CARD_BG, TEXT_PRIMARY, BORDER_COLOR);
        JButton deleteGroupButton = createSoftButton("删除分组", SURFACE_BG, TEXT_MUTED, BORDER_COLOR);
        JButton joinGroupButton = createSoftButton("加入分组", GROUP_CARD_BG, TEXT_PRIMARY, BORDER_COLOR);
        JButton leaveGroupButton = createSoftButton("移出分组", SURFACE_BG, TEXT_MUTED, BORDER_COLOR);

        addButton.addActionListener(e -> openContactDialog(null));
        editButton.addActionListener(e -> editSelectedContact());
        deleteButton.addActionListener(e -> deleteCheckedContacts());
        addGroupButton.addActionListener(e -> addGroup());
        deleteGroupButton.addActionListener(e -> deleteSelectedGroup());
        joinGroupButton.addActionListener(e -> assignGroupToChecked());
        leaveGroupButton.addActionListener(e -> removeGroupFromChecked());

        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(addGroupButton);
        actions.add(deleteGroupButton);
        actions.add(joinGroupButton);
        actions.add(leaveGroupButton);

        JLabel hintLabel = new JLabel("双击联系人可直接编辑，右键查看更多操作");
        hintLabel.setFont(SMALL_FONT);
        hintLabel.setForeground(TEXT_MUTED);

        card.add(actions, BorderLayout.WEST);
        card.add(hintLabel, BorderLayout.EAST);
        return card;
    }

    private JButton createFilledButton(String text, Color background, Color foreground, Color borderColor) {
        return createActionButton(text, Color.WHITE, ACCENT, ACCENT_BORDER, ACCENT_SOFT);
    }

    private JButton createSoftButton(String text, Color background, Color foreground, Color borderColor) {
        return createActionButton(text, background, foreground, borderColor, blend(background, Color.BLACK, 0.03f));
    }

    private JButton createGhostButton(String text) {
        JButton button = createActionButton(text, SURFACE_BG, TEXT_MUTED, BORDER_COLOR, SURFACE_ALT);
        button.setBorder(compoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR), new EmptyBorder(6, 12, 6, 12)));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        return button;
    }

    private JButton createActionButton(String text, Color background, Color foreground, Color borderColor, Color hoverBackground) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(compoundBorder(roundedLineBorder(borderColor), new EmptyBorder(9, 14, 9, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverBackground);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(APP_BG);
        mainPanel.setBorder(new EmptyBorder(0, 20, 18, 20));

        JPanel sidebarPanel = createGroupPanel();
        JPanel tablePanel = createTablePanel();
        JPanel contactWithToggle = new JPanel(new BorderLayout());
        contactWithToggle.setOpaque(false);
        contactWithToggle.add(tablePanel, BorderLayout.CENTER);
        contactWithToggle.add(createSideToggle(), BorderLayout.EAST);

        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setMinimumSize(new Dimension(260, 0));
        JPanel detailCard = createSurfacePanel(new BorderLayout(), new EmptyBorder(0, 0, 0, 0));
        detailCard.add(posterPanel, BorderLayout.CENTER);
        rightPanel.add(detailCard, BorderLayout.CENTER);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, contactWithToggle);
        leftSplit.setDividerLocation(230);
        leftSplit.setDividerSize(6);
        leftSplit.setBorder(BorderFactory.createEmptyBorder());
        leftSplit.setOpaque(false);
        leftSplit.setResizeWeight(0.0);
        styleSplitPane(leftSplit);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerSize(8);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setOpaque(false);
        mainSplit.setResizeWeight(1.0);
        styleSplitPane(mainSplit);

        mainPanel.add(mainSplit, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::hideDetailPanel);
        return mainPanel;
    }

    private JPanel createGroupPanel() {
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setFont(BODY_FONT);
        groupList.setFixedCellHeight(46);
        groupList.setCellRenderer(new GroupListRenderer());
        groupList.setBackground(GROUP_CARD_BG);
        groupList.setSelectionBackground(ACCENT_SOFT);
        groupList.setSelectionForeground(TEXT_PRIMARY);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshContacts();
            }
        });

        JScrollPane scrollPane = new JScrollPane(groupList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(GROUP_CARD_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = createSurfacePanel(new BorderLayout(0, 12), new EmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(230, 0));
        panel.setMinimumSize(new Dimension(200, 0));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("分组");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("快速筛选联系人");
        subtitle.setFont(SMALL_FONT);
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        header.add(title);
        header.add(subtitle);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBackground(GROUP_CARD_BG);
        listWrapper.setBorder(compoundBorder(roundedLineBorder(BORDER_COLOR), new EmptyBorder(8, 8, 8, 8)));
        listWrapper.add(scrollPane, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(listWrapper, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTablePanel() {
        configureTable();

        JScrollPane scrollPane = new JScrollPane(contactTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(SURFACE_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = createSurfacePanel(new BorderLayout(0, 12), new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        tableTitleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        tableTitleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("勾选可批量操作，单击查看详情");
        subtitleLabel.setFont(SMALL_FONT);
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        titlePanel.add(tableTitleLabel);
        titlePanel.add(subtitleLabel);

        selectAllBox = new JCheckBox("全选");
        selectAllBox.setOpaque(false);
        selectAllBox.setFont(BODY_FONT);
        selectAllBox.setForeground(TEXT_MUTED);
        selectAllBox.addActionListener(e -> {
            tableModel.toggleAll(selectAllBox.isSelected());
            updateStatus();
        });

        header.add(titlePanel, BorderLayout.WEST);
        header.add(selectAllBox, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void configureTable() {
        contactTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactTable.setRowHeight(48);
        contactTable.setFont(BODY_FONT);
        contactTable.setForeground(TEXT_PRIMARY);
        contactTable.setShowGrid(false);
        contactTable.setFillsViewportHeight(true);
        contactTable.setIntercellSpacing(new Dimension(0, 0));
        contactTable.setBackground(SURFACE_BG);
        contactTable.setSelectionBackground(SELECTION_BG);
        contactTable.setSelectionForeground(TEXT_PRIMARY);
        contactTable.setDefaultRenderer(Object.class, new ContactCellRenderer());
        contactTable.setDefaultRenderer(String.class, new ContactCellRenderer());
        contactTable.setDefaultRenderer(Boolean.class, new CheckBoxCellRenderer());
        contactTable.getTableHeader().setReorderingAllowed(false);
        contactTable.getTableHeader().setResizingAllowed(true);
        contactTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        contactTable.getTableHeader().setBackground(SURFACE_ALT);
        contactTable.getTableHeader().setForeground(TEXT_MUTED);
        contactTable.getTableHeader().setDefaultRenderer(new HeaderRenderer(contactTable.getTableHeader().getDefaultRenderer()));

        contactTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (contactTable.getSelectedRow() >= 0) {
                    showDetailPanel();
                }
                updatePoster();
                updateStatus();
            }
        });

        contactTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    editSelectedContact();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int row = contactTable.rowAtPoint(event.getPoint());
                if (row >= 0 && !contactTable.isRowSelected(row)) {
                    contactTable.setRowSelectionInterval(row, row);
                }
                tablePopupMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });

        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                syncSelectAllState();
                updateStatus();
            }
        });
    }

    private JPanel createSideToggle() {
        JPanel toggle = new JPanel(new BorderLayout());
        toggle.setPreferredSize(new Dimension(28, 0));
        toggle.setMinimumSize(new Dimension(28, 0));
        toggle.setBackground(new Color(0xE4ECF5));
        toggle.setBorder(new EmptyBorder(0, 4, 0, 4));
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sideToggleLabel = new JLabel("◀", SwingConstants.CENTER);
        sideToggleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        sideToggleLabel.setForeground(TEXT_MUTED);
        toggle.add(sideToggleLabel, BorderLayout.CENTER);

        toggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                toggle.setBackground(new Color(0xD8E5F5));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                toggle.setBackground(new Color(0xE4ECF5));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                toggleDetailPanel();
            }
        });
        return toggle;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE_BG);
        bar.setBorder(compoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR), new EmptyBorder(8, 18, 8, 18)));

        statusLabel.setFont(SMALL_FONT);
        statusLabel.setForeground(TEXT_MUTED);

        JLabel hintLabel = new JLabel("快捷键: Ctrl+N 新建, Ctrl+E 编辑, Ctrl+F 搜索, Delete 删除");
        hintLabel.setFont(SMALL_FONT);
        hintLabel.setForeground(TEXT_MUTED);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(hintLabel, BorderLayout.EAST);
        return bar;
    }

    private JPopupMenu createTablePopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(popupItem("编辑联系人", e -> editSelectedContact()));
        menu.add(popupItem("删除联系人", e -> deleteCheckedContacts()));
        menu.addSeparator();
        menu.add(popupItem("加入分组", e -> assignGroupToChecked()));
        menu.add(popupItem("移出分组", e -> removeGroupFromChecked()));
        menu.addSeparator();
        menu.add(popupItem("导出到 CSV", e -> exportContacts("csv")));
        menu.add(popupItem("导出到 vCard", e -> exportContacts("vcf")));
        return menu;
    }

    private JMenuItem popupItem(String text, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(BODY_FONT);
        item.addActionListener(listener);
        return item;
    }

    private void registerKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        mapKey(rootPane, KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, "new-contact", e -> openContactDialog(null));
        mapKey(rootPane, KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, "edit-contact", e -> editSelectedContact());
        mapKey(rootPane, KeyEvent.VK_DELETE, 0, "delete-contact", e -> deleteCheckedContacts());
        mapKey(rootPane, KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, "focus-search", e -> {
            searchField.requestFocusInWindow();
            searchField.selectAll();
        });
    }

    private void mapKey(JRootPane rootPane, int key, int modifiers, String name, ActionListener listener) {
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, modifiers), name);
        rootPane.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        });
    }

    private void refreshGroups() {
        String selectedGroup = groupList.getSelectedValue();
        groupListModel.clear();
        groupListModel.addElement(AddressBook.ALL_CONTACTS);
        groupListModel.addElement(AddressBook.UNGROUPED);
        for (String group : addressBook.getGroups()) {
            groupListModel.addElement(group);
        }

        if (selectedGroup != null) {
            groupList.setSelectedValue(selectedGroup, true);
        }
        if (groupList.getSelectedIndex() < 0 && groupListModel.getSize() > 0) {
            groupList.setSelectedIndex(0);
        }
    }

    private void refreshContacts() {
        String selectedId = getSelectedContactId();
        tableModel.setContacts(addressBook.queryContacts(groupList.getSelectedValue(), searchField.getText()));
        configureTableColumns();
        syncSelectAllState();
        updateListHeader();
        if (selectedId != null) {
            selectContactById(selectedId);
        }
        if (contactTable.getSelectedRow() < 0) {
            updatePoster();
        }
        updateStatus();
    }

    private void configureTableColumns() {
        if (contactTable.getColumnCount() == 0) {
            return;
        }

        TableColumn checkColumn = contactTable.getColumnModel().getColumn(0);
        checkColumn.setPreferredWidth(44);
        checkColumn.setMaxWidth(44);
        checkColumn.setMinWidth(44);

        for (int i = 1; i < contactTable.getColumnCount(); i++) {
            TableColumn column = contactTable.getColumnModel().getColumn(i);
            String name = contactTable.getColumnName(i);
            if ("姓名".equals(name)) {
                column.setPreferredWidth(230);
            } else if ("备注".equals(name) || "家庭地址".equals(name)) {
                column.setPreferredWidth(280);
            } else if ("所属组".equals(name)) {
                column.setPreferredWidth(220);
            } else {
                column.setPreferredWidth(180);
            }
        }
    }

    private void updateListHeader() {
        String group = currentGroup();
        int count = tableModel.getRowCount();
        if (AddressBook.ALL_CONTACTS.equals(group)) {
            tableTitleLabel.setText("联系人列表 · 共 " + count + " 人");
        } else {
            tableTitleLabel.setText(group + " · " + count + " 人");
        }
    }

    private void updatePoster() {
        int selectedRow = contactTable.getSelectedRow();
        Contact contact = selectedRow >= 0 ? tableModel.getContactAt(contactTable.convertRowIndexToModel(selectedRow)) : null;
        posterPanel.showCompactContact(contact);
    }

    private void updateStatus() {
        StringBuilder builder = new StringBuilder();
        builder.append("当前分组: ").append(currentGroup());
        builder.append("  |  列表人数: ").append(tableModel.getRowCount());

        int checked = tableModel.getCheckedCount();
        if (checked > 0) {
            builder.append("  |  已勾选: ").append(checked);
        }

        if (!searchField.getText().isBlank()) {
            builder.append("  |  搜索: ").append(searchField.getText().trim());
        }
        statusLabel.setText(builder.toString());
    }

    private void syncSelectAllState() {
        if (selectAllBox == null) {
            return;
        }
        selectAllBox.setSelected(tableModel.getRowCount() > 0 && tableModel.getCheckedCount() == tableModel.getRowCount());
    }

    private void showDetailPanel() {
        if (detailVisible) {
            return;
        }
        detailVisible = true;
        int dividerLocation = Math.max(320, mainSplit.getWidth() - rightPanelWidth);
        mainSplit.setDividerLocation(dividerLocation);
        sideToggleLabel.setText("▶");
        mainSplit.revalidate();
    }

    private void hideDetailPanel() {
        detailVisible = false;
        mainSplit.setDividerLocation(1.0);
        sideToggleLabel.setText("◀");
        mainSplit.revalidate();
    }

    private void toggleDetailPanel() {
        if (detailVisible) {
            hideDetailPanel();
        } else if (contactTable.getSelectedRow() >= 0) {
            showDetailPanel();
        }
    }

    private void openContactDialog(Contact contact) {
        ContactDialog dialog = new ContactDialog(this, contact == null ? "新增联系人" : "编辑联系人", contact);
        dialog.setVisible(true);
        Contact result = dialog.getResult();
        if (result != null) {
            addressBook.updateContact(result);
            persistAndRefresh(contact == null ? "已新增联系人" : "已更新联系人");
            selectContactById(result.getId());
        }
    }

    private void editSelectedContact() {
        int selectedRow = contactTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个联系人。");
            return;
        }
        openContactDialog(tableModel.getContactAt(contactTable.convertRowIndexToModel(selectedRow)));
    }

    private List<Contact> checkedOrSelected() {
        List<Contact> checked = tableModel.getCheckedContacts();
        if (!checked.isEmpty()) {
            return checked;
        }
        int row = contactTable.getSelectedRow();
        if (row >= 0) {
            Contact contact = tableModel.getContactAt(contactTable.convertRowIndexToModel(row));
            List<Contact> selected = new ArrayList<>();
            if (contact != null) {
                selected.add(contact);
            }
            return selected;
        }
        return checked;
    }

    private void deleteCheckedContacts() {
        List<Contact> selected = checkedOrSelected();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先勾选或选择联系人。");
            return;
        }
        int confirmed = JOptionPane.showConfirmDialog(
                this,
                "确定删除这 " + selected.size() + " 位联系人吗？",
                "删除联系人",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmed != JOptionPane.YES_OPTION) {
            return;
        }
        for (Contact contact : selected) {
            addressBook.removeContact(contact.getId());
        }
        persistAndRefresh("已删除 " + selected.size() + " 位联系人");
    }

    private void addGroup() {
        String input = JOptionPane.showInputDialog(this, "请输入分组名称：", "新建分组", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        if (!addressBook.addGroup(input)) {
            JOptionPane.showMessageDialog(this, "该分组已存在，或名称为空。");
            return;
        }
        persistAndRefresh("已新增分组");
        groupList.setSelectedValue(input.trim(), true);
    }

    private void deleteSelectedGroup() {
        String group = currentGroup();
        if (AddressBook.ALL_CONTACTS.equals(group) || AddressBook.UNGROUPED.equals(group)) {
            JOptionPane.showMessageDialog(this, "这个分组不能删除。");
            return;
        }
        int confirmed = JOptionPane.showConfirmDialog(
                this,
                "删除分组后，联系人会保留，但会移出该分组。\n确定继续吗？",
                "删除分组",
                JOptionPane.YES_NO_OPTION
        );
        if (confirmed != JOptionPane.YES_OPTION) {
            return;
        }
        addressBook.removeGroup(group);
        persistAndRefresh("已删除分组");
    }

    private void assignGroupToChecked() {
        groupOperation(true);
    }

    private void removeGroupFromChecked() {
        groupOperation(false);
    }

    private void groupOperation(boolean assign) {
        List<Contact> selected = checkedOrSelected();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先勾选或选择联系人。");
            return;
        }
        String group = chooseGroup(assign ? "选择要加入的分组" : "选择要移出的分组");
        if (group == null) {
            return;
        }
        if (assign) {
            addressBook.assignContactsToGroup(selected, group);
            persistAndRefresh("已加入分组");
        } else {
            addressBook.removeContactsFromGroup(selected, group);
            persistAndRefresh("已移出分组");
        }
    }

    private void chooseColumns() {
        List<JCheckBox> boxes = new ArrayList<>();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 6, 4, 16);

        Set<String> selectedColumns = tableModel.getVisibleColumns();
        int index = 0;
        for (String column : tableModel.getAvailableColumns()) {
            JCheckBox box = new JCheckBox(column, selectedColumns.contains(column));
            box.setFont(BODY_FONT);
            box.setOpaque(false);
            boxes.add(box);
            gbc.gridx = index % 2;
            gbc.gridy = index / 2;
            panel.add(box, gbc);
            index++;
        }

        int confirmed = JOptionPane.showConfirmDialog(this, panel, "选择要显示的字段", JOptionPane.OK_CANCEL_OPTION);
        if (confirmed != JOptionPane.OK_OPTION) {
            return;
        }

        Set<String> newColumns = new LinkedHashSet<>();
        for (JCheckBox box : boxes) {
            if (box.isSelected()) {
                newColumns.add(box.getText());
            }
        }
        if (newColumns.isEmpty()) {
            JOptionPane.showMessageDialog(this, "至少保留一个字段。");
            return;
        }
        tableModel.setVisibleColumns(newColumns);
        refreshContacts();
    }

    private void exportContacts(String type) {
        List<Contact> selected = checkedOrSelected();
        List<Contact> dataToExport;
        if (!selected.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "检测到已勾选联系人。\n是：只导出勾选项\n否：导出当前列表全部联系人",
                    "导出联系人",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
            dataToExport = choice == JOptionPane.YES_OPTION
                    ? selected
                    : addressBook.queryContacts(currentGroup(), searchField.getText());
        } else {
            dataToExport = addressBook.queryContacts(currentGroup(), searchField.getText());
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("contacts." + ("csv".equals(type) ? "csv" : "vcf")));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            if ("csv".equals(type)) {
                csvService.exportContacts(dataToExport, fileChooser.getSelectedFile());
            } else {
                vCardService.exportContacts(dataToExport, fileChooser.getSelectedFile());
            }
            statusLabel.setText("已导出到: " + fileChooser.getSelectedFile().getAbsolutePath());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "导出失败: " + exception.getMessage());
        }
    }

    private void importContacts(String type) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            List<Contact> imported = "csv".equals(type)
                    ? csvService.importContacts(fileChooser.getSelectedFile())
                    : vCardService.importContacts(fileChooser.getSelectedFile());

            List<Contact> duplicates = new ArrayList<>();
            List<Contact> freshContacts = new ArrayList<>();
            for (Contact contact : imported) {
                if (isDuplicate(contact)) {
                    duplicates.add(contact);
                } else {
                    freshContacts.add(contact);
                }
            }

            if (!duplicates.isEmpty()) {
                StringBuilder builder = new StringBuilder("发现 ").append(duplicates.size()).append(" 个疑似重复联系人：\n");
                for (Contact duplicate : duplicates) {
                    builder.append(" - ").append(duplicate.getName());
                    if (!duplicate.getMobile().isBlank()) {
                        builder.append(" (").append(duplicate.getMobile()).append(')');
                    }
                    builder.append('\n');
                }
                builder.append("\n选择“是”将全部导入，选择“否”则跳过重复项。");
                int choice = JOptionPane.showConfirmDialog(this, builder.toString(), "重复联系人", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    freshContacts.addAll(duplicates);
                }
            }

            for (Contact contact : freshContacts) {
                addressBook.addContact(contact);
            }
            persistAndRefresh("已导入 " + freshContacts.size() + " 位联系人");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "导入失败: " + exception.getMessage());
        }
    }

    private boolean isDuplicate(Contact candidate) {
        String normalizedName = candidate.getName().toLowerCase().trim();
        String mobile = candidate.getMobile().trim();
        String phone = candidate.getPhone().trim();

        for (Contact existing : addressBook.getContacts()) {
            boolean sameName = existing.getName().toLowerCase().trim().equals(normalizedName);
            boolean sameMobile = !mobile.isEmpty() && mobile.equals(existing.getMobile().trim());
            boolean samePhone = !phone.isEmpty() && phone.equals(existing.getPhone().trim());
            if (sameName && (sameMobile || samePhone)) {
                return true;
            }
        }
        return false;
    }

    private String chooseGroup(String title) {
        List<String> groups = new ArrayList<>(addressBook.getGroups());
        if (groups.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先创建分组。");
            return null;
        }
        return (String) JOptionPane.showInputDialog(this, title, title, JOptionPane.PLAIN_MESSAGE, null, groups.toArray(), groups.get(0));
    }

    private String currentGroup() {
        String value = groupList.getSelectedValue();
        return value == null ? AddressBook.ALL_CONTACTS : value;
    }

    private void persistAndRefresh(String message) {
        storage.save(addressBook);
        refreshGroups();
        refreshContacts();
        statusLabel.setText(message);
    }

    private String getSelectedContactId() {
        int selectedRow = contactTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        Contact contact = tableModel.getContactAt(contactTable.convertRowIndexToModel(selectedRow));
        return contact == null ? null : contact.getId();
    }

    private void selectContactById(String contactId) {
        if (contactId == null) {
            return;
        }
        for (int modelRow = 0; modelRow < tableModel.getRowCount(); modelRow++) {
            Contact contact = tableModel.getContactAt(modelRow);
            if (contact != null && contactId.equals(contact.getId())) {
                int viewRow = contactTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    contactTable.setRowSelectionInterval(viewRow, viewRow);
                    contactTable.scrollRectToVisible(contactTable.getCellRect(viewRow, 0, true));
                    updatePoster();
                    return;
                }
            }
        }
    }

    private void styleSplitPane(JSplitPane splitPane) {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = super.createDefaultDivider();
                divider.setBackground(APP_BG);
                divider.setBorder(BorderFactory.createEmptyBorder());
                return divider;
            }
        });
    }

    private JPanel createSurfacePanel(LayoutManager layout, Border padding) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(SURFACE_BG);
        panel.setBorder(compoundBorder(roundedLineBorder(BORDER_COLOR), padding));
        return panel;
    }

    private Border roundedLineBorder(Color color) {
        return BorderFactory.createLineBorder(color, 1, true);
    }

    private Border compoundBorder(Border outer, Border inner) {
        return BorderFactory.createCompoundBorder(outer, inner);
    }

    private Color blend(Color base, Color target, double ratio) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round(base.getRed() + (target.getRed() - base.getRed()) * clamped);
        int green = (int) Math.round(base.getGreen() + (target.getGreen() - base.getGreen()) * clamped);
        int blue = (int) Math.round(base.getBlue() + (target.getBlue() - base.getBlue()) * clamped);
        return new Color(red, green, blue);
    }

    private int countInGroup(String groupName) {
        if (AddressBook.ALL_CONTACTS.equals(groupName)) {
            return addressBook.getContacts().size();
        }
        if (AddressBook.UNGROUPED.equals(groupName)) {
            return (int) addressBook.getContacts().stream().filter(contact -> contact.getGroups().isEmpty()).count();
        }
        return (int) addressBook.getContacts().stream().filter(contact -> contact.getGroups().contains(groupName)).count();
    }

    private Icon avatarFor(Contact contact) {
        if (contact == null) {
            return createFallbackAvatar("", LIST_AVATAR_SIZE);
        }

        String photoPath = contact.getPhotoPath();
        if (photoPath != null && !photoPath.isBlank()) {
            File photoFile = new File(photoPath.trim());
            if (photoFile.isFile()) {
                String cacheKey = photoFile.getAbsolutePath() + "#" + photoFile.lastModified() + "#" + LIST_AVATAR_SIZE;
                Icon cached = avatarCache.get(cacheKey);
                if (cached != null) {
                    return cached;
                }

                Icon icon = createPhotoAvatar(photoFile, LIST_AVATAR_SIZE);
                if (icon != null) {
                    avatarCache.put(cacheKey, icon);
                    return icon;
                }
            }
        }
        return createFallbackAvatar(contact.getName(), LIST_AVATAR_SIZE);
    }

    private Icon createPhotoAvatar(File photoFile, int size) {
        try {
            BufferedImage source = ImageIO.read(photoFile);
            if (source == null) {
                return null;
            }
            BufferedImage square = cropCenterSquare(source);
            BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = avatar.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Shape clip = new Ellipse2D.Float(0, 0, size, size);
            graphics.setClip(clip);
            graphics.drawImage(square, 0, 0, size, size, null);
            graphics.setClip(null);
            graphics.setColor(new Color(255, 255, 255, 200));
            graphics.setStroke(new BasicStroke(1.4f));
            graphics.draw(clip);
            graphics.dispose();
            return new javax.swing.ImageIcon(avatar);
        } catch (IOException ignored) {
            return null;
        }
    }

    private BufferedImage cropCenterSquare(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int side = Math.min(width, height);
        int x = (width - side) / 2;
        int y = (height - side) / 2;
        return source.getSubimage(x, y, side, side);
    }

    private Icon createFallbackAvatar(String name, int size) {
        String normalizedName = name == null ? "" : name.trim();
        String cacheKey = "fallback#" + normalizedName + "#" + size;
        Icon cached = avatarCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = avatar.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(avatarColorFor(normalizedName));
        graphics.fillOval(0, 0, size, size);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, Math.max(12, size / 2)));

        String initials = initialsFor(normalizedName);
        java.awt.FontMetrics metrics = graphics.getFontMetrics();
        int textX = (size - metrics.stringWidth(initials)) / 2;
        int textY = (size - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString(initials, textX, textY);
        graphics.dispose();

        Icon icon = new javax.swing.ImageIcon(avatar);
        avatarCache.put(cacheKey, icon);
        return icon;
    }

    private Color avatarColorFor(String name) {
        int index = Math.floorMod(name.hashCode(), AVATAR_FALLBACK_COLORS.length);
        return AVATAR_FALLBACK_COLORS[index];
    }

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 2) {
            return trimmed.toUpperCase();
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
            return (firstCharacter(parts[0]) + firstCharacter(parts[1])).toUpperCase();
        }
        return trimmed.substring(0, 2).toUpperCase();
    }

    private String firstCharacter(String value) {
        return value == null || value.isBlank() ? "" : value.substring(0, 1);
    }

    private class GroupListRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel countLabel = new JLabel();

        private GroupListRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(9, 12, 9, 12));
            setOpaque(true);

            nameLabel.setFont(BODY_FONT);
            countLabel.setFont(SMALL_FONT);
            countLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            countLabel.setBorder(compoundBorder(roundedLineBorder(BORDER_COLOR), new EmptyBorder(3, 8, 3, 8)));

            add(nameLabel, BorderLayout.CENTER);
            add(countLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            String groupName = value == null ? "" : value;
            nameLabel.setText(groupName);
            countLabel.setText(String.valueOf(countInGroup(groupName)));

            if (isSelected) {
                setBackground(ACCENT_SOFT);
                nameLabel.setForeground(TEXT_PRIMARY);
                countLabel.setForeground(ACCENT);
                countLabel.setBackground(Color.WHITE);
            } else {
                setBackground(index % 2 == 0 ? Color.WHITE : SURFACE_ALT);
                nameLabel.setForeground(TEXT_PRIMARY);
                countLabel.setForeground(TEXT_MUTED);
                countLabel.setBackground(GROUP_CARD_BG);
            }
            countLabel.setOpaque(true);
            return this;
        }
    }

    private class HeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        private HeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(10, column == 0 ? 0 : 8, 10, 8));
            label.setBackground(SURFACE_ALT);
            label.setForeground(TEXT_MUTED);
            label.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
            return label;
        }
    }

    private class CheckBoxCellRenderer extends JCheckBox implements TableCellRenderer {
        private CheckBoxCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected(Boolean.TRUE.equals(value));
            setBackground(isSelected ? SELECTION_BG : (row % 2 == 0 ? SURFACE_BG : ROW_ALT));
            return this;
        }
    }

    private class ContactCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            boolean nameColumn = column > 0 && "姓名".equals(table.getColumnName(column));

            label.setFont(nameColumn ? new Font("Microsoft YaHei", Font.BOLD, 13) : BODY_FONT);
            label.setForeground(TEXT_PRIMARY);
            label.setBackground(isSelected ? SELECTION_BG : (row % 2 == 0 ? SURFACE_BG : ROW_ALT));
            label.setBorder(new EmptyBorder(0, nameColumn ? 2 : 8, 0, 10));
            label.setIcon(null);
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setVerticalAlignment(SwingConstants.CENTER);

            if (nameColumn) {
                Contact contact = tableModel.getContactAt(table.convertRowIndexToModel(row));
                label.setIcon(avatarFor(contact));
                label.setIconTextGap(10);
            }
            return label;
        }
    }
}
