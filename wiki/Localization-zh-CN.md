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

语言目录只认以下划线开头的形式。像 `en_us/` 和 `zh_cn/` 这样的普通目录不再被当作本地化根目录。

## 页面查找顺序

对于每个请求的页面 id，GuideNH 会依次尝试：

1. `_<current language>/<page>`
2. 若当前语言页面缺失，则尝试 `_<default language>/<page>`
3. 不带语言目录的 `<page>`

指南页面只会回退到该 guide 的 `defaultLanguage`。自动发现的资源包 guide 仍然默认把这个值设为 `en_us`，所以不会因为某个别的语言存在，就把它自动提升成兜底语言。

## 页面 Lang Key 覆盖

Guide 页面还可以通过 `.lang` key 覆盖整页 markdown 源文本，但前提是这个页面对应的实体 `.md` 文件必须真实存在。
文件本身仍然负责决定页面是否存在，并且继续作为回退来源。

- GuideNH 仍然先按正常的语言回退顺序解析页面文件
- 找到某个实际文件后，再按“请求语言”查找该页面对应的 `.lang` 值
- 如果该 key 存在且非空，就在解析前用它的完整值替换整页 markdown 源文本
- 如果该 key 缺失或为空，则回退到刚刚解析到的标准页面文件内容

key 格式如下：

```text
guidenh.page.<namespace>.<folder>.<去掉 .md 后的页面路径>
```

例如：

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

路径分隔符 `/` 会在 key 中变成 `.`。如果某个路径段自身带有字面句号，为了避免与层级分隔冲突，会进行转义：

```text
foo.bar.md -> foo_x2e_bar
```

其他非字母数字字符也会使用同样的 `_x<hex>_` 规则转义。

`.lang` 值中的字面量 `\n` 与 `\r` 会在 markdown 解析前转换成真正的换行，因此可以直接写完整页面内容，
包括 frontmatter、标题、列表以及 MDX 标签。

编写规则：

- 该 key 对应的整页内容必须仍然写在 `.lang` 文件中的同一条物理行里
- 需要 markdown 换行时，在值里写字面量 `\n`
- 不要直接在 `.lang` 的值里插入真实换行，因为 Forge 读取 `.lang` 时是按物理行分隔的
- 不要写 `\\n`，除非你就是想让最终 markdown 源文本里保留字面量 `\n`

GuideNH 不会仅凭 `.lang` 自动生成一个新页面；实体页面文件仍然必须存在。

## Key 长度

GuideNH 本身没有再额外给这类页面 key 施加字符上限。Minecraft 1.7.10 / Forge 这层的语言数据本质上更接近
字符串属性表，所以实际限制主要来自正常内存占用和可维护性，而不是一个单独的硬编码长度上限。页面路径尽量简洁，
仍然会更容易编写和检查。

## 编写建议

- 如果你希望某个 guide 使用非英文作为回退语言，请显式设置 `defaultLanguage`
- 只有在确实希望跨语言共享时，才添加无语言的共享页面
- 先翻译页面，再在资源中确实嵌入了文本时才翻译资源
- 若共享资源足够通用，就不要额外引入语言特定的资源文件名

## 资源查找顺序

指南资源使用稍微更丰富的回退顺序：

1. `_<current language>/<path>`
2. 若当前语言不是指南默认语言，则尝试 `_<default language>/<path>`
3. `<path>`

这样在需要时，就可以对图片或类似纹理的资源进行本地化。

## 搜索与语言

搜索文档会同时记录原始 Minecraft 语言和 Lucene 实际使用的 analyzer 语言。若当前 Minecraft 语言未映射到已知 analyzer，搜索会回退到英文分词。

## 忽略翻译配置

GuideNH 目前没有提供全局“忽略翻译”开关。如果你希望某个 guide 回退到非英文语言，请在代码里显式设置该 guide 的 `defaultLanguage`。

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
