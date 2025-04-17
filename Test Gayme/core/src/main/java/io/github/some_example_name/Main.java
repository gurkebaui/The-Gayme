package io.github.some_example_name;





import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import io.github.some_example_name.Terrain.HeightMapTerrain;
import io.github.some_example_name.Terrain.Terrain;
import io.github.some_example_name.enums.CameraMode;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class Main extends ApplicationAdapter implements AnimationController.AnimationListener, InputProcessor {
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene playerScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private FirstPersonCameraController cameraController;
    private ParticleEffect pe;



    //player movement
    float speed = 50;
    float rotationSpeed = 40f;
    private Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    private final Vector3 currentPosition = new Vector3();

    // Camera
    private CameraMode cameraMode= CameraMode.BEHIND_PLAYER;
    private float camPitch = Settings.CAMERA_START_PITCH;
    private float distanceFromPlayer = 35f;
    private float angleAroundPlayer = 0f;
    private float angleBehindPlayer = 0f;

    // Terrainn
    private Terrain terrain;
    private Scene terrainScene;




    @Override
    public void create() {

        Graphics.Monitor currMonitor = Gdx.graphics.getMonitor();
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor);
        if(!Gdx.graphics.setFullscreenMode(displayMode)) {
            // switching to full-screen mode failed
        }







        // create scene
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("Models/drive.gltf"));
        playerScene = new Scene(sceneAsset.scene);
        sceneManager = new SceneManager();
        sceneManager.addScene(playerScene);

        // setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
                camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                camera.near =1;
                camera.far = 20000;
                sceneManager.setCamera(camera);
                camera.position.set(0,0,4f);

                Gdx.input.setCursorCatched(true);
                Gdx.input.setInputProcessor(this);



        //cameraController = new FirstPersonCameraController(camera);
        //Gdx.input.setInputProcessor(cameraController);

        // setup light
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        playerScene.animationController.setAnimation("driving",-1);

        createTerrain();

    }

    private void createTerrain() {
        if (terrain != null) {
            terrain.dispose();
            sceneManager.removeScene(terrainScene);
        }

        terrain = new HeightMapTerrain(new Pixmap(Gdx.files.internal("textures/heightmap.png")), 30f);
        terrainScene = new Scene(terrain.getModelInstance());
        sceneManager.addScene(terrainScene);
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }


    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        //cameraController.update();

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            playerScene.animationController.action("left.001", 1,1f,this,0.5f);


        }

        // processes Inbput lol
        processInput(deltaTime);
        updateCamera();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            createTerrain();
        }


        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime);
        sceneManager.render();
    }

    private void updateCamera() {
        float horDistance = calculateHorizontalDistance(distanceFromPlayer);
        float vertDistance = calculateVerticalDistance(distanceFromPlayer);

        calculatePitch();
        calculateAngleAroundPlayer();
        calculateCameraPosition(currentPosition, horDistance, vertDistance);

        camera.up.set(Vector3.Y);
        camera.lookAt(currentPosition);
        camera.update();
    }

    private void calculateCameraPosition(Vector3 currentPosition, float horDistance, float vertDistance) {
        float offsetX = (float) (horDistance * Math.sin(Math.toRadians(angleAroundPlayer)));
        float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(angleAroundPlayer)));

        camera.position.x = currentPosition.x - offsetX;
        camera.position.z = currentPosition.z - offsetZ;
        camera.position.y = currentPosition.y + vertDistance;
    }

    private void calculateAngleAroundPlayer() {
        if (cameraMode == CameraMode.FREE_LOOK) {
            float angleChange = Gdx.input.getDeltaX() * Settings.CAMERA_ANGLE_AROUND_PLAYER_FACTOR;
            angleAroundPlayer -= angleChange;
        } else {
            angleAroundPlayer = angleBehindPlayer;
        }
    }

    private void calculatePitch() {
        float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
        camPitch -= pitchChange;

        if (camPitch < Settings.CAMERA_MIN_PITCH)
            camPitch = Settings.CAMERA_MIN_PITCH;
        else if (camPitch > Settings.CAMERA_MAX_PITCH)
            camPitch = Settings.CAMERA_MAX_PITCH;
    }

    private float calculateVerticalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateHorizontalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
    }


    public void processInput(float deltaTime){
        // Update the player transform
        playerTransform.set(playerScene.modelInstance.transform);


        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveTranslation.z += speed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveTranslation.z -= speed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerTransform.rotate(Vector3.Y, rotationSpeed * deltaTime);
            angleBehindPlayer += rotationSpeed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerTransform.rotate(Vector3.Y, -rotationSpeed * deltaTime);
            angleBehindPlayer -= rotationSpeed * deltaTime;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            switch (cameraMode) {

                case FREE_LOOK:
                    cameraMode = CameraMode.BEHIND_PLAYER;
                    angleAroundPlayer = angleBehindPlayer;
                    break;
                case BEHIND_PLAYER:
                    cameraMode = CameraMode.FREE_LOOK;
                    break;
            }
        }




        // Apply the move translation to the transform
        playerTransform.translate(moveTranslation);

        // Set the modified transform
        playerScene.modelInstance.transform.set(playerTransform);

        // Update vector position
        playerScene.modelInstance.transform.getTranslation(currentPosition);

        // Clear the move translation out
        moveTranslation.set(0,0,0);
    }



    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }

    @Override
    public void onEnd(AnimationController.AnimationDesc animation) {

    }

    @Override
    public void onLoop(AnimationController.AnimationDesc animation) {

    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float zoomLevel = amountY * Settings.CAMERA_ZOOM_LEVEL_FACTOR;
        distanceFromPlayer += zoomLevel;
        if (distanceFromPlayer < Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER)
            distanceFromPlayer = Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER;
        return false;
    }
}
