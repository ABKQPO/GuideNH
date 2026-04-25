# Recipes

GuideNH can render crafting and NEI-backed recipes directly inside guide pages.

## Supported Tags

- `<Recipe>`
- `<RecipeFor>`
- `<RecipesFor>`

All three share the same compiler and attribute set.

## Tag Semantics

| Tag | Behavior |
| --- | --- |
| `<Recipe>` | render a single recipe for the target item |
| `<RecipeFor>` | same single-recipe behavior, more author-friendly name |
| `<RecipesFor>` | render multiple matching recipes |

If multiple recipes exist and you use the single-recipe forms, GuideNH renders only one result unless filters narrow it further.

## Common Attributes

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | target item reference |
| `fallbackText` | no | text shown when no usable recipe is found |
| `handlerName` | no | case-insensitive substring filter on handler name |
| `handlerId` | no | exact overlay/handler id filter, case-insensitive |
| `handlerOrder` | no | 0-based index after handler filtering |
| `input` | no | ingredient filter expression |
| `output` | no | result filter expression |
| `limit` | no | positive integer max number of rendered recipes |

## Item Id Syntax

The `id`, `input`, and `output` attributes all use the GuideNH extended item reference format:

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

Wildcard meta values:

- `*`
- `32767`
- uppercase tokens such as `ANY`

## Filter Expression Syntax

The `input` and `output` filters support:

- `,` for OR
- `&` for AND
- `!` for NOT

Examples:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

## Rendering Order

GuideNH tries recipes in this order:

1. NEI native handler rendering
2. NEI slot-data fallback
3. built-in vanilla crafting fallback

If nothing matches:

- `fallbackText` is used when present
- otherwise an inline authoring error is shown

## Examples

### Single recipe

````md
<RecipeFor id="minecraft:crafting_table" />
````

### Multiple recipes

````md
<RecipesFor id="minecraft:torch" />
````

### Handler filtering

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### Input/output filtering

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### Fallback text

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## Best Practices

- use `fallbackText` for optional-mod integrations
- use `handlerId` when you know the exact NEI handler you want
- use `limit` when a tag could expand into many recipes
- keep complex filter logic in comments near the tag for maintainability

## Live Runtime Example

See `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` for extensive recipe samples, including handler filters and wildcard/NBT item ids.
