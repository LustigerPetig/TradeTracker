package jasper.bot.client.mixin;

import jasper.bot.client.duck.JasperBotRenderStateAccessor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class JasperBotRenderStateMixin implements JasperBotRenderStateAccessor {
    @Unique private java.util.UUID jasperbot$uuid;
    @Unique private float jasperbot$width;
    @Unique private float jasperbot$height;

    @Override public java.util.UUID jasperbot$getUuid() { return jasperbot$uuid; }
    @Override public void jasperbot$setUuid(java.util.UUID uuid) { this.jasperbot$uuid = uuid; }
    @Override public float jasperbot$getWidth() { return jasperbot$width; }
    @Override public void jasperbot$setWidth(float w) { this.jasperbot$width = w; }
    @Override public float jasperbot$getHeight() { return jasperbot$height; }
    @Override public void jasperbot$setHeight(float h) { this.jasperbot$height = h; }
}