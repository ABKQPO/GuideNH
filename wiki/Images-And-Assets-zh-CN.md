[English](Images-And-Assets)

# 图片与资源

GuideNH 同时支持普通 Markdown 图片，以及若干运行时专用的视觉元素。

## 资源解析规则

指南资源使用与页面链接相同的解析规则。

| 路径形式 | 示例 | 含义 |
| --- | --- | --- |
| 相对路径 | `test1.png` | 相对于当前页面文件 |
| 根路径 | `/assets/example_structure.snbt` | 相对于当前指南根目录 |
| 显式资源 id | `guidenh:textures/gui/example.png` | 绝对 `modid:path` 查找 |

## Markdown 图片

支持普通 Markdown 图片：

````md
![Example](test1.png)
````

GuideNH 会解析路径，并从指南内容根目录加载对应的二进制资源。

## `FloatingImage`

`<FloatingImage>` 是 GuideNH 用于左浮动 / 右浮动图片布局的专用标签。

### 属性

| 属性 | 必需 | 含义 |
| --- | --- | --- |
| `src` | 是 | 图片路径 |
| `align` | 否 | `left` 或 `right`，默认 `left` |
| `title` | 否 | tooltip/title 文本 |
| `width` | 否 | 显式宽度（像素） |
| `height` | 否 | 显式高度（像素） |

### 说明

- 只给一个尺寸时会保持纵横比
- 两个尺寸都给时会拉伸图片
- 非法的 `align` 值会渲染为内联错误

### 示例

````md
<FloatingImage src="test1.png" align="left" width="64" title="Example" />
````

## 导航纹理图标

frontmatter 可以使用 `icon_texture`，在导航/搜索中显示纹理而不是物品：

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

该文件必须能被解码为图片。路径的解析规则与其他指南资源路径完全一致。

## 非图片资源

GuideNH 页面也可以引用非图片类运行时资源，最常见的是结构文件，例如：

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

这些资源会通过同一套指南资源管线加载，但不是直接作为图片渲染，而是交由自定义标签消费。

## 最佳实践

- 页面私有图片尽量放在使用它们的页面旁边
- 可复用文件尽量放在指南根的 `assets/` 目录中
- 多页面共享文件优先使用根路径 `/assets/...`
- 只有在资源确实是图片时才使用纹理图标

## 运行时示例文件

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## 相关页面

- [指南页面格式](Guide-Page-Format-zh-CN)
- [标签参考](Tags-Reference-zh-CN)
- [GameScene](GameScene-zh-CN)
