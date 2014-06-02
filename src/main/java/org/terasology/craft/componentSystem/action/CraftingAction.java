/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.craft.componentSystem.action;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.craft.components.actions.CraftingActionComponent;
import org.terasology.craft.components.utility.CraftRecipeComponent;
import org.terasology.craft.events.crafting.AddItemEvent;
import org.terasology.craft.events.crafting.ChangeLevelEvent;
import org.terasology.craft.events.crafting.CheckRefinementEvent;
import org.terasology.craft.events.crafting.DeleteItemEvent;
import org.terasology.craft.rendering.CraftingGrid;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.common.DisplayNameComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.AABB;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.world.block.BlockManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Small-Jeeper
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CraftingAction extends BaseComponentSystem {
    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;
    @In
    private PrefabManager prefManager;
    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;

    private static final int MAX_STACK = 99;
    private Map<String, ArrayList<Prefab>> entitesWithRecipes = Maps.newHashMap();
    private Map<String, ArrayList<RefinementData>> entitesWithRefinement = Maps.newHashMap();
    private static final String EMPTY_ROW = " ";
    private static final Logger logger = LoggerFactory.getLogger(CraftingAction.class);


    @Override
    public void initialise() {
        //prefMan.getPrefab()
        for (Prefab prefab : prefManager.listPrefabs(CraftRecipeComponent.class)) {
            CraftRecipeComponent recipe = prefab.getComponent(CraftRecipeComponent.class);

            if (recipe.refinement.size() > 0) {
                for (Map<String, String> refinement : recipe.refinement.values()) {
                    if (refinement.containsKey("instigator") && refinement.containsKey("target")) {

                        RefinementData refinementData = new RefinementData();
                        refinementData.instigator = refinement.get("instigator").toLowerCase();
                        refinementData.target = refinement.get("target").toLowerCase();

                        if (refinement.containsKey("resultCount")) {
                            try {
                                refinementData.resultCount = Byte.parseByte(refinement.get("resultCount"));
                            } catch (NumberFormatException exception) {
                                logger.warn("Refinement: {}. The resultCount must be a byte!", prefab.getName());
                            }
                        }

                        refinementData.resultPrefab = entityManager.create(prefab);

                        if (!entitesWithRefinement.containsKey(refinementData.target)) {
                            entitesWithRefinement.put(refinementData.target, new ArrayList<RefinementData>());
                        }
                        logger.info("Found refinement: {}", prefab.getName());
                        entitesWithRefinement.get(refinementData.target).add(refinementData);
                    }
                }
            }

            if (recipe.recipe.size() > 0) {
                String key = getRecipeKey(recipe.recipe);
                if (!entitesWithRecipes.containsKey(key)) {
                    entitesWithRecipes.put(key, new ArrayList<Prefab>());
                }

                logger.info("Found recipe: {}", prefab.getName());
                entitesWithRecipes.get(key).add(prefab);
            }

        }
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = {CraftingActionComponent.class})
    public void onActivate(ActivateEvent event, EntityRef entity) {
        CraftingActionComponent craftingComponent = entity.getComponent(CraftingActionComponent.class);

        if (!craftingComponent.possibleItem.equals(EntityRef.NULL)) {
            EntityRef player = localPlayer.getCharacterEntity();
            inventoryManager.giveItem(player,player,craftingComponent.possibleItem);
            //decreaseItems(entity, localPlayer.getCharacterEntity());

            checkEmptyCraftBlock(entity);

            if (entity.exists()) {
                //EntityRef possibleCraft = tryCraft(entity);

                //craftingComponent.possibleItem = possibleCraft;
                //entity.saveComponent(craftingComponent);
            }

        } else {
            entity.send(new AddItemEvent(event.getTarget(), event.getInstigator()));
        }
    }

   /* @ReceiveEvent(components = {CraftingActionComponent.class})
    public void onAddItem(AddItemEvent event, EntityRef entity) {
        CraftingActionComponent craftingComponent = entity.getComponent(CraftingActionComponent.class);

        int selectedCell = getSelectedItemFromCraftBlock(entity, craftingComponent.getCurrentLevel());
        EntityRef entityFromPlayer = event.getInstigator();
        EntityRef selectedEntity = craftingComponent.getCurrentLevelElements().get(selectedCell);

        float percent = event.getPercent();
        byte sendingCount = 1;
        byte returnedCount = 0;

        ItemComponent playerItem = entityFromPlayer.getComponent(ItemComponent.class);
        DisplayNameComponent playerItemInfo = entityFromPlayer.getComponent(DisplayNameComponent.class);

        if (playerItem == null) {
            return;
        }


        if (percent > 0 && playerItem.stackCount > 1) {
            sendingCount = (byte) Math.round(percent * playerItem.stackCount);

            playerItem.stackCount -= sendingCount;

            if (playerItem.stackCount < 0) {
                playerItem.stackCount = 0;
                sendingCount--;
            }
        } else {
            playerItem.stackCount--;
        }

        DisplayNameComponent craftItemInfo = selectedEntity.getComponent(DisplayNameComponent.class);
        ItemComponent craftItem = selectedEntity.getComponent(ItemComponent.class);

        if (craftItemInfo != null && craftItemInfo.name.toLowerCase().equals(playerItemInfo.name.toLowerCase())) {

            if (craftItem.stackCount >= MAX_STACK) {
                return;
            }

            if ((craftItem.stackCount + sendingCount) > MAX_STACK) {
                returnedCount = (byte) ((craftItem.stackCount + sendingCount) - MAX_STACK);
                craftItem.stackCount = MAX_STACK;

            } else {
                craftItem.stackCount += sendingCount;
            }

            selectedEntity.saveComponent(craftItem);

        } else if (selectedEntity.equals(EntityRef.NULL)) {

            if (entityFromPlayer.getComponent(ItemComponent.class) == null) {
                return;
            }

            EntityRef entityToCraftBlock = entityManager.copy(entityFromPlayer);
            craftItem = entityToCraftBlock.getComponent(ItemComponent.class);

            craftItem.stackCount = sendingCount;
            entityToCraftBlock.saveComponent(craftItem);
            craftingComponent.getCurrentLevelElements().set(selectedCell, entityToCraftBlock);

        } else {
            EntityRef entityToCraftBlock = entityManager.copy(entityFromPlayer);
            craftItem = entityToCraftBlock.getComponent(ItemComponent.class);

            craftItem.stackCount = sendingCount;
            entityToCraftBlock.saveComponent(craftItem);

            EntityRef player = localPlayer.getCharacterEntity();
            //ItemComponent tItem = selectedEntity.getComponent(ItemComponent.class);
            //tItem.container = EntityRef.NULL;
            //selectedEntity.saveComponent(tItem);
            //player.send(new ReceiveItemEvent(entityManager.copy(selectedEntity)));
            inventoryManager.giveItem(player,player,selectedEntity);
            craftingComponent.getCurrentLevelElements().set(selectedCell, entityToCraftBlock);
        }

        playerItem.stackCount += returnedCount;

        if (playerItem.stackCount == 0) {
            entityFromPlayer.destroy();
        } else {
            entityFromPlayer.saveComponent(playerItem);
        }


        entity.saveComponent(craftingComponent);

        craftingComponent.possibleItem = tryCraft(entity);
        entity.saveComponent(craftingComponent);

    }    */

    /*
     * If Changed "Y position" of craft grid
     */
    @ReceiveEvent(components = {CraftingActionComponent.class})
    public void onChangeLevel(ChangeLevelEvent event, EntityRef entity) {
        if (event.isDecreaseEvent()) {
            entity.getComponent(CraftingActionComponent.class).decreaseLevel();
        } else {
            entity.getComponent(CraftingActionComponent.class).increaseLevel();
        }
    }

   /* @ReceiveEvent(components = {CraftingActionComponent.class})
    public void onDeleteItem(DeleteItemEvent event, EntityRef entity) {
        CraftingActionComponent craftingComponent = entity.getComponent(CraftingActionComponent.class);

        int selectedCell = getSelectedItemFromCraftBlock(entity, craftingComponent.getCurrentLevel());
        EntityRef selectedEntity = craftingComponent.getCurrentLevelElements().get(selectedCell);

        if (selectedEntity.equals(EntityRef.NULL)) {
            return;
        }

        float percent = event.getPercent();
        byte sendingCount = 1;
        ItemComponent craftItem = selectedEntity.getComponent(ItemComponent.class);

        if (percent > 0 && craftItem.stackCount > 1) {
            sendingCount = (byte) Math.round(percent * craftItem.stackCount);
            craftItem.stackCount -= sendingCount;

            if (craftItem.stackCount < 0) {
                craftItem.stackCount = 0;
                sendingCount--;
            }
        } else {
            craftItem.stackCount--;
        }

        //Send item to player
        EntityRef entityForPlayer = entityManager.copy(selectedEntity);
        ItemComponent entityForPlayerItem = entityForPlayer.getComponent(ItemComponent.class);
        //entityForPlayerItem.container = EntityRef.NULL;
        entityForPlayerItem.stackCount = sendingCount;
        entityForPlayer.saveComponent(entityForPlayerItem);

        EntityRef player = localPlayer.getCharacterEntity();
        inventoryManager.giveItem(player,player,selectedEntity);

        if (craftItem.stackCount == 0) {
            craftingComponent.deleteItem(getSelectedItemFromCraftBlock(entity, craftingComponent.getCurrentLevel()));
            entity.saveComponent(craftingComponent);
        } else {
            selectedEntity.saveComponent(craftItem);
        }

        checkEmptyCraftBlock(entity);

        if (entity.exists()) {

            EntityRef possibleCraft = tryCraft(entity);

            if (!possibleCraft.equals(EntityRef.NULL)) {
                craftingComponent.possibleItem = possibleCraft;
                entity.saveComponent(craftingComponent);
            } else if (!craftingComponent.possibleItem.equals(EntityRef.NULL)) {
                craftingComponent.possibleItem = EntityRef.NULL;
                entity.saveComponent(craftingComponent);
            }
        }
    }*/

   /* @ReceiveEvent(components = {CraftingActionComponent.class})
    public void checkRefinement(CheckRefinementEvent event, EntityRef entity) {
        CraftingActionComponent craftingComponent = entity.getComponent(CraftingActionComponent.class);

        if (event.getInstigator().equals(EntityRef.NULL) || entity.equals(EntityRef.NULL)) {
            disablePossibleItem(craftingComponent);
            return;
        }

        CharacterComponent characterComponent = event.getInstigator().getComponent(CharacterComponent.class);

        if (characterComponent == null) {
            disablePossibleItem(craftingComponent);
            return;
        }

        if (craftingComponent.getAllElements().size() > 1) {
            return;
        } else {
            int countNotNulledElements = 0;
            for (EntityRef element : craftingComponent.getCurrentLevelElements()) {
                if (!element.equals(EntityRef.NULL)) {
                    countNotNulledElements++;
                }

                if (countNotNulledElements > 1) {
                    disablePossibleItem(craftingComponent);
                    return;
                }
            }
        }

        //UIItemContainer toolbar = (UIItemContainer) CoreRegistry.get(GUIManager.class).getWindowById("hud").getElementById("toolbar");
        EntityRef heldItem = InventoryUtils.getItemAt(entity, characterComponent.selectedItem);
        ItemComponent instigatorItem = heldItem.getComponent(ItemComponent.class);
        DisplayNameComponent instigatorItemInfo = heldItem.getComponent(DisplayNameComponent.class);

        int selectedCell = getSelectedItemFromCraftBlock(entity, craftingComponent.getCurrentLevel());

        EntityRef selectedEntity = craftingComponent.getCurrentLevelElements().get(selectedCell);

        ItemComponent targetItem = selectedEntity.getComponent(ItemComponent.class);
        DisplayNameComponent targetItemInfo = selectedEntity.getComponent(DisplayNameComponent.class);

        if (instigatorItem == null || targetItem == null) {
            disablePossibleItem(craftingComponent);
            return;
        }

        String instigatorName = instigatorItemInfo.name.toLowerCase();
        String targetName = targetItemInfo.name.toLowerCase();

        if (entitesWithRefinement.containsKey(targetName) && !entitesWithRefinement.get(targetName).isEmpty()) {

            for (RefinementData refinementData : entitesWithRefinement.get(targetName)) {
                if (refinementData.instigator.equals(instigatorName)) {

                    EntityRef refinementElement = refinementData.resultPrefab.copy();

                    CraftRecipeComponent craftRecipe = refinementElement.getComponent(CraftRecipeComponent.class);
                    craftRecipe.resultCount = refinementData.resultCount;
                    refinementElement.saveComponent(craftRecipe);
                    EntityRef refinementElement = createNewElement(refinementData.resultPrefab);

                    if (!refinementElement.equals(EntityRef.NULL)) {
                        craftingComponent.possibleItem = refinementElement;
                        craftingComponent.isRefinement = true;
                        entity.saveComponent(craftingComponent);
                    }

                    break;
                }
            }
        } else {

            if (craftingComponent.isRefinement) {

                if (!craftingComponent.possibleItem.equals(EntityRef.NULL)) {
                    craftingComponent.possibleItem = EntityRef.NULL;
                }

                craftingComponent.isRefinement = false;
                entity.saveComponent(craftingComponent);
            }

        }
    }
         */
    private void disablePossibleItem(CraftingActionComponent craftingComponent) {
        if (craftingComponent.isRefinement) {
            craftingComponent.isRefinement = false;

            if (!craftingComponent.possibleItem.equals(EntityRef.NULL)) {
                craftingComponent.possibleItem = EntityRef.NULL;
            }

        }
    }

    /*
     * Check current craft block for the recipe
     */
    /*private EntityRef tryCraft(EntityRef entity) {
        CraftingActionComponent craftingComponent = entity.getComponent(CraftingActionComponent.class);
        Map<String, List<String>> possibleRecipe = Maps.newHashMap();

        //Converting entites from craft block to the string recipe

        for (String level : CraftingActionComponent.levels) {

            int countNotEmptyElements = 0;
            ArrayList<EntityRef> craftLevel = craftingComponent.getLevelElements(level);
            ArrayList<String> translatedLevel = new ArrayList<String>();

            if (craftLevel != null) {
                for (EntityRef craftElement : craftLevel) {
                    DisplayNameComponent item = craftElement.getComponent(DisplayNameComponent.class);
                    if (item != null) {
                        translatedLevel.add(item.name.toLowerCase());
                        countNotEmptyElements++;
                    } else {
                        translatedLevel.add(EMPTY_ROW);
                    }

                }
                possibleRecipe.put(level, translatedLevel);
            }
        }

        String searchingKey = getRecipeKey(possibleRecipe);

        if (entitesWithRecipes.containsKey(searchingKey)) {
            boolean isRecipe = false;
            for (Prefab prefabWithRecipe : entitesWithRecipes.get(searchingKey)) {

                CraftRecipeComponent craftRecipe = prefabWithRecipe.getComponent(CraftRecipeComponent.class);

                RecipeMatrix possibleRecipeMatrix = new RecipeMatrix(possibleRecipe, 3, 3);
                RecipeMatrix craftMatrix = new RecipeMatrix(craftRecipe.recipe, 3, 3);

                isRecipe = !craftRecipe.fullMatch ?
                        possibleRecipeMatrix.trim().equals(craftMatrix.trim()) :
                        possibleRecipeMatrix.equals(craftMatrix);

                //Recipe founded. Return result Entity!
                if (isRecipe) {
                    return createNewElement(prefabWithRecipe);
                }
            }
        }

        return EntityRef.NULL;
    }   */

    /*private EntityRef createNewElement(Prefab prefab) {

        Prefab resultPrefab = prefab;
        CraftRecipeComponent craftRecipe = prefab.getComponent(CraftRecipeComponent.class);

        String name = "";

        if (craftRecipe.type != CraftRecipeComponent.CraftRecipeType.SELF && craftRecipe.result.isEmpty()) {
            logger.warn("The recipe does not have result name");
            return EntityRef.NULL;
        }

        if (craftRecipe.type != CraftRecipeComponent.CraftRecipeType.SELF) {
            resultPrefab = prefabManager.getPrefab(craftRecipe.result);
            name = craftRecipe.result;
        } else {
            resultPrefab = prefab;
            name = resultPrefab.getName();
        }

        EntityRef recipe = EntityRef.NULL;
        EntityRef result = EntityRef.NULL;

        if (resultPrefab != null) {
            recipe = entityManager.create(resultPrefab);
        }


        if (recipe.equals(EntityRef.NULL) || recipe.getComponent(ItemComponent.class) == null) {
            Block recipeBlock = null;
            BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
            if (craftRecipe.type != CraftRecipeComponent.CraftRecipeType.SELF) {
                result = blockFactory.newInstance(BlockManager.getInstance().getBlockFamily(craftRecipe.result));
            } else {
                recipeBlock = BlockManager.getInstance().getBlock(resultPrefab);

                if (recipeBlock != null) {
                    result = blockFactory.newInstance(recipeBlock.getBlockFamily(), recipe);
                }
            }


        } else {
            result = recipe;
            ItemComponent oldItem = result.getComponent(ItemComponent.class);

            ItemComponent newItem = new ItemComponent();

            newItem.stackCount = oldItem.stackCount;
            //newItem.container = oldItem.container;
            //newItem.name = oldItem.name;
            newItem.baseDamage = oldItem.baseDamage;
            newItem.consumedOnUse = oldItem.consumedOnUse;
            newItem.icon = oldItem.icon;
            newItem.stackId = oldItem.stackId;
            newItem.renderWithIcon = oldItem.renderWithIcon;
            newItem.usage = oldItem.usage;

            result.saveComponent(newItem);
        }


        ItemComponent item = result.getComponent(ItemComponent.class);

        if (item != null) {
            CraftRecipeComponent recipeComponent = prefab.getComponent(CraftRecipeComponent.class);
            item.stackCount = recipeComponent.resultCount;
            result.saveComponent(item);
        } else {
            logger.warn("Failed to create entity with name {}", name);
            result = EntityRef.NULL;
        }


        return result;
    }   */

    /*
     * Decrease stackCount of the item
     */

   /* private void decreaseItems(EntityRef craftBlockEntity, EntityRef playerEntity) {
        CraftingActionComponent craftingComponent = craftBlockEntity.getComponent(CraftingActionComponent.class);

        if (craftingComponent == null) {
            return;
        }

        int countLevels = craftingComponent.getAllElements().size();

        for (int i = 0; i < countLevels; i++) {
            for (int j = 0; j < CraftingActionComponent.MAX_SLOTS; j++) {
                ArrayList<EntityRef> list = craftingComponent.getLevelElements(CraftingActionComponent.levels[i]);

                if (list == null) {
                    continue;
                }

                EntityRef itemEntity = list.get(j);

                if (itemEntity != null) {
                    ItemComponent item = itemEntity.getComponent(ItemComponent.class);
                    if (item == null) {
                        continue;
                    }

                    item.stackCount--;

                    if (item.stackCount <= 0) {
                        craftingComponent.deleteItem(i, j);
                    } else {
                        itemEntity.saveComponent(item);
                    }
                }

            }
        }

        if (craftingComponent.isRefinement) {

            LocalPlayerComponent localPlayer = playerEntity.getComponent(LocalPlayerComponent.class);
            InventoryComponent inventory = playerEntity.getComponent(InventoryComponent.class);

            ItemComponent instigatorItem = inventory.itemSlots.get(localPlayer.selectedTool).getComponent(ItemComponent.class);

            instigatorItem.stackCount--;

            if (instigatorItem.stackCount < 1) {
                inventory.itemSlots.get(localPlayer.selectedTool).destroy();
            }

        }


    }     */

    /*
     * Check craft block for the emptiness
     */
    private void checkEmptyCraftBlock(EntityRef craftBlockEntity) {
        CraftingActionComponent craftingComponent = craftBlockEntity.getComponent(CraftingActionComponent.class);

        if (craftingComponent.getAllElements().size() == 0) {

            BlockComponent blockComp = craftBlockEntity.getComponent(BlockComponent.class);
            Block currentBlock = worldProvider.getBlock(blockComp.getPosition());
            worldProvider.setBlock(blockComp.getPosition(), BlockManager.getAir());

            craftBlockEntity.destroy();
            return;
        }
    }

    private int getSelectedItemFromCraftBlock(EntityRef entity, int level) {
        AABB aabb = null;
        int blockSelected = 0;
        BlockComponent blockComp = entity.getComponent(BlockComponent.class);

        if (blockComp != null) {
            Block newBlock = worldProvider.getBlock(blockComp.getPosition());
            if (newBlock.isTargetable()) {
                aabb = newBlock.getBounds(blockComp.getPosition());
                CraftingGrid craftingGridRenderer = new CraftingGrid();
                craftingGridRenderer.setAABB(aabb, level);
                blockSelected = craftingGridRenderer.getSelectedBlock();
            }
        }

        return blockSelected;
    }

    private String getRecipeKey(Map<String, List<String>> map) {
        String key = "" + map.size();

        for (List<String> currentLevel : map.values()) {
            int countNotEmptyElements = 0;
            for (String element : currentLevel) {
                if (!element.equals(EMPTY_ROW)) {
                    countNotEmptyElements++;
                }
            }
            key += "-" + countNotEmptyElements;
        }

        return key;
    }

    private static class RefinementData {
        public byte resultCount = 1;
        public String instigator = "";
        public String target = "";
        public EntityRef resultPrefab = null;
    }
}
