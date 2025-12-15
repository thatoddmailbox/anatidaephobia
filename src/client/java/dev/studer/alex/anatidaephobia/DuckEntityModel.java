package dev.studer.alex.anatidaephobia;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DuckEntityModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart base;

    public DuckEntityModel(ModelPart modelPart) {
        super(modelPart);
        this.base = modelPart.getChild("cube");
    }

    public static LayerDefinition getTexturedModelData() {
        // TODO: make a real model in BlockBench
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        modelPartData.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 12F, -6F, 12F, 12F, 12F), PartPose.rotation(0F, 0F, 0F));
        return LayerDefinition.create(modelData, 64, 64);
    }
}
