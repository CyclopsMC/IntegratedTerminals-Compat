package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.util.Pair;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Caches recipe errors for JEI recipe transfer
 * @author Snonky
 */
public class RecipeErrorCache {
    // The tick when the cache has been populated last
    private long populationTime = -1;
    // The tick when the cache last had a hit
    private long lastHitTime = -1;
    // The amount of hits during the current tick
    private int currentHitIndex = 0;
    private List<Pair<CraftingRecipe, IRecipeTransferError>> recipeErrors;

    public RecipeErrorCache() {
        // Init capacity of 4 because the JEI screen shows max. 4 crafting recipes
        this.recipeErrors = new ArrayList<>(4);
    }

    @Nullable
    public IRecipeTransferError getRecipeError(CraftingRecipe recipe, Supplier<IRecipeTransferError> recipeErrorSupplier) {
        IRecipeTransferError recipeError;
        long currentTime = Minecraft.getInstance().level.getGameTime();
        if(currentTime == lastHitTime) {
            // Cache hit tick
            Pair<CraftingRecipe, IRecipeTransferError> hit = Iterables.get(recipeErrors, currentHitIndex, null);
            // In theory the hit should always be correct because the list of recipes doesn't change inside a tick
            // Still do a safety check
            if (hit == null || hit.getFirst() != recipe) {
                recipeError = recipeErrorSupplier.get();
            } else {
                recipeError = hit.getSecond();
            }
            currentHitIndex++;
        } else if(currentTime == populationTime) {
            // Population tick
            recipeError = recipeErrorSupplier.get();
            recipeErrors.add(new Pair<>(recipe, recipeError));
        } else {
            // New tick
            currentHitIndex = 0;
            Pair<CraftingRecipe, IRecipeTransferError> possibleHit = Iterables.get(recipeErrors, 0, null);
            if(possibleHit == null || possibleHit.getFirst() != recipe) {
                // Cache missed (because new JEI recipes coming in), begin repopulation tick
                populationTime = currentTime;
                recipeErrors.clear();
                recipeError = recipeErrorSupplier.get();
                recipeErrors.add(new Pair<>(recipe, recipeError));
            } else {
                // Begin cache hit tick (same JEI recipes as last tick)
                lastHitTime = currentTime;
                recipeError = possibleHit.getSecond();
                currentHitIndex++;
            }
        }
        return recipeError;
    }

    public void clear() {
        // Clear the cache
        lastHitTime = -1;
        populationTime = -1;
        recipeErrors.clear();
    }
}
