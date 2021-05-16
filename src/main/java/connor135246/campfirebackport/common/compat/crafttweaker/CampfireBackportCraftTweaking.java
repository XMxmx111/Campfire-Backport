package connor135246.campfirebackport.common.compat.crafttweaker;

import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.common.recipes.CustomInput;
import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.FMLCommonHandler;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.MineTweakerImplementationAPI.ReloadEvent;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.util.IEventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.campfirebackport")
public class CampfireBackportCraftTweaking
{

    // Registering & Reload

    public static void load()
    {
        if (FMLCommonHandler.instance().findContainerFor("MineTweaker3").getVersion().startsWith("3.0"))
            MineTweakerAPI.logError(StringParsers.translateCT("version_warning"));
        
        MineTweakerAPI.registerClass(CampfireBackportCraftTweaking.class);
        MineTweakerImplementationAPI.onReloadEvent(new ReloadEventHandler());
        MineTweakerImplementationAPI.onPostReload(new PostReloadEventHandler());
    }

    /**
     * Ensures recipes added before this reload are removed.
     */
    public static class ReloadEventHandler implements IEventHandler<ReloadEvent>
    {
        @Override
        public void handle(ReloadEvent event)
        {
            CampfireRecipe.getCraftTweakerList().clear();
            CampfireStateChanger.getCraftTweakerList().clear();
            BurnOutRule.getCraftTweakerRules().clear();
        }
    }

    /**
     * Ensures recipe lists are sorted after recipes were added.
     */
    public static class PostReloadEventHandler implements IEventHandler<ReloadEvent>
    {
        @Override
        public void handle(ReloadEvent event)
        {
            CampfireRecipe.sortRecipeLists();
            CampfireStateChanger.sortStateChangerLists();
        }
    }

    // Campfire Recipes

    /**
     * ZenScript method that adds a Campfire Recipe.
     */
    @ZenMethod
    public static void addCampfireRecipe(String types, IIngredient[] input, IItemStack output, @Optional Integer cookingTime, @Optional String signalFire,
            @Optional IItemStack byproduct, @Optional Double byproductChance)
    {
        if (input.length > 0)
        {
            CustomInput[] cinputs = new CustomInput[Math.min(input.length, 4)];

            for (int i = 0; i < cinputs.length; ++i)
                cinputs[i] = new CustomInput(new ActiveCraftTweakerIngredient(input[i]), 1, OreDictionary.WILDCARD_VALUE, null, false, -1);

            addCampfireRecipe(types, cinputs, output, cookingTime, signalFire, byproduct, byproductChance);
        }
        else
            MineTweakerAPI.logError(StringParsers.translateCT("error.empty_array"));
    }

    /**
     * Helper ZenScript method so that you don't have to create an array for simpler recipes.
     */
    @ZenMethod
    public static void addCampfireRecipe(String types, IIngredient input, IItemStack output, @Optional Integer cookingTime, @Optional String signalFire,
            @Optional IItemStack byproduct, @Optional Double byproductChance)
    {
        addCampfireRecipe(types, new IIngredient[] { input }, output, cookingTime, signalFire, byproduct, byproductChance);
    }

    private static void addCampfireRecipe(String types, CustomInput[] cinputs, IItemStack output, Integer cookingTime, String signalFire, IItemStack byproduct,
            Double byproductChance)
    {
        try
        {
            EnumCampfireType typesVerified = getTypes(types);
            byte signalFireVerified = getSignalFire(signalFire);
            if (typesVerified != null && signalFireVerified != -2)
            {
                CampfireRecipe crecipe = new CampfireRecipe(typesVerified, cinputs, new ItemStack[] { MineTweakerMC.getItemStack(output) }, cookingTime,
                        signalFireVerified, MineTweakerMC.getItemStack(byproduct), byproductChance, 100);

                MineTweakerAPI.apply(new AddCampfireRecipeAction(crecipe));
            }
        }
        catch (Exception excep)
        {
            MineTweakerAPI.logError(StringParsers.translateCT("recipe.error"), excep);
        }
    }

