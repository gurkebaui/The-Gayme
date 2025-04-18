package io.github.some_example_name;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer; // Import für Debug Drawer
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import io.github.some_example_name.Physiks.CharacterPhysics;
import io.github.some_example_name.Physiks.PhysicsSystem;
import io.github.some_example_name.Physiks.TerrainPhysics;
import io.github.some_example_name.Player.Player;
import io.github.some_example_name.Player.PlayerCameraController;
import io.github.some_example_name.Terrain.HeightMapTerrain;
import io.github.some_example_name.Terrain.Terrain;
import io.github.some_example_name.Terrain.HeightField;
import io.github.some_example_name.enums.CameraMode; // Import für CameraMode Enum
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw; // Import für Debug Drawer

public class Main extends ApplicationAdapter implements AnimationController.AnimationListener, InputProcessor {

    // --- Rendering & Scene Manager ---
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene playerScene;
    private Scene terrainScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private SceneSkybox skybox;
    private DirectionalLightEx light;

    // --- Spiel-Logik ---
    private Player player;
    private PlayerCameraController playerCameraController;
    private Terrain terrain; // Visuelles Terrain-Objekt (HeightMapTerrain)

    // --- Physik ---
    private PhysicsSystem physicsSystem;
    private TerrainPhysics terrainPhysics;
    private CharacterPhysics characterPhysics;
    private HeightField physicsHeightField; // HeightField für die Physik

    // --- Physik Debug Drawing --- NEU
    private btIDebugDraw debugDrawer;
    private ShapeRenderer shapeRenderer;
    private boolean drawDebug = true; // Standardmäßig Debug zeichnen

    // --- Hilfsvariablen ---
    private final Vector3 tmpVec = new Vector3();
    private final Vector3 currentVelocity = new Vector3(); // Wird jetzt von getLinearVelocity() zurückgegeben
    private final Vector3 newVelocity = new Vector3();
    private final Vector3 stopHorizontalVelocity = new Vector3();
    private final Vector3 playerPhysicsPosition = new Vector3();

    // --- Konstanten (ANPASSEN!) ---
    private final String CHARACTER_MODEL_PATH = "Models/drive.gltf";
    private final String HEIGHTMAP_PATH = "textures/heightmap.png";
    private final float TERRAIN_MAX_HEIGHT = 30f;
    // Dimensionen für das physicsHeightField (müssen zu HeightMapTerrain passen!)
    private final float VISUAL_TERRAIN_WIDTH = 100f;  // << ANPASSEN!
    private final float VISUAL_TERRAIN_DEPTH = 100f;  // << ANPASSEN!

    private final float CHARACTER_RADIUS = 0.8f;
    private final float CHARACTER_HEIGHT = 2.0f;
    private final float CHARACTER_MASS = 7090f;
    private final float MOVE_SPEED = 70.0f;
    private final float JUMP_FORCE = 4500f;


    @Override
    public void create() {
        // --- Initialisierung ---
        Bullet.init();
        Gdx.app.log("Main", "Bullet initialized.");

        physicsSystem = new PhysicsSystem();
        Gdx.app.log("Main", "Physics system created.");

        // --- Initialisiere Debug Drawer --- NEU
        shapeRenderer = new ShapeRenderer();
        debugDrawer = new btIDebugDraw() {
            @Override
            public void drawLine(Vector3 from, Vector3 to, Vector3 color) {
                shapeRenderer.setColor(color.x, color.y, color.z, 1);
                shapeRenderer.line(from, to);
            }

            @Override
            public void drawContactPoint(Vector3 PointOnB, Vector3 normalOnB, float distance, int lifeTime, Vector3 color) {
                shapeRenderer.setColor(color.x, color.y, color.z, 1);
                shapeRenderer.point(PointOnB.x, PointOnB.y, PointOnB.z); // Etwas größerer Punkt
            }

            @Override
            public void reportErrorWarning(String warningString) {
                Gdx.app.error("BulletDebugDrawer", warningString);
            }

            @Override
            public void draw3dText(Vector3 location, String textString) { } // Leer

            private int debugMode = DebugDrawModes.DBG_DrawWireframe | DebugDrawModes.DBG_DrawContactPoints;

            @Override
            public void setDebugMode(int debugMode) { this.debugMode = debugMode; }
            @Override
            public int getDebugMode() { return debugMode; }
        };
        physicsSystem.dynamicsWorld.setDebugDrawer(debugDrawer);
        Gdx.app.log("Main", "Bullet Debug Drawer initialized and set.");
        // --- Ende Initialisierung Debug Drawer ---

        // Vollbildmodus (optional)
        Graphics.Monitor currMonitor = Gdx.graphics.getMonitor();
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor);
        if (!Gdx.graphics.setFullscreenMode(displayMode)) {
            Gdx.app.error("Graphics", "Fullscreen mode failed, using windowed.");
            Gdx.graphics.setWindowedMode(1280, 720);
        }

