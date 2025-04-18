package io.github.some_example_name;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
// ... (andere g3d imports bleiben)
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3; // Behalte Vector3
import io.github.some_example_name.Player.Player;
import io.github.some_example_name.Player.PlayerCameraController;
import io.github.some_example_name.Terrain.HeightMapTerrain;
import io.github.some_example_name.Terrain.Terrain;
import io.github.some_example_name.enums.CameraMode; // Importiere deine Enum
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
// ... (andere gltf imports bleiben)
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

// Main implementiert immer noch InputProcessor für das Scrollen und den Moduswechsel
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
    // private FirstPersonCameraController cameraController; // Entfernt

    // Terrain
    private Terrain terrain;
    private Scene terrainScene;

    // Neue Handler-Instanzen
    private Player player;
    private PlayerCameraController playerCameraController;

    @Override
    public void create() {

        Graphics.Monitor currMonitor = Gdx.graphics.getMonitor();
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor);
        if (!Gdx.graphics.setFullscreenMode(displayMode)) {
            // switching to full-screen mode failed
            Gdx.app.error("Graphics", "Fullscreen mode failed");
        }

        // Szene erstellen
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("Models/drive.gltf"));
        playerScene = new Scene(sceneAsset.scene);
        sceneManager = new SceneManager();
        sceneManager.addScene(playerScene);

        // Kamera Setup
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1;
        camera.far = 20000;
        sceneManager.setCamera(camera);
        // Startposition der Kamera wird jetzt vom Controller gehandhabt, aber wir setzen eine initiale Position
        camera.position.set(0, 10, 50f); // Beispielposition, wird im ersten Update überschrieben

        // Input Processor setzen (Main ist immer noch der Processor)
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(this);

        // Licht Setup
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // IBL Setup
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // Skybox Setup
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        playerScene.animationController.setAnimation("driving", -1);

        // Handler Instanzen erstellen
        player = new Player(playerScene);
        playerCameraController = new PlayerCameraController(camera);

        createTerrain();

        // Initiale Kameraposition setzen (optional, nach Erstellung der Handler)
        // Holen der initialen Spielerposition und Winkel nach der Handler-Initialisierung
        playerCameraController.update(player.getCurrentPosition(), player.getAngleBehindPlayer(), 0);


    }

    private void createTerrain() {
        if (terrain != null) {
            terrain.dispose();
            if (terrainScene != null) sceneManager.removeScene(terrainScene); // Sicherstellen, dass terrainScene existiert
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

        // 1. Spieler-Input verarbeiten -> Aktualisiert Spielerposition und -rotation
        player.processInput(deltaTime);

        // 2. Kamera aktualisieren -> Nutzt die neue Spielerposition und Mausbewegung
        playerCameraController.update(player.getCurrentPosition(), player.getAngleBehindPlayer(), deltaTime);

        // Andere Tastenabfragen, die nicht direkt zur Bewegung/Kamera gehören
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            playerScene.animationController.action("left.001", 1, 1f, this, 0.5f);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            createTerrain();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { // Beispiel: Vollbild verlassen
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(1280, 720); // Oder eine andere Standardgröße
            } else {
                // Optional: Zurück zum Vollbild, falls gewünscht
                // Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(Gdx.graphics.getPrimaryMonitor());
                // Gdx.graphics.setFullscreenMode(displayMode);
            }
        }

        // Rendern
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime); // SceneManager muss immer noch updaten (für Animationen etc.)
        sceneManager.render();
    }

    // --- InputProcessor Methoden ---
    // Main behandelt weiterhin die Events und delegiert oder handelt selbst

    @Override
    public boolean keyDown(int keycode) {
        // Kameramodus-Wechsel mit TAB
        if (keycode == Input.Keys.TAB) {
            playerCameraController.toggleCameraMode(player.getAngleBehindPlayer());
            return true; // Event wurde behandelt
        }
        return false; // Event nicht behandelt (oder weiterleiten, falls nötig)
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Delegiere Zoom an den Camera Controller
        return playerCameraController.scrolled(amountY);
    }

    // Die restlichen InputProcessor-Methoden bleiben vorerst leer oder return false
    @Override
    public boolean keyUp(int keycode) { return false; }
    @Override
    public boolean keyTyped(char character) { return false; }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // Mausbewegung wird jetzt direkt im PlayerCameraController.update() über Gdx.input.getDeltaX/Y() abgefragt
        return false;
    }


    // --- AnimationListener Methoden ---
    @Override
    public void onEnd(AnimationController.AnimationDesc animation) { }
    @Override
    public void onLoop(AnimationController.AnimationDesc animation) { }

    // --- Dispose ---
    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        if (terrain != null) terrain.dispose(); // Terrain auch disposen
        // ParticleEffect 'pe' wurde nie initialisiert oder verwendet, daher hier kein dispose nötig
    }
}