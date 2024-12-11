package com.flemmli97.improvedmobs.handler;

import com.flemmli97.improvedmobs.handler.config.ConfigHandler;
import com.flemmli97.improvedmobs.handler.packet.PacketDifficulty;
import com.flemmli97.improvedmobs.handler.packet.PacketHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DifficultyHandler {

	@SubscribeEvent
	public void worldJoin(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof EntityPlayer && !event.getEntity().world.isRemote){
			PacketHandler.sendTo(new PacketDifficulty(DifficultyData.get(event.getEntity().world)), (EntityPlayerMP) event.getEntity());
		}
	}

	@SubscribeEvent
	public void increaseDifficulty(WorldTickEvent e) {
		if(e.phase == Phase.END && e.world != null && !e.world.isRemote && e.world.provider.getDimension() == 0){
			boolean shouldIncrease = (ConfigHandler.ignorePlayers || !e.world.getMinecraftServer().getPlayerList().getPlayers().isEmpty()) && e.world.getWorldTime() > ConfigHandler.difficultyDelay;
			DifficultyData data = DifficultyData.get(e.world);
			if(ConfigHandler.shouldPunishTimeSkip){
				long timeDiff = (int) Math.abs(e.world.getWorldTime() - data.getPrevTime());
				if(timeDiff > 2400){
					long i = timeDiff / 2400;
					if(timeDiff - i * 2400 < (i + 1) * 2400 - timeDiff)
						i *= 2400;
					else
						i *= 2400 + 2400;
					data.increaseDifficultyBy(shouldIncrease ? e.world.getGameRules().getBoolean("doIMDifficulty") ? i / 24000F : 0 : 0, e.world.getWorldTime());
				}
			}else{
				if(e.world.getWorldTime() - data.getPrevTime() > 2400){
					data.increaseDifficultyBy(shouldIncrease ? e.world.getGameRules().getBoolean("doIMDifficulty") ? 0.1F : 0 : 0, e.world.getWorldTime());
				}
			}
		}
	}
}
