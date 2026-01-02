package com.remag.throughput.block.blockentities.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.remag.throughput.FTBQuestsThroughputAddon;
import com.remag.throughput.block.ThroughputMeterBlock;
import com.remag.throughput.block.blockentities.ThroughputMeterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ThroughputMeterRenderer implements BlockEntityRenderer<ThroughputMeterBlockEntity> {

    private static final float TEXT_SCALE = 0.01f;

    public ThroughputMeterRenderer(BlockEntityRendererProvider.Context ctx) {
        // you don't need anything here yet
    }

    @Override
    public void render(ThroughputMeterBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {

        if (be.getLevel() == null) return;

        Direction facing = be.getBlockState().getValue(ThroughputMeterBlock.FACING);

        pose.pushPose();


        // 1. Move to block center
        pose.translate(0.5, 0.5, 0.5);

        // 2. Rotate to face direction
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // 3. Move to the front face (slightly off to avoid z-fighting)
        pose.translate(0.0, 0.0, 0.501);

        // 4. Flip to text-facing orientation
        // pose.mulPose(Axis.XP.rotationDegrees(180));
        // pose.mulPose(Axis.YP.rotationDegrees(180));

        // pose.scale(1.0f, -1.0f, 1.0f);

        // 5. Move origin to top-left-ish of text area
        pose.translate(0.0, -0.35, 0.0);

        Font font = Minecraft.getInstance().font;

        // --------------------------
        // All text we will render
        // --------------------------
        String throughputText = be.getThroughputDisplay();     // e.g. "42 items/s"
        String taskText       = be.getTaskNameForRender();     // task title
        String progressText   = be.getProgressDisplay();       // "12 / 40"
        float pct             = be.getProgressPercent();       // 0..1


        // Dynamic scaling same as before
        float baseScale = TEXT_SCALE;
        float maxWidthPx = 80f;

        float sThroughput = Math.min(baseScale, (maxWidthPx / font.width(throughputText)) * baseScale);
        float sTask       = Math.min(baseScale, (maxWidthPx / font.width(taskText)) * baseScale);
        float sProgress   = Math.min(baseScale, (maxWidthPx / font.width(progressText)) * baseScale);

        // -----------------------------------
        // Line 1 — throughput ("42 items/s")
        // -----------------------------------
        pose.pushPose();
        pose.scale(sThroughput, -sThroughput, sThroughput);
        drawCentered(font, throughputText, pose, buffers, packedLight);
        pose.popPose();

        pose.translate(0, 10 * baseScale, 0);

        // -------------------------------
        // Line 2 — Progress text ("12 / 40")
        // -------------------------------
        pose.pushPose();
        pose.scale(sProgress, -sProgress, sProgress);
        drawCentered(font, progressText, pose, buffers, packedLight);
        pose.popPose();

        pose.translate(0, 8 * baseScale, 0);

        // -------------------------------
        // Line 3 — Progress Bar ( FIXED )
        // -------------------------------
        pose.pushPose();  // <── isolate bar transforms
        pose.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        renderProgressBar(pose, buffers, pct, packedLight);
        pose.popPose();   // <── restore scale

        pose.translate(0, 55 * baseScale, 0);

        // -------------------------------
        // Line 4 — Task name
        // -------------------------------
        pose.pushPose();
        pose.scale(sTask, -sTask, sTask);
        drawCentered(font, taskText, pose, buffers, packedLight);
        pose.popPose();

        pose.popPose();
    }

    private void drawCentered(Font font, String text,
                              PoseStack pose, MultiBufferSource buffers, int light) {
        float w = font.width(text);
        font.drawInBatch(
                text,
                -w / 2,
                0,
                0xFFFFFFFF,
                false,
                pose.last().pose(),
                buffers,
                Font.DisplayMode.NORMAL,
                0,
                light
        );
    }

    // ==========================
    // Progress bar rendering
    // ==========================
    private void renderProgressBar(PoseStack pose, MultiBufferSource buffers, float pct, int light) {

        // width & height in **text scale space**
        float fullW = 60f;
        float fullH = 8f;
        float w = fullW * Mth.clamp(pct, 0f, 1f);

        Matrix4f mat = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.TEXT_BACKGROUND);

        // Colors (ARGB)
        int bg = 0xFF444444; // dark gray background
        int fg = 0xFF00FF00; // bright green foreground

        // Background bar
        drawQuad(vc, mat, -fullW/2, 0, fullW, fullH, bg, light);

        pose.translate(0.0f, 0.0f, 0.001f);

        // Progress foreground
        drawQuad(vc, mat, -fullW/2, 0, w, fullH, fg, light);
    }

    private void drawQuad(VertexConsumer vc, Matrix4f mat,
                          float x, float y, float w, float h,
                          int color, int light) {

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float nx = 0f, ny = 0f, nz = 1f;

        // Fake UVs (not using a texture, but must provide valid values)
        float u0 = 0f, v0 = 0f;
        float u1 = 1f, v1 = 1f;

        vc.addVertex(mat, x,     y,     0).setColor(r, g, b, a).setUv(u0, v0).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x + w, y,     0).setColor(r, g, b, a).setUv(u1, v0).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x + w, y + h, 0).setColor(r, g, b, a).setUv(u1, v1).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(mat, x,     y + h, 0).setColor(r, g, b, a).setUv(u0, v1).setLight(light).setNormal(nx, ny, nz);
    }
}