        // SceneManager erstellen
        sceneManager = new SceneManager();

        // Kamera Setup
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 1000f;
        sceneManager.setCamera(camera);
        camera.position.set(0, 5, 10f);

        // Input Processor setzen
        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);

        // Licht Setup (IBL & Directional)
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);
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
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        // --- Spieler laden ---
        sceneAsset = new GLTFLoader().load(Gdx.files.internal(CHARACTER_MODEL_PATH));
        playerScene = new Scene(sceneAsset.scene);
        sceneManager.addScene(playerScene);
        if (playerScene.modelInstance.animations.size > 0) {
            playerScene.animationController.setAnimation(playerScene.modelInstance.animations.first().id, -1);
            Gdx.app.log("Main", "Started player animation: " + playerScene.modelInstance.animations.first().id);
        } else {
            Gdx.app.log("Main", "Player model has no animations.");
        }

        // --- Handler Instanzen erstellen ---
        player = new Player(playerScene);
        playerCameraController = new PlayerCameraController(camera);

        // --- Terrain erstellen (Visuell & Physik) ---
        createTerrain(); // Stellt sicher, dass beide erstellt werden

        // --- Charakter Physik erstellen ---
        if (playerScene.modelInstance != null) {
            float startHeight = CHARACTER_HEIGHT * 1.5f; // Default, falls Terrain fehlt
            Vector3 basePosition = new Vector3(); // Startpunkt (X,Y,Z) des Terrains (Vertex 0,0)

            if (physicsHeightField != null) {
                physicsHeightField.getPositionAt(basePosition, 0, 0);
                startHeight = basePosition.y + CHARACTER_HEIGHT * 1.5f;
                Gdx.app.log("Main", "Terrain height at vertex (0,0) from HeightField: " + basePosition.y);
            } else {
                startHeight += TERRAIN_MAX_HEIGHT; // Addiere max Höhe, wenn kein Heightfield da
                //Gdx.app.warn("Main", "physicsHeightField is null, using fallback start height.");
                basePosition.set(0, 0, 0); // Annahme: Start bei Weltursprung
            }

            // Offsets für die Startposition definieren
            float offsetX = 10.0f; // Beispiel: Nach rechts
            float offsetZ = 15.0f; // Beispiel: Nach vorne

            // Finale Startposition berechnen
            Vector3 startPos = new Vector3(
                    basePosition.x + offsetX,
                    startHeight, // Höhe über Vertex (0,0) + Puffer
                    basePosition.z + offsetZ
            );

            characterPhysics = new CharacterPhysics(
                    physicsSystem,
                    playerScene.modelInstance,
                    CHARACTER_RADIUS,
                    CHARACTER_HEIGHT,
                    CHARACTER_MASS,
                    startPos
            );
            Gdx.app.log("Main", "Character physics created at (modified): " + startPos);
        } else {
            Gdx.app.error("Main", "playerScene.modelInstance is NULL. Cannot create CharacterPhysics!");
        }

        // Initiale Kameraposition setzen
        if (characterPhysics != null) {
            characterPhysics.updateGraphicsTransform();
            playerPhysicsPosition.set(characterPhysics.modelInstance.transform.getTranslation(tmpVec));
            playerCameraController.update(playerPhysicsPosition, player.getAngleBehindPlayer(), 0);
            Gdx.app.log("Main", "Initial camera update set.");
        }
    }

    // In Main.java

    private void createTerrain() {
        Gdx.app.log("Main", "Creating terrain...");
        // --- Altes Terrain entfernen (Physik, dann Visuell) ---
        if (terrainPhysics != null) {
            // Wichtig: Erst aus der Welt entfernen, bevor disposed wird, um Fehler zu vermeiden
            if (physicsSystem != null && physicsSystem.dynamicsWorld != null && terrainPhysics.body != null) {
                physicsSystem.dynamicsWorld.removeRigidBody(terrainPhysics.body);
                Gdx.app.log("Main", "Removed old terrain body from physics world.");
            }
            terrainPhysics.dispose();
            terrainPhysics = null;
            Gdx.app.log("Main", "Disposed old terrain physics.");
        }
        if (physicsHeightField != null) {
            physicsHeightField.dispose();
            physicsHeightField = null;
            Gdx.app.log("Main", "Disposed old physics HeightField object.");
        }
        if (terrainScene != null) {
            // Sicherstellen, dass das ModelInstance auch aus dem SceneManager entfernt wird
            sceneManager.removeScene(terrainScene); // SceneManager sollte sich um die Instanz kümmern
            terrainScene = null; // Referenz löschen
            Gdx.app.log("Main", "Removed old terrain scene from SceneManager.");

        }
        if (terrain != null) {
            // Das Terrain-Objekt selbst (und sein Mesh/Material) disposen
            terrain.dispose();
            terrain = null;
            Gdx.app.log("Main", "Disposed old visual terrain object.");
        }

        // --- Neues Visuelles Terrain ---
        Pixmap visualPixmap = null;
        try {
            visualPixmap = new Pixmap(Gdx.files.internal(HEIGHTMAP_PATH));
            Gdx.app.log("Main", "Loaded heightmap Pixmap for visual terrain.");

            // Erstelle das visuelle Terrain.
            // ANNAHME: HeightMapTerrain disposed die Pixmap intern!
            terrain = new HeightMapTerrain(visualPixmap, TERRAIN_MAX_HEIGHT);

            // Füge die ModelInstance des visuellen Terrains zur Szene hinzu
            terrainScene = new Scene(terrain.getModelInstance());
            sceneManager.addScene(terrainScene);
            Gdx.app.log("Main", "Created visual terrain (HeightMapTerrain) and added to SceneManager.");

        } catch (Exception e) {
            Gdx.app.error("Main", "Error creating visual terrain", e);
            // Sicherstellen, dass die Pixmap freigegeben wird, falls nicht von HeightMapTerrain getan
            if (visualPixmap != null && !visualPixmap.isDisposed()) {
                visualPixmap.dispose();
                Gdx.app.error("Main", "Disposed visualPixmap after visual terrain creation failed.");
            }
            return; // Nicht weitermachen ohne visuelles Terrain
        }
        // visualPixmap sollte jetzt disposed sein (entweder durch HeightMapTerrain oder im catch-Block)

        // --- Neues Physikalisches Terrain ---
        Pixmap physicsPixmap = null;
        try {
            // 1. Pixmap für PHYSIKALISCHES Terrain ERNEUT laden
            physicsPixmap = new Pixmap(Gdx.files.internal(HEIGHTMAP_PATH));
            Gdx.app.log("Main", "Loaded heightmap Pixmap AGAIN for physics terrain.");

            // 2. HeightField für die Physik erstellen (verwendet die NEUE Pixmap)
            //    Dieses Objekt hält die Höhendaten und berechnet Vertex-Positionen.
            physicsHeightField = new HeightField(
                    true, // isStatic (für das interne Mesh, nicht relevant für die Physik-Daten)
                    physicsPixmap, // NEUE Pixmap übergeben
                    true, // smooth (beeinflusst Normalen im HeightField, nicht die Physik-Shape direkt)
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
            );
            Gdx.app.log("Main", "Created physics HeightField instance.");

            // 3. >>> ANPASSUNG HIER <<<
            // Konfiguriere die Dimensionen und Position des physicsHeightField,
            // damit es EXAKT dem visuellen Terrain entspricht.
            // Finde die tatsächliche Größe und Position deines `HeightMapTerrain`-Meshes heraus!
            // Beispielwerte (DU MUSST DIESE ANPASSEN!):
            final float actualVisualWidth = 1200.0f;  // Z.B. Breite von -50 bis +50
            final float actualVisualDepth = 1200.0f;  // Z.B. Tiefe von -50 bis +50
            final float visualBaseY = 13.0f;      // Y-Position der Basis des visuellen Terrains

            // Setze die Eckpunkte des physicsHeightField entsprechend.
            //Wenn dein visuelles Terrain bei (0,0,0) beginnt und nach +X/+Z geht:
             physicsHeightField.corner00.set(400,               visualBaseY, 400);
             physicsHeightField.corner10.set(actualVisualWidth, visualBaseY, 400);
             physicsHeightField.corner01.set(400,               visualBaseY, actualVisualDepth);
             physicsHeightField.corner11.set(actualVisualDepth, visualBaseY, actualVisualDepth);

            // Wenn dein visuelles Terrain um (0, visualBaseY, 0) zentriert ist:
            //physicsHeightField.corner00.set(-actualVisualWidth / 2f, visualBaseY, -actualVisualDepth / 2f);
            //physicsHeightField.corner10.set( actualVisualWidth / 2f, visualBaseY, -actualVisualDepth / 2f);
            //physicsHeightField.corner01.set(-actualVisualWidth / 2f, visualBaseY,  actualVisualDepth / 2f);
            //physicsHeightField.corner11.set( actualVisualWidth / 2f, visualBaseY,  actualVisualDepth / 2f);

            // Setze die Höhenskalierung (Magnitude)
            physicsHeightField.magnitude.set(0, TERRAIN_MAX_HEIGHT, 0);

            // Berechne die internen Vertex-Positionen neu basierend auf den Ecken/Magnitude
            physicsHeightField.update();
            Gdx.app.log("Main", "Configured physics HeightField dimensions to match visual (Assumed W:" + actualVisualWidth + ", D:" + actualVisualDepth + ")");
            Gdx.app.log("Main", "Physics HeightField corner00 set to: " + physicsHeightField.corner00);


            // 4. TerrainPhysics erstellen (verwendet das konfigurierte physicsHeightField)
            //    TerrainPhysics liest die konfigurierten Daten (corners, magnitude, data)
            //    und erstellt daraus das btHeightfieldTerrainShape mit korrekter Skalierung/Position.
            terrainPhysics = new TerrainPhysics(physicsSystem, physicsHeightField);
            Gdx.app.log("Main", "Created TerrainPhysics using configured HeightField.");

        } catch (Exception e) {
            Gdx.app.error("Main", "Error creating physics terrain", e);
            // Aufräumen im Fehlerfall
            if (physicsHeightField != null) physicsHeightField.dispose();
            physicsHeightField = null;
            // terrainPhysics wurde nicht erstellt oder muss disposed werden, falls Fehler nach Erstellung auftrat
            if (terrainPhysics != null) terrainPhysics.dispose();
            terrainPhysics = null;
        } finally {
            // 5. Die zweite Pixmap (für Physik) freigeben, da sie jetzt im HeightField verarbeitet ist
            if (physicsPixmap != null && !physicsPixmap.isDisposed()) {
                physicsPixmap.dispose();
                Gdx.app.log("Main", "Disposed physics heightmap Pixmap.");
            }
        }

        Gdx.app.log("Main", "Terrain creation routine finished.");
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        deltaTime = Math.min(deltaTime, 1f / 30f); // Max Timestep für Physik

        // Input für Physik verarbeiten
        handleInput();

        // Physik-Welt aktualisieren
        if (physicsSystem != null) {
            physicsSystem.update(deltaTime);
        }

        // Grafik an Physik anpassen
        if (characterPhysics != null) {
            characterPhysics.updateGraphicsTransform();
        }

        // Kamera aktualisieren
        if (characterPhysics != null && playerCameraController != null && player != null) {
            playerPhysicsPosition.set(characterPhysics.modelInstance.transform.getTranslation(tmpVec));
            playerCameraController.update(playerPhysicsPosition, player.getAngleBehindPlayer(), deltaTime);
        }

        // --- SceneManager und Rendern ---
        sceneManager.update(deltaTime);
        Gdx.gl.glClearColor(0.3f, 0.5f, 0.7f, 1f); // Himmelblau als Hintergrund
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.render();

        // --- Physik Debug zeichnen (wenn aktiviert) --- NEU
        if (drawDebug && debugDrawer != null && shapeRenderer != null && physicsSystem != null) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            physicsSystem.dynamicsWorld.debugDrawWorld();
            shapeRenderer.end();
        }
        // --- Ende Physik Debug zeichnen ---

        // --- Andere Tastenabfragen ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            createTerrain();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleFullscreen();
        }
        // if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { // Wird in handleInput() für Sprung genutzt
        //     Gdx.app.log("Main", "Space pressed - potential action trigger");
        // }
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
            Gdx.app.log("Main", "Switched to windowed mode.");
            Gdx.input.setCursorCatched(false);
        } else {
            Graphics.Monitor currMonitor = Gdx.graphics.getPrimaryMonitor();
            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor);
            if (Gdx.graphics.setFullscreenMode(displayMode)) {
                Gdx.app.log("Main", "Switched to fullscreen mode.");
                Gdx.input.setCursorCatched(true);
            } else {
                Gdx.app.error("Graphics", "Fullscreen mode failed.");
            }
        }
    }

    private void handleInput() {
        if (characterPhysics == null || characterPhysics.body == null) return;

        tmpVec.set(0, 0, 0);
        Vector3 moveDirection = tmpVec;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveDirection.add(camera.direction.x, 0, camera.direction.z);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveDirection.sub(camera.direction.x, 0, camera.direction.z);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveDirection.add(tmpVec.set(camera.direction).crs(camera.up).nor().x, 0, tmpVec.z);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveDirection.sub(tmpVec.set(camera.direction).crs(camera.up).nor().x, 0, tmpVec.z);

        moveDirection.nor();

        if (!moveDirection.isZero()) {
            Vector3 currentLinearVelocity = characterPhysics.body.getLinearVelocity(); // Erzeugt Garbage!
            newVelocity.set(moveDirection).scl(MOVE_SPEED);
            newVelocity.y = currentLinearVelocity.y;
            characterPhysics.body.setLinearVelocity(newVelocity);
            characterPhysics.body.activate();
        } else {
            Vector3 currentLinearVelocity = characterPhysics.body.getLinearVelocity(); // Erzeugt Garbage!
            stopHorizontalVelocity.set(0, currentLinearVelocity.y, 0);
            characterPhysics.body.setLinearVelocity(stopHorizontalVelocity);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Vector3 currentLinearVelocity = characterPhysics.body.getLinearVelocity(); // Erzeugt Garbage!
            if (Math.abs(currentLinearVelocity.y) < 0.5f) {
                characterPhysics.jump(JUMP_FORCE); // jump() in CharacterPhysics sollte applyCentralImpulse verwenden
                Gdx.app.log("Main", "Jump initiated with force: " + JUMP_FORCE);
            } else {
                Gdx.app.log("Main", "Jump blocked (in air). Current Y velocity: " + currentLinearVelocity.y);
            }
        }
    }

    // --- InputProcessor Methoden ---
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.TAB) {
            if (playerCameraController != null && player != null) {
                playerCameraController.toggleCameraMode(player.getAngleBehindPlayer());
                CameraMode currentMode = playerCameraController.getCameraMode();
                boolean captureCursor = (currentMode == CameraMode.FREE_LOOK);
                Gdx.input.setCursorCatched(captureCursor);
                Gdx.app.log("Main", "Toggled camera mode to: " + currentMode + ", Cursor captured: " + captureCursor);
                return true;
            }
        }
        // Debug Drawing Toggle - NEU
        if (keycode == Input.Keys.F3) {
            drawDebug = !drawDebug;
            Gdx.app.log("Main", "Debug Drawing toggled: " + drawDebug);
            return true;
        }
        return false;
    }

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
    public boolean mouseMoved(int screenX, int screenY) { return false; }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (playerCameraController != null) return playerCameraController.scrolled(amountY);
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
        Gdx.app.log("Main", "Disposing resources...");

        // Reihenfolge: Physik -> Debug Drawer -> Szenen/Assets -> IBL/Skybox -> Visuelles Terrain

        // Physik
        if (characterPhysics != null) characterPhysics.dispose();
        if (terrainPhysics != null) terrainPhysics.dispose();
        if (physicsHeightField != null) physicsHeightField.dispose(); // Das separate HeightField Objekt
        if (physicsSystem != null) physicsSystem.dispose();
        Gdx.app.log("Main", "Physics disposed.");

        // Debug Drawer Ressourcen - NEU
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (debugDrawer != null) debugDrawer.dispose();
        Gdx.app.log("Main", "Disposed debug drawer resources.");

        // SceneManager und Assets
        if (sceneManager != null) sceneManager.dispose(); // Sollte enthaltene Scenes/Ressourcen disposen
        if (sceneAsset != null) sceneAsset.dispose();
        Gdx.app.log("Main", "SceneManager and player asset disposed.");

        // IBL Texturen und Skybox
        if (environmentCubemap != null) environmentCubemap.dispose();
        if (diffuseCubemap != null) diffuseCubemap.dispose();
        if (specularCubemap != null) specularCubemap.dispose();
        if (brdfLUT != null) brdfLUT.dispose();
        if (skybox != null) skybox.dispose();
        Gdx.app.log("Main", "IBL and skybox disposed.");

        // Visuelles Terrain
        if (terrain != null) terrain.dispose(); // Das HeightMapTerrain Objekt
        Gdx.app.log("Main", "Visual terrain disposed.");

        Gdx.app.log("Main", "Dispose complete.");
    }
}