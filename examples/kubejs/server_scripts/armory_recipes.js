// Recipe edits reload with /reload. Definition edits in startup_scripts need a full restart.
ServerEvents.recipes(event => {
  event.custom({
    type: 'conarm:armor_modifier',
    modifier: 'example:amethyst_guard',
    points: 2,
    ingredients: [{ ingredient: { item: 'minecraft:amethyst_shard' }, count: 1 }]
  }).id('example:armory/amethyst_guard')

  // Example removal, disabled by default:
  // event.remove({ id: 'conarm:modifiers/diamond' })
})
