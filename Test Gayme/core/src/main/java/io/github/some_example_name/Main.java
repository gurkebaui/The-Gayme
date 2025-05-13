package io.github.some_example_name; // Definiert, zu welchem Paket diese Klasse gehört. Wichtig für die Organisation.

// --- LibGDX Core Imports ---
// Diese Klassen sind grundlegend für fast jede LibGDX Anwendung.
import com.badlogic.gdx.*; // Stellt grundlegende App-Funktionen bereit (Input, Graphics, Files etc.)
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*; // Klassen für Grafik (Farben, Texturen, Kamera, Pixmap etc.)
import com.badlogic.gdx.graphics.g3d.Environment; // Enthält Informationen über die 3D-Umgebung (Licht, Schatten etc.) - Wird vom SceneManager verwendet
import com.badlogic.gdx.graphics.g3d.ModelInstance; // Eine konkrete Instanz eines 3D-Modells in der Welt
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute; // Attribut für Umgebungslichtfarbe
import com.badlogic.gdx.graphics.g3d.utils.AnimationController; // Steuert Animationen von ModelInstances
import com.badlogic.gdx.graphics.glutils.ShapeRenderer; // Zum Zeichnen einfacher Formen (Linien, Rechtecke) - für Debug Drawing
import com.badlogic.gdx.math.Matrix4; // Eine 4x4 Matrix, oft für Transformationen (Position, Rotation, Skalierung) verwendet
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3; // Ein Vektor mit 3 Komponenten (x, y, z) für Positionen, Richtungen, etc.
import com.badlogic.gdx.math.collision.BoundingBox; // Definiert einen achsenparallelen Quader (nützlich für Kollisionsabschätzung etc.)
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable; // Interface für Klassen, die Ressourcen freigeben müssen (z.B. Texturen, Meshes)

// --- Physik (Bullet Wrapper) Imports ---
// Klassen spezifisch für die Bullet Physik-Engine Anbindung in LibGDX
import com.badlogic.gdx.physics.bullet.Bullet; // Hauptklasse zum Initialisieren der Bullet-Bibliothek
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw; // Interface für das Debug Drawing der Physikwelt

// --- GLTF Scene Management Imports ---
// Klassen für das Laden und Managen von GLTF 3D-Szenen (ein modernes 3D-Format)
import io.github.some_example_name.Physiks.StaticObjectPhysics;
import net.mgsx.gltf.loaders.gltf.GLTFLoader; // Lädt GLTF-Dateien
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute; // Spezielle Attribute für Physically Based Rendering (PBR) mit Cubemaps
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;  // PBR-Attribute mit Texturen
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx; // Eine erweiterte Version eines gerichteten Lichts (für PBR)
import net.mgsx.gltf.scene3d.scene.Scene; // Repräsentiert eine geladene Szene (kann Modelle, Lichter etc. enthalten)
import net.mgsx.gltf.scene3d.scene.SceneAsset; // Das Ergebnis des Ladens einer GLTF-Datei (enthält die Szene und andere Assets)
import net.mgsx.gltf.scene3d.scene.SceneManager; // Verwaltet das Rendern und Updaten von mehreren Szenen, Kamera, Umgebung etc.
import net.mgsx.gltf.scene3d.scene.SceneSkybox; // Zeichnet eine Skybox (Hintergrundhimmel) basierend auf einer Cubemap
import net.mgsx.gltf.scene3d.utils.IBLBuilder; // Hilfsklasse zum Erstellen von Image Based Lighting (IBL) Texturen (Cubemaps)

// --- Deine Projekt-spezifischen Imports ---
// Klassen, die du selbst erstellt hast oder die spezifisch für dein Spiel sind.
import io.github.some_example_name.Physiks.CharacterPhysics; // Deine Klasse für die Spieler-Physik
import io.github.some_example_name.Physiks.PhysicsSystem;   // Deine Klasse für das Physik-System (Welt, Konfiguration)
import io.github.some_example_name.Physiks.TerrainPhysics;  // Deine Klasse für die Terrain-Physik
import io.github.some_example_name.Player.Player; // Deine Klasse, die Spieler-Zustand etc. hält
import io.github.some_example_name.Player.PlayerCameraController; // Deine Klasse für die Kamera-Steuerung
import io.github.some_example_name.Terrain.HeightMapTerrain; // Deine Klasse für das visuelle Terrain
import io.github.some_example_name.Terrain.Terrain; // Basis-Interface/Klasse für dein Terrain (optional, je nach Design)
import io.github.some_example_name.Terrain.HeightField; // Deine Klasse, die die Heightmap-Daten für die Physik aufbereitet
import io.github.some_example_name.enums.CameraMode; // Deine Enum für verschiedene Kameramodi
// *** NEUE IMPORTS für die ausgelagerten Klassen ***
import io.github.some_example_name.Player.PlayerInputHandler; // Der neue Handler für Spielereingaben
import io.github.some_example_name.Terrain.TerrainManager; // Der neue Manager für das Terrain

/**
 * Die Hauptklasse deines Spiels. Sie initialisiert alle Systeme,
 * verwaltet den Haupt-Game-Loop (rendern, updaten) und kümmert sich um das Aufräumen.
 * <p>
 * Erweitert ApplicationAdapter: Eine Basisklasse von LibGDX, die die wichtigsten Methoden
 * (create, render, dispose, resize etc.) bereitstellt.
 * <p>
 * Implementiert AnimationListener: (Optional) Falls du auf Animations-Events reagieren willst.
 * Implementiert InputProcessor: Damit die Klasse direkt auf Tastatur- und Mauseingaben reagieren kann.
 */
public class Main extends ApplicationAdapter implements AnimationController.AnimationListener, InputProcessor {

