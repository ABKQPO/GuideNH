[English](Getting-Started)

# 快速开始

本页展示一个最小但实用的 GuideNH 运行时指南目录结构，以及你可以编写的第一个页面。

## 最小运行时结构

```text
wiki/resourcepack/
`-- assets/
    `-- <modid>/
        `-- guidenh/
            |-- assets/
            |   `-- example_structure.snbt
            `-- _en_us/
                `-- index.md
```

对于本仓库内置的示例指南，对应路径是：

```text
wiki/resourcepack/assets/guidenh/guidenh/
```

## 指南发现机制

GuideNH 现在会直接从资源树中发现页面。凡是位于
`assets/<modid>/guidenh/_<lang>/...` 下的 Markdown 文件，都会被视为 `<modid>:guidenh` 这本指南的一部分。
`index.md` 仍然是约定俗成的起始页，也是推荐的开始位置。

## 本地快速迭代

如果你正在编辑本仓库的内置示例指南，推荐使用 [实时预览](Live-Preview-zh-CN) 中说明的工作流。

该工作流会把 GuideNH 指向：

```text
wiki/resourcepack/assets/guidenh/guidenh/
```

并在启动时自动打开这本指南。

## 第一个页面

````md
---
navigation:
  title: Root
---

# Start Page

Welcome to GuideNH.

[Next Page](subpage.md)
````

## 添加导航元数据

用于导航的最小 frontmatter 形式是：

```yaml
navigation:
  title: Root
```

如果没有 `navigation` frontmatter，页面仍然可以存在，也可以被直接链接到，但不会自动出现在指南导航树中。

## 添加资源

把页面私有资源放在页面文件旁边：

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
```

然后在 Markdown 中使用相对路径引用：

````md
![Example](test1.png)
````

把共享指南资源放到该指南自己的 `assets/` 目录下：

```text
wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt
```

然后使用指南根路径引用：

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

## 下一步

- [实时预览](Live-Preview-zh-CN)
- [指南页面格式](Guide-Page-Format-zh-CN)
- [导航](Navigation-zh-CN)
- [图片与资源](Images-And-Assets-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
