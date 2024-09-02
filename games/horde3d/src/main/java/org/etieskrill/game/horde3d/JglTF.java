package org.etieskrill.game.horde3d;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v2.GltfModelCreatorV2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class JglTF {

    public static void main(String[] args) throws IOException {
        var reader = new GltfAssetReader();
        var genericAsset = reader.read(Path.of("games/horde/src/main/resources/models/vampire.glb"));

        var model = GltfModelCreatorV2.create(new GltfAssetV2((GlTF) genericAsset.getGltf(), genericAsset.getBinaryData()));

        var meshPrimitives = model.getMeshModel(0).getMeshPrimitiveModels().getFirst();
        var boneAccessor = meshPrimitives.getAttributes().get("JOINTS_0");
        var weightAccessor = meshPrimitives.getAttributes().get("WEIGHTS_0");

        System.out.println("num vertices: " + meshPrimitives.getAttributes().get("POSITION").getCount());
        System.out.println("num indices: " + meshPrimitives.getIndices().getCount());

        ByteBuffer boneBuffer = boneAccessor.getAccessorData().createByteBuffer();
        System.out.println(boneAccessor.getElementType());
        System.out.println(boneAccessor.getComponentType());
        System.out.println(boneAccessor.getComponentDataType());
        ByteBuffer weightBuffer = weightAccessor.getAccessorData().createByteBuffer();
        System.out.println("len: " + boneAccessor.getCount() + " and " + weightAccessor.getCount());
        System.out.println(weightAccessor.getElementType());
        System.out.println(weightAccessor.getComponentDataType());
        for (int i = 0; i < boneAccessor.getCount(); i++) {
            System.out.print("bone: " + boneBuffer.get() + ", weight: " + weightBuffer.getFloat());
            if (i % 4 == 3) System.out.println();
            else System.out.print(", ");
        }
    }

}
