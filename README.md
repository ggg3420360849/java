# 通讯录管理程序

这是一个基于 Java Swing 的单机版通讯录管理程序，数据通过本地 XML 文件保存在 `data/address-book.xml`。

## 已实现功能

- 联系人增删改
- 分组增删
- 联系人加入分组、移出分组
- 左侧分组筛选，右侧联系人列表展示
- 搜索姓名、电话、手机、姓名拼音首字母、姓名全拼
- 联系人列表显示字段可自定义
- CSV 导入导出
- vCard 导入导出

## 运行方式

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -cp out contacts.App
```

## 主要目录

- `src/contacts/App.java` 程序入口
- `src/contacts/ui` Swing 图形界面
- `src/contacts/model` 联系人与通讯录模型
- `src/contacts/io` XML、CSV、vCard 读写
- `src/contacts/util/PinyinUtils.java` 拼音搜索工具