    public static class AddCampfireRecipeAction implements IUndoableAction
    {
        private final CampfireRecipe crecipe;

        public AddCampfireRecipeAction(CampfireRecipe crecipe)
        {
            this.crecipe = crecipe;
        }

        @Override
        public void apply()
        {
            CampfireRecipe.getCraftTweakerList().add(crecipe);
            CampfireRecipe.addToRecipeLists(crecipe);
        }

        @Override
        public boolean canUndo()
        {
            return true;
        }

        @Override
        public String describe()
        {
            return StringParsers.translateCT("recipe.add", crecipe.getOutput().getDisplayName());
        }

        @Override
        public String describeUndo()
        {
            return StringParsers.translateCT("recipe.remove", crecipe.getOutput().getDisplayName());
        }

        @Override
        public Object getOverrideKey()
        {
            return null;
        }

        @Override
        public void undo()
        {
            CampfireRecipe.getCraftTweakerList().remove(crecipe);
            CampfireRecipe.removeFromRecipeLists(crecipe);
        }
    }

    // Campfire State Changers

    /**
     * ZenScript method that adds a Campfire Extinguisher.
     */
    @ZenMethod
    public static void addCampfireExtinguisher(String types, IIngredient input, String usageType, @Optional int damageOrReduceBy, @Optional IItemStack output,
            @Optional boolean leftClick)
    {
        addCampfireStateChanger(types, input, usageType, damageOrReduceBy, output, leftClick, true);
    }

    /**
     * ZenScript method that adds a Campfire Ignitor.
     */
    @ZenMethod
    public static void addCampfireIgnitor(String types, IIngredient input, String usageType, @Optional int damageOrReduceBy, @Optional IItemStack output,
            @Optional boolean leftClick)
    {
        addCampfireStateChanger(types, input, usageType, damageOrReduceBy, output, leftClick, false);
    }

    private static void addCampfireStateChanger(String types, IIngredient input, String usageType, int damageOrReduceBy, IItemStack output, boolean leftClick,
            boolean extinguisher)
    {
        try
        {
            EnumCampfireType typesVerified = getTypes(types);
            if (typesVerified != null && verifyUsageType(usageType))
            {
                boolean damageable = usageType.equals(CampfireStateChanger.DAMAGEABLE);

                CustomInput[] cinputs = new CustomInput[] { new CustomInput(new ActiveCraftTweakerIngredient(input),
                        damageable ? Math.max(damageOrReduceBy, 1) : MathHelper.clamp_int(damageOrReduceBy, 1, 64), OreDictionary.WILDCARD_VALUE, null,
                        !damageable, -1) };
                ItemStack[] outputs = output == null ? null : new ItemStack[] { MineTweakerMC.getItemStack(output) };

                CampfireStateChanger cstate = new CampfireStateChanger(typesVerified, cinputs, leftClick, extinguisher, usageType, outputs, false, 100);

                MineTweakerAPI.apply(new AddCampfireStateChangerAction(cstate));
            }
        }
        catch (Exception excep)
        {
            MineTweakerAPI.logError(StringParsers.translateCT("state_changer.error"), excep);
        }
    }

    public static class AddCampfireStateChangerAction implements IUndoableAction
    {
        private final CampfireStateChanger cstate;

        public AddCampfireStateChangerAction(CampfireStateChanger cstate)
        {
            this.cstate = cstate;
        }

        @Override
        public void apply()
        {
            CampfireStateChanger.getCraftTweakerList().add(cstate);
            CampfireStateChanger.addToStateChangerLists(cstate);
        }

        @Override
        public boolean canUndo()
        {
            return true;
        }

        @Override
        public String describe()
        {
            return StringParsers.translateCT("state_changer.add", cstate.isExtinguisher() ? ConfigReference.extinguisher : ConfigReference.ignitor);
        }

        @Override
        public String describeUndo()
        {
            return StringParsers.translateCT("state_changer.remove", cstate.isExtinguisher() ? ConfigReference.extinguisher : ConfigReference.ignitor);
        }

        @Override
        public Object getOverrideKey()
        {
            return null;
        }

        @Override
        public void undo()
        {
            CampfireStateChanger.getCraftTweakerList().remove(cstate);
            CampfireStateChanger.removeFromStateChangerLists(cstate);
        }
    }

    // Burn Out Rules

    /**
     * ZenScript method that adds a Burn Out Rule.
     */
    @ZenMethod
    public static void addBurnOutTimer(String types, Integer biomeId, Integer dimensionId, int timer)
    {
        try
        {
            EnumCampfireType typesVerified = getTypes(types);
            if (typesVerified != null && verifyIDs(biomeId, dimensionId))
            {
                BurnOutRule brule = new BurnOutRule(typesVerified, biomeId, dimensionId, timer, false);

                MineTweakerAPI.apply(new AddBurnOutRuleAction(brule));
            }
        }
        catch (Exception excep)
        {
            MineTweakerAPI.logError(StringParsers.translateCT("burn_out_rule.error"), excep);
        }
    }

    public static class AddBurnOutRuleAction implements IUndoableAction
    {
        private final BurnOutRule brule;

        public AddBurnOutRuleAction(BurnOutRule brule)
        {
            this.brule = brule;
        }

        @Override
        public void apply()
        {
            BurnOutRule.getCraftTweakerRules().add(brule);
            BurnOutRule.addToRules(brule);
        }

        @Override
        public boolean canUndo()
        {
            return true;
        }

        @Override
        public String describe()
        {
            return StringParsers.translateCT("burn_out_rule.add", brule.getTimer());
        }

        @Override
        public String describeUndo()
        {
            return StringParsers.translateCT("burn_out_rule.remove", brule.getTimer());
        }

        @Override
        public Object getOverrideKey()
        {
            return null;
        }

        @Override
        public void undo()
        {
            BurnOutRule.getCraftTweakerRules().remove(brule);
            BurnOutRule.removeFromRules(brule);
        }
    }

    // Helper Functions

    /**
     * Gets an EnumCampfireType from a String. Logs an error in CraftTweaker if it can't.
     */
    private static EnumCampfireType getTypes(String types)
    {
        EnumCampfireType campfireTypes = EnumCampfireType.FROM_NAME.get(types);

        if (campfireTypes == null || campfireTypes == EnumCampfireType.NEITHER)
        {
            MineTweakerAPI.logError(StringParsers.translateCT("error.types", types));
            return null;
        }

        return campfireTypes;
    }

    /**
     * Gets a byte representing the signal fire requirement from a String. Logs an error in CraftTweaker if it can't.
     */
    private static byte getSignalFire(String signalFire)
    {
        if (signalFire == null || signalFire.equals(CampfireRecipe.ANY))
            return 0;
        else if (signalFire.equals(CampfireRecipe.SIGNAL))
            return 1;
        else if (signalFire.equals(CampfireRecipe.NOTSIGNAL))
            return -1;
        else
        {
            MineTweakerAPI.logError(StringParsers.translateCT("error.signal", signalFire));
            return -2;
        }
    }

    /**
     * Verifies that the given string is a valid usage type for a CampfireStateChanger. Logs an error in CraftTweaker if it isn't.
     */
    private static boolean verifyUsageType(String usageType)
    {
        if (usageType.equals(CampfireStateChanger.DAMAGEABLE) || usageType.equals(CampfireStateChanger.STACKABLE)
                || usageType.equals(CampfireStateChanger.NONE))
        {
            return true;
        }
        else
        {
            MineTweakerAPI.logError(StringParsers.translateCT("error.usage", usageType));
            return false;
        }
    }

    /**
     * Verifies that the given biome and/or dimension ids are valid. Logs an error in CraftTweaker if they aren't.
     */
    private static boolean verifyIDs(Integer biomeId, Integer dimensionId)
    {
        if (biomeId == null && dimensionId == null)
        {
            MineTweakerAPI.logError(StringParsers.translateCT("error.null_ids"));
            return false;
        }
        else if (biomeId != null && (biomeId > 255 || 0 > biomeId))
        {
            MineTweakerAPI.logError(StringParsers.translateCT("error.biome_id", biomeId));
            return false;
        }
        return true;
    }

}