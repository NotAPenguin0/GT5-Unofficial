package gregtech.api.ModernMaterials;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import crazypants.enderio.conduit.oc.OCUtil;
import gregtech.api.GregTech_API;
import gregtech.api.ModernMaterials.Blocks.BlocksEnum;
import gregtech.api.ModernMaterials.Blocks.DumbBase.NewDumb.NewDumb;
import gregtech.api.ModernMaterials.Blocks.DumbBase.NewDumb.NewDumbItemBlock;
import gregtech.api.ModernMaterials.Blocks.DumbBase.Special.MasterItemRenderer;
import gregtech.api.ModernMaterials.Blocks.DumbBase.Special.MasterTESR;
import gregtech.api.ModernMaterials.Blocks.DumbBase.Base.BaseBlock;
import gregtech.api.ModernMaterials.Blocks.DumbBase.Base.BaseItemBlock;
import gregtech.api.ModernMaterials.Blocks.FrameBox.FrameBoxSimpleBlockRenderer;
import gregtech.api.ModernMaterials.Fluids.ModernMaterialFluid;
import gregtech.api.ModernMaterials.PartProperties.Rendering.ModernMaterialItemRenderer;
import gregtech.api.ModernMaterials.PartRecipeGenerators.ModernMaterialsPlateRecipeGenerator;
import gregtech.api.ModernMaterials.PartsClasses.IGetItem;
import gregtech.api.ModernMaterials.PartsClasses.MaterialPart;
import gregtech.api.ModernMaterials.PartsClasses.MaterialPartsEnum;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fluids.FluidRegistry;
import org.lwjgl.opengl.GL11;
import scala.Int;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static gregtech.api.enums.ConfigCategories.ModernMaterials.materialID;

public class ModernMaterialUtilities {

    private static final List<ModernMaterial> newMaterials = new ArrayList<>();
    public static final HashMap<Integer, ModernMaterial> materialIDToMaterial = new HashMap<>();
    public static final HashMap<String, ModernMaterial> materialNameToMaterialMap = new HashMap<>();

    public static void registerMaterial(ModernMaterial material) {
        materialIDToMaterial.put(material.getMaterialID(), material);
        materialNameToMaterialMap.put(material.getMaterialName(), material);
    }

    public static void registerAllMaterialsItems() {
        for (ModernMaterial tMaterial : newMaterials) {
            tMaterial.setMaterialID(++GregTech_API.lastMaterialID);
            GregTech_API.modernMaterialIDs.mConfig.get(materialID.name(), tMaterial.getMaterialName(), 0)
                    .set(GregTech_API.lastMaterialID);
            materialIDToMaterial.put(GregTech_API.lastMaterialID, tMaterial);
        }

        for (MaterialPartsEnum part : MaterialPartsEnum.values()) {

            MaterialPart materialPart = new MaterialPart(part);
            materialPart.setUnlocalizedName(part.partName);

            // Registers the item with the game, only available in preInit.
            GameRegistry.registerItem(materialPart, part.partName);

            // Store the Item so these parts can be retrieved later for recipe generation etc.
            part.setAssociatedItem(materialPart);

            // Registers the renderer which allows for part colouring.
            MinecraftForgeClient.registerItemRenderer(materialPart, new ModernMaterialItemRenderer());
        }

        // Register all material parts.
        for (ModernMaterial material : materialIDToMaterial.values()) {
            registerAllMaterialPartRecipes(material);
        }

    }




    public static void registerAllMaterialsBlocks() {
        BlocksEnum.FrameBox.getAssociatedMaterials().addAll(materialIDToMaterial.values());

        for (BlocksEnum blockType : BlocksEnum.values()) {
            registerSimpleBlock(blockType);
            //registerTESRBlock(blockType);

        }

        new FrameBoxSimpleBlockRenderer();

    }

//    private static void registerTESRBlock(BlocksEnum blockType) {
//
//        final String name = "TESR:" + blockType;
//
//        try {
//            // Register the actual block.
//            BaseBlock simpleBlock = blockType.getBlockClass().getDeclaredConstructor().newInstance();
//            simpleBlock.setBlockName(name);
//            GameRegistry.registerBlock(simpleBlock, BaseItemBlock.class, name);
//
//            // Register the tile entity itself.
//            GameRegistry.registerTileEntity(blockType.getTileEntityClass(), name);
//
//            Item blockItem = Item.getItemFromBlock(simpleBlock);
//            MinecraftForgeClient.registerItemRenderer(blockItem, new MasterItemRenderer());
//
//            // Register the master TESR. This only exists to redirect to different TESRs for different materials.
//            ClientRegistry.bindTileEntitySpecialRenderer(blockType.getTileEntityClass(), new MasterTESR());
//
//        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
//            throw new RuntimeException("Failed to instantiate block", e);
//        }
//
//    }

