package contacts.model;

import contacts.util.PinyinUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AddressBook {
    public static final String ALL_CONTACTS = "全部联系人";
    public static final String UNGROUPED = "未分组";

    private static final Comparator<Contact> CONTACT_COMPARATOR = (a, b) -> {
        Collator collator = Collator.getInstance(Locale.CHINA);
        int nameCompare = collator.compare(a.getName(), b.getName());
        if (nameCompare != 0) {
            return nameCompare;
        }
        return a.getId().compareTo(b.getId());
    };

    private final List<Contact> contacts = new ArrayList<>();
    private final Set<String> groups = new LinkedHashSet<>();

    public AddressBook() {
        groups.add("家人");
        groups.add("朋友");
        groups.add("同事");
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void replaceAll(List<Contact> newContacts, Collection<String> newGroups) {
        contacts.clear();
        groups.clear();
        if (newGroups != null) {
            newGroups.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(groups::add);
        }
        if (newContacts != null) {
            newContacts.forEach(this::addContact);
        }
        groups.addAll(discoverGroupsFromContacts());
    }

    public void addContact(Contact contact) {
        Contact stored = contact.copy();
        contacts.add(stored);
        groups.addAll(stored.getGroups());
    }

    public void updateContact(Contact updatedContact) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(updatedContact.getId())) {
                contacts.set(i, updatedContact.copy());
                groups.addAll(updatedContact.getGroups());
                return;
            }
        }
        addContact(updatedContact);
    }

    public void removeContact(String contactId) {
        contacts.removeIf(contact -> contact.getId().equals(contactId));
    }

    public boolean addGroup(String groupName) {
        String normalized = normalize(groupName);
        return !normalized.isEmpty() && groups.add(normalized);
    }

    public void removeGroup(String groupName) {
        String normalized = normalize(groupName);
        if (normalized.isEmpty()) {
            return;
        }
        groups.remove(normalized);
        contacts.forEach(contact -> contact.getGroups().remove(normalized));
    }

    public List<Contact> queryContacts(String groupFilter, String searchText) {
        String group = groupFilter == null ? ALL_CONTACTS : groupFilter;
        String keyword = normalize(searchText).toLowerCase(Locale.ROOT);
        return contacts.stream()
                .filter(contact -> matchesGroup(contact, group))
                .filter(contact -> matchesKeyword(contact, keyword))
                .sorted(CONTACT_COMPARATOR)
                .collect(Collectors.toList());
    }

    public void assignContactsToGroup(Collection<Contact> selectedContacts, String groupName) {
        String normalized = normalize(groupName);
        if (normalized.isEmpty()) {
            return;
        }
        groups.add(normalized);
        selectedContacts.forEach(contact -> contact.getGroups().add(normalized));
    }

    public void removeContactsFromGroup(Collection<Contact> selectedContacts, String groupName) {
        String normalized = normalize(groupName);
        if (normalized.isEmpty()) {
            return;
        }
        selectedContacts.forEach(contact -> contact.getGroups().remove(normalized));
    }

    private boolean matchesGroup(Contact contact, String group) {
        if (ALL_CONTACTS.equals(group)) {
            return true;
        }
        if (UNGROUPED.equals(group)) {
            return contact.getGroups().isEmpty();
        }
        return contact.getGroups().contains(group);
    }

    private boolean matchesKeyword(Contact contact, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }
        String fullPinyin = PinyinUtils.toPinyin(contact.getName()).toLowerCase(Locale.ROOT);
        String initials = PinyinUtils.toInitials(contact.getName()).toLowerCase(Locale.ROOT);
        return contact.getName().toLowerCase(Locale.ROOT).contains(keyword)
                || contact.getPhone().toLowerCase(Locale.ROOT).contains(keyword)
                || contact.getMobile().toLowerCase(Locale.ROOT).contains(keyword)
                || fullPinyin.contains(keyword)
                || initials.contains(keyword);
    }

    private Set<String> discoverGroupsFromContacts() {
        return contacts.stream()
                .flatMap(contact -> contact.getGroups().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
