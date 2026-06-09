package contacts.io;

import contacts.model.Contact;

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

public class VCardService {
    public void exportContacts(List<Contact> contacts, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (Contact contact : contacts) {
                writeLine(writer, "BEGIN:VCARD");
                writeLine(writer, "VERSION:3.0");
                writeLine(writer, "FN;CHARSET=UTF-8:" + escape(contact.getName()));
                writeLine(writer, "N;CHARSET=UTF-8:" + escape(contact.getName()) + ";;;;");
                if (!contact.getPhone().isBlank()) {
                    writeLine(writer, "TEL;TYPE=HOME,VOICE:" + escape(contact.getPhone()));
                }
                if (!contact.getMobile().isBlank()) {
                    writeLine(writer, "TEL;TYPE=CELL:" + escape(contact.getMobile()));
                }
                if (!contact.getEmail().isBlank()) {
                    writeLine(writer, "EMAIL;TYPE=INTERNET:" + escape(contact.getEmail()));
                }
                if (!contact.getWebsite().isBlank()) {
                    writeLine(writer, "URL:" + escape(contact.getWebsite()));
                }
                if (contact.getBirthday() != null) {
                    writeLine(writer, "BDAY:" + contact.getBirthday().toString().replace("-", ""));
                }
                if (!contact.getCompany().isBlank()) {
                    writeLine(writer, "ORG;CHARSET=UTF-8:" + escape(contact.getCompany()));
                }
                if (!contact.getHomeAddress().isBlank() || !contact.getPostalCode().isBlank()) {
                    writeLine(writer, "ADR;TYPE=HOME;CHARSET=UTF-8:;;" + escape(contact.getHomeAddress())
                            + ";;;;" + escape(contact.getPostalCode()));
                }
                if (!contact.getImType().isBlank() || !contact.getImAccount().isBlank()) {
                    writeLine(writer, "X-IM:" + escape(contact.getImType() + ":" + contact.getImAccount()));
                }
                if (!contact.getPhotoPath().isBlank()) {
                    writeLine(writer, "PHOTO;VALUE=URI:" + escape(contact.getPhotoPath()));
                }
                if (!contact.getGroups().isEmpty()) {
                    writeLine(writer, "CATEGORIES;CHARSET=UTF-8:" + escape(String.join(",", contact.getGroups())));
                }
                if (!contact.getNotes().isBlank()) {
                    writeLine(writer, "NOTE;CHARSET=UTF-8:" + escape(contact.getNotes()));
                }
                writeLine(writer, "END:VCARD");
            }
        }
    }

    public List<Contact> importContacts(File file) throws IOException {
        List<Contact> contacts = new ArrayList<>();
        List<String> allLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<String> logicalLines = unfold(allLines);
        Contact current = null;
        String charset = null;
        for (String line : logicalLines) {
            if ("BEGIN:VCARD".equalsIgnoreCase(line)) {
                current = new Contact();
                charset = null;
                continue;
            }
            if ("END:VCARD".equalsIgnoreCase(line)) {
                if (current != null) {
                    contacts.add(current);
                }
                current = null;
                charset = null;
                continue;
            }
            if (current == null) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String rawKey = line.substring(0, colon);
            String value = unescape(line.substring(colon + 1));

            // Parse CHARSET
            String upperKey = rawKey.toUpperCase();
            if (upperKey.contains("CHARSET=UTF-8")) {
                upperKey = upperKey.replace(";CHARSET=UTF-8", "");
            } else if (upperKey.contains("CHARSET=UTF8")) {
                upperKey = upperKey.replace(";CHARSET=UTF8", "");
            } else {
                String cleanedKey = upperKey.replaceAll(";CHARSET=[^;:]+", "");
                if (!cleanedKey.equals(upperKey)) {
                    upperKey = cleanedKey;
                }
            }

            // Remove encoding prefix for vCard 2.1
            int encodingColon = upperKey.indexOf(';');
            if (encodingColon > 0) {
                String prefix = upperKey.substring(0, encodingColon);
                String suffix = upperKey.substring(encodingColon);
                if (suffix.contains("ENCODING=QUOTED-PRINTABLE")) {
                    suffix = suffix.replace(";ENCODING=QUOTED-PRINTABLE", "");
                    upperKey = prefix + suffix;
                }
            }

            if (upperKey.startsWith("FN") && !upperKey.contains("TYPE=")) {
                current.setName(value);
            } else if (upperKey.startsWith("N") && (current.getName().isEmpty() || rawKey.toUpperCase().contains("N:") || rawKey.toUpperCase().contains("N;"))) {
                // Parse structured name: Last;First;Middle;Prefix;Suffix
                String[] nameParts = splitStructured(value);
                if (nameParts.length >= 2 && !nameParts[1].isEmpty()) {
                    current.setName(nameParts[1] + (nameParts.length >= 1 ? nameParts[0] : ""));
                } else if (nameParts.length >= 1 && !nameParts[0].isEmpty()) {
                    current.setName(nameParts[0]);
                }
            } else if (upperKey.startsWith("TEL")) {
                if (upperKey.contains("CELL") || upperKey.contains("MOBILE")) {
                    current.setMobile(value);
                } else if (upperKey.contains("WORK") || current.getPhone().isEmpty()) {
                    if (current.getPhone().isEmpty()) {
                        current.setPhone(value);
                    } else {
                        if (current.getMobile().isEmpty()) {
                            current.setMobile(value);
                        }
                    }
                }
            } else if (upperKey.startsWith("EMAIL")) {
                if (current.getEmail().isEmpty()) {
                    current.setEmail(value);
                }
            } else if (upperKey.startsWith("URL")) {
                current.setWebsite(value);
            } else if (upperKey.startsWith("BDAY")) {
                current.setBirthday(parseBirthday(value));
            } else if (upperKey.startsWith("ORG")) {
                current.setCompany(value);
            } else if (upperKey.startsWith("TITLE")) {
                // Store in notes if no dedicated field
                if (current.getNotes().isEmpty()) {
                    current.setNotes("职位: " + value);
                }
            } else if (upperKey.startsWith("ADR")) {
                String[] parts = splitStructured(value);
                // ADR: post-office-box;extended;street;city;region;postal-code;country
                if (parts.length >= 3) {
                    current.setHomeAddress(parts[2]);
                }
                if (parts.length >= 6 && !parts[5].isEmpty()) {
                    current.setPostalCode(parts[5]);
                }
            } else if (upperKey.startsWith("NOTE")) {
                current.setNotes(value);
            } else if (upperKey.startsWith("PHOTO")) {
                current.setPhotoPath(value);
            } else if (upperKey.startsWith("CATEGORIES")) {
                Set<String> groups = new LinkedHashSet<>();
                for (String part : value.split("[,;]")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        groups.add(trimmed);
                    }
                }
                current.setGroups(groups);
            } else if (upperKey.startsWith("X-IM") || upperKey.startsWith("X-QQ") || upperKey.startsWith("X-MSN")
                    || upperKey.startsWith("X-WECHAT") || upperKey.startsWith("X-SKYPE")) {
                String imType = upperKey.replace("X-", "");
                current.setImType(imType);
                current.setImAccount(value);
            } else if (upperKey.startsWith("X-")) {
                if (current.getNotes().isEmpty()) {
                    current.setNotes(upperKey.replace("X-", "") + ": " + value);
                } else {
                    current.setNotes(current.getNotes() + "\n" + upperKey.replace("X-", "") + ": " + value);
                }
            }
        }
        return contacts;
    }

    private List<String> unfold(List<String> lines) {
        List<String> logicalLines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                current.append(line.substring(1));
            } else {
                if (current.length() > 0) {
                    logicalLines.add(current.toString());
                }
                current = new StringBuilder(line);
            }
        }
        if (current.length() > 0) {
            logicalLines.add(current.toString());
        }
        return logicalLines;
    }

    private String[] splitStructured(String value) {
        // Split by ; handling escaped semicolons
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == ';') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private LocalDate parseBirthday(String value) {
        String normalized = value.replace("-", "").replace("T", "").trim();
        if (normalized.length() >= 8) {
            try {
                return LocalDate.of(
                        Integer.parseInt(normalized.substring(0, 4)),
                        Integer.parseInt(normalized.substring(4, 6)),
                        Integer.parseInt(normalized.substring(6, 8)));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    private String unescape(String text) {
        return text.replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }
}
