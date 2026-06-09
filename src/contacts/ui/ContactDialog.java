package contacts.ui;

import contacts.model.Contact;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContactDialog extends JDialog {
    private static final Color APP_BG = new Color(0xF2FBF8);
    private static final Color SURFACE_BG = Color.WHITE;
    private static final Color SURFACE_ALT = new Color(0xF7FCFB);
    private static final Color BORDER_COLOR = new Color(0xD8EAE3);
    private static final Color TEXT_PRIMARY = new Color(0x25423B);
    private static final Color TEXT_MUTED = new Color(0x6D8A82);
    private static final Color ACCENT = new Color(0x4DAF95);
    private static final Font SECTION_TITLE_FONT = new Font("Microsoft YaHei", Font.BOLD, 15);
    private static final Font BODY_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final Font SMALL_FONT = new Font("Microsoft YaHei", Font.PLAIN, 11);
    private static final String[] PRESET_GROUPS = {"家人", "同事", "朋友"};

    private final JTextField nameField = new JTextField();
    private final JTextField birthdayField = new JTextField();
    private final JTextField companyField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField mobileField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField websiteField = new JTextField();
    private final JTextField imTypeField = new JTextField();
    private final JTextField imAccountField = new JTextField();
    private final JTextField photoField = new JTextField();
    private final JTextField addressField = new JTextField();
    private final JTextField postalCodeField = new JTextField();
    private final JTextArea notesArea = new JTextArea(5, 20);
    private final JLabel photoPreviewLabel = new JLabel("暂无照片", SwingConstants.CENTER);
    private final List<JCheckBox> presetBoxes = new ArrayList<>();
    private final List<JCheckBox> customBoxes = new ArrayList<>();
    private final JPanel customGroupsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JCheckBox otherBox = new JCheckBox("添加自定义分组");
    private JButton saveButton;

    private Contact result;

    public ContactDialog(Frame owner, String title, Contact contact) {
        super(owner, title, true);
        prepareInputs();
        normalizeDialogText();
        setLayout(new BorderLayout());
        add(createCompactHeader(title), BorderLayout.NORTH);
        add(createCompactScrollBody(), BorderLayout.CENTER);
        add(createCompactButtons(), BorderLayout.SOUTH);

        fillFields(contact);
        refreshCompactPhotoPreview();
        registerShortcuts();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setSize(840, 760);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(saveButton);
    }

    public Contact getResult() {
        return result;
    }

    private void prepareInputs() {
        for (String group : PRESET_GROUPS) {
            JCheckBox box = new JCheckBox(group);
            styleCheckBox(box);
            presetBoxes.add(box);
        }

        styleTextField(nameField, "请输入姓名");
        styleTextField(birthdayField, "yyyy-MM-dd");
        styleTextField(companyField, "公司 / 学校 / 机构");
        styleTextField(phoneField, "固定电话");
        styleTextField(mobileField, "手机号码");
        styleTextField(emailField, "name@example.com");
        styleTextField(websiteField, "https://example.com");
        styleTextField(imTypeField, "微信 / QQ / Telegram");
        styleTextField(imAccountField, "账号");
        styleTextField(photoField, "可直接粘贴本地图片路径");
        styleTextField(addressField, "家庭地址");
        styleTextField(postalCodeField, "邮政编码");

        photoField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshCompactPhotoPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshCompactPhotoPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshCompactPhotoPreview();
            }
        });

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(BODY_FONT);
        notesArea.setForeground(TEXT_PRIMARY);
        notesArea.setBackground(SURFACE_ALT);
        notesArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        photoPreviewLabel.setOpaque(true);
        photoPreviewLabel.setBackground(SURFACE_ALT);
        photoPreviewLabel.setForeground(TEXT_MUTED);
        photoPreviewLabel.setFont(BODY_FONT);
        photoPreviewLabel.setPreferredSize(new Dimension(116, 116));
        photoPreviewLabel.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(6, 6, 6, 6)));

        customGroupsPanel.setOpaque(false);
        otherBox.setOpaque(false);
        otherBox.setFont(BODY_FONT);
        otherBox.setForeground(TEXT_PRIMARY);
        otherBox.addActionListener(e -> {
            if (otherBox.isSelected()) {
                addCompactCustomGroup();
            } else {
                customBoxes.clear();
                refreshCompactCustomPanel();
            }
        });
    }

    private void normalizeDialogText() {
        photoPreviewLabel.setText("暂无照片");
        otherBox.setText("添加自定义分组");

        String[] presetNames = {"家人", "同事", "朋友"};
        for (int i = 0; i < presetBoxes.size() && i < presetNames.length; i++) {
            presetBoxes.get(i).setText(presetNames[i]);
        }

        nameField.putClientProperty("JTextField.placeholderText", "请输入姓名");
        birthdayField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
        companyField.putClientProperty("JTextField.placeholderText", "公司 / 学校 / 机构");
        phoneField.putClientProperty("JTextField.placeholderText", "固定电话");
        mobileField.putClientProperty("JTextField.placeholderText", "手机号码");
        emailField.putClientProperty("JTextField.placeholderText", "name@example.com");
        websiteField.putClientProperty("JTextField.placeholderText", "https://example.com");
        imTypeField.putClientProperty("JTextField.placeholderText", "微信 / QQ / Telegram");
        imAccountField.putClientProperty("JTextField.placeholderText", "账号");
        photoField.putClientProperty("JTextField.placeholderText", "本地图片路径");
        addressField.putClientProperty("JTextField.placeholderText", "家庭地址");
        postalCodeField.putClientProperty("JTextField.placeholderText", "邮政编码");
    }

    private JPanel createCompactHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE_BG);
        header.setBorder(compoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR), new EmptyBorder(18, 22, 18, 22)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.WEST);
        return header;
    }

    private JScrollPane createCompactScrollBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(APP_BG);
        body.setBorder(new EmptyBorder(18, 22, 18, 22));

        body.add(createCompactBasicSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createCompactContactSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createCompactPhotoSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createCompactGroupsSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createCompactAddressSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createCompactNotesSection());

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(APP_BG);
        return scrollPane;
    }

    private JPanel createCompactBasicSection() {
        JPanel content = createGridContent();
        addFieldPair(content, 0, "姓名", nameField, "生日", birthdayField);
        addFieldPair(content, 1, "公司 / 学校", companyField, "邮编", postalCodeField);
        return createCompactSectionCard("基本信息", content);
    }

    private JPanel createCompactContactSection() {
        JPanel content = createGridContent();
        addFieldPair(content, 0, "电话", phoneField, "手机", mobileField);
        addFieldPair(content, 1, "电子邮箱", emailField, "个人主页", websiteField);
        addFieldPair(content, 2, "即时通讯", imTypeField, "账号", imAccountField);
        return createCompactSectionCard("联系方式", content);
    }

    private JPanel createCompactPhotoSection() {
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setOpaque(false);
        previewPanel.add(photoPreviewLabel, BorderLayout.NORTH);

        JPanel editorPanel = new JPanel();
        editorPanel.setOpaque(false);
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.add(photoField, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setOpaque(false);
        JButton browseButton = createDialogButton("选择照片", true);
        JButton clearButton = createDialogButton("清除照片", false);
        browseButton.addActionListener(e -> choosePhoto());
        clearButton.addActionListener(e -> {
            photoField.setText("");
            photoField.requestFocusInWindow();
        });
        buttonRow.add(browseButton);
        buttonRow.add(clearButton);

        editorPanel.add(pathRow);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(buttonRow);

        content.add(previewPanel, BorderLayout.WEST);
        content.add(editorPanel, BorderLayout.CENTER);
        return createCompactSectionCard("照片", content);
    }

    private JPanel createCompactGroupsSection() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        presetPanel.setOpaque(false);
        for (JCheckBox box : presetBoxes) {
            presetPanel.add(box);
        }
        presetPanel.add(otherBox);

        JPanel customWrapper = new JPanel(new BorderLayout());
        customWrapper.setOpaque(false);
        customWrapper.add(customGroupsPanel, BorderLayout.CENTER);

        content.add(presetPanel);
        content.add(customWrapper);
        return createCompactSectionCard("分组", content);
    }

    private JPanel createCompactAddressSection() {
        JPanel content = createGridContent();
        addFieldPair(content, 0, "家庭地址", addressField, "", new JLabel());
        return createCompactSectionCard("地址信息", content);
    }

    private JPanel createCompactNotesSection() {
        JScrollPane notesScrollPane = new JScrollPane(notesArea);
        notesScrollPane.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), BorderFactory.createEmptyBorder()));
        notesScrollPane.getViewport().setBackground(SURFACE_ALT);
        notesScrollPane.setPreferredSize(new Dimension(0, 140));
        return createCompactSectionCard("备注", notesScrollPane);
    }

    private JPanel createCompactButtons() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_BG);
        panel.setBorder(compoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR), new EmptyBorder(14, 22, 14, 22)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JButton cancelButton = createDialogButton("取消", false);
        saveButton = createDialogButton("保存联系人", true);
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> saveCompactContact());

        buttons.add(cancelButton);
        buttons.add(saveButton);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private JPanel createCompactSectionCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(SURFACE_BG);
        card.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(16, 18, 16, 18)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void refreshCompactPhotoPreview() {
        String path = photoField.getText().trim();
        if (path.isEmpty()) {
            photoPreviewLabel.setText("暂无照片");
            photoPreviewLabel.setIcon(null);
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            photoPreviewLabel.setText("文件不存在");
            photoPreviewLabel.setIcon(null);
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                photoPreviewLabel.setText("无法加载");
                photoPreviewLabel.setIcon(null);
                return;
            }

            BufferedImage scaled = scaleForPreview(image, 104);
            photoPreviewLabel.setText("");
            photoPreviewLabel.setIcon(new javax.swing.ImageIcon(scaled));
        } catch (IOException exception) {
            photoPreviewLabel.setText("读取失败");
            photoPreviewLabel.setIcon(null);
        }
    }

    private void saveCompactContact() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "姓名不能为空。");
            return;
        }

        Contact contact = result == null ? new Contact() : result.copy();
        contact.setName(nameField.getText());
        contact.setPhone(phoneField.getText());
        contact.setMobile(mobileField.getText());
        contact.setEmail(emailField.getText());
        contact.setWebsite(websiteField.getText());
        contact.setImType(imTypeField.getText());
        contact.setImAccount(imAccountField.getText());
        contact.setPhotoPath(photoField.getText());
        contact.setCompany(companyField.getText());
        contact.setHomeAddress(addressField.getText());
        contact.setPostalCode(postalCodeField.getText());
        contact.setNotes(notesArea.getText());

        if (!birthdayField.getText().trim().isEmpty()) {
            try {
                contact.setBirthday(LocalDate.parse(birthdayField.getText().trim()));
            } catch (DateTimeParseException exception) {
                JOptionPane.showMessageDialog(this, "生日格式请使用 yyyy-MM-dd。");
                return;
            }
        } else {
            contact.setBirthday(null);
        }

        Set<String> groups = new LinkedHashSet<>();
        for (JCheckBox box : presetBoxes) {
            if (box.isSelected()) {
                groups.add(box.getText());
            }
        }
        for (JCheckBox box : customBoxes) {
            if (box.isSelected()) {
                groups.add(box.getText());
            }
        }
        contact.setGroups(groups);

        result = contact;
        dispose();
    }

    private void addCompactCustomGroup() {
        String input = JOptionPane.showInputDialog(this, "请输入自定义分组名称：", "添加分组", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) {
            otherBox.setSelected(!customBoxes.isEmpty());
            return;
        }

        String name = input.trim();
        for (JCheckBox box : presetBoxes) {
            if (box.getText().equals(name)) {
                box.setSelected(true);
                otherBox.setSelected(!customBoxes.isEmpty());
                return;
            }
        }
        for (JCheckBox box : customBoxes) {
            if (box.getText().equals(name)) {
                box.setSelected(true);
                return;
            }
        }

        JCheckBox box = new JCheckBox(name, true);
        styleCheckBox(box);
        customBoxes.add(box);
        refreshCompactCustomPanel();
    }

    private JPanel createHeader(String title, boolean creating) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE_BG);
        header.setBorder(compoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR), new EmptyBorder(18, 22, 18, 22)));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(creating ? "补充联系人资料、照片和分组信息" : "整理联系人资料，让信息更完整");
        subtitleLabel.setFont(BODY_FONT);
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        header.add(textPanel, BorderLayout.WEST);
        return header;
    }

    private JScrollPane createScrollBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(APP_BG);
        body.setBorder(new EmptyBorder(18, 22, 18, 22));

        body.add(createBasicSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createContactSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createPhotoSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createGroupsSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createAddressSection());
        body.add(Box.createVerticalStrut(12));
        body.add(createNotesSection());
        body.add(Box.createVerticalStrut(8));

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(APP_BG);
        return scrollPane;
    }

    private JPanel createBasicSection() {
        JPanel content = createGridContent();
        addFieldPair(content, 0, "姓名", nameField, "生日", birthdayField);
        addFieldPair(content, 1, "公司 / 学校", companyField, "邮编", postalCodeField);
        return createSectionCard("基本信息", "先把最常用的识别信息放在前面", content);
    }

    private JPanel createContactSection() {
        JPanel content = createGridContent();
        addFieldPair(content, 0, "电话", phoneField, "手机", mobileField);
        addFieldPair(content, 1, "电子邮箱", emailField, "个人主页", websiteField);
        addFieldPair(content, 2, "即时通讯", imTypeField, "账号", imAccountField);
        return createSectionCard("联系方式", "支持固定电话、手机、邮箱和即时通讯账号", content);
    }

    private JPanel createPhotoSection() {
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setOpaque(false);
        previewPanel.add(photoPreviewLabel, BorderLayout.NORTH);

        JPanel editorPanel = new JPanel();
        editorPanel.setOpaque(false);
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));

        JLabel helper = new JLabel("建议使用清晰的人像照片，主列表会自动显示为头像。");
        helper.setFont(SMALL_FONT);
        helper.setForeground(TEXT_MUTED);

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.add(photoField, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setOpaque(false);
        JButton browseButton = createDialogButton("选择照片", true);
        JButton clearButton = createDialogButton("清除照片", false);
        browseButton.addActionListener(e -> choosePhoto());
        clearButton.addActionListener(e -> {
            photoField.setText("");
            photoField.requestFocusInWindow();
        });
        buttonRow.add(browseButton);
        buttonRow.add(clearButton);

        editorPanel.add(helper);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(pathRow);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(buttonRow);

        content.add(previewPanel, BorderLayout.WEST);
        content.add(editorPanel, BorderLayout.CENTER);
        return createSectionCard("照片", "保存后的照片会同步显示在联系人列表和详情面板", content);
    }

    private JPanel createGroupsSection() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel helper = new JLabel("勾选常用分组，或添加一个自定义分组。");
        helper.setFont(SMALL_FONT);
        helper.setForeground(TEXT_MUTED);

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        presetPanel.setOpaque(false);
        for (JCheckBox box : presetBoxes) {
            presetPanel.add(box);
        }
        presetPanel.add(otherBox);

        JPanel customWrapper = new JPanel(new BorderLayout());
        customWrapper.setOpaque(false);
        customWrapper.add(customGroupsPanel, BorderLayout.CENTER);

        content.add(helper);
        content.add(Box.createVerticalStrut(8));
        content.add(presetPanel);
        content.add(customWrapper);
        return createSectionCard("分组", "分组能帮助你在主界面快速筛选联系人", content);
    }

    private JPanel createAddressSection() {
        JPanel content = createGridContent();
        addFieldPair(content, 0, "家庭地址", addressField, "", new JLabel());
        return createSectionCard("地址信息", "如果需要邮寄、拜访或节日问候，这里会很方便", content);
    }

    private JPanel createNotesSection() {
        JScrollPane notesScrollPane = new JScrollPane(notesArea);
        notesScrollPane.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), BorderFactory.createEmptyBorder()));
        notesScrollPane.getViewport().setBackground(SURFACE_ALT);
        notesScrollPane.setPreferredSize(new Dimension(0, 140));
        return createSectionCard("备注", "补充工作背景、关系说明或其他需要记住的事情", notesScrollPane);
    }

    private JPanel createButtons() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_BG);
        panel.setBorder(compoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR), new EmptyBorder(14, 22, 14, 22)));

        JLabel hint = new JLabel("Ctrl+Enter 保存，Esc 取消");
        hint.setFont(SMALL_FONT);
        hint.setForeground(TEXT_MUTED);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JButton cancelButton = createDialogButton("取消", false);
        saveButton = createDialogButton("保存联系人", true);
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> saveCompactContact());

        buttons.add(cancelButton);
        buttons.add(saveButton);

        panel.add(hint, BorderLayout.WEST);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private void registerShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "save");
        getRootPane().getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveCompactContact();
            }
        });
    }

    private void choosePhoto() {
        JFileChooser fileChooser = new JFileChooser();
        if (!photoField.getText().isBlank()) {
            fileChooser.setSelectedFile(new File(photoField.getText()));
        }
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            photoField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            refreshCompactPhotoPreview();
        }
    }

    private void updatePhotoPreview() {
        String path = photoField.getText().trim();
        if (path.isEmpty()) {
            photoPreviewLabel.setText("暂无照片");
            photoPreviewLabel.setIcon(null);
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            photoPreviewLabel.setText("文件不存在");
            photoPreviewLabel.setIcon(null);
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                photoPreviewLabel.setText("无法加载");
                photoPreviewLabel.setIcon(null);
                return;
            }

            BufferedImage scaled = scaleForPreview(image, 104);
            photoPreviewLabel.setText("");
            photoPreviewLabel.setIcon(new javax.swing.ImageIcon(scaled));
        } catch (IOException exception) {
            photoPreviewLabel.setText("读取失败");
            photoPreviewLabel.setIcon(null);
        }
    }

    private BufferedImage scaleForPreview(BufferedImage image, int size) {
        int width = image.getWidth();
        int height = image.getHeight();
        double ratio = Math.min((double) size / width, (double) size / height);
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));

        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        return output;
    }

    private void fillFields(Contact contact) {
        if (contact == null) {
            return;
        }

        nameField.setText(contact.getName());
        birthdayField.setText(contact.getDisplayBirthday());
        companyField.setText(contact.getCompany());
        phoneField.setText(contact.getPhone());
        mobileField.setText(contact.getMobile());
        emailField.setText(contact.getEmail());
        websiteField.setText(contact.getWebsite());
        imTypeField.setText(contact.getImType());
        imAccountField.setText(contact.getImAccount());
        photoField.setText(contact.getPhotoPath());
        addressField.setText(contact.getHomeAddress());
        postalCodeField.setText(contact.getPostalCode());
        notesArea.setText(contact.getNotes());

        Set<String> groups = new LinkedHashSet<>(contact.getGroups());
        for (JCheckBox box : presetBoxes) {
            box.setSelected(groups.contains(box.getText()));
            groups.remove(box.getText());
        }

        customBoxes.clear();
        for (String group : groups) {
            JCheckBox box = new JCheckBox(group, true);
            styleCheckBox(box);
            customBoxes.add(box);
        }
        otherBox.setSelected(!customBoxes.isEmpty());
        refreshCompactCustomPanel();
        result = contact.copy();
    }

    private void saveContact() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "姓名不能为空。");
            return;
        }

        Contact contact = result == null ? new Contact() : result.copy();
        contact.setName(nameField.getText());
        contact.setPhone(phoneField.getText());
        contact.setMobile(mobileField.getText());
        contact.setEmail(emailField.getText());
        contact.setWebsite(websiteField.getText());
        contact.setImType(imTypeField.getText());
        contact.setImAccount(imAccountField.getText());
        contact.setPhotoPath(photoField.getText());
        contact.setCompany(companyField.getText());
        contact.setHomeAddress(addressField.getText());
        contact.setPostalCode(postalCodeField.getText());
        contact.setNotes(notesArea.getText());

        if (!birthdayField.getText().trim().isEmpty()) {
            try {
                contact.setBirthday(LocalDate.parse(birthdayField.getText().trim()));
            } catch (DateTimeParseException exception) {
                JOptionPane.showMessageDialog(this, "生日格式请使用 yyyy-MM-dd。");
                return;
            }
        } else {
            contact.setBirthday(null);
        }

        Set<String> groups = new LinkedHashSet<>();
        for (JCheckBox box : presetBoxes) {
            if (box.isSelected()) {
                groups.add(box.getText());
            }
        }
        for (JCheckBox box : customBoxes) {
            if (box.isSelected()) {
                groups.add(box.getText());
            }
        }
        contact.setGroups(groups);

        result = contact;
        dispose();
    }

    private void addCustomGroup() {
        String input = JOptionPane.showInputDialog(this, "请输入自定义分组名称：", "添加分组", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) {
            otherBox.setSelected(!customBoxes.isEmpty());
            return;
        }

        String name = input.trim();
        for (JCheckBox box : presetBoxes) {
            if (box.getText().equals(name)) {
                box.setSelected(true);
                otherBox.setSelected(!customBoxes.isEmpty());
                return;
            }
        }
        for (JCheckBox box : customBoxes) {
            if (box.getText().equals(name)) {
                box.setSelected(true);
                return;
            }
        }

        JCheckBox box = new JCheckBox(name, true);
        styleCheckBox(box);
        customBoxes.add(box);
        refreshCompactCustomPanel();
    }

    private void refreshCompactCustomPanel() {
        customGroupsPanel.removeAll();
        for (JCheckBox box : customBoxes) {
            customGroupsPanel.add(box);
        }
        customGroupsPanel.revalidate();
        customGroupsPanel.repaint();
    }

    private void refreshCustomPanel() {
        customGroupsPanel.removeAll();
        for (JCheckBox box : customBoxes) {
            customGroupsPanel.add(box);
        }

        if (customBoxes.isEmpty()) {
            JLabel emptyLabel = new JLabel("暂无自定义分组");
            emptyLabel.setFont(SMALL_FONT);
            emptyLabel.setForeground(TEXT_MUTED);
            customGroupsPanel.add(emptyLabel);
        }

        customGroupsPanel.revalidate();
        customGroupsPanel.repaint();
    }

    private JPanel createGridContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    private void addFieldPair(JPanel panel, int row, String leftLabel, JComponent leftComponent, String rightLabel, JComponent rightComponent) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 0, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        if (!leftLabel.isBlank()) {
            panel.add(createFieldLabel(leftLabel), gbc);
        }

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 0, 20);
        panel.add(leftComponent, gbc);

        if (!rightLabel.isBlank()) {
            gbc.gridx = 2;
            gbc.weightx = 0;
            gbc.insets = new Insets(6, 0, 0, 12);
            panel.add(createFieldLabel(rightLabel), gbc);

            gbc.gridx = 3;
            gbc.weightx = 1;
            gbc.insets = new Insets(6, 0, 0, 0);
            panel.add(rightComponent, gbc);
        }
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private JPanel createSectionCard(String title, String subtitle, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(SURFACE_BG);
        card.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(16, 18, 16, 18)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION_TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(SMALL_FONT);
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(BODY_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(SURFACE_ALT);
        field.setCaretColor(ACCENT);
        field.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 12, 10, 12)));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setPreferredSize(new Dimension(0, 40));
    }

    private void styleCheckBox(JCheckBox box) {
        box.setFont(BODY_FONT);
        box.setForeground(TEXT_PRIMARY);
        box.setOpaque(false);
    }

    private JButton createDialogButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(BODY_FONT);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setOpaque(true);
        if (primary) {
            button.setBackground(Color.WHITE);
            button.setForeground(ACCENT);
            button.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(9, 18, 9, 18)));
        } else {
            button.setBackground(SURFACE_BG);
            button.setForeground(TEXT_PRIMARY);
            button.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(9, 18, 9, 18)));
        }
        return button;
    }

    private Border compoundBorder(Border outer, Border inner) {
        return BorderFactory.createCompoundBorder(outer, inner);
    }
}
