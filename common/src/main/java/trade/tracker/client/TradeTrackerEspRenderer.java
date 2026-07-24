package trade.tracker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import trade.tracker.client.render.TradeTrackerRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TradeTrackerEspRenderer {

    /**
     * Call this from your loader-specific rendering events.
     * It accepts standard Mojang mapping objects instead of Fabric-specific contexts.
     */
    public static void render(PoseStack poseStack, SubmitNodeCollector collector) {
        if (TradeTrackerConfig.glowStyle != TradeTrackerConfig.GlowStyle.BOUNDING_BOX) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = mc.gameRenderer.mainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Villager)) continue;
            if (!VillagerRolodex.isTargetedOrSearched(entity.getUUID())) continue;

            drawBoundingBox(poseStack, collector, entity, camPos);
        }
    }

    private static void drawBoundingBox(PoseStack poseStack, SubmitNodeCollector collector, Entity entity, Vec3 camPos) {
        AABB worldBox = entity.getBoundingBox();

        double minX = worldBox.minX - camPos.x;
        double minY = worldBox.minY - camPos.y;
        double minZ = worldBox.minZ - camPos.z;
        double maxX = worldBox.maxX - camPos.x;
        double maxY = worldBox.maxY - camPos.y;
        double maxZ = worldBox.maxZ - camPos.z;

        RenderType type = TradeTrackerRenderTypes.TRADETRACKER_LINES_NO_DEPTH;

        poseStack.pushPose();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            int combinedColor = TradeTrackerConfig.glowColor;

            int r = (combinedColor >> 16) & 0xFF;
            int g = (combinedColor >> 8) & 0xFF;
            int b = combinedColor & 0xFF;
            int a = (combinedColor >> 24) & 0xFF;

            // Bottom square
            tradetracker$drawLine(buffer, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

            // Top square
            tradetracker$drawLine(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

            // Vertical pillars
            tradetracker$drawLine(buffer, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
            tradetracker$drawLine(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        });
        poseStack.popPose();
    }

    private static void tradetracker$drawLine(VertexConsumer buffer, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;

        float lineWidth = 2.0f;

        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(lineWidth);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(lineWidth);
    }
}