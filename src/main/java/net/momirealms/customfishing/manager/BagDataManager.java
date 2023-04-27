/*
 *  Copyright (C) <2022> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.manager;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.momirealms.customfishing.CustomFishing;
import net.momirealms.customfishing.listener.InventoryListener;
import net.momirealms.customfishing.listener.JoinQuitListener;
import net.momirealms.customfishing.listener.WindowPacketListener;
import net.momirealms.customfishing.object.InventoryFunction;
import net.momirealms.customfishing.util.AdventureUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BagDataManager extends InventoryFunction {

    private final ConcurrentHashMap<UUID, Inventory> dataMap;
    private final HashMap<UUID, Inventory> tempData;
    private final InventoryListener inventoryListener;
    private final WindowPacketListener windowPacketListener;
    private final JoinQuitListener joinQuitListener;
    private final CustomFishing plugin;

    public BagDataManager(CustomFishing plugin) {
        super();
        this.plugin = plugin;
        this.dataMap = new ConcurrentHashMap<>();
        this.tempData = new HashMap<>();
        this.inventoryListener = new InventoryListener(this);
        this.windowPacketListener = new WindowPacketListener(this);
        this.joinQuitListener = new JoinQuitListener(this);
    }

    public void saveBagDataForOnlinePlayers(boolean unlock) {
        plugin.getDataManager().getDataStorageInterface().saveBagData(dataMap.entrySet(), unlock);
    }

    @Override
    public void load() {
        if (!ConfigManager.enableFishingBag) return;
        Bukkit.getPluginManager().registerEvents(inventoryListener, plugin);
        Bukkit.getPluginManager().registerEvents(joinQuitListener, plugin);
        CustomFishing.getProtocolManager().addPacketListener(windowPacketListener);
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(inventoryListener);
        HandlerList.unregisterAll(joinQuitListener);
        CustomFishing.getProtocolManager().removePacketListener(windowPacketListener);
    }

    @Override
    public void disable() {
        unload();
        saveBagDataForOnlinePlayers(true);
        dataMap.clear();
        tempData.clear();
    }

    public Inventory getPlayerBagData(UUID uuid) {
        return dataMap.get(uuid);
    }

    public void openFishingBag(Player viewer, OfflinePlayer ownerOffline, boolean force) {
        Player owner = ownerOffline.getPlayer();
        //not online
        if (owner == null) {
            Inventory inventory = plugin.getDataManager().getDataStorageInterface().loadBagData(ownerOffline.getUniqueId(), force);
            if (inventory == null) {
                AdventureUtils.playerMessage(viewer, "<red>[CustomFishing] Failed to load bag data for player " + ownerOffline.getName());
                AdventureUtils.playerMessage(viewer, "<red>This might be caused by the target player is online but on another server");
                AdventureUtils.playerMessage(viewer, "<red>Use /fishingbag open [Player] --force to ignore this warning");
                return;
            }
            tempData.put(ownerOffline.getUniqueId(), inventory);
            viewer.openInventory(inventory);
        }
        //online
        else {
            Inventory inventory = dataMap.get(owner.getUniqueId());
            if (inventory == null) {
                AdventureUtils.consoleMessage("<red>[CustomFishing] Bag data is not loaded for player " + owner.getName());
            }
            else {
                openBagGUI(owner, viewer, inventory);
            }
        }
    }

    @Override
    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inventory = dataMap.remove(uuid);
        triedTimes.remove(player.getUniqueId());
        if (inventory != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDataManager().getDataStorageInterface().saveBagData(uuid, inventory, true));
        }
    }

    @Override
    public void onJoin(Player player) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> joinReadData(player, false), 15);
    }

    public void joinReadData(Player player, boolean force) {
        if (player == null || !player.isOnline()) return;
        Inventory inventory = plugin.getDataManager().getDataStorageInterface().loadBagData(player.getUniqueId(), force);
        if (inventory != null) {
            dataMap.put(player.getUniqueId(), inventory);
        } else if (!force) {
            if (checkTriedTimes(player.getUniqueId())) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> joinReadData(player, false), 50);
            } else {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> joinReadData(player, true), 50);
            }
        }
    }

    @Override
    public void onWindowTitlePacketSend(PacketContainer packet, Player receiver) {
        StructureModifier<WrappedChatComponent> wrappedChatComponentStructureModifier = packet.getChatComponents();
        WrappedChatComponent component = wrappedChatComponentStructureModifier.getValues().get(0);
        String windowTitleJson = component.getJson();
        if (windowTitleJson.startsWith("{\"text\":\"{CustomFishing_Bag_")) {
            String player = windowTitleJson.substring(28, windowTitleJson.length() - 3);
            String text = ConfigManager.fishingBagTitle.replace("{player}", player);
            wrappedChatComponentStructureModifier.write(0,
                    WrappedChatComponent.fromJson(
                            GsonComponentSerializer.gson().serialize(
                                    MiniMessage.miniMessage().deserialize(
                                            AdventureUtils.replaceLegacy(text)
                                    )
                            )
                    )
            );
        }
    }

    @Override
    public void onClickInventory(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        Inventory fishingBagInv = dataMap.get(player.getUniqueId());
        if (fishingBagInv == event.getInventory()) {
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null || currentItem.getType() == Material.AIR) return;
            NBTItem nbtItem = new NBTItem(currentItem);
            if (!nbtItem.hasTag("CustomFishing") && !ConfigManager.bagWhiteListItems.contains(currentItem.getType())) {
                event.setCancelled(true);
                return;
            }
            NBTCompound nbtCompound = nbtItem.getCompound("CustomFishing");
            if (nbtCompound == null) {
                event.setCancelled(true);
                return;
            }
            String type = nbtCompound.getString("type");
            if (!ConfigManager.canStoreLoot && type.equals("loot")) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        final Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        Inventory fishingBagInv = dataMap.get(player.getUniqueId());
        if (fishingBagInv != null) {
            if (inventory == fishingBagInv) {
                for (ItemStack itemStack : event.getInventory().getContents()) {
                    if (itemStack == null || itemStack.getType() == Material.AIR) continue;
                    NBTItem nbtItem = new NBTItem(itemStack);
                    if (nbtItem.hasTag("CustomFishing") || ConfigManager.bagWhiteListItems.contains(itemStack.getType())) continue;
                    player.getInventory().addItem(itemStack.clone());
                    itemStack.setAmount(0);
                }
                return;
            }
            for (Map.Entry<UUID, Inventory> entry : tempData.entrySet()) {
                if (entry.getValue() == inventory) {
                    tempData.remove(entry.getKey());
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getDataManager().getDataStorageInterface().saveBagData(entry.getKey(), entry.getValue(), true);
                    });
                }
            }
        }
    }

    public void openBagGUI(Player owner, Player viewer, Inventory inventory) {
        int size = 1;
        for (int i = 6; i > 1; i--) {
            if (owner.hasPermission("fishingbag.rows." + i)) {
                size = i;
                break;
            }
        }
        if (size * 9 != inventory.getSize()) {
            ItemStack[] itemStacks = inventory.getContents();
            Inventory newInv = plugin.getVersionHelper().isSpigot() ? Bukkit.createInventory(null, size * 9, AdventureUtils.replaceMiniMessage(ConfigManager.fishingBagTitle.replace("{player}", owner.getName()))) : Bukkit.createInventory(null, size * 9, "{CustomFishing_Bag_" + owner.getName() + "}");
            newInv.setContents(itemStacks);
            dataMap.put(owner.getUniqueId(), newInv);
            viewer.openInventory(newInv);
        }
        else {
            viewer.openInventory(inventory);
        }
    }
}