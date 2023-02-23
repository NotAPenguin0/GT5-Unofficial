package gregtech.api.ModernMaterials;

import static gregtech.api.enums.ConfigCategories.ModernMaterials.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTech_API;
import gregtech.api.ModernMaterials.PartProperties.Rendering.ModernMaterialRenderer;
import gregtech.api.ModernMaterials.PartRecipeGenerators.ModernMaterialsPlateRecipeGenerator;
import gregtech.api.ModernMaterials.PartsClasses.MaterialPart;
import gregtech.api.ModernMaterials.PartsClasses.PartsEnum;

public class ModernMaterialUtilities {

    private static final List<ModernMaterial> mNewMaterials = new ArrayList<>();
    public static final HashMap<Integer, ModernMaterial> materialIdToMaterial = new HashMap<>();
    public static final HashMap<String, ModernMaterial> mNameMaterialMap = new HashMap<>();
    public static final HashMap<PartsEnum, MaterialPart> materialPartItemMap = new HashMap<>();

    public static void registerMaterial(ModernMaterial aMaterial) {
        final int tCurrentMaterialID = GregTech_API.sModernMaterialIDs.mConfig
                .get(materialID.name(), aMaterial.getName(), -1).getInt();
        if (tCurrentMaterialID == -1) {
            mNewMaterials.add(aMaterial);
        } else {
            aMaterial.setID(tCurrentMaterialID);
            materialIdToMaterial.put(tCurrentMaterialID, aMaterial);
            if (tCurrentMaterialID > GregTech_API.mLastMaterialID) {
                GregTech_API.mLastMaterialID = tCurrentMaterialID;
            }
        }
        mNameMaterialMap.put(aMaterial.getName(), aMaterial);
    }

    public static void registerAllMaterials() {
        for (ModernMaterial tMaterial : mNewMaterials) {
            tMaterial.setID(++GregTech_API.mLastMaterialID);
            GregTech_API.sModernMaterialIDs.mConfig.get(materialID.name(), tMaterial.getName(), 0)
                    .set(GregTech_API.mLastMaterialID);
            materialIdToMaterial.put(GregTech_API.mLastMaterialID, tMaterial);
        }

        for (PartsEnum tPart : PartsEnum.values()) {
            MaterialPart materialPart = new MaterialPart(tPart);
            materialPart.setUnlocalizedName(tPart.partName);

            // Registers the item with the game, only available in preInit.
            GameRegistry.registerItem(materialPart, tPart.partName);

            // Store the Item so these parts can be retrieved later.
            materialPartItemMap.put(tPart, materialPart);

            // Registers the renderer which allows for part colouring.
            MinecraftForgeClient.registerItemRenderer(materialPart, new ModernMaterialRenderer());
        }

        // Register all material parts.
        for (ModernMaterial material : materialIdToMaterial.values()) {
            registerAllMaterialPartRecipes(material);
        }

    }

    private static void registerAllMaterialPartRecipes(ModernMaterial material) {
        new ModernMaterialsPlateRecipeGenerator().run(material);
    }

    public static ItemStack getPart(final ModernMaterial material, final PartsEnum part, final int stackSize) {

        if (!material.doesPartExist(part)) {
            throw new RuntimeException(
                    "Registered material " + material.getName() + " does not have a " + part.toString() + " part.");
        }

        return new ItemStack(materialPartItemMap.get(part), stackSize, material.getID());
    }

    public static ItemStack getPart(final String materialName, final PartsEnum part, final int stackSize) {
        return getPart(getMaterialFromName(materialName), part, stackSize);
    }

    public static ModernMaterial getMaterialFromName(final String materialName) {
        return mNameMaterialMap.get(materialName);
    }
}
