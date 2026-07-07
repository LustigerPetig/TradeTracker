package jasper.bot.client.mixin;

import jasper.bot.client.JasperBotConfig;
import jasper.bot.client.VillagerRolodex;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jasper.bot.client.duck.JasperBotRenderStateAccessor;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import jasper.bot.client.render.JasperBotRenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into the generic EntityRenderer<T, S> base class.
 *
 * In the new deferred-rendering pipeline there is no single "render everything"
 * hook on EntityRenderDispatcher anymore. Instead:
 *   - extractRenderState(entity, state, partialTick) runs first, with the real
 *     Entity available - we use this to stash the entity's UUID onto the state.
 *   - submit(state, poseStack, collector, cameraState) runs later, but only
 *     has the EntityRenderState snapshot - no direct Entity reference.
 *
 * @param <T> the entity type being rendered
 * @param <S> the render state type for that entity
 */
@Mixin(EntityRenderer.class)
public abstract class JasperBotESPMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
    private void jasperbot$captureUuid(T entity, S state, float partialTick, CallbackInfo ci) {
        JasperBotRenderStateAccessor accessor = (JasperBotRenderStateAccessor) (Object) state;
        accessor.jasperbot$setUuid(entity.getUUID());
        accessor.jasperbot$setWidth(entity.getBbWidth());
        accessor.jasperbot$setHeight(entity.getBbHeight());
    }

    @Inject(method = "submit", at = @At("TAIL"), require = 1)
    private void jasperbot$drawBoundingBox(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {

        JasperBotRenderStateAccessor accessor = (JasperBotRenderStateAccessor) (Object) state;
        java.util.UUID uuid = accessor.jasperbot$getUuid();
        if (uuid == null || !VillagerRolodex.isTargetedOrSearched(uuid)) return;

        double halfWidth = accessor.jasperbot$getWidth() / 2.0;
        double height = accessor.jasperbot$getHeight();
        AABB box = new AABB(-halfWidth, 0.0, -halfWidth, halfWidth, height, halfWidth);

        // 4. Submit custom geometry through the collector - the lambda gets the
        //    correct Pose + VertexConsumer at draw time.
        RenderType type = JasperBotRenderTypes.JASPERBOT_LINES_NO_DEPTH;
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {

            // Neon Green
            int r = 0, g = 255, b = 0, a = 255;

            // Bottom square
            jasperbot$drawLine(buffer, pose, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);

            // Top square
            jasperbot$drawLine(buffer, pose, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);

            // Vertical pillars
            jasperbot$drawLine(buffer, pose, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
            jasperbot$drawLine(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        });
    }

    @Unique
    private void jasperbot$drawLine(VertexConsumer buffer, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;

        float lineWidth = 2.0f; // pick whatever thickness you want

        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(lineWidth);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(lineWidth);
    }
}