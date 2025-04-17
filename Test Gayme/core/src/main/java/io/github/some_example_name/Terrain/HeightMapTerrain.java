package io.github.some_example_name.Terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

/**
 * inspiration von JamesTKhan
 *
 */
public class HeightMapTerrain extends Terrain {

    private final HeightField field;
    //macht map
    public HeightMapTerrain(Pixmap data, float magnitude) {
        this.size = 800;
        this.width = data.getWidth();
        this.heightMagnitude = magnitude;
        //plaziert map
        field = new HeightField(true, data, true, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        data.dispose();
        field.corner00.set(0, 0, 0);
        field.corner10.set(size, 0, 0);
        field.corner01.set(0, 0, size);
        field.corner11.set(size, 0, size);
        field.magnitude.set(0f, magnitude, 0f);
        field.update();
        //gibt map textur
        Texture texture = new Texture(Gdx.files.internal("textures/sand-dunes1_albedo.png"), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        //größe von Textur
        PBRTextureAttribute textureAttribute = PBRTextureAttribute.createBaseColorTexture(texture);
        textureAttribute.scaleU = 40f;
        textureAttribute.scaleV = 40f;
        //macht Material
        Material material = new Material();
        material.set(textureAttribute);
        //macht alles zu nem rendderbaren objekt
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("terrain", field.mesh, GL20.GL_TRIANGLES, material);
        modelInstance = new ModelInstance(mb.end());
    }

    @Override
    public void dispose() {
        //macht ram frei
        field.dispose();
    }
}