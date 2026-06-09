package contacts.ui;

import contacts.model.Contact;
import contacts.util.PinyinUtils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ContactTableModel extends AbstractTableModel {
    private static final Map<String, Function<Contact, ?>> COLUMN_MAP = new LinkedHashMap<>();

    static {
        COLUMN_MAP.put("姓名", Contact::getName);
        COLUMN_MAP.put("分类", contact -> PinyinUtils.sortBucket(contact.getName()));
        COLUMN_MAP.put("电话", Contact::getPhone);
        COLUMN_MAP.put("手机", Contact::getMobile);
        COLUMN_MAP.put("即时通讯", contact -> {
            if (contact.getImType().isBlank()) {
                return contact.getImAccount();
            }
            if (contact.getImAccount().isBlank()) {
                return contact.getImType();
            }
            return contact.getImType() + " · " + contact.getImAccount();
        });
        COLUMN_MAP.put("电子邮箱", Contact::getEmail);
        COLUMN_MAP.put("个人主页", Contact::getWebsite);
        COLUMN_MAP.put("生日", Contact::getDisplayBirthday);
        COLUMN_MAP.put("公司", Contact::getCompany);
        COLUMN_MAP.put("家庭地址", Contact::getHomeAddress);
        COLUMN_MAP.put("邮编", Contact::getPostalCode);
        COLUMN_MAP.put("所属组", contact -> String.join(", ", contact.getGroups()));
        COLUMN_MAP.put("备注", Contact::getNotes);
    }

    private final List<Contact> contacts = new ArrayList<>();
    private final List<String> visibleColumns = new ArrayList<>(
            Arrays.asList("姓名", "手机", "公司", "所属组"));
    private final Set<String> checkedIds = new LinkedHashSet<>();

    public void setContacts(List<Contact> values) {
        contacts.clear();
        contacts.addAll(values);
        checkedIds.clear();
        fireTableDataChanged();
    }

    public Contact getContactAt(int row) {
        return row >= 0 && row < contacts.size() ? contacts.get(row) : null;
    }

    public List<Contact> getContactsAt(int[] rows) {
        List<Contact> selected = new ArrayList<>();
        for (int row : rows) {
            Contact contact = getContactAt(row);
            if (contact != null) {
                selected.add(contact);
            }
        }
        return selected;
    }

    public List<Contact> getCheckedContacts() {
        List<Contact> selected = new ArrayList<>();
        for (Contact contact : contacts) {
            if (checkedIds.contains(contact.getId())) {
                selected.add(contact);
            }
        }
        return selected;
    }

    public int getCheckedCount() {
        return checkedIds.size();
    }

    public void toggleAll(boolean select) {
        if (select) {
            for (Contact contact : contacts) {
                checkedIds.add(contact.getId());
            }
        } else {
            checkedIds.clear();
        }
        fireTableDataChanged();
    }

    public void setVisibleColumns(Set<String> columns) {
        visibleColumns.clear();
        COLUMN_MAP.keySet().stream()
                .filter(columns::contains)
                .forEach(visibleColumns::add);
        fireTableStructureChanged();
    }

    public Set<String> getAvailableColumns() {
        return new LinkedHashSet<>(COLUMN_MAP.keySet());
    }

    public Set<String> getVisibleColumns() {
        return new LinkedHashSet<>(visibleColumns);
    }

    @Override
    public int getRowCount() {
        return contacts.size();
    }

    @Override
    public int getColumnCount() {
        return 1 + visibleColumns.size();
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? " " : visibleColumns.get(column - 1);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 0 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return checkedIds.contains(contacts.get(rowIndex).getId());
        }
        return COLUMN_MAP.get(visibleColumns.get(columnIndex - 1)).apply(contacts.get(rowIndex));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && value instanceof Boolean selected) {
            String id = contacts.get(rowIndex).getId();
            if (selected) {
                checkedIds.add(id);
            } else {
                checkedIds.remove(id);
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
