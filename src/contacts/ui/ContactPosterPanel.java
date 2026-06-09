package contacts.ui;

import contacts.model.Contact;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContactPosterPanel extends JPanel {
    private static final Color APP_BG = new Color(0xF2FBF8);
    private static final Color SURFACE_BG = Color.WHITE;
    private static final Color SURFACE_ALT = new Color(0xF7FCFB);
    private static final Color BORDER_COLOR = new Color(0xD8EAE3);
    private static final Color TEXT_PRIMARY = new Color(0x25423B);
    private static final Color TEXT_MUTED = new Color(0x6D8A82);
    private static final Color HEADER_START = new Color(0x61B99E);
    private static final Color HEADER_END = new Color(0x8BCDB5);
    private static final Color[] TAG_BG = {
            new Color(0xE8F7F1),
            new Color(0xEAF6FB),
            new Color(0xFFF3E6),
            new Color(0xFAEEEE),
            new Color(0xF1F7EB)
    };
    private static final Color[] TAG_FG = {
            new Color(0x4DAF95),
            new Color(0x5DB7D1),
            new Color(0xD4883D),
            new Color(0xC96B6B),
            new Color(0x7EA36A)
    };
    private static final Color[] AVATAR_FALLBACK_COLORS = {
            new Color(0x4DAF95),
            new Color(0x5DB7D1),
            new Color(0xF2A65A),
            new Color(0x89C2A8),
            new Color(0xE58B8B),
            new Color(0x79B7A0)
    };
    private static final Font NAME_FONT = new Font("Microsoft YaHei", Font.BOLD, 28);
    private static final Font SUMMARY_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final Font LABEL_FONT = new Font("Microsoft YaHei", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("Microsoft YaHei", Font.PLAIN, 14);
    private static final Font EMPTY_TITLE_FONT = new Font("Microsoft YaHei", Font.BOLD, 16);
    private static final Font EMPTY_BODY_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final int HEADER_AVATAR_SIZE = 78;

    private final JPanel headerPanel = new GradientHeaderPanel();
    private final JLabel avatarLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel nameLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel();
    private final JPanel headerTagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JPanel contentPanel = new JPanel();
    private final JScrollPane scrollPane;

    private Runnable onImportCsv;
    private Runnable onImportVcf;

    public ContactPosterPanel() {
        setLayout(new BorderLayout());
        setBackground(APP_BG);

        buildHeader();

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(APP_BG);
        contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(APP_BG);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        showCompactEmpty();
    }

    public void setOnImportCsv(Runnable r) { this.onImportCsv = r; }
    public void setOnImportVcf(Runnable r) { this.onImportVcf = r; }

    public void showCompactContact(Contact contact) {
        if (contact == null) {
            showCompactEmpty();
            return;
        }

        contentPanel.removeAll();
        headerTagsPanel.removeAll();
        setAvatar(contact);

        nameLabel.setText(contact.getName().isBlank() ? "未命名联系人" : contact.getName());
        summaryLabel.setText("");

        if (!contact.getGroups().isEmpty()) {
            int index = 0;
            for (String group : contact.getGroups()) {
                headerTagsPanel.add(createTag(group, index++));
            }
        }

        Map<String, String> contactInfo = new LinkedHashMap<>();
        if (!contact.getPhone().isBlank()) {
            contactInfo.put("电话", contact.getPhone());
        }
        if (!contact.getMobile().isBlank()) {
            contactInfo.put("手机", contact.getMobile());
        }
        if (!contact.getEmail().isBlank()) {
            contactInfo.put("电子邮箱", contact.getEmail());
        }
        if (!contact.getWebsite().isBlank()) {
            contactInfo.put("个人主页", contact.getWebsite());
        }
        if (!contact.getImType().isBlank() || !contact.getImAccount().isBlank()) {
            contactInfo.put(contact.getImType().isBlank() ? "即时通讯" : contact.getImType(), contact.getImAccount());
        }
        addTextSection("联系方式", contactInfo);

        Map<String, String> personalInfo = new LinkedHashMap<>();
        if (!contact.getDisplayBirthday().isBlank()) {
            personalInfo.put("生日", contact.getDisplayBirthday());
        }
        if (!contact.getCompany().isBlank()) {
            personalInfo.put("公司 / 学校", contact.getCompany());
        }
        if (!contact.getHomeAddress().isBlank()) {
            personalInfo.put("家庭地址", contact.getHomeAddress());
        }
        if (!contact.getPostalCode().isBlank()) {
            personalInfo.put("邮编", contact.getPostalCode());
        }
        addTextSection("基础资料", personalInfo);

        if (!contact.getGroups().isEmpty()) {
            JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            tags.setOpaque(false);
            int index = 0;
            for (String group : contact.getGroups()) {
                tags.add(createTag(group, index++));
            }
            addComponentSection("所属分组", tags);
        }

        if (!contact.getNotes().isBlank()) {
            Map<String, String> notes = new LinkedHashMap<>();
            notes.put("备注", contact.getNotes());
            addTextSection("备注", notes);
        }

        contentPanel.add(Box.createVerticalGlue());
        contentPanel.revalidate();
        contentPanel.repaint();
        headerTagsPanel.revalidate();
        headerTagsPanel.repaint();

        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    public void showCompactEmpty() {
        headerTagsPanel.removeAll();
        avatarLabel.setIcon(createFallbackAvatar("通讯录", HEADER_AVATAR_SIZE));
        avatarLabel.setText("");
        nameLabel.setText("通讯录");
        summaryLabel.setText("");

        contentPanel.removeAll();

        JPanel emptyCard = createCard(new BorderLayout(0, 12), new EmptyBorder(24, 24, 24, 24));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("导入联系人");
        title.setFont(EMPTY_TITLE_FONT);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel body = new JLabel("<html>从 CSV 或 vCard 文件导入联系人数据。<br>"
                + "<font color='#8AB5A5'>右键联系人列表可导出选中项为 CSV / vCard。</font></html>");
        body.setFont(EMPTY_BODY_FONT);
        body.setForeground(TEXT_MUTED);
        body.setBorder(new EmptyBorder(6, 0, 14, 0));
        body.setAlignmentX(LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(LEFT_ALIGNMENT);

        JButton csvBtn = new JButton("导入 CSV");
        csvBtn.setFont(LABEL_FONT);
        csvBtn.addActionListener(e -> { if (onImportCsv != null) onImportCsv.run(); });

        JButton vcfBtn = new JButton("导入 vCard");
        vcfBtn.setFont(LABEL_FONT);
        vcfBtn.addActionListener(e -> { if (onImportVcf != null) onImportVcf.run(); });

        btnRow.add(csvBtn);
        btnRow.add(vcfBtn);

        center.add(title);
        center.add(body);
        center.add(btnRow);

        emptyCard.add(center, BorderLayout.CENTER);
        contentPanel.add(emptyCard);
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addCompactQuickFacts(Contact contact) {
        JPanel facts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        facts.setOpaque(false);

        int count = 0;
        count += addFactChip(facts, contact.getMobile(), "手机");
        count += addFactChip(facts, contact.getPhone(), "电话");
        count += addFactChip(facts, contact.getEmail(), "邮箱");
        count += addFactChip(facts, contact.getCompany(), "公司");

        if (count == 0) {
            return;
        }

        JPanel card = createCard(new BorderLayout(0, 10), new EmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("摘要");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        title.setForeground(TEXT_PRIMARY);

        card.add(title, BorderLayout.NORTH);
        card.add(facts, BorderLayout.CENTER);
        contentPanel.add(card);
        contentPanel.add(Box.createVerticalStrut(12));
    }

    private String buildCompactSummary(Contact contact) {
        StringBuilder builder = new StringBuilder();
        appendSummaryPart(builder, contact.getCompany());
        appendSummaryPart(builder, contact.getMobile());
        appendSummaryPart(builder, contact.getEmail());
        return builder.toString();
    }

    public void showContact(Contact contact) {
        if (contact == null) {
            showEmpty();
            return;
        }

        contentPanel.removeAll();
        headerTagsPanel.removeAll();
        setAvatar(contact);

        nameLabel.setText(contact.getName().isBlank() ? "未命名联系人" : contact.getName());
        summaryLabel.setText("");

        if (!contact.getGroups().isEmpty()) {
            int index = 0;
            for (String group : contact.getGroups()) {
                headerTagsPanel.add(createTag(group, index++));
            }
        }

        Map<String, String> contactInfo = new LinkedHashMap<>();
        if (!contact.getPhone().isBlank()) {
            contactInfo.put("电话", contact.getPhone());
        }
        if (!contact.getMobile().isBlank()) {
            contactInfo.put("手机", contact.getMobile());
        }
        if (!contact.getEmail().isBlank()) {
            contactInfo.put("电子邮箱", contact.getEmail());
        }
        if (!contact.getWebsite().isBlank()) {
            contactInfo.put("个人主页", contact.getWebsite());
        }
        if (!contact.getImType().isBlank() || !contact.getImAccount().isBlank()) {
            contactInfo.put(contact.getImType().isBlank() ? "即时通讯" : contact.getImType(), contact.getImAccount());
        }
        addTextSection("联系方式", contactInfo);

        Map<String, String> personalInfo = new LinkedHashMap<>();
        if (!contact.getDisplayBirthday().isBlank()) {
            personalInfo.put("生日", contact.getDisplayBirthday());
        }
        if (!contact.getCompany().isBlank()) {
            personalInfo.put("公司 / 学校", contact.getCompany());
        }
        if (!contact.getHomeAddress().isBlank()) {
            personalInfo.put("家庭地址", contact.getHomeAddress());
        }
        if (!contact.getPostalCode().isBlank()) {
            personalInfo.put("邮编", contact.getPostalCode());
        }
        addTextSection("基础资料", personalInfo);

        if (!contact.getGroups().isEmpty()) {
            JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            tags.setOpaque(false);
            int index = 0;
            for (String group : contact.getGroups()) {
                tags.add(createTag(group, index++));
            }
            addComponentSection("所属分组", tags);
        }

        if (!contact.getNotes().isBlank()) {
            Map<String, String> notes = new LinkedHashMap<>();
            notes.put("备注", contact.getNotes());
            addTextSection("备注", notes);
        }

        contentPanel.add(Box.createVerticalGlue());
        contentPanel.revalidate();
        contentPanel.repaint();
        headerTagsPanel.revalidate();
        headerTagsPanel.repaint();

        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    public void showEmpty() {
        headerTagsPanel.removeAll();
        avatarLabel.setIcon(createFallbackAvatar("通讯录", HEADER_AVATAR_SIZE));
        avatarLabel.setText("");
        nameLabel.setText("请选择一个联系人");
        summaryLabel.setText("右侧会展示头像、分组、联系方式和备注信息");

        contentPanel.removeAll();

        JPanel emptyCard = createCard(new BorderLayout(0, 10), new EmptyBorder(28, 24, 28, 24));
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("联系人详情");
        title.setFont(EMPTY_TITLE_FONT);
        title.setForeground(TEXT_PRIMARY);

        JLabel body = new JLabel("<html>从左侧列表选择一个联系人，或者先新增联系人。<br>保存过的照片会直接显示为头像。</html>");
        body.setFont(EMPTY_BODY_FONT);
        body.setForeground(TEXT_MUTED);
        body.setBorder(new EmptyBorder(8, 0, 0, 0));

        text.add(title);
        text.add(body);
        center.add(text);
        emptyCard.add(center, BorderLayout.CENTER);

        contentPanel.add(emptyCard);
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void buildHeader() {
        headerPanel.setLayout(new BorderLayout(16, 0));
        headerPanel.setBorder(new EmptyBorder(22, 22, 20, 22));

        avatarLabel.setPreferredSize(new Dimension(HEADER_AVATAR_SIZE, HEADER_AVATAR_SIZE));
        avatarLabel.setMinimumSize(new Dimension(HEADER_AVATAR_SIZE, HEADER_AVATAR_SIZE));
        avatarLabel.setMaximumSize(new Dimension(HEADER_AVATAR_SIZE, HEADER_AVATAR_SIZE));
        avatarLabel.setOpaque(false);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        nameLabel.setFont(NAME_FONT);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        nameLabel.setAlignmentX(0.0f);

        summaryLabel.setFont(SUMMARY_FONT);
        summaryLabel.setForeground(new Color(0xE8F0FF));
        summaryLabel.setBorder(new EmptyBorder(0, 0, 0, 0));

        headerTagsPanel.setOpaque(false);
        headerTagsPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        headerTagsPanel.setAlignmentX(0.0f);

        textPanel.add(nameLabel);
        textPanel.add(summaryLabel);
        textPanel.add(headerTagsPanel);

        headerPanel.add(avatarLabel, BorderLayout.WEST);
        headerPanel.add(textPanel, BorderLayout.CENTER);
    }

    private void addQuickFacts(Contact contact) {
        JPanel card = createCard(new BorderLayout(0, 10), new EmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("摘要");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        title.setForeground(TEXT_PRIMARY);

        JPanel facts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        facts.setOpaque(false);

        int count = 0;
        count += addFactChip(facts, contact.getMobile(), "手机");
        count += addFactChip(facts, contact.getPhone(), "电话");
        count += addFactChip(facts, contact.getEmail(), "邮箱");
        count += addFactChip(facts, contact.getCompany(), "公司");

        if (count == 0) {
            addFactChip(facts, "资料还比较少，可以在编辑弹窗里继续补充。", "提示");
        }

        card.add(title, BorderLayout.NORTH);
        card.add(facts, BorderLayout.CENTER);
        contentPanel.add(card);
        contentPanel.add(Box.createVerticalStrut(12));
    }

    private int addFactChip(JPanel parent, String value, String label) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        JPanel chip = new JPanel(new BorderLayout(8, 0));
        chip.setBackground(SURFACE_ALT);
        chip.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(8, 10, 8, 10)));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(LABEL_FONT);
        labelComponent.setForeground(TEXT_MUTED);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(VALUE_FONT);
        valueComponent.setForeground(TEXT_PRIMARY);

        chip.add(labelComponent, BorderLayout.WEST);
        chip.add(valueComponent, BorderLayout.CENTER);
        parent.add(chip);
        return 1;
    }

    private void addTextSection(String title, Map<String, String> items) {
        if (items.isEmpty()) {
            return;
        }

        CollapsibleSection section = new CollapsibleSection(title);
        JPanel content = section.getContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        for (Map.Entry<String, String> entry : items.entrySet()) {
            JPanel row = new JPanel(new BorderLayout(14, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(6, 0, 6, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            JLabel key = new JLabel(entry.getKey());
            key.setFont(LABEL_FONT);
            key.setForeground(TEXT_MUTED);
            key.setPreferredSize(new Dimension(84, 22));

            JLabel value = new JLabel("<html>" + entry.getValue().replace("\n", "<br>") + "</html>");
            value.setFont(VALUE_FONT);
            value.setForeground(TEXT_PRIMARY);

            row.add(key, BorderLayout.WEST);
            row.add(value, BorderLayout.CENTER);
            content.add(row);
        }

        contentPanel.add(section);
        contentPanel.add(Box.createVerticalStrut(12));
    }

    private void addComponentSection(String title, JComponent component) {
        CollapsibleSection section = new CollapsibleSection(title);
        JPanel content = section.getContentPanel();
        content.setLayout(new BorderLayout());
        content.add(component, BorderLayout.CENTER);

        contentPanel.add(section);
        contentPanel.add(Box.createVerticalStrut(12));
    }

    private void setAvatar(Contact contact) {
        String photoPath = contact.getPhotoPath();
        if (photoPath != null && !photoPath.isBlank()) {
            Icon icon = createPhotoAvatar(photoPath, HEADER_AVATAR_SIZE);
            if (icon != null) {
                avatarLabel.setIcon(icon);
                avatarLabel.setText("");
                return;
            }
        }

        avatarLabel.setIcon(createFallbackAvatar(contact.getName(), HEADER_AVATAR_SIZE));
        avatarLabel.setText("");
    }

    private String buildSummary(Contact contact) {
        StringBuilder builder = new StringBuilder();
        appendSummaryPart(builder, contact.getCompany());
        appendSummaryPart(builder, contact.getMobile());
        appendSummaryPart(builder, contact.getEmail());
        if (builder.length() == 0) {
            builder.append("已保存联系人详情");
        }
        return builder.toString();
    }

    private void appendSummaryPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("  ·  ");
        }
        builder.append(value);
    }

    private JLabel createTag(String text, int index) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        label.setOpaque(true);
        label.setForeground(TAG_FG[index % TAG_FG.length]);
        label.setBackground(TAG_BG[index % TAG_BG.length]);
        label.setBorder(new EmptyBorder(3, 8, 3, 8));
        return label;
    }

    private JPanel createCard(java.awt.LayoutManager layout, Border padding) {
        JPanel card = new JPanel(layout);
        card.setBackground(SURFACE_BG);
        card.setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), padding));
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private Border compoundBorder(Border outer, Border inner) {
        return BorderFactory.createCompoundBorder(outer, inner);
    }

    private Icon createPhotoAvatar(String path, int size) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }

            BufferedImage source = ImageIO.read(file);
            if (source == null) {
                return null;
            }

            BufferedImage square = cropCenterSquare(source);
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(square, 0, 0, size, size, null);
            graphics.dispose();

            BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D circleGraphics = circle.createGraphics();
            circleGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape clip = new Ellipse2D.Float(0, 0, size, size);
            circleGraphics.setClip(clip);
            circleGraphics.drawImage(scaled, 0, 0, null);
            circleGraphics.setClip(null);
            circleGraphics.setColor(new Color(255, 255, 255, 180));
            circleGraphics.setStroke(new BasicStroke(2f));
            circleGraphics.draw(clip);
            circleGraphics.dispose();
            return new ImageIcon(circle);
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
        BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = avatar.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(avatarColorFor(name));
        graphics.fillOval(0, 0, size, size);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, Math.max(14, size / 2)));

        String initials = initialsFor(name);
        java.awt.FontMetrics metrics = graphics.getFontMetrics();
        int textX = (size - metrics.stringWidth(initials)) / 2;
        int textY = (size - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString(initials, textX, textY);
        graphics.dispose();
        return new ImageIcon(avatar);
    }

    private Color avatarColorFor(String name) {
        String safeName = name == null ? "" : name.trim();
        int index = Math.floorMod(safeName.hashCode(), AVATAR_FALLBACK_COLORS.length);
        return AVATAR_FALLBACK_COLORS[index];
    }

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String trimmed = name.trim();
        return trimmed.length() <= 2 ? trimmed : trimmed.substring(0, 2);
    }

    private class CollapsibleSection extends JPanel {
        private final JButton headerButton = new JButton();
        private final JPanel contentWrapper = new JPanel(new BorderLayout());
        private final JPanel contentPanel = new JPanel();
        private boolean expanded = true;

        private CollapsibleSection(String title) {
            setLayout(new BorderLayout());
            setAlignmentX(LEFT_ALIGNMENT);
            setBackground(SURFACE_BG);
            setBorder(compoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true), new EmptyBorder(0, 0, 0, 0)));

            headerButton.setText(title);
            headerButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            headerButton.setForeground(TEXT_PRIMARY);
            headerButton.setBackground(SURFACE_BG);
            headerButton.setOpaque(true);
            headerButton.setFocusPainted(false);
            headerButton.setContentAreaFilled(false);
            headerButton.setHorizontalAlignment(SwingConstants.LEFT);
            headerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            headerButton.setBorder(new EmptyBorder(14, 16, 14, 16));
            headerButton.addActionListener(e -> toggle(title));

            contentPanel.setOpaque(false);
            contentPanel.setBorder(new EmptyBorder(0, 16, 14, 16));

            contentWrapper.setOpaque(false);
            contentWrapper.add(contentPanel, BorderLayout.CENTER);

            add(headerButton, BorderLayout.NORTH);
            add(contentWrapper, BorderLayout.CENTER);
        }

        private void toggle(String title) {
            expanded = !expanded;
            headerButton.setText(title);
            contentWrapper.setVisible(expanded);
            revalidate();
            repaint();
        }

        private JPanel getContentPanel() {
            return contentPanel;
        }
    }

    private static class GradientHeaderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setPaint(new GradientPaint(0, 0, HEADER_START, getWidth(), getHeight(), HEADER_END));
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.dispose();
        }
    }
}