    // --- Rendering & Scene Manager ---
    // Diese Objekte sind für das Anzeigen der 3D-Welt zuständig.
    private SceneManager sceneManager;      // Rendert die GLTF-Szenen, Skybox, etc.
    private SceneAsset sceneAsset;          // Hält die geladenen Daten der Spieler-GLTF-Datei
    private Scene playerScene;// Die spezifische Szene des Spielers aus der GLTF-Datei
    // terrainScene wird jetzt vom TerrainManager verwaltet
    private PerspectiveCamera camera;       // Die virtuelle Kamera, durch die wir die Welt sehen
    private Cubemap diffuseCubemap;         // Texturen für Image Based Lighting (diffuse Reflexionen)
    private Cubemap environmentCubemap;     // Texturen für Image Based Lighting (Umgebungsreflexionen)
    private Cubemap specularCubemap;        // Texturen für Image Based Lighting (spiegelnde Reflexionen)
    private Texture brdfLUT;                // Eine Lookup-Textur für PBR-Berechnungen
    private SceneSkybox skybox;             // Das Objekt, das die Skybox zeichnet
    private DirectionalLightEx light;// Das Haupt-Sonnenlicht in der Szene
    private SceneAsset houseSceneAsset;     // Asset für das HAUS-Modell (wird einmal geladen)
    private Array<Scene> houseVisualScenes = new Array<>(); // Hält die einzelnen visuellen Szenen der platzierten Häuser
    private Array<StaticObjectPhysics> housePhysicsBodies = new Array<>(); // Hält die Physik-Körper der Häuser
    private SceneAsset kaktiSceneAsset;     // Asset für das HAUS-Modell (wird einmal geladen)
    private Array<Scene> kaktiVisualScenes = new Array<>(); // Hält die einzelnen visuellen Szenen der platzierten Häuser
    private Array<StaticObjectPhysics> kaktiPhysicsBodies = new Array<>(); // Hält die Physik-Körper der Häuser


    // --- Spiel-Logik ---
    // Objekte, die den Zustand und das Verhalten von Spielelementen steuern.
    private Player player;                     // Hält Spielerdaten (z.B. aktueller Zustand, Winkel)
    private PlayerCameraController playerCameraController;
    private final Vector3 tmpVec2 = new Vector3();
    private final Quaternion tmpQuat = new Quaternion();// Steuert die Bewegung und Ausrichtung der Kamera
    private final Vector3 tmpVec = new Vector3(); // Für die berechnete Rotation
    private final Matrix4 tmpMat = new Matrix4();
    private AssetManager assetManager;

    // --- Physik ---
    // Objekte für die Physiksimulation mit Bullet.
    private PhysicsSystem physicsSystem;        // Verwaltet die Physik-Welt (Schwerkraft, Kollisionserkennung etc.)
    // terrainPhysics wird jetzt vom TerrainManager verwaltet
    private CharacterPhysics characterPhysics;   // Verwaltet den Physik-Körper des Spielers

    // --- NEUE Handler-Klassen ---
    // Ausgelagerte Logik für bessere Organisation.
    private PlayerInputHandler playerInputHandler; // Verarbeitet jetzt WASD, Sprung etc.
    private TerrainManager terrainManager;     // Verwaltet jetzt Erstellung und Lebenszyklus des Terrains

    // --- Physik Debug Drawing ---
    // Hilfsmittel zur Visualisierung der unsichtbaren Physik-Formen.
    private btIDebugDraw debugDrawer;           // Das Bullet-Interface zum Debug-Zeichnen
    private ShapeRenderer shapeRenderer;        // LibGDX-Klasse zum Zeichnen der Debug-Linien
    private boolean drawDebug = true;           // Schalter, um Debug-Zeichnen an/aus zu machen (z.B. mit F3)

    // --- Hilfsvariablen ---
    // Temporäre Objekte, um im render-Loop nicht ständig neue Objekte erstellen zu müssen (vermeidet Garbage Collection).              // Allgemeiner temporärer Vektor
    private final Vector3 playerPhysicsPosition = new Vector3(); // Zum Abfragen der Spielerposition aus der Physik

    // --- Konstanten (ANPASSEN!) ---
    // Konfigurationswerte für das Spiel. Gut, sie hier zentral zu haben.
    private final String CHARACTER_MODEL_PATH = "Models/bean.gltf";  // Pfad zur GLTF-Datei des Spielers
    private final String HEIGHTMAP_PATH = "textures/heightmap.png"; // Pfad zur Höhentextur für das Terrain
    private final float TERRAIN_MAX_HEIGHT = 30f;                   // Maximale Höhe, die das Terrain erreichen kann
    // Dimensionen des *visuellen* Terrains (wichtig für die Physik-Anpassung!)
    private final float VISUAL_TERRAIN_WIDTH = 100f;  // << PASSE DIES AN die tatsächliche Breite deines Terrains an!
    private final float VISUAL_TERRAIN_DEPTH = 100f;  // << PASSE DIES AN die tatsächliche Tiefe deines Terrains an!

    // Charakter-Physik Werte
    private final float CHARACTER_RADIUS = 0.8f;    // Radius der Kollisionskapsel
    private final float CHARACTER_HEIGHT = 2.0f;    // Höhe des zylindrischen Teils der Kapsel
    private final float CHARACTER_MASS = 70f;       // Masse des Charakters in kg
    // Steuerungs-Werte
    private final float MOVE_SPEED = 7.0f;          // Bewegungsgeschwindigkeit
    private final float JUMP_FORCE = 450f;          // Kraft des Sprungimpulses


    /**
     * Wird EINMAL beim Start der Anwendung aufgerufen.
     * Hier werden alle Ressourcen geladen und Objekte initialisiert.
     */