    /**
     * Registers a simple block with the game based on the provided block type and valid material IDs.
     *
     * @param blockType The type of block to be registered.
     */
    private static void registerSimpleBlock(BlocksEnum blockType) {

        // Extract the materials associated with the given block type
        HashSet<ModernMaterial> associatedMaterials = blockType.getSimpleBlockRenderAssociatedMaterials();

        // Extract and sort the IDs associated with the materials. We process this in generateIDGroups.
        List<Integer> sortedIDs = associatedMaterials.stream()
            .map(ModernMaterial::getMaterialID)
            .sorted()
            .collect(Collectors.toList());

        int offset = -1;
        for (List<Integer> IDs : generateIDGroups(sortedIDs)) {
            offset++;
            if (IDs.isEmpty()) {
                continue;
            }

            try {
                NewDumb block = blockType.getBlockClass()
                    .getDeclaredConstructor(int.class, List.class)
                    .newInstance(offset, IDs);
                GameRegistry.registerBlock(block, NewDumbItemBlock.class, blockType + "." + offset);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate block", e);
            }

        }
    }

    private static final int GROUP_SIZE = 16;

    public static List<List<Integer>> generateIDGroups(List<Integer> sortedIDs) {
        // Handle null or empty input
        if (sortedIDs == null || sortedIDs.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Integer>> groupedIDs = new ArrayList<>();

        // Process each ID from the sorted list
        for (int currentID : sortedIDs) {
            int groupIndex = calculateGroupIndex(currentID);

            // Ensure the groupedIDs list has a list initialized for this groupIndex
            ensureGroupExists(groupedIDs, groupIndex);

            groupedIDs.get(groupIndex).add(currentID);
        }

        return groupedIDs;
    }

    // Calculate the group index for the given ID
    private static int calculateGroupIndex(int id) {
        return id / GROUP_SIZE;
    }

    // Ensure that the groupedIDs list has a list initialized for the given groupIndex
    private static void ensureGroupExists(List<List<Integer>> groupedIDs, int groupIndex) {
        while (groupedIDs.size() <= groupIndex) {
            groupedIDs.add(new ArrayList<>());
        }
    }

    public static void registerAllMaterialsFluids() {

        // Register the fluids with forge.
        for (ModernMaterial material : materialIDToMaterial.values()) {
            for (ModernMaterialFluid fluid : material.existingFluids) {
                FluidRegistry.registerFluid(fluid);
            }
        }
    }

    private static void registerAllMaterialPartRecipes(ModernMaterial material) {
        new ModernMaterialsPlateRecipeGenerator().run(material);
    }

    public static ItemStack getPart(final ModernMaterial material, final IGetItem part, final int stackSize) {
        return new ItemStack(part.getItem(), stackSize, material.getMaterialID());
    }

    public static ItemStack getPart(final String materialName, final IGetItem part, final int stackSize) {
        return getPart(getMaterialFromName(materialName), part, stackSize);
    }

    public static ModernMaterial getMaterialFromName(final String materialName) {

        ModernMaterial modernMaterial = materialNameToMaterialMap.getOrDefault(materialName, null);

        if (modernMaterial == null) {
            throw new IllegalArgumentException("Material % does not exist. Make sure you spelt it correctly.".replace("%", materialName));
        }

        return modernMaterial;
    }

    public static ModernMaterial getMaterialFromID(final int materialID) {

        ModernMaterial modernMaterial = materialIDToMaterial.getOrDefault(materialID, null);

        if (modernMaterial == null) {
            throw new IllegalArgumentException("Material with ID " + materialID + " does not exist.");
        }

        return modernMaterial;
    }

    public static ArrayList<String> tooltipGenerator(Item part, ModernMaterial material) {
        // Todo, this is just temporary as a proof of concept/debug info.
        // Probably will put radioactive warning here. Not sure what else yet, if any.

        ArrayList<String> tooltip = new ArrayList<>();

        tooltip.add("Generic Tooltip");
        tooltip.add("Material Name: " + material.getMaterialName());

        if (part instanceof BaseItemBlock blockPart) {
            tooltip.add("Material Part Type: " + "Blah blah do later");
        } else if (part instanceof MaterialPart itemPart) {
            tooltip.add("Material Part Type: " + material.getCustomPartInfo(itemPart.getPart()).getTextureType());
        }

        return tooltip;
    }

    public static void drawBlock(Block block, int meta, RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;

        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(0, meta));

        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(1, meta));

        tessellator.setNormal(0.0F, 0.0F, -1.0F);
        renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(2, meta));

        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(3, meta));

        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(4, meta));

        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(5, meta));

    }

    public static void renderBlock(IIcon texture) {
        Tessellator tes = Tessellator.instance;

        double x = 0;
        double y = 0;
        double z = 0;

        double[] X = { x - 0.5, x - 0.5, x + 0.5, x + 0.5, x + 0.5, x + 0.5, x - 0.5, x - 0.5 };
        double[] Y = { y + 0.5, y - 0.5, y - 0.5, y + 0.5, y + 0.5, y - 0.5, y - 0.5, y + 0.5 };
        double[] Z = { z + 0.5, z + 0.5, z + 0.5, z + 0.5, z - 0.5, z - 0.5, z - 0.5, z - 0.5 };

        tes.startDrawingQuads();

        double minU;
        double maxU;
        double minV;
        double maxV;

        {
            minU = texture.getMinU();
            maxU = texture.getMaxU();
            minV = texture.getMinV();
            maxV = texture.getMaxV();

            tes.addVertexWithUV(X[1], Y[1], Z[1], maxU, maxV);
            tes.addVertexWithUV(X[0], Y[0], Z[0], maxU, minV);
            tes.addVertexWithUV(X[7], Y[7], Z[7], minU, minV);
            tes.addVertexWithUV(X[6], Y[6], Z[6], minU, maxV);
        }

        {
            // Bottom face.

            minU = texture.getMinU();
            maxU = texture.getMaxU();
            minV = texture.getMinV();
            maxV = texture.getMaxV();

            tes.addVertexWithUV(X[1], Y[1], Z[1], minU, maxV);
            tes.addVertexWithUV(X[6], Y[6], Z[6], minU, minV);
            tes.addVertexWithUV(X[5], Y[5], Z[5], maxU, minV);
            tes.addVertexWithUV(X[2], Y[2], Z[2], maxU, maxV);
        }

        {

            minU = texture.getMinU();
            maxU = texture.getMaxU();
            minV = texture.getMinV();
            maxV = texture.getMaxV();

            tes.addVertexWithUV(X[6], Y[6], Z[6], maxU, maxV);
            tes.addVertexWithUV(X[7], Y[7], Z[7], maxU, minV);
            tes.addVertexWithUV(X[4], Y[4], Z[4], minU, minV);
            tes.addVertexWithUV(X[5], Y[5], Z[5], minU, maxV);
        }

        {

            minU = texture.getMinU();
            maxU = texture.getMaxU();
            minV = texture.getMinV();
            maxV = texture.getMaxV();

            tes.addVertexWithUV(X[5], Y[5], Z[5], maxU, maxV);
            tes.addVertexWithUV(X[4], Y[4], Z[4], maxU, minV);
            tes.addVertexWithUV(X[3], Y[3], Z[3], minU, minV);
            tes.addVertexWithUV(X[2], Y[2], Z[2], minU, maxV);
        }

        {

            minU = texture.getMinU();
            maxU = texture.getMaxU();
            minV = texture.getMinV();
            maxV = texture.getMaxV();

            tes.addVertexWithUV(X[3], Y[3], Z[3], maxU, maxV);
            tes.addVertexWithUV(X[4], Y[4], Z[4], maxU, minV);
            tes.addVertexWithUV(X[7], Y[7], Z[7], minU, minV);
            tes.addVertexWithUV(X[0], Y[0], Z[0], minU, maxV);
        }

        {

            minU = texture.getMinU();
            maxU = texture.getMaxU();
            minV = texture.getMinV();
            maxV = texture.getMaxV();

            tes.addVertexWithUV(X[2], Y[2], Z[2], maxU, maxV);
            tes.addVertexWithUV(X[3], Y[3], Z[3], maxU, minV);
            tes.addVertexWithUV(X[0], Y[0], Z[0], minU, minV);
            tes.addVertexWithUV(X[1], Y[1], Z[1], minU, maxV);
        }

        tes.draw();

        // ----------------------------------------------
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

}