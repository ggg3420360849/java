package contacts.io;

import contacts.model.AddressBook;
import contacts.model.Contact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AddressBookStorage {
    private final File storageFile;

    public AddressBookStorage(File storageFile) {
        this.storageFile = storageFile;
    }

    public AddressBook load() {
        AddressBook addressBook = new AddressBook();
        if (!storageFile.exists()) {
            return addressBook;
        }
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(storageFile);
            document.getDocumentElement().normalize();

            Set<String> groups = new LinkedHashSet<>();
            List<Contact> contacts = new ArrayList<>();

            NodeList groupNodes = document.getElementsByTagName("group");
            for (int i = 0; i < groupNodes.getLength(); i++) {
                Node node = groupNodes.item(i);
                if (node.getParentNode() != null && "groups".equals(node.getParentNode().getNodeName())) {
                    groups.add(node.getTextContent().trim());
                }
            }

            NodeList contactNodes = document.getElementsByTagName("contact");
            for (int i = 0; i < contactNodes.getLength(); i++) {
                Node node = contactNodes.item(i);
                if (node instanceof Element element) {
                    contacts.add(readContact(element));
                }
            }

            addressBook.replaceAll(contacts, groups);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return addressBook;
    }

    public void save(AddressBook addressBook) {
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            Element root = document.createElement("addressBook");
            document.appendChild(root);

            Element groupsElement = document.createElement("groups");
            root.appendChild(groupsElement);
            for (String group : addressBook.getGroups()) {
                appendChild(document, groupsElement, "group", group);
            }

            Element contactsElement = document.createElement("contacts");
            root.appendChild(contactsElement);
            for (Contact contact : addressBook.getContacts()) {
                contactsElement.appendChild(writeContact(document, contact));
            }

            storageFile.getParentFile().mkdirs();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(storageFile));
        } catch (Exception exception) {
            throw new RuntimeException("保存通讯录失败", exception);
        }
    }

    private Contact readContact(Element element) {
        Contact contact = new Contact();
        contact.setId(element.getAttribute("id"));
        contact.setName(readText(element, "name"));
        contact.setPhone(readText(element, "phone"));
        contact.setMobile(readText(element, "mobile"));
        contact.setImType(readText(element, "imType"));
        contact.setImAccount(readText(element, "imAccount"));
        contact.setEmail(readText(element, "email"));
        contact.setWebsite(readText(element, "website"));
        String birthday = readText(element, "birthday");
        if (!birthday.isBlank()) {
            contact.setBirthday(LocalDate.parse(birthday));
        }
        contact.setPhotoPath(readText(element, "photoPath"));
        contact.setCompany(readText(element, "company"));
        contact.setHomeAddress(readText(element, "homeAddress"));
        contact.setPostalCode(readText(element, "postalCode"));
        contact.setNotes(readText(element, "notes"));

        NodeList groupNodes = element.getElementsByTagName("group");
        Set<String> groups = new LinkedHashSet<>();
        for (int i = 0; i < groupNodes.getLength(); i++) {
            Node node = groupNodes.item(i);
            if (node.getParentNode() != null && "contactGroups".equals(node.getParentNode().getNodeName())) {
                groups.add(node.getTextContent().trim());
            }
        }
        contact.setGroups(groups);
        return contact;
    }

    private Element writeContact(Document document, Contact contact) {
        Element element = document.createElement("contact");
        element.setAttribute("id", contact.getId());
        appendChild(document, element, "name", contact.getName());
        appendChild(document, element, "phone", contact.getPhone());
        appendChild(document, element, "mobile", contact.getMobile());
        appendChild(document, element, "imType", contact.getImType());
        appendChild(document, element, "imAccount", contact.getImAccount());
        appendChild(document, element, "email", contact.getEmail());
        appendChild(document, element, "website", contact.getWebsite());
        appendChild(document, element, "birthday", contact.getDisplayBirthday());
        appendChild(document, element, "photoPath", contact.getPhotoPath());
        appendChild(document, element, "company", contact.getCompany());
        appendChild(document, element, "homeAddress", contact.getHomeAddress());
        appendChild(document, element, "postalCode", contact.getPostalCode());
        appendChild(document, element, "notes", contact.getNotes());

        Element groupsElement = document.createElement("contactGroups");
        element.appendChild(groupsElement);
        for (String group : contact.getGroups()) {
            appendChild(document, groupsElement, "group", group);
        }
        return element;
    }

    private void appendChild(Document document, Element parent, String tagName, String value) {
        Element child = document.createElement(tagName);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
    }

    private String readText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return "";
        }
        return nodeList.item(0).getTextContent().trim();
    }
}