    @Override
    public void create() {
        // --- Grundlegende Initialisierungen ---
        Bullet.init(); // SEHR WICHTIG: Muss vor jeder Bullet-Nutzung aufgerufen werden!
        Gdx.app.log("Main", "Bullet initialized."); // Log-Ausgabe zur Kontrolle

        physicsSystem = new PhysicsSystem(); // Erstellt die Physik-Welt, Schwerkraft etc.
        Gdx.app.log("Main", "Physics system created.");

        // --- Initialisiere Debug Drawer ---
        // Erstellt die notwendigen Objekte, um die Physik-Formen zu zeichnen.
        shapeRenderer = new ShapeRenderer();
        debugDrawer = new btIDebugDraw() { // Anonyme Implementierung des Debug-Interfaces
            @Override
            public void drawLine(Vector3 from, Vector3 to, Vector3 color) {
                shapeRenderer.setColor(color.x, color.y, color.z, 1); // Farbe setzen
                shapeRenderer.line(from, to); // Linie zeichnen
            }

            @Override
            public void drawContactPoint(Vector3 PointOnB, Vector3 normalOnB, float distance, int lifeTime, Vector3 color) {
                shapeRenderer.setColor(color.x, color.y, color.z, 1); // Farbe setzen
                shapeRenderer.point(PointOnB.x, PointOnB.y, PointOnB.z); // Kontaktpunkt zeichnen
            }

            @Override
            public void reportErrorWarning(String warningString) { Gdx.app.error("BulletDebugDrawer", warningString); } // Fehler loggen

            @Override
            public void draw3dText(Vector3 location, String textString) {} // Text nicht benötigt

            private int debugMode = DebugDrawModes.DBG_DrawWireframe | DebugDrawModes.DBG_DrawContactPoints; // Was gezeichnet wird

            @Override
            public void setDebugMode(int debugMode) { this.debugMode = debugMode; } // Modus setzen
            @Override
            public int getDebugMode() { return debugMode; } // Modus abfragen
        };
        physicsSystem.dynamicsWorld.setDebugDrawer(debugDrawer); // Den Drawer der Physik-Welt zuweisen
        Gdx.app.log("Main", "Bullet Debug Drawer initialized and set.");

        // --- Fenster / Grafik Setup ---
        // Optional: Versucht Vollbild zu starten
        Graphics.Monitor currMonitor = Gdx.graphics.getMonitor();
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor);
        if (!Gdx.graphics.setFullscreenMode(displayMode)) {
            Gdx.app.error("Graphics", "Fullscreen mode failed, using windowed.");
            Gdx.graphics.setWindowedMode(1280, 720); // Fallback
        }

        // --- SceneManager und Kamera ---
        sceneManager = new SceneManager(); // Erstellt den Manager für die 3D-Szenen
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // Erstellt die Kamera
        camera.near = 0.1f; // Nahe Clipping-Distanz (Objekte näher als das werden nicht gezeichnet)
        camera.far = 1000f; // Ferne Clipping-Distanz (Objekte weiter weg werden nicht gezeichnet)
        sceneManager.setCamera(camera); // Weist die Kamera dem SceneManager zu
        camera.position.set(0, 5, 10f); // Setzt eine initiale Kameraposition (wird später überschrieben)

        // --- Input Handling ---
        Gdx.input.setInputProcessor(this); // Setzt DIESE Klasse als Haupt-Input-Handler (für keyDown, scrolled etc.)
        Gdx.input.setCursorCatched(true); // Versteckt den Mauszeiger und fängt ihn im Fenster ein (wichtig für Kamerasteuerung)

        // --- Licht, Umgebung (IBL), Skybox ---
        // Erstellt Lichtquellen und Reflexions-Texturen für realistisches Rendering
        light = new DirectionalLightEx(); // Hauptlicht (Sonne)
        light.direction.set(1, -3, 1).nor(); // Richtung des Lichts
        light.color.set(Color.WHITE);       // Farbe des Lichts
        sceneManager.environment.add(light); // Licht zur Umgebung hinzufügen

        // IBL (Image Based Lighting) Setup - Erzeugt Cubemaps für Umgebungsreflexionen
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light); // Erstellt einen Builder basierend auf dem Licht
        environmentCubemap = iblBuilder.buildEnvMap(1024);      // Cubemap für Spiegelungen
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);   // Cubemap für diffuse Beleuchtung
        specularCubemap = iblBuilder.buildRadianceMap(10);      // Cubemap für Glanzlichter
        iblBuilder.dispose(); // Builder wird nicht mehr gebraucht

        // BRDF Lookup Texture - Eine Hilfstextur für PBR
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        // Fügt die IBL-Texturen zur Umgebung des SceneManagers hinzu
        sceneManager.setAmbientLight(1f); // Generelles Umgebungslicht
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // Skybox Setup - Zeichnet den Himmel im Hintergrund
        skybox = new SceneSkybox(environmentCubemap); // Verwendet die Spiegelungs-Cubemap
        sceneManager.setSkyBox(skybox);

        assetManager = new AssetManager();
        assetManager.setLoader(SceneAsset.class, ".gltf", new net.mgsx.gltf.loaders.gltf.GLTFAssetLoader());

        // --- Spieler-Modell laden ---
        // Lädt die GLTF-Datei und erstellt eine Szene daraus.
        sceneAsset = new GLTFLoader().load(Gdx.files.internal(CHARACTER_MODEL_PATH)); // Lädt die Datei
        playerScene = new Scene(sceneAsset.scene); // Erstellt eine renderbare Szene aus dem geladenen Asset
        sceneManager.addScene(playerScene); // Fügt die Spieler-Szene zum SceneManager hinzu
        // Optional: Startet die erste Animation des Modells im Loop (-1 = unendlich)
        if (playerScene.modelInstance.animations.size > 0) {
            playerScene.animationController.setAnimation(playerScene.modelInstance.animations.first().id, -1);
            Gdx.app.log("Main", "Started player animation: " + playerScene.modelInstance.animations.first().id);
        } else {
            Gdx.app.log("Main", "Player model has no animations.");
        }

        String houseModelPath = "Models/drivee.gltf"; // DEIN HAUSPFAD als Variable definieren
        assetManager.load(houseModelPath, SceneAsset.class);
        assetManager.finishLoading();
        if (assetManager.isLoaded(houseModelPath, SceneAsset.class)) {
            houseSceneAsset = assetManager.get(houseModelPath, SceneAsset.class);
            Gdx.app.log("Main", "House SceneAsset '" + houseModelPath + "' erfolgreich geladen.");
        } else {
            Gdx.app.error("Main", "House SceneAsset '" + houseModelPath + "' konnte NICHT geladen werden!");
            // Hier könntest du entscheiden, ob das Spiel ohne Häuser weiterlaufen soll
            // oder ob es ein kritischer Fehler ist.
        }

        String kaktiModelPath = "Models/bean.gltf"; // DEIN HAUSPFAD als Variable definieren
        assetManager.load(kaktiModelPath, SceneAsset.class);
        assetManager.finishLoading();
        if (assetManager.isLoaded(kaktiModelPath, SceneAsset.class)) {
            kaktiSceneAsset = assetManager.get(kaktiModelPath, SceneAsset.class);
            Gdx.app.log("Main", "House SceneAsset '" + kaktiModelPath + "' erfolgreich geladen.");
        } else {
            Gdx.app.error("Main", "House SceneAsset '" + kaktiModelPath + "' konnte NICHT geladen werden!");
            // Hier könntest du entscheiden, ob das Spiel ohne Häuser weiterlaufen soll
            // oder ob es ein kritischer Fehler ist.
        }

        // --- Handler-Instanzen erstellen (Spieler-spezifisch) ---
        // Erstellt die Objekte, die die Spielerlogik und Kamerasteuerung kapseln.
        player = new Player(playerScene); // Übergibt die Szene an die Player-Logik
        playerCameraController = new PlayerCameraController(camera); // Übergibt die Kamera an den Controller

        // --- TerrainManager erstellen und initiales Terrain bauen --- NEU
        // Der TerrainManager kümmert sich jetzt um alles, was mit dem Terrain zu tun hat.
        terrainManager = new TerrainManager(sceneManager, physicsSystem,
                HEIGHTMAP_PATH, TERRAIN_MAX_HEIGHT,
                VISUAL_TERRAIN_WIDTH, VISUAL_TERRAIN_DEPTH); // Übergabe der Abhängigkeiten und Konfiguration
        // Erstellt das erste Terrain beim Start
        if (!terrainManager.createOrReplaceTerrain()) {
            // Wichtige Fehlerbehandlung: Was tun, wenn das Terrain nicht erstellt werden kann?
            Gdx.app.error("Main", "INITIAL TERRAIN CREATION FAILED! Check paths and configurations.");
            // Ggf. Standard-Boden erstellen oder Spiel beenden
            Gdx.app.exit(); // Beendet die Anwendung
            return; // Verhindert weitere Ausführung von create()
        }

        // --- Charakter Physik erstellen ---
        // Erstellt den Physik-Körper für den Spieler. Muss NACH TerrainManager erfolgen,
        // damit wir die Terrainhöhe für die Startposition abfragen können.
        if (playerScene.modelInstance != null) {
            float startHeight = CHARACTER_HEIGHT * 1.5f; // Standardhöhe über der Basis
            Vector3 basePosition = new Vector3(); // Startpunkt (X,Y,Z) des Terrains am Vertex (0,0)

            // Frage die Höhe vom TerrainManager ab (der hält das physicsHeightField)
            HeightField currentPhysicsHF = terrainManager.getPhysicsHeightField(); // Getter verwenden
            if (currentPhysicsHF != null) {
                currentPhysicsHF.getPositionAt(basePosition, 0, 0); // Weltposition von Vertex (0,0)
                startHeight = basePosition.y + CHARACTER_HEIGHT * 1.5f; // Höhe über diesem Punkt + Puffer
                Gdx.app.log("Main", "Terrain height at vertex (0,0) from HeightField: " + basePosition.y);
            } else {
                // Fallback, falls kein Terrain da ist (sollte wegen Fehlerbehandlung oben nicht passieren)
                startHeight += TERRAIN_MAX_HEIGHT;
                Gdx.app.log("Main", "Could not get physicsHeightField from TerrainManager, using fallback start height.");
                basePosition.set(0, 0, 0); // Annahme: Start bei Weltursprung
            }

            // Definiere die gewünschten Offsets für die Startposition
            float offsetX = 10.0f; // Beispiel: Nach rechts
            float offsetZ = 15.0f; // Beispiel: Nach vorne

            // Berechne die finale Startposition
            Vector3 startPos = new Vector3(
                    basePosition.x + offsetX, // Start X + Offset X
                    startHeight,              // Berechnete Starthöhe
                    basePosition.z + offsetZ  // Start Z + Offset Z
            );

            // Erstelle das CharacterPhysics Objekt mit der berechneten Startposition
            characterPhysics = new CharacterPhysics(
                    physicsSystem,             // Das Physik-System
                    playerScene.modelInstance, // Das visuelle Modell des Spielers
                    CHARACTER_RADIUS, CHARACTER_HEIGHT, CHARACTER_MASS, // Konfiguration
                    startPos                   // Die berechnete Startposition
            );
            Gdx.app.log("Main", "Character physics created at (modified): " + startPos);

            // --- PlayerInputHandler erstellen --- NEU
            // Muss NACH characterPhysics erstellt werden, da er davon abhängt.
            playerInputHandler = new PlayerInputHandler(characterPhysics, camera, MOVE_SPEED, JUMP_FORCE);
            Gdx.app.log("Main", "PlayerInputHandler created.");

            // --- Initiale Kameraposition ---
            // Setzt die Kamera einmalig auf die Startposition des Spielers.
            characterPhysics.updateGraphicsTransform(); // Stellt sicher, dass die Grafik-Transform aktuell ist
            playerPhysicsPosition.set(characterPhysics.modelInstance.transform.getTranslation(tmpVec)); // Hole Position
            playerCameraController.update(playerPhysicsPosition, player.getAngleBehindPlayer(), 0); // Kamera updaten
            Gdx.app.log("Main", "Initial camera update set.");

        } else {
            // Sollte nicht passieren, wenn GLTF-Laden funktioniert hat
            Gdx.app.error("Main", "playerScene.modelInstance is NULL. Cannot create CharacterPhysics or PlayerInputHandler!");
        }

        if (houseSceneAsset != null) { // Nur wenn das Haus-Asset geladen wurde
            createStaticObjects();
            Gdx.app.log("Main", "house created");
        } else {
            Gdx.app.log("Main", "houseSceneAsset ist null, createStaticObjects() wird übersprungen.");
        }
    } // Ende create()

    // In Main.java

// ... (andere Member-Variablen und Methoden der Main-Klasse) ...

    private void createStaticObjects() {
        Gdx.app.log("Main", "Creating static objects (houses)...");

        // Stelle sicher, dass das Haus-Asset geladen ist (houseSceneAsset wurde in create() zugewiesen)
        if (houseSceneAsset == null) {
            Gdx.app.error("Main", "House SceneAsset is null! Cannot create houses.");
            return; // Abbrechen, wenn das Asset fehlt
        }
        if (kaktiSceneAsset == null) {
            Gdx.app.error("Main", "Kakti SceneAsset is null! Cannot create houses.");
            return; // Abbrechen, wenn das Asset fehlt
        }

        // --- Haus 1 ---
        Vector3 house1Position = new Vector3(400f, 0f, 450f); // ANPASSEN: X, Basis-Y, Z
        float house1RotationY = 45f; // ANPASSEN: Rotation um Y in Grad

        // Visuelle Szene für Haus 1 erstellen
        Scene house1Visual = new Scene(houseSceneAsset.scene); // Neue Instanz der Szene
        house1Visual.modelInstance.transform.setToTranslation(house1Position);
        house1Visual.modelInstance.transform.rotate(Vector3.Y, house1RotationY);
        sceneManager.addScene(house1Visual);
        houseVisualScenes.add(house1Visual);
        Gdx.app.log("Main", "Created and placed visual house 1 (Scene) at " + house1Position + " with rotation " + house1RotationY);

        // Physik-Körper erstellen
        // WICHTIG: halfExtents an dein Hausmodell anpassen!
        Vector3 houseHalfExtents = new Vector3(5f, 6f, 4f); // Bsp: halbe Breite, halbe HÖHE, halbe Tiefe

        // Welt-Transformation für das Physik-Objekt
        Matrix4 house1PhysicsTransform = new Matrix4(); // Erstellt Identitätsmatrix

        // Position des Zentrums der Kollisionsbox
        Vector3 house1PhysicsCenterPos = new Vector3(house1Position.x, house1Position.y + houseHalfExtents.y, house1Position.z);

        // Rotation der Kollisionsbox
        Quaternion house1Rotation = tmpQuat.setFromAxis(Vector3.Y, house1RotationY); // tmpQuat ist Member-Variable

        // Skalierung (normalerweise 1,1,1 für Physik)
        Vector3 house1Scale = tmpVec2.set(1, 1, 1); // tmpVec2 ist Member-Variable

        // --- KORREKTE METHODE ZUM SETZEN DER MATRIX ---
        // Reihenfolge: Zuerst Skalieren (auf Identität, also keine Änderung),
        // dann Rotieren um den Ursprung, dann zum Zielort Translatieren.
        // ODER: Matrix für jede Komponente einzeln aufbauen und kombinieren.
        // Die .set(position, rotation, scale) Methode ist oft die einfachste.
        // Wenn diese nicht existiert, bauen wir es manuell auf:

        house1PhysicsTransform.idt(); // 1. Zurücksetzen zur Identitätsmatrix

        // Wende die Transformationen in der Reihenfolge an, die das gewünschte Ergebnis liefert.
        // Um ein Objekt an einer Position mit einer Rotation und Skalierung zu platzieren:
        // 1. Verschiebe zum Zielort (Translation)
        // 2. Rotiere um den lokalen Ursprung (der jetzt am Zielort ist)
        // 3. Skaliere um den lokalen Ursprung (der jetzt am Zielort und rotiert ist)
        // LibGDX Matrix-Multiplikationen sind von rechts nach links (M = T * R * S * Vertex)
        // Also müssen wir die Matrix so aufbauen, dass sie S, dann R, dann T anwendet.
        // Mit den set-Methoden ist es oft intuitiver:

        house1PhysicsTransform.translate(house1PhysicsCenterPos); // 3. Zuletzt die Translation zum Weltmittelpunkt
        house1PhysicsTransform.rotate(house1Rotation);            // 2. Davor die Rotation
        // Skalierung wird hier nicht explizit angewendet, da sie (1,1,1) ist und die btBoxShape bereits skaliert ist.
        // Wenn du eine Skalierung > 1 hättest, wäre es:
        // house1PhysicsTransform.scale(house1Scale.x, house1Scale.y, house1Scale.z); // 1. Zuerst die Skalierung

        // Alternativ und oft sicherer für TRS (Translate-Rotate-Scale)-Reihenfolge beim Aufbau:
        // house1PhysicsTransform.setToTranslation(house1PhysicsCenterPos); // T
        // house1PhysicsTransform.rotate(house1Rotation); // R (multipliziert von rechts: T * R)
        // house1PhysicsTransform.scale(house1Scale.x, house1Scale.y, house1Scale.z); // S (multipliziert von rechts: T * R * S)
        // Dies entspricht oft der set(pos, rot, scale) Logik.

        // --- ENDE KORREKTUR ---

        StaticObjectPhysics physicsForHouse1 = new StaticObjectPhysics(
                physicsSystem,
                houseHalfExtents,
                house1PhysicsTransform // Die korrekt gesetzte Transformation
        );
        housePhysicsBodies.add(physicsForHouse1);
        physicsForHouse1.body.userData = "Haus_1"; // Eindeutige ID
        Gdx.app.log("Main", "Created physics for house 1.");


        // --- Haus 2 (Beispiel) ---
        Vector3 house2Position = new Vector3(450f, 0f, 400f); // ANPASSEN
        float house2RotationY = -30f; // ANPASSEN

        Scene house2Visual = new Scene(houseSceneAsset.scene);
        house2Visual.modelInstance.transform.setToTranslation(house2Position);
        house2Visual.modelInstance.transform.rotate(Vector3.Y, house2RotationY);
        sceneManager.addScene(house2Visual);
        houseVisualScenes.add(house2Visual);

        // Annahme: Gleiche Größe wie Haus 1, sonst eigene halfExtents definieren
        Matrix4 house2PhysicsTransform = new Matrix4();
        Vector3 house2PhysicsCenterPos = new Vector3(house2Position.x, house2Position.y + houseHalfExtents.y, house2Position.z);
        Quaternion house2Rotation = tmpQuat.setFromAxis(Vector3.Y, house2RotationY); // tmpQuat wiederverwenden
        Vector3 house2Scale = tmpVec2.set(1, 1, 1); // tmpVec2 wiederverwenden

        // Manuelles Setzen für Haus 2
        house2PhysicsTransform.idt();
        house2PhysicsTransform.translate(house2PhysicsCenterPos);
        house2PhysicsTransform.rotate(house2Rotation);
        // house2PhysicsTransform.scale(house2Scale.x, house2Scale.y, house2Scale.z); // Wenn Skalierung nötig

        StaticObjectPhysics physicsForHouse2 = new StaticObjectPhysics(
                physicsSystem,
                houseHalfExtents, // Oder andere Maße
                house2PhysicsTransform
        );
        housePhysicsBodies.add(physicsForHouse2);
        physicsForHouse2.body.userData = "Haus_2";
        Gdx.app.log("Main", "Created and placed house 2.");

        // --- Haus 3 (Beispiel) ---
        Vector3 house3Position = new Vector3(470f, 10f, 470f); // ANPASSEN
        float house3RotationY = -30f; // ANPASSEN

        Scene house3Visual = new Scene(houseSceneAsset.scene);
        house3Visual.modelInstance.transform.setToTranslation(house3Position);
        house3Visual.modelInstance.transform.rotate(Vector3.Y, house3RotationY);
        sceneManager.addScene(house3Visual);
        houseVisualScenes.add(house3Visual);

        // Annahme: Gleiche Größe wie Haus 1, sonst eigene halfExtents definieren
        Matrix4 house3PhysicsTransform = new Matrix4();
        Vector3 house3PhysicsCenterPos = new Vector3(house3Position.x, house3Position.y + houseHalfExtents.y, house3Position.z);
        Quaternion house3Rotation = tmpQuat.setFromAxis(Vector3.Y, house3RotationY); // tmpQuat wiederverwenden
        Vector3 house3Scale = tmpVec2.set(1, 1, 1); // tmpVec2 wiederverwenden

        // Manuelles Setzen für Haus 2
        house3PhysicsTransform.idt();
        house3PhysicsTransform.translate(house3PhysicsCenterPos);
        house3PhysicsTransform.rotate(house3Rotation);
        // house2PhysicsTransform.scale(house2Scale.x, house2Scale.y, house2Scale.z); // Wenn Skalierung nötig

        StaticObjectPhysics physicsForHouse3 = new StaticObjectPhysics(
                physicsSystem,
                houseHalfExtents, // Oder andere Maße
                house3PhysicsTransform
        );
        housePhysicsBodies.add(physicsForHouse3);
        physicsForHouse3.body.userData = "Haus_3";
        Gdx.app.log("Main", "Created and placed house 3.");

        // --- kakti1 (Beispiel) ---
        Vector3 kakti1Position = new Vector3(450f, 30f, 400f); // ANPASSEN
        float kakti1RotationY = -30f; // ANPASSEN

        Scene kakti1Visual = new Scene(kaktiSceneAsset.scene);
        kakti1Visual.modelInstance.transform.setToTranslation(kakti1Position);
        kakti1Visual.modelInstance.transform.rotate(Vector3.Y, kakti1RotationY);
        sceneManager.addScene(kakti1Visual);
        houseVisualScenes.add(kakti1Visual);

        Vector3 kaktiHalfExtents = new Vector3(5f, 6f, 4f);
        // Annahme: Gleiche Größe wie Haus 1, sonst eigene halfExtents definieren
        Matrix4 kakti1PhysicsTransform = new Matrix4();
        Vector3 kakti1PhysicsCenterPos = new Vector3(kakti1Position.x, kakti1Position.y + kaktiHalfExtents.y, kakti1Position.z);
        Quaternion kakti1Rotation = tmpQuat.setFromAxis(Vector3.Y, kakti1RotationY); // tmpQuat wiederverwenden
        Vector3 kakti1Scale = tmpVec2.set(1, 1, 1); // tmpVec2 wiederverwenden

        // Manuelles Setzen für Haus 2
        kakti1PhysicsTransform.idt();
        kakti1PhysicsTransform.translate(kakti1PhysicsCenterPos);
        kakti1PhysicsTransform.rotate(kakti1Rotation);
        // house2PhysicsTransform.scale(house2Scale.x, house2Scale.y, house2Scale.z); // Wenn Skalierung nötig

        StaticObjectPhysics physicsForKakti1 = new StaticObjectPhysics(
                physicsSystem,
                houseHalfExtents, // Oder andere Maße
                kakti1PhysicsTransform
        );
        housePhysicsBodies.add(physicsForKakti1);
        physicsForKakti1.body.userData = "Kakti_1";
        Gdx.app.log("Main", "Created and placed kakti1.");

        // ... Füge weitere Häuser hinzu ...
    }

// ... (Rest der Main-Klasse: create(), render(), dispose(), etc.) ...
    /**
     * Wird kontinuierlich aufgerufen (typischerweise 60 Mal pro Sekunde).
     * Hier findet die Hauptlogik des Spiels statt: Input verarbeiten,
     * Physik simulieren, Zustände aktualisieren, Rendern.
     */
    @Override
    public void render() {
        // 1. Delta Time holen und begrenzen
        // deltaTime: Zeit seit dem letzten Frame in Sekunden. Wichtig für zeitbasierte Bewegung/Physik.
        float deltaTime = Gdx.graphics.getDeltaTime();
        // Begrenzung: Verhindert "Explosionen" in der Physik, wenn das Spiel kurz hängt (großes deltaTime).
        deltaTime = Math.min(deltaTime, 1f / 30f); // Maximal 1/30 Sekunde pro Physikschritt

        // 2. Spieler-Input verarbeiten --- NEU
        // Delegiert die Verarbeitung von WASD, Sprung etc. an den Handler.
        if (playerInputHandler != null) {
            playerInputHandler.processInput();
        }



        // 3. Physik-Welt aktualisieren
        // Lässt die Physik-Engine die Zeit (deltaTime) weiterschreiten.
        // Bewegt Objekte, prüft Kollisionen, wendet Schwerkraft an etc.
        if (physicsSystem != null) {
            physicsSystem.update(deltaTime);
        }




        // 4. Grafik an Physik anpassen
        // Holt die neue Position/Rotation des Physik-Körpers des Spielers
        // und wendet sie auf die Transformation des sichtbaren Spieler-Modells an.
        if (characterPhysics != null) {
            characterPhysics.updateGraphicsTransform();
            // HINWEIS: Hier war der Ort für den manuellen Grafik-Offset, falls das Modell schwebt!
            // float modelOffsetY = -1.0f; // Beispiel
            // characterPhysics.modelInstance.transform.translate(0, modelOffsetY, 0);
        }

        // 5. Kamera aktualisieren
        // Aktualisiert die Kameraposition basierend auf der (aktualisierten) Spielerposition.
        if (characterPhysics != null && playerCameraController != null && player != null) {
            playerPhysicsPosition.set(characterPhysics.modelInstance.transform.getTranslation(tmpVec)); // Hole aktuelle Grafik-Position
            playerCameraController.update(playerPhysicsPosition, player.getAngleBehindPlayer(), deltaTime); // Kamera-Controller updaten
        }

        // 6. SceneManager updaten
        // Aktualisiert interne Zustände des SceneManagers (z.B. Animationen).
        sceneManager.update(deltaTime);

        // 7. Bildschirm löschen
        // Bereitet den Bildschirm für das Zeichnen des neuen Frames vor.
        Gdx.gl.glClearColor(0.3f, 0.5f, 0.7f, 1f); // Setzt die Hintergrundfarbe (Himmelblau)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT); // Löscht Farb- und Tiefenpuffer

        // 8. Szene rendern
        // Zeichnet alle sichtbaren Objekte (Spieler, Terrain, Skybox) mit Licht etc.
        sceneManager.render();

        // 9. Physik Debug zeichnen (wenn aktiviert) --- NEU
        // Zeichnet die Drahtgitter-Formen der Physik-Körper über die gerenderte Szene.
        if (drawDebug && debugDrawer != null && shapeRenderer != null && physicsSystem != null) {
            shapeRenderer.setProjectionMatrix(camera.combined); // WICHTIG: Gleiche Sicht wie die Kamera
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line); // Beginne Linien zu zeichnen
            physicsSystem.dynamicsWorld.debugDrawWorld(); // Sage Bullet, es soll sich zeichnen
            shapeRenderer.end(); // Beende das Zeichnen
        }

        // 10. Andere Tastenabfragen (die nicht zur Spielerbewegung gehören)
        // Diese werden einmal pro Frame geprüft.
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            if (terrainManager != null) { // Prüfen ob der Manager existiert
                Gdx.app.log("Main", "F1 pressed, requesting terrain recreation...");
                if (!terrainManager.createOrReplaceTerrain()) { // Rufe die Methode im Manager auf
                    Gdx.app.error("Main", "Terrain recreation FAILED!");
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleFullscreen(); // Schaltet Vollbild an/aus
        }
    } // Ende render()


    /**
     * Hilfsmethode zum Umschalten des Vollbildmodus.
     */
    private void toggleFullscreen() {
        // Prüft, ob das Spiel gerade im Vollbild ist
        if (Gdx.graphics.isFullscreen()) {
            // Wenn ja, wechsle zurück zum Fenster-Modus
            Gdx.graphics.setWindowedMode(1280, 720); // Feste Größe oder letzte bekannte Größe
            Gdx.app.log("Main", "Switched to windowed mode.");
            Gdx.input.setCursorCatched(false); // Mauszeiger wieder sichtbar machen
        } else {
            // Wenn nein, versuche in den Vollbildmodus zu wechseln
            Graphics.Monitor currMonitor = Gdx.graphics.getPrimaryMonitor(); // Hauptmonitor holen
            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor); // Dessen Auflösung holen
            if (Gdx.graphics.setFullscreenMode(displayMode)) { // Versuche umzuschalten
                Gdx.app.log("Main", "Switched to fullscreen mode.");
                Gdx.input.setCursorCatched(true); // Mauszeiger wieder fangen
            } else {
                Gdx.app.error("Graphics", "Fullscreen mode failed."); // Falls es nicht klappt
            }
        }
    } // Ende toggleFullscreen()


    // --- InputProcessor Methoden ---
    // Diese Methoden werden von LibGDX aufgerufen, wenn ein entsprechendes Input-Event passiert.
    // Da wir `Gdx.input.setInputProcessor(this);` gesetzt haben, landen die Events hier.

    /** Wird aufgerufen, wenn eine Taste GEDRÜCKT wird. */
    @Override
    public boolean keyDown(int keycode) {
        // Kameramodus mit TAB umschalten
        if (keycode == Input.Keys.TAB) {
            if (playerCameraController != null && player != null) { // Sicherstellen, dass Objekte existieren
                playerCameraController.toggleCameraMode(player.getAngleBehindPlayer()); // Modus im Controller umschalten
                CameraMode currentMode = playerCameraController.getCameraMode(); // Neuen Modus abfragen
                boolean captureCursor = (currentMode == CameraMode.FREE_LOOK); // Cursor nur im Free Look fangen
                Gdx.input.setCursorCatched(captureCursor); // Cursor entsprechend setzen
                Gdx.app.log("Main", "Toggled camera mode to: " + currentMode + ", Cursor captured: " + captureCursor);
                return true; // true: Signalisiert, dass das Event hier behandelt wurde
            }
        }
        // Debug Drawing mit F3 umschalten - NEU
        if (keycode == Input.Keys.F3) {
            drawDebug = !drawDebug; // Invertiert den boolean-Wert
            Gdx.app.log("Main", "Debug Drawing toggled: " + drawDebug);
            return true; // Event behandelt
        }
        // F1 und ESC werden in render() über isKeyJustPressed geprüft, da sie nur einmal auslösen sollen.
        return false; // false: Signalisiert, dass das Event nicht behandelt wurde (andere Listener könnten es bekommen)
    }

    /** Wird aufgerufen, wenn das Mausrad gescrollt wird. */
    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Delegiert das Scroll-Event an den Kamera-Controller für Zoom
        if (playerCameraController != null) {
            return playerCameraController.scrolled(amountY); // amountY ist die vertikale Scroll-Richtung
        }
        return false; // Nicht behandelt, falls kein Controller da ist
    }

    // --- Andere InputProcessor Methoden (meist nicht benötigt für einfaches Spiel) ---
    @Override public boolean keyUp(int keycode) { return false; } // Taste losgelassen
    @Override public boolean keyTyped(char character) { return false; } // Taste getippt (Zeicheneingabe)
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; } // Touchscreen/Maus Klick Start
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; } // Touchscreen/Maus Klick Ende
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; } // Touch abgebrochen (Android)
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; } // Touchscreen/Maus gezogen
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; } // Maus bewegt (ohne Klick) - wird oft direkt in Controllern abgefragt


    // --- AnimationListener Methoden ---
    // (Optional) Werden aufgerufen, wenn Animationen enden oder loopen.
    @Override public void onEnd(AnimationController.AnimationDesc animation) { }
    @Override public void onLoop(AnimationController.AnimationDesc animation) { }


    /**
     * Wird EINMAL beim Beenden der Anwendung aufgerufen.
     * SEHR WICHTIG: Hier müssen alle Ressourcen freigegeben werden, die nicht
     * automatisch vom Garbage Collector entfernt werden (z.B. Texturen, Meshes, Physik-Objekte, Shader).
     * Sonst entstehen Speicherlecks!
     */
    @Override
    public void dispose() {
        Gdx.app.log("Main", "Disposing resources...");

        // Reihenfolge ist wichtig: Abhängige Objekte zuerst, dann die Systeme.

        // 1. Eigene Handler/Manager zuerst (damit sie ihre internen Ressourcen freigeben)
        if (terrainManager != null) {
            terrainManager.dispose(); // Ruft disposeCurrentTerrain() auf
        }
        Gdx.app.log("Main", "Disposed TerrainManager.");
        // PlayerInputHandler hat normalerweise nichts zum Disposen

        // 2. Charakter-Physik (wenn nicht schon vom Manager disposed)
        if (characterPhysics != null) {
            characterPhysics.dispose();
        }
        Gdx.app.log("Main", "Disposed CharacterPhysics.");

        // 3. Physik-System (Welt, Konfiguration etc.)
        if (physicsSystem != null) {
            physicsSystem.dispose();
        }
        Gdx.app.log("Main", "Disposed PhysicsSystem.");

        // 4. Debug Drawer Ressourcen
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (debugDrawer != null) {
            // Obwohl btIDebugDraw kein direktes dispose() hat, ist es gute Praxis,
            // die Referenz auf null zu setzen oder sicherzustellen, dass es nicht mehr verwendet wird.
            // Manche Implementierungen könnten Ressourcen halten.
            debugDrawer = null; // Referenz entfernen
        }
        Gdx.app.log("Main", "Disposed debug drawer resources.");

        // 5. SceneManager und GLTF Assets
        // SceneManager.dispose() sollte die von ihm verwalteten Szenen und deren
        // ModelInstances/Meshes/Texturen freigeben, wenn sie nicht woanders noch referenziert werden.
        if (sceneManager != null) {
            sceneManager.dispose();
        }
        // Das SceneAsset enthält die Rohdaten, die auch freigegeben werden müssen.
        if (sceneAsset != null) {
            sceneAsset.dispose();
        }
        Gdx.app.log("Main", "Disposed SceneManager and player sceneAsset.");

        // 6. IBL Texturen und Skybox
        if (environmentCubemap != null) environmentCubemap.dispose();
        if (diffuseCubemap != null) diffuseCubemap.dispose();
        if (specularCubemap != null) specularCubemap.dispose();
        if (brdfLUT != null) brdfLUT.dispose();
        if (skybox != null) skybox.dispose(); // Skybox hält auch Referenzen
        Gdx.app.log("Main", "Disposed IBL and skybox resources.");
        if (houseSceneAsset != null) {
            houseSceneAsset.dispose();
            houseSceneAsset = null;
        }
        if (assetManager != null) {
            assetManager.dispose(); // Gibt alle vom AssetManager geladenen Ressourcen frei
        }

        // Visuelles Terrain wird jetzt vom TerrainManager disposed.
        Gdx.app.log("Main", "Dispose complete.");
    } // Ende dispose()
} // Ende Main Klasse