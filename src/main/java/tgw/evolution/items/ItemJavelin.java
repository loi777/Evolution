package tgw.evolution.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import tgw.evolution.Evolution;
import tgw.evolution.entities.projectiles.EntityGenericProjectile;
import tgw.evolution.entities.projectiles.EntitySpear;
import tgw.evolution.init.EvolutionAttributes;
import tgw.evolution.init.EvolutionDamage;
import tgw.evolution.init.EvolutionSounds;
import tgw.evolution.util.PlayerHelper;

import javax.annotation.Nonnull;

public class ItemJavelin extends ItemEv implements IDurability, IThrowable, ISpear, IMelee, IMass {

    private final float damage;
    private final float speed;
    private final double mass;
    private final ResourceLocation modelTexture;

    public ItemJavelin(Properties builder, float damage, float speed, String name, double mass) {
        super(builder);
        this.modelTexture = Evolution.location("textures/entity/javelin/javelin_" + name + ".png");
        this.damage = damage;
        this.speed = speed;
        this.mass = mass;
        this.addPropertyOverride(new ResourceLocation("throwing"),
                                 (stack, world, entity) -> entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack ?
                                                           1.0F :
                                                           0.0F);
    }

    @Override
    public boolean canPlayerBreakBlockWhileHolding(BlockState state, World worldIn, BlockPos pos, PlayerEntity player) {
        return !player.isCreative();
    }

    @Override
    public double getAttackDamage() {
        return this.damage;
    }

    @Override
    public double getAttackSpeed() {
        return this.speed - PlayerHelper.ATTACK_SPEED;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EquipmentSlotType equipmentSlot) {
        Multimap<String, AttributeModifier> multimap = HashMultimap.create();
        if (equipmentSlot == EquipmentSlotType.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                         new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
                                               "Tool modifier",
                                               this.getAttackDamage(),
                                               AttributeModifier.Operation.ADDITION));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                         new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", this.getAttackSpeed(), AttributeModifier.Operation.ADDITION));
            multimap.put(PlayerEntity.REACH_DISTANCE.getName(),
                         new AttributeModifier(EvolutionAttributes.REACH_DISTANCE_MODIFIER,
                                               "Reach Modifier",
                                               this.getReach(),
                                               AttributeModifier.Operation.ADDITION));
            multimap.put(EvolutionAttributes.MASS.getName(),
                         new AttributeModifier(EvolutionAttributes.MASS_MODIFIER, "Mass Modifier", this.mass, AttributeModifier.Operation.ADDITION));
        }
        else if (equipmentSlot == EquipmentSlotType.OFFHAND) {
            multimap.put(EvolutionAttributes.MASS.getName(),
                         new AttributeModifier(EvolutionAttributes.MASS_MODIFIER_OFFHAND,
                                               "Mass Modifier",
                                               this.mass,
                                               AttributeModifier.Operation.ADDITION));
        }
        return multimap;
    }

    @Nonnull
    @Override
    public EvolutionDamage.Type getDamageType() {
        return EvolutionDamage.Type.PIERCING;
    }

    @Override
    public double getMass() {
        return this.mass;
    }

    @Override
    public double getReach() {
        return 5 - PlayerHelper.REACH_DISTANCE;
    }

    @Override
    public ResourceLocation getTexture() {
        return this.modelTexture;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72_000;
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damageItem(1, attacker, entity -> entity.sendBreakAnimation(entity.getActiveHand()));
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        if (state.getBlockHardness(worldIn, pos) != 0) {
            stack.damageItem(2, entityLiving, entity -> entity.sendBreakAnimation(EquipmentSlotType.MAINHAND));
        }
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (stack.getDamage() >= stack.getMaxDamage() || handIn == Hand.OFF_HAND) {
            return new ActionResult<>(ActionResultType.FAIL, stack);
        }
        playerIn.setActiveHand(handIn);
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
        if (entityLiving instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entityLiving;
            int i = this.getUseDuration(stack) - timeLeft;
            if (i >= 10) {
                if (!worldIn.isRemote) {
                    EntitySpear spear = new EntitySpear(worldIn, player, stack, this.damage, this.mass);
                    spear.shoot(player, player.rotationPitch, player.rotationYaw, 0.825f, 2.5F);
                    if (player.abilities.isCreativeMode) {
                        spear.pickupStatus = EntityGenericProjectile.PickupStatus.CREATIVE_ONLY;
                    }
                    worldIn.addEntity(spear);
                    worldIn.playMovingSound(null, spear, EvolutionSounds.JAVELIN_THROW.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                    if (!player.abilities.isCreativeMode) {
                        player.inventory.deleteStack(stack);
                    }
                }
                player.addStat(Stats.ITEM_USED.get(this));
            }
        }
    }
}
