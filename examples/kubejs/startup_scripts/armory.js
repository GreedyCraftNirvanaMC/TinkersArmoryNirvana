// Optional example pack. Copy to kubejs/startup_scripts and restart the game.
StartupEvents.registry('tconstruct:material', event => {
  event.create('example:amethyst')
    .color(0xA77CE3)
    .craftable(true)
    .head(600, 6, 5, 2)
    .handle(1.05, 50)
    .extra(75)
    .item('minecraft:amethyst_shard', 1)
})

ArmoryEvents.modifyTraits(event => {
  event.create('example:resolve', trait => {
    trait.maxLevel(2).on('modifyStats', ctx => {
      ctx.stats.toughness += 1.5 * ctx.level
    })
  })
})

ArmoryEvents.modifyModifiers(event => {
  event.create('example:amethyst_guard', modifier => {
    modifier.maxLevel(3).pointsPerLevel(8).on('incomingDamage', ctx => {
      ctx.amount *= 0.96
    })
  })
})

ArmoryEvents.modifyMaterials(event => {
  event.create('example:amethyst', material => {
    material.core(16, 15).plates(1.1, 3, 2).trim(7)
      .trait('core', 'example:resolve', 1)
      .trait('plates', 'example:resolve', 1)
      .trait('trim', 'example:resolve', 1)
  })
  event.modify('wood', material => {
    material.core(6, 8)
  })
})
