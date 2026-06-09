package contacts.io;

import contacts.model.Contact;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CsvService {
    private static final String[] HEADERS = {
            "姓名", "电话", "手机", "即时通信工具", "即时通信号码", "电子邮箱", "个人主页",
            "生日", "像片", "工作单位", "家庭地址", "邮编", "所属组", "备注"
    };

    public void exportContacts(List<Contact> contacts, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(String.join(",", HEADERS));
            writer.newLine();
            for (Contact contact : contacts) {
                writer.write(String.join(",",
                        quote(contact.getName()),
                        quote(contact.getPhone()),
                        quote(contact.getMobile()),
                        quote(contact.getImType()),
                        quote(contact.getImAccount()),
                        quote(contact.getEmail()),
                        quote(contact.getWebsite()),
                        quote(contact.getDisplayBirthday()),
                        quote(contact.getPhotoPath()),
                        quote(contact.getCompany()),
                        quote(contact.getHomeAddress()),
                        quote(contact.getPostalCode()),
                        quote(String.join("|", contact.getGroups())),
                        quote(contact.getNotes())));
                writer.newLine();
            }
        }
    }

    public List<Contact> importContacts(File file) throws IOException {
        List<Contact> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null) {
                return result;
            }
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseLine(line);
                Contact contact = new Contact();
                contact.setName(get(values, 0));
                contact.setPhone(get(values, 1));
                contact.setMobile(get(values, 2));
                contact.setImType(get(values, 3));
                contact.setImAccount(get(values, 4));
                contact.setEmail(get(values, 5));
                contact.setWebsite(get(values, 6));
                String birthday = get(values, 7);
                if (!birthday.isBlank()) {
                    contact.setBirthday(LocalDate.parse(birthday));
                }
                contact.setPhotoPath(get(values, 8));
                contact.setCompany(get(values, 9));
                contact.setHomeAddress(get(values, 10));
                contact.setPostalCode(get(values, 11));
                Set<String> groups = new LinkedHashSet<>();
                String[] groupParts = get(values, 12).split("\\|");
                for (String groupPart : groupParts) {
                    String trimmed = groupPart.trim();
                    if (!trimmed.isEmpty()) {
                        groups.add(trimmed);
                    }
                }
                contact.setGroups(groups);
                contact.setNotes(get(values, 13));
                result.add(contact);
            }
        }
        return result;
    }

    private String quote(String value) {
        String content = value == null ? "" : value;
        return "\"" + content.replace("\"", "\"\"") + "\"";
    }

    private List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String get(List<String> values, int index) {
        return index < values.size() ? values.get(index).trim() : "";
    }
}
