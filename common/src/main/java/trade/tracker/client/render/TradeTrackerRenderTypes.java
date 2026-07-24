package trade.tracker.client.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TradeTrackerRenderTypes {

    public static final RenderType TRADETRACKER_LINES_NO_DEPTH = initRenderType();

    private static RenderType initRenderType() {
        try {
            // 1. Use Reflection to access the private LINES_SNIPPET field
            Field snippetField = RenderPipelines.class.getDeclaredField("LINES_SNIPPET");
            snippetField.setAccessible(true);
            RenderPipeline.Snippet linesSnippet = (RenderPipeline.Snippet) snippetField.get(null);

            // 2. Build the pipeline exactly as you had it
            RenderPipeline pipeline = RenderPipeline.builder(linesSnippet)
                    .withLocation("jasperbot_lines_no_depth")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .build();

            // 3. Build the setup exactly as you had it
            RenderSetup setup = RenderSetup.builder(pipeline)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup();

            // 4. Use Reflection to access the package-private RenderType.create() method
            Method createMethod = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
            createMethod.setAccessible(true);

            // 5. Invoke the method and return the result
            return (RenderType) createMethod.invoke(null, "jasperbot_lines_no_depth", setup);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TradeTrackerRenderTypes via Reflection", e);
        }
    }

    private TradeTrackerRenderTypes() {}
}