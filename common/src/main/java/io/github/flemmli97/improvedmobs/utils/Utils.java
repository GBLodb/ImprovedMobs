package io.github.flemmli97.improvedmobs.utils;

import io.github.flemmli97.improvedmobs.ImprovedMobs;
import io.github.flemmli97.improvedmobs.config.Config;
import io.github.flemmli97.improvedmobs.config.EnchantCalcConf;
import io.github.flemmli97.improvedmobs.config.EquipmentList;
import io.github.flemmli97.tenshilib.common.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

public class Utils {

    public static final Function<Entity, ResourceLocation> ENTITY_ID = e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
    public static final ResourceLocation ATTRIBUTE_ID = ImprovedMobs.modRes("attribute_modifiers");

    public static <T> boolean isInList(T entry, List<? extends String> list, boolean reverse, Function<T, ResourceLocation> mapper) {
        if (reverse)
            return !isInList(entry, list, false, mapper);
        ResourceLocation res = mapper.apply(entry);
        return list.contains(res.getPath()) || list.contains(res.toString());
    }

    public static boolean canHarvest(BlockState block, ItemStack item) {
        if (Config.CommonConfig.ignoreHarvestLevel)
            return true;
        return item.isCorrectToolForDrops(block) || !block.requiresCorrectToolForDrops();
    }

    public static void equipArmor(Mob living, float difficulty) {
        if (Config.CommonConfig.baseEquipChance != 0) {
            float time = difficulty * Config.CommonConfig.diffEquipAdd * 0.01F;
            if (living.getRandom().nextFloat() < (Config.CommonConfig.baseEquipChance + time)) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.HAND)
                        continue;
                    boolean shouldAdd = slot == EquipmentSlot.HEAD || (Config.CommonConfig.baseEquipChanceAdd != 0 && living.getRandom().nextFloat() < (Config.CommonConfig.baseEquipChanceAdd + time));
                    if (shouldAdd && living.getItemBySlot(slot).isEmpty()) {
                        ItemStack equip = EquipmentList.getEquip(living, slot, difficulty);
                        if (living.getRandom().nextFloat() < Config.CommonConfig.randomTrimChance) {
                            RegistryAccess registryAccess = living.getServer().registryAccess();
                            Optional<Holder.Reference<TrimMaterial>> trim = registryAccess.registry(Registries.TRIM_MATERIAL).flatMap(r -> r.getRandom(living.getRandom()));
                            Optional<Holder.Reference<TrimPattern>> pattern = living.getServer().registryAccess().registry(Registries.TRIM_PATTERN).flatMap(r -> r.getRandom(living.getRandom()));
                            if (trim.isPresent() && pattern.isPresent()) {
                                equip.set(DataComponents.TRIM, new ArmorTrim(trim.get(), pattern.get()));
                            }
                        }
                        if (!equip.isEmpty()) {
                            if (!Config.CommonConfig.shouldDropEquip)
                                living.setDropChance(slot, -100);
                            living.setItemSlot(slot, equip);
                        }
                    }
                }
            }
        }
    }

    public static void equipHeld(Mob living, float difficulty) {
        float add = difficulty * Config.CommonConfig.diffWeaponChance * 0.01F;
        if (Config.CommonConfig.baseWeaponChance != 0 && living.getRandom().nextFloat() < (Config.CommonConfig.baseWeaponChance + add)) {
            if (living.getMainHandItem().isEmpty()) {
                ItemStack stack = EquipmentList.getEquip(living, EquipmentSlot.MAINHAND, difficulty);
                if (!Config.CommonConfig.shouldDropEquip)
                    living.setDropChance(EquipmentSlot.MAINHAND, -100);
                living.setItemSlot(EquipmentSlot.MAINHAND, stack);
            }
        }
        // Cause bartering they throw it out immediately
        if (living instanceof AbstractPiglin)
            return;
        add = difficulty * Config.CommonConfig.diffItemChanceAdd * 0.01F;
        if (Config.CommonConfig.baseItemChance != 0 && living.getRandom().nextFloat() < (Config.CommonConfig.baseItemChance + add)) {
            if (living.getOffhandItem().isEmpty()) {
                ItemStack stack = EquipmentList.getEquip(living, EquipmentSlot.OFFHAND, difficulty);
                if (!Config.CommonConfig.shouldDropEquip)
                    living.setDropChance(EquipmentSlot.OFFHAND, -100);
                living.setItemSlot(EquipmentSlot.OFFHAND, stack);
            }
        }
    }

    public static void enchantGear(Mob living, float difficulty) {
        EnchantCalcConf.Value val = Config.CommonConfig.enchantCalc.get(difficulty);
        if (val.max == 0)
            return;
        for (EquipmentSlot entityequipmentslot : EquipmentSlot.values()) {
            ItemStack itemstack = living.getItemBySlot(entityequipmentslot);
            if (itemstack.isEnchanted())
                continue;
            if (!itemstack.isEmpty() && living.getRandom().nextFloat() < (Config.CommonConfig.baseEnchantChance + (difficulty * Config.CommonConfig.diffEnchantAdd * 0.01F))) {
                RegistryAccess registryAccess = living.registryAccess();
                EnchantmentHelper.enchantItem(living.getRandom(), itemstack, Mth.nextInt(living.getRandom(), val.min, val.max),
                        registryAccess.registryOrThrow(Registries.ENCHANTMENT).holders().filter(r -> Config.CommonConfig.enchantWhitelist == Config.CommonConfig.enchantBlacklist.contains(r.key().location().toString()))
                                .map(r -> r));
            }
        }
    }

    public static float getBlockStrength(Mob entityLiving, BlockState state, Level world, BlockPos pos) {
        float hardness = world.getBlockState(pos).getDestroySpeed(world, pos);
        if (hardness < 0) {
            return 0.0F;
        }
        ItemStack main = entityLiving.getMainHandItem();
        ItemStack off = entityLiving.getOffhandItem();
        if (canHarvest(state, main)) {
            float speed = getBreakSpeed(entityLiving, main, state);
            if (canHarvest(state, off)) {
                float offSpeed = getBreakSpeed(entityLiving, off, state);
                if (offSpeed > speed)
                    speed = offSpeed;
            }
            return speed / hardness / 30F;
        } else if (canHarvest(state, off)) {
            return getBreakSpeed(entityLiving, off, state) / hardness / 30F;
        } else {
            return getBreakSpeed(entityLiving, main, state) / hardness / 100F;
        }
    }

    public static float getBreakSpeed(Mob entity, ItemStack stack, BlockState state) {
        float f = stack.getDestroySpeed(state);
        if (f > 1.0f && entity.getAttributes().hasAttribute(Attributes.MINING_EFFICIENCY)) {
            f += (float) entity.getAttributeValue(Attributes.MINING_EFFICIENCY);
        }
        if (MobEffectUtil.hasDigSpeed(entity))
            f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(entity) + 1) * 0.2F;
        if (entity.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            switch (entity.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
                case 0 -> f *= 0.3F;
                case 1 -> f *= 0.09F;
                case 2 -> f *= 0.0027F;
                default -> f *= 8.1E-4F;
            }
        }
        if (entity.isEyeInFluid(FluidTags.WATER) && entity.getAttribute(Attributes.SUBMERGED_MINING_SPEED) != null) {
            f *= (float) entity.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
        }
        if (!entity.onGround())
            f /= 5.0F;
        return f;
    }

    public static void modifyAttr(Mob living, Holder<Attribute> att, double value, double max, float difficulty, boolean multiply) {
        AttributeInstance inst = living.getAttribute(att);
        if (inst == null || inst.getModifier(ATTRIBUTE_ID) != null)
            return;
        double oldValue = inst.getBaseValue();
        value *= difficulty;
        if (multiply) {
            value = max <= 0 ? value : Math.min(value, max - 1);
            value = oldValue * value;
            if (att == Attributes.MAX_HEALTH)
                value = Config.CommonConfig.roundHP > 0 ? MathUtils.roundTo(value, Config.CommonConfig.roundHP) : value;
        } else {
            value = max <= 0 ? value : Math.min(value, max);
        }
        inst.addPermanentModifier(new AttributeModifier(ATTRIBUTE_ID, value, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void modifyScale(Mob living, float min, float max) {
        var random = new Random();
        //float minRange= 0.6F,maxRange= 2.0F;
        ScaleTypes.BASE.getScaleData(living).setScaleTickDelay(20);
        ScaleTypes.BASE.getScaleData(living).setTargetScale(random.nextFloat(min, max));
    }
}