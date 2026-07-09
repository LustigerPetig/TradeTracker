package jasper.bot.client.render;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;


public final class JasperBotRenderTypes {

    private static final RenderPipeline JASPERBOT_LINES_NO_DEPTH_PIPELINE =
            RenderPipeline.builder(new RenderPipeline.Snippet[]{ RenderPipelines.LINES_SNIPPET })
                    .withLocation("jasperbot_lines_no_depth")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .build();

    public static final RenderType JASPERBOT_LINES_NO_DEPTH = RenderType.create(
            "jasperbot_lines_no_depth",
            RenderSetup.builder(JASPERBOT_LINES_NO_DEPTH_PIPELINE)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    private JasperBotRenderTypes() {}
}