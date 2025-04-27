package io.github.some_example_name.Terrain;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import io.github.some_example_name.Terrain.HeightField;
import io.github.some_example_name.Physiks.PhysicsSystem;
import io.github.some_example_name.Physiks.TerrainPhysics;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import com.badlogic.gdx.utils.Disposable;

/**
 * Verwaltet die Erstellung, das Ersetzen und das Aufräumen
 * des visuellen und physikalischen Terrains.
 */
public class TerrainManager implements Disposable {

    // Abhängigkeiten von außen
    private final SceneManager sceneManager;
    private final PhysicsSystem physicsSystem;

    // Pfad zur Heightmap
    private final String heightmapPath;
    // Terrain-Konfiguration
    private final float terrainMaxHeight;
    private final float visualTerrainWidth;
    private final float visualTerrainDepth;

    // Aktuell verwaltete Terrain-Objekte
    private Terrain visualTerrain;          // HeightMapTerrain
    private Scene terrainScene;             // Szene für visuelles Terrain
    private HeightField physicsHeightField; // HeightField für Physik-Daten
    private TerrainPhysics terrainPhysics;      // Bullet-Objekt

    public TerrainManager(SceneManager sceneManager, PhysicsSystem physicsSystem,
                          String heightmapPath, float terrainMaxHeight,
                          float visualTerrainWidth, float visualTerrainDepth) {
        this.sceneManager = sceneManager;
        this.physicsSystem = physicsSystem;
        this.heightmapPath = heightmapPath;
        this.terrainMaxHeight = terrainMaxHeight;
        this.visualTerrainWidth = visualTerrainWidth;
        this.visualTerrainDepth = visualTerrainDepth;

        if (this.sceneManager == null || this.physicsSystem == null) {
            Gdx.app.error("TerrainManager", "SceneManager and PhysicsSystem cannot be null!");
            // Fehler werfen oder fortfahren mit dem Risiko von NPEs
        }
    }

    /**
     * Erstellt neues Terrain oder ersetzt vorhandenes.
     * Gibt vorherige Terrain-Ressourcen frei.
     * Gibt true zurück, wenn erfolgreich, false bei Fehlern.
     */
    public boolean createOrReplaceTerrain() {
        Gdx.app.log("TerrainManager", "Attempting to create or replace terrain...");

        // 1. Altes Terrain sicher aufräumen
        disposeCurrentTerrain();

        // --- Neues Visuelles Terrain ---
        Pixmap visualPixmap = null;
        try {
            visualPixmap = new Pixmap(Gdx.files.internal(heightmapPath));
            Gdx.app.log("TerrainManager", "Loaded heightmap Pixmap for visual terrain.");
            // ANNAHME: HeightMapTerrain disposed die Pixmap intern!
            this.visualTerrain = new HeightMapTerrain(visualPixmap, terrainMaxHeight);
            this.terrainScene = new Scene(this.visualTerrain.getModelInstance());
            this.sceneManager.addScene(this.terrainScene);
            Gdx.app.log("TerrainManager", "Created visual terrain and added to SceneManager.");
        } catch (Exception e) {
            Gdx.app.error("TerrainManager", "Error creating visual terrain", e);
            if (visualPixmap != null && !visualPixmap.isDisposed()) visualPixmap.dispose();
            disposeCurrentTerrain(); // Sicherstellen, dass alles aufgeräumt ist
            return false; // Fehler signalisieren
        }
        // visualPixmap sollte disposed sein

        // --- Neues Physikalisches Terrain ---
        Pixmap physicsPixmap = null;
        try {
            physicsPixmap = new Pixmap(Gdx.files.internal(heightmapPath)); // Erneut laden
            Gdx.app.log("TerrainManager", "Loaded heightmap Pixmap AGAIN for physics terrain.");

            this.physicsHeightField = new HeightField(
                    true, physicsPixmap, true,
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
            );
            Gdx.app.log("TerrainManager", "Created physics HeightField instance.");

            // physicsHeightField Dimensionen konfigurieren
            // Annahme: Visuelles Terrain ist zentriert
            final float actualVisualWidth = 1200.0f;  // Z.B. Breite von -50 bis +50
            final float actualVisualDepth = 1200.0f;  // Z.B. Tiefe von -50 bis +50
            final float visualBaseY = 14.5f;      // Y-Position der Basis des visuellen Terrains

            // Setze die Eckpunkte des physicsHeightField entsprechend.
            //Wenn dein visuelles Terrain bei (0,0,0) beginnt und nach +X/+Z geht:
            physicsHeightField.corner00.set(400,               visualBaseY, 400);
            physicsHeightField.corner10.set(actualVisualWidth, visualBaseY, 400);
            physicsHeightField.corner01.set(400,               visualBaseY, actualVisualDepth);
            physicsHeightField.corner11.set(actualVisualDepth, visualBaseY, actualVisualDepth);
            Gdx.app.log("TerrainManager", "Configured physics HeightField dimensions (W:" + visualTerrainWidth + ", D:" + visualTerrainDepth + ")");

            // TerrainPhysics erstellen
            this.terrainPhysics = new TerrainPhysics(physicsSystem, this.physicsHeightField);
            Gdx.app.log("TerrainManager", "Created TerrainPhysics.");

        } catch (Exception e) {
            Gdx.app.error("TerrainManager", "Error creating physics terrain", e);
            if (physicsPixmap != null && !physicsPixmap.isDisposed()) physicsPixmap.dispose();
            disposeCurrentTerrain(); // Alles wieder aufräumen
            return false; // Fehler signalisieren
        } finally {
            // Die Physik-Pixmap immer freigeben
            if (physicsPixmap != null && !physicsPixmap.isDisposed()) {
                physicsPixmap.dispose();
                Gdx.app.log("TerrainManager", "Disposed physics heightmap Pixmap.");
            }
        }

        Gdx.app.log("TerrainManager", "Terrain creation/replacement successful.");
        return true; // Erfolg
    }

    /**
     * Gibt die aktuell verwalteten Terrain-Ressourcen sicher frei.
     */
    private void disposeCurrentTerrain() {
        Gdx.app.log("TerrainManager", "Disposing current terrain resources...");
        // Reihenfolge: Physik -> HeightField -> Visuell (Szene + Objekt)
        if (terrainPhysics != null) {
            if (physicsSystem != null && physicsSystem.dynamicsWorld != null && terrainPhysics.body != null) {
                try {
                    physicsSystem.dynamicsWorld.removeRigidBody(terrainPhysics.body);
                    Gdx.app.log("TerrainManager", "Removed terrain body from physics world.");
                } catch (Exception e) {
                    Gdx.app.error("TerrainManager", "Error removing terrain body from world", e);
                }
            }
            terrainPhysics.dispose();
            terrainPhysics = null; // Wichtig: Referenz löschen
            Gdx.app.log("TerrainManager", "Disposed TerrainPhysics.");
        }
        if (physicsHeightField != null) {
            physicsHeightField.dispose();
            physicsHeightField = null;
            Gdx.app.log("TerrainManager", "Disposed physics HeightField object.");
        }
        if (terrainScene != null) {
            if (sceneManager != null) {
                try {
                    sceneManager.removeScene(terrainScene);
                    Gdx.app.log("TerrainManager", "Removed terrain scene from SceneManager.");
                } catch (Exception e) {
                    Gdx.app.error("TerrainManager", "Error removing terrain scene", e);
                }
            }
            // Scene selbst muss normalerweise nicht disposed werden, nur die Inhalte
            terrainScene = null; // Wichtig: Referenz löschen
        }
        if (visualTerrain != null) {
            visualTerrain.dispose();
            visualTerrain = null; // Wichtig: Referenz löschen
            Gdx.app.log("TerrainManager", "Disposed visual Terrain object.");
        }
    }

    // Optional: Getter, falls andere Teile des Spiels Zugriff brauchen
    public TerrainPhysics getCurrentTerrainPhysics() {
        return terrainPhysics;
    }
    public HeightField getPhysicsHeightField() {
        return physicsHeightField;
    }

    /**
     * Gibt alle vom Manager gehaltenen Ressourcen frei.
     * Sollte in der dispose() Methode der Hauptklasse aufgerufen werden.
     */
    @Override
    public void dispose() {
        Gdx.app.log("TerrainManager", "Disposing TerrainManager...");
        disposeCurrentTerrain(); // Stellt sicher, dass das letzte Terrain aufgeräumt wird
        // Die übergebenen sceneManager und physicsSystem werden *nicht* hier disposed!
    }
}
