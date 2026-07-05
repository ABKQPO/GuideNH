# Flowchart Parser — Supported Features

## Graph Structure

- **Frontmatter**: YAML `---` blocks; reads `direction:` and `layout:` config (builtin / elk)
- **Graph declaration**: `graph`, `flowchart`, `flowchart-elk` followed by direction (`TB`, `BT`, `LR`, `RL`, `TD`)
- **Direction override**: `direction TB/BT/LR/RL/TD`

## Node Shapes (14+)

| Shape | Syntax | Example |
|---|---|---|
| Default | bare id | `myNode` |
| Square | `[text]` | `Node[Hello]` |
| Rounded | `(text)` | `Node(Hello)` |
| Stadium pill | `([text])` | `Node([Hello])` |
| Subprocess | `[[text]]` | `Node[[Hello]]` |
| Diamond | `{text}` | `Node{Hello}` |
| Cylinder | `[(text)]` | `Node[(Hello)]` |
| Asymmetric | `>text]` | `Node>Hello]` |
| Trapezoid | `[/text/]` | `Node[/Hello/]` |
| Inv trapezoid | `[\\text\\]` | `Node[\\Hello\\]` |
| Lean right | `[/text\\]` | `Node[/Hello\\]` |
| Lean left | `[\\text/]` | `Node[\\Hello/]` |
| Hexagon | `{{text}}` | `Node{{Hello}}` |
| Circle | `((text))` | `Node((Hello))` |
| Double circle | `(((text)))` | `Node(((Hello)))` |
| Ellipse | `(-text-)` | `Node(-Hello-)` |
| Cloud | `)text(` | `Node)Hello(` |
| Bang | `))text((` | `Node))Hello((` |

## Extended Node Syntax

```
nodeId@{ shape: stadium, label: "My Label", icon: "fa:fa-flask" }
```

Comma-separated key:value pairs inside `{}` after `@`. Values may be quoted strings.

## Edge Types

| Style | Patterns |
|---|---|
| **Solid** | `-->`, `---`, `<---`, `<-->`, `--o`, `o--`, `o--o`, `--x`, `x--`, `x--x` |
| **Thick** | `==>`, `===`, `<===`, `<=>`, `==o`, `o==`, `o==o`, `==x`, `x==`, `x==x` |
| **Dashed** | `-.->`, `-.-`, `<-.--`, `<-.->`, `-.o`, `o-.` |
| **Dotted** | `~~>`, `~~~`, `<~~~`, `<~~>` |
| **Invisible** | `~~~` (length 3) |

### Arrow Heads

- **Triangle**: `>` forward, `<` reverse
- **Circle**: `o` (e.g. `--o`)
- **Cross**: `x` (e.g. `--x`)
- Bidirectional and reverse variants for all

### Variable-Length Edges

```
----->    longer solid arrow
=====>    longer thick arrow
~~~~~>    longer dotted arrow
-.->      dashed (dots are fixed length)
```

## Edge Labels

```
A -->|"label text"| B
A -->|label| B
```

Pipe-delimited text between nodes, supports quoted and unquoted strings.

## Node Chaining

```
A --> B --> C --> D
```

Multiple edges on a single line, left-to-right.

## Subgraphs

```
subgraph Title["Optional Title"]
  direction LR
  A --> B
  subgraph Inner["Nested"]
    C --> D
  end
  B --> C
end
```

- Nested subgraphs
- Optional direction override per subgraph (`direction TB/BT/LR/RL/TD`)
- Direction override is passed through to the layout engine
- Labels with optional quoted strings

## Styling & Classes

### Inline Classes

```
nodeId:::className1 className2
```

### Class Statements

```
class nodeId className
class nodeId1,nodeId2 className
```

### ClassDef Statements

```
classDef className fill:#fff,stroke:#333,stroke-width:2px
```

### Style Statements

```
style nodeId fill:#fff,stroke:#333
```

### LinkStyle Statements

```
linkStyle 0,1 stroke:red
linkStyle default stroke:blue
linkStyle 0 interpolate basis
```

## Click & Href Events

```
click nodeId callbackName
click nodeId callbackName(args)
click nodeId href "https://example.com"
click nodeId href "https://example.com" _blank
click nodeId href "url" "tooltip text" _blank
```

- `_blank`, `_self`, `_parent`, `_top` as link targets

## Multi-Node Parallel (`&`)

```
A & B & C --> D
```

Multiple source nodes on the same edge.

## Comments

```
%% This is a comment
```

Lines starting with `%%` are ignored.

## Separators

- Newline
- Semicolon (`;`)

## Embedded Content (`<NodeContent>`)

```
<NodeContent id="nodeId">
  <RecipeFor id="minecraft:crafting_table" />
</NodeContent>
```

- `<NodeContent>` blocks are stripped from diagram source during parsing
- Content is compiled separately and mapped to the matching node by ID
- At runtime, the content is rendered inside the node's tooltip/popup

## Label Normalization

- `<br/>`, `<br />`, `<br>` → newline
- `&lt;` → `<`, `&gt;` → `>`, `&amp;` → `&`
- Markdown links stripped to plain text
- Wrapping quotes removed

## Not Yet Supported (Future)

- `swimlane-beta` diagram type
- `call` callback directly (only through `click`)
