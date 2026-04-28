[English](Localization)

# 本地化

GuideNH 支持本地化的指南页面与本地化的指南资源。

## 目录结构

运行时本地化基于目录：

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

语言目录只认带下划线前缀的形式。像 `en_us/` 和 `zh_cn/` 这样的普通目录不再被当作本地化根目录。

## 页面查找顺序

对于每个请求的页面 id，GuideNH 会依次尝试：

1. `_<current language>/<page>`
2. 若当前语言不是指南默认语言，则尝试 `_<default language>/<page>`
3. 不带语言目录的 `<page>`

在实际使用中，建议把页面文件都放进带下划线的语言目录，并把默认语言作为主要回退来源。

## 资源查找顺序

指南资源使用稍微更丰富的回退顺序：

1. `_<current language>/<path>`
2. 若当前语言不是指南默认语言，则尝试 `_<default language>/<path>`
3. `<path>`

这样在需要时，就可以对图片或类似纹理的资源进行本地化。

## 搜索与语言

搜索文档会同时记录原始 Minecraft 语言和 Lucene 实际使用的 analyzer 语言。若当前 Minecraft 语言未映射到已知 analyzer，搜索会回退到英文分词。

## 忽略翻译配置

GuideNH 提供一个客户端配置项，名称为 `Ignore Guide Translations`。启用后，无论当前 UI 语言是什么，都将强制使用指南原始语言。

## 编写建议

- 始终保留一份完整的默认语言
- 先翻译页面，再在资源中确实嵌入了文本时才翻译资源
- 若共享资源足够通用，就不要额外引入语言特定的资源文件名

## 示例

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## 相关页面

- [指南页面格式](Guide-Page-Format-zh-CN)
- [图片与资源](Images-And-Assets-zh-CN)
