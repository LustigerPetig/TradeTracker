package trade.tracker.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.lang.reflect.Method;
import java.util.OptionalDouble;

public final class TradeTrackerRenderTypes extends RenderType {

    public static final RenderType TRADETRACKER_LINES_NO_DEPTH = initRenderType();

    private static RenderType initRenderType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_LINES_SHADER)
                .setLineState(new LineStateShard(OptionalDouble.of(2.0D)))
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(NO_DEPTH_TEST)
                .setCullState(NO_CULL)
                .createCompositeState(false);

        try {
            for (Method method : RenderType.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 5
                        && params[0] == String.class
                        && params[1] == VertexFormat.class
                        && params[2] == VertexFormat.Mode.class
                        && params[3] == int.class
                        && params[4] == RenderType.CompositeState.class) {
                    method.setAccessible(true);
                    return (RenderType) method.invoke(null, "tradetracker_lines_no_depth", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, state);
                } else if (params.length == 7
                        && params[0] == String.class
                        && params[1] == VertexFormat.class
                        && params[2] == VertexFormat.Mode.class
                        && params[3] == int.class
                        && params[4] == boolean.class
                        && params[5] == boolean.class
                        && params[6] == RenderType.CompositeState.class) {
                    method.setAccessible(true);
                    return (RenderType) method.invoke(null, "tradetracker_lines_no_depth", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false, state);
                }
            }
            throw new NoSuchMethodException("RenderType.create method not found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TradeTrackerRenderTypes", e);
        }
    }

    private TradeTrackerRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}