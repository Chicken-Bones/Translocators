Translocators
==============

THIS PROJECT IS RETIRED: See [here] for the current repo.

Overview Translocators is a mod that allows you to transfer items and liquids between nearby inventories with a heavily configureable amount of control in a way that feels natural to Minecraft. It was developed in 36 hours during the ModJam modding contest and managed to take first prize.

Translocators the crafting grid, a placeable one item crafting grid.
The translocator comes in item and liquid forms. You place them on the side of an inventory or tank. They transfer items and liquids within the one block space.

- If the center piece is protruding, items/liquids will flow from this side, to the sides with the center inset.
- Items/Liquids will always travel to non-redstone outputs if they can.
- Items/Liquids will be evenly split between available outputs. Items will be taken from the slot with the largest quantity.
- Right clicking on the plate of the item translocator brings up a configurable filter. These slots are fake and can be configured with NEI’s drag ‘n’ drop.
- Adding glowstone to the plate makes it transfer stacks at a time.
- Adding redstone lets you toggle input/output with a redstone signal.
- Adding a diamond nugget (diamond in a crafting table gives 9) puts a translocator in regulate mode. In this mode it will maintain a certain amount of items set in the filter in the inventory it’s attached to.
	- On an inserting face, it will only accept items to fill the inventory to the matching filter
	- On an ejecting face, it will eject any items that don’t match the filter.
- Adding an iron ingot makes the translocator emit redstone signals to the attached inventory on certain conditions.
	- On an inserting face, it will emit if there is no room for any item that matches the filter.
	- On an ejecting face, it will emit if there is no place where any item can be put that matches the filter.
- Shift-Right clicking on the plate will strip and drop all upgrades placed on the translocator.

Crafting Grid The crafting grid is placed by pressing C (default key bind, changeable in settings) while looking at a solid top surface.

- The grid will remain for 20 seconds, or 2 minutes since it’s last use.
- Right clicking with items will add/remove them from the grid.
- Pressing C again on the grid, will craft the items

Current recommended and latest builds can be found at http://chickenbones.net/Pages/links.html

Current maven: http://chickenbones.net/maven/

Join us on IRC:
- Server: Esper.net
- Channel: #ChickenBones

[here]: <https://github.com/TheCBProject/Translocators>
