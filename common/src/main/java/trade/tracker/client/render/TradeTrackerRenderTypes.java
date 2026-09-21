package trade.tracker.client.render;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TradeTrackerRenderTypes {

    public static final RenderType TRADETRACKER_LINES_NO_DEPTH = initRenderType();

    private static RenderType initRenderType() {
        try {
            Field snippetField = RenderPipelines.class.getDeclaredField("LINES_SNIPPET");
            snippetField.setAccessible(true);
            RenderPipeline.Snippet linesSnippet = (RenderPipeline.Snippet) snippetField.get(null);

            RenderPipeline pipeline = RenderPipeline.builder(linesSnippet)
                    .withLocation("tradetracker_lines_no_depth")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .build();

            RenderSetup setup = RenderSetup.builder(pipeline)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .createRenderSetup();

            Method createMethod = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
            createMethod.setAccessible(true);

            return (RenderType) createMethod.invoke(null, "tradetracker_lines_no_depth", setup);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TradeTrackerRenderTypes via Reflection", e);
        }
    }

    private TradeTrackerRenderTypes() {}
}