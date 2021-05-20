package connor135246.campfirebackport.client.compat.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import connor135246.campfirebackport.common.recipes.CustomInput;
import connor135246.campfirebackport.common.recipes.GenericRecipe;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

public abstract class NEIGenericRecipeHandler extends TemplateRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public static final FontRenderer fonty = Minecraft.getMinecraft().fontRenderer;
    public static final TextureManager rendy = Minecraft.getMinecraft().renderEngine;

    public static final String basicBackground = "minecraft:textures/gui/container/furnace.png",
            neiBackground = Reference.MODID + ":" + "textures/gui/neiElements.png";

    public static final ItemStack hayStack = new ItemStack(Blocks.hay_block),
            stoneStack = new ItemStack(Blocks.stone),
            grassStack = new ItemStack(Blocks.grass);

    public abstract class CachedGenericRecipe extends CachedRecipe
    {
        /** {@link GenericRecipe#types} as array */
        public String[] types;
        /** {@link GenericRecipe#inputs} */
        public List<PositionedStack> inputs;
        /** {@link GenericRecipe#outputs} */
        public PositionedStack output;
        /** the length of {@link GenericRecipe#inputs} */
        public int numInputs;

        /** {@link CustomInput#inputType} */
        public byte[] inputTypes;
        /** {@link CustomInput#dataType} */
        public byte[] dataTypes;
        /** {@link CustomInput#neiTooltip} */
        public List<LinkedList<String>> tooltips;

        /** rectangles that cover each input */
        public Rectangle[] inputRects;

        public CachedGenericRecipe(GenericRecipe grecipe)
        {
            if (grecipe != null)
            {
                types = grecipe.getTypes().asArray();

                numInputs = grecipe.getInputs().length;

                inputTypes = new byte[numInputs];
                dataTypes = new byte[numInputs];
                tooltips = new ArrayList<LinkedList<String>>(numInputs);

                for (int cinputIndex = 0; cinputIndex < numInputs; ++cinputIndex)
                {
                    CustomInput cinput = grecipe.getInputs()[cinputIndex];

                    inputTypes[cinputIndex] = cinput.getInputType();
                    dataTypes[cinputIndex] = cinput.getDataType();
                    tooltips.add(new LinkedList<String>());
                    tooltips.get(cinputIndex).addAll(cinput.getNEITooltip());
                }

                inputs = new ArrayList<PositionedStack>(numInputs);
                inputRects = new Rectangle[numInputs];
            }
        }

        public String getType()
        {
            if (types.length == 0)
                return EnumCampfireType.regular;
            else
                return types[(cycleticks % (20 * types.length)) / 20];
        }

    }

    @Override
    public int recipiesPerPage()
    {
        return 2;
    }

    @Override
    public String getGuiTexture()
    {
        return basicBackground;
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results)
    {
        if (outputId.equals(getOutputID()))
            loadAllRecipes();
        else
            super.loadCraftingRecipes(outputId, results);
    }

    /**
     * @return the output id that identifies the type of recipe this is
     */
    public abstract String getOutputID();

    /**
     * Loads all the possible recipes to the matching recipes list.
     */
    public abstract void loadAllRecipes();

    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> tooltip, int recipe)
    {
        CachedGenericRecipe cachedGrecipe = (CachedGenericRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedGrecipe != null && cachedGrecipe.types.length != 0)
        {
            Point mouse = GuiDraw.getMousePosition();
            Point offset = gui.getRecipePosition(recipe);
            Point relMouse = new Point(mouse.x - (gui.width - 176) / 2 - offset.x, mouse.y - (gui.height - 166) / 2 - offset.y);

            if (handleMiscTooltipFromMousePosition(relMouse, cachedGrecipe, stack, tooltip))
                handleInputTooltipFromMousePosition(relMouse, cachedGrecipe, stack, tooltip);
        }

        return tooltip;
    }

    /**
     * Override to add recipe-specific tooltips.
     * 
     * @param relMouse
     *            - the player's mouse position
     * @return true to continue on to {@link #handleInputTooltipFromMousePosition}, false to skip checking for recipe input tooltips
     */
    public boolean handleMiscTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        return true;
    }

    /**
     * Creates tooltips for the recipe's inputs.
     * 
     * @param relMouse
     *            - the player's mouse position
     */
    public void handleInputTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        if (!tooltip.isEmpty())
        {
            int cinputIndex = hoveringOverInput(relMouse, cachedGrecipe);
            if (cinputIndex > -1 && cinputIndex < cachedGrecipe.numInputs)
            {
                if (cachedGrecipe.inputTypes[cinputIndex] == 5)
                {
                    if (cachedGrecipe.dataTypes[cinputIndex] == 4)
                        tooltip.set(0, EnumChatFormatting.ITALIC + StringParsers.translateNEI("any_tinkers"));
                    else
                        tooltip.set(0, EnumChatFormatting.ITALIC + StringParsers.translateNEI("anything"));
                }

                tooltip.addAll(cachedGrecipe.tooltips.get(cinputIndex));
            }
        }
    }

    /**
     * @return the index of the CustomInput the player is hovering over, or -1 if they aren't hovering over an input.
     */
    public int hoveringOverInput(Point relMouse, CachedGenericRecipe cachedGrecipe)
    {
        for (int i = 0; i < cachedGrecipe.inputRects.length; ++i)
        {
            if (cachedGrecipe.inputRects[i].contains(relMouse))
                return i;
        }
        return -1;
    }

    // Static Methods

    /**
     * Returns the {@link CustomInput#inputList} expanded using {@link net.minecraft.item.Item#getSubItems}. <br>
     * Because {@link PositionedStack#generatePermutations()} doesn't always do what I want it to...
     */
    public static List<ItemStack> expandInputList(CustomInput cinput)
    {
        List<ItemStack> neiList = new ArrayList<ItemStack>();

        for (ItemStack cinputStack : cinput.getInputList())
        {
            if (cinputStack.getItemDamage() == OreDictionary.WILDCARD_VALUE)
            {
                List<ItemStack> metaList = new ArrayList<ItemStack>();
                cinputStack.getItem().getSubItems(cinputStack.getItem(), CreativeTabs.tabAllSearch, metaList);

                if (cinput.getInputType() == 6)
                {
                    if (cinput.doesInputSizeMatter() || cinputStack.hasTagCompound())
                        for (ItemStack metaStack : metaList)
                        {
                            if (cinput.doesInputSizeMatter())
                                metaStack.stackSize = cinput.getInputSize();
                            if (cinputStack.hasTagCompound())
                                metaStack.setTagCompound((NBTTagCompound) cinputStack.getTagCompound().copy());
                        }
                }
                else
                {
                    if (cinput.doesInputSizeMatter() || cinput.hasExtraData())
                        for (ItemStack metaStack : metaList)
                        {
                            if (cinput.doesInputSizeMatter())
                                metaStack.stackSize = cinput.getInputSize();
                            if (cinput.hasExtraData())
                                metaStack.setTagCompound((NBTTagCompound) cinput.getExtraData().copy());
                        }
                }

                neiList.addAll(metaList);
            }
            else
            {
                if (cinput.doesInputSizeMatter())
                    cinputStack.stackSize = cinput.getInputSize();

                neiList.add(cinputStack);
            }
        }

        return neiList;
    }

    /**
     * Draws an inventory slot at x, y.
     */
    public static void drawSlot(int x, int y)
    {
        GuiDraw.drawRect(x + 8 - 16 / 2, y + 8 - 16 / 2 - 1, 16, 1, 0xff373737);
        GuiDraw.drawRect(x + 8 - 16 / 2 - 1, y + 8 - 16 / 2 - 1, 1, 16 + 1, 0xff373737);
        GuiDraw.drawRect(x + 8 - 16 / 2, y + 8 - 16 / 2, 16, 16, 0xff8b8b8b);
        GuiDraw.drawRect(x + 8 - 16 / 2, y + 8 + 16 / 2, 16 + 1, 1, 0xffffffff);
        GuiDraw.drawRect(x + 8 + 16 / 2, y + 8 - 16 / 2, 1, 16, 0xffffffff);
    }

}
