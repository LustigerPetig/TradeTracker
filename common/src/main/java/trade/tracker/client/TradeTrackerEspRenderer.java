package trade.tracker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import trade.tracker.client.render.TradeTrackerRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TradeTrackerEspRenderer {

    /**
     * Call this from your loader-specific rendering events.
     * It accepts standard Mojang mapping objects instead of Fabric-specific contexts.
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (TradeTrackerConfig.glowStyle != TradeTrackerConfig.GlowStyle.BOUNDING_BOX) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        boolean renderedAny = false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Villager)) continue;
            if (!VillagerRolodex.isTargetedOrSearched(entity.getUUID())) continue;

            drawBoundingBox(poseStack, bufferSource, entity, camPos);
            renderedAny = true;
        }

        if (renderedAny && bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(TradeTrackerRenderTypes.TRADETRACKER_LINES_NO_DEPTH);
        }
    }

    private static void drawBoundingBox(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, Vec3 camPos) {
        AABB worldBox = entity.getBoundingBox();

        RenderType type = TradeTrackerRenderTypes.TRADETRACKER_LINES_NO_DEPTH;
        VertexConsumer buffer = bufferSource.getBuffer(type);

        int combinedColor = TradeTrackerConfig.glowColor;

        float r = ((combinedColor >> 16) & 0xFF) / 255.0f;
        float g = ((combinedColor >> 8) & 0xFF) / 255.0f;
        float b = (combinedColor & 0xFF) / 255.0f;
        float a = ((combinedColor >> 24) & 0xFF) / 255.0f;
        if (a <= 0.0f) a = 1.0f;

        LevelRenderer.renderLineBox(poseStack, buffer, worldBox.move(-camPos.x, -camPos.y, -camPos.z), r, g, b, a);
    }
}