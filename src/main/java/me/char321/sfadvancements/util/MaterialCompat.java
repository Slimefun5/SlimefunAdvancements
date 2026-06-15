package me.char321.sfadvancements.util;

import javax.annotation.Nonnull;

import org.bukkit.Material;

import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;

/**
 * Resolves {@link XMaterial} constants to a {@link Material} that exists on the
 * running server. Keeps the addon loadable on legacy versions (e.g. 1.8) where
 * modern constants like {@code FILLED_MAP} or {@code PLAYER_HEAD} are absent.
 *
 * @author char321
 */
public final class MaterialCompat {

    private MaterialCompat() {}

    @Nonnull
    public static Material safe(@Nonnull XMaterial material) {
        Material resolved = material.parseMaterial();
        return resolved != null ? resolved : Material.STONE;
    }

    /** Resolves an XMaterial to an ItemStack, preserving the legacy data value safe(XMaterial) drops on 1.8-1.12. */
    @javax.annotation.Nonnull
    public static org.bukkit.inventory.ItemStack stack(@javax.annotation.Nonnull XMaterial material) {
        org.bukkit.inventory.ItemStack item = material.parseItem();
        return item != null ? item : new org.bukkit.inventory.ItemStack(safe(material));
    }
}
