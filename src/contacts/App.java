package contacts;

import contacts.io.AddressBookStorage;
import contacts.model.AddressBook;
import contacts.model.Contact;
import contacts.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;
import java.time.LocalDate;
import java.util.Set;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
            }

            File dataDir = new File("data");
            File dataFile = new File(dataDir, "address-book.xml");
            AddressBookStorage storage = new AddressBookStorage(dataFile);
            AddressBook addressBook = storage.load();

            if (addressBook.getContacts().isEmpty()) {
                seedSampleContacts(addressBook);
                storage.save(addressBook);
            }

            MainFrame frame = new MainFrame(addressBook, storage);
            frame.setVisible(true);
        });
    }

    private static void seedSampleContacts(AddressBook book) {
        Contact c1 = new Contact();
        c1.setName("张三");
        c1.setPhone("010-62310001");
        c1.setMobile("13800001111");
        c1.setImType("微信");
        c1.setImAccount("zhangsan_wx");
        c1.setEmail("zhangsan@example.com");
        c1.setWebsite("https://zhangsan.blog.com");
        c1.setBirthday(LocalDate.of(1992, 5, 15));
        c1.setCompany("北京科技有限公司");
        c1.setHomeAddress("北京市海淀区中关村大街 1 号");
        c1.setPostalCode("100080");
        c1.setGroups(Set.of("同事", "朋友"));
        c1.setNotes("技术部工程师，擅长 Java 开发。");
        book.addContact(c1);

        Contact c2 = new Contact();
        c2.setName("李丽");
        c2.setPhone("021-52340002");
        c2.setMobile("13900002222");
        c2.setImType("QQ");
        c2.setImAccount("987654321");
        c2.setEmail("lili@example.com");
        c2.setWebsite("https://lili.design.com");
        c2.setBirthday(LocalDate.of(1995, 8, 22));
        c2.setCompany("上海设计工作室");
        c2.setHomeAddress("上海市静安区南京西路 1888 号");
        c2.setPostalCode("200040");
        c2.setGroups(Set.of("朋友"));
        c2.setNotes("UI 设计师，喜欢摄影和旅行。");
        book.addContact(c2);

        Contact c3 = new Contact();
        c3.setName("王小明");
        c3.setPhone("0755-88660003");
        c3.setMobile("13600003333");
        c3.setImType("微信");
        c3.setImAccount("wx_wangxm");
        c3.setEmail("wangxm@example.com");
        c3.setWebsite("");
        c3.setBirthday(LocalDate.of(1988, 12, 3));
        c3.setCompany("深圳创新科技有限公司");
        c3.setHomeAddress("深圳市南山区科技园南路 8 号");
        c3.setPostalCode("518057");
        c3.setGroups(Set.of("同事"));
        c3.setNotes("项目经理，负责海外市场拓展。");
        book.addContact(c3);

        Contact c4 = new Contact();
        c4.setName("赵芳");
        c4.setPhone("010-88870004");
        c4.setMobile("13700004444");
        c4.setImType("微信");
        c4.setImAccount("zhaofang_family");
        c4.setEmail("zhaofang@example.com");
        c4.setWebsite("");
        c4.setBirthday(LocalDate.of(1965, 3, 18));
        c4.setCompany("北京大学附属中学");
        c4.setHomeAddress("北京市朝阳区望京花园东区 5 号楼");
        c4.setPostalCode("100102");
        c4.setGroups(Set.of("家人"));
        c4.setNotes("母亲，已退休教师。");
        book.addContact(c4);

        Contact c5 = new Contact();
        c5.setName("陈伟");
        c5.setPhone("020-33660005");
        c5.setMobile("13500005555");
        c5.setImType("WhatsApp");
        c5.setImAccount("+8613500005555");
        c5.setEmail("chenwei@example.com");
        c5.setWebsite("https://chenwei.github.io");
        c5.setBirthday(LocalDate.of(1993, 7, 9));
        c5.setCompany("广州开源软件有限公司");
        c5.setHomeAddress("广州市天河区体育西路 111 号");
        c5.setPostalCode("510620");
        c5.setGroups(Set.of("同事", "朋友", "家人"));
        c5.setNotes("全栈工程师，活跃于开源社区。");
        book.addContact(c5);
    }
}
