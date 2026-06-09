package contacts.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Contact {
    private String id;
    private String name = "";
    private String phone = "";
    private String mobile = "";
    private String imType = "";
    private String imAccount = "";
    private String email = "";
    private String website = "";
    private LocalDate birthday;
    private String photoPath = "";
    private String company = "";
    private String homeAddress = "";
    private String postalCode = "";
    private final Set<String> groups = new LinkedHashSet<>();
    private String notes = "";

    public Contact() {
        this.id = UUID.randomUUID().toString();
    }

    public Contact(Contact other) {
        this.id = other.id;
        this.name = other.name;
        this.phone = other.phone;
        this.mobile = other.mobile;
        this.imType = other.imType;
        this.imAccount = other.imAccount;
        this.email = other.email;
        this.website = other.website;
        this.birthday = other.birthday;
        this.photoPath = other.photoPath;
        this.company = other.company;
        this.homeAddress = other.homeAddress;
        this.postalCode = other.postalCode;
        this.groups.addAll(other.groups);
        this.notes = other.notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = safe(name);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = safe(phone);
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = safe(mobile);
    }

    public String getImType() {
        return imType;
    }

    public void setImType(String imType) {
        this.imType = safe(imType);
    }

    public String getImAccount() {
        return imAccount;
    }

    public void setImAccount(String imAccount) {
        this.imAccount = safe(imAccount);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = safe(email);
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = safe(website);
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = safe(photoPath);
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = safe(company);
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = safe(homeAddress);
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = safe(postalCode);
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groupNames) {
        groups.clear();
        if (groupNames != null) {
            groupNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(groups::add);
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = safe(notes);
    }

    public String getDisplayBirthday() {
        return birthday == null ? "" : birthday.toString();
    }

    public Contact copy() {
        return new Contact(this);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
