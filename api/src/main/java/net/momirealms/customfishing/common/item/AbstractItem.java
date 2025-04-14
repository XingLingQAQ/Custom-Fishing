/*
 *  Copyright (C) <2024> <XiaoMoMi>
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

package net.momirealms.customfishing.common.item;

import net.momirealms.customfishing.common.plugin.CustomFishingPlugin;
import net.momirealms.customfishing.common.util.Key;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AbstractItem<R, I> implements Item<I> {
    private final CustomFishingPlugin plugin;
    private final ItemFactory<?, R, I> factory;
    private final R item;

    AbstractItem(CustomFishingPlugin plugin, ItemFactory<?, R, I> factory, R item) {
        this.plugin = plugin;
        this.factory = factory;
        this.item = item;
    }

    @Override
    public Item<I> damage(Integer data) {
        factory.damage(item, data);
        return this;
    }

    @Override
    public Optional<Integer> damage() {
        return factory.damage(item);
    }

    @Override
    public Item<I> maxDamage(Integer data) {
        factory.maxDamage(item, data);
        return this;
    }

    @Override
    public Optional<Integer> maxDamage() {
        return factory.maxDamage(item);
    }

    @Override
    public Item<I> customModelData(Integer data) {
        factory.customModelData(item, data);
        return this;
    }

    @Override
    public Optional<Integer> customModelData() {
        return factory.customModelData(item);
    }

    @Override
    public Optional<String> displayName() {
        return factory.displayName(item);
    }

    @Override
    public Item<I> lore(List<String> lore) {
        factory.lore(item, lore);
        return this;
    }

    @Override
    public Optional<List<String>> lore() {
        return factory.lore(item);
    }

    @Override
    public Item<I> unbreakable(boolean unbreakable) {
        factory.unbreakable(item, unbreakable);
        return this;
    }

    @Override
    public boolean unbreakable() {
        return factory.unbreakable(item);
    }

    @Override
    public Item<I> displayName(String displayName) {
        factory.displayName(item, displayName);
        return this;
    }

    @Override
    public Item<I> skull(String data) {
        factory.skull(item, data);
        return this;
    }

    @Override
    public Item<I> enchantments(Map<Key, Short> enchantments) {
        factory.enchantments(item, enchantments);
        return this;
    }

    @Override
    public Item<I> addEnchantment(Key enchantment, int level) {
        factory.addEnchantment(item, enchantment, level);
        return this;
    }

    @Override
    public Item<I> storedEnchantments(Map<Key, Short> enchantments) {
        factory.storedEnchantments(item, enchantments);
        return this;
    }

    @Override
    public Item<I> addStoredEnchantment(Key enchantment, int level) {
        factory.addStoredEnchantment(item, enchantment, level);
        return this;
    }

    @Override
    public Item<I> itemFlags(List<String> flags) {
        factory.itemFlags(item, flags);
        return this;
    }

    @Override
    public Optional<Object> getTag(Object... path) {
        return factory.getTag(item, path);
    }

    @Override
    public Item<I> setTag(Object value, Object... path) {
        factory.setTag(item, value, path);
        return this;
    }

    @Override
    public boolean hasTag(Object... path) {
        return factory.hasTag(item, path);
    }

    @Override
    public boolean removeTag(Object... path) {
        return factory.removeTag(item, path);
    }

    @Override
    public I getItem() {
        return factory.getItem(item);
    }

    @Override
    public I load() {
        return factory.load(item);
    }

    @Override
    public I loadCopy() {
        return factory.loadCopy(item);
    }

    @Override
    public void update() {
        factory.update(item);
    }

    public R getRTagItem() {
        return item;
    }
}
