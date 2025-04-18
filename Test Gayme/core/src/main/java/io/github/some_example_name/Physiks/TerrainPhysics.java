package io.github.some_example_name.Physiks; // Passe den Paketnamen an

// Importiere die notwendigen Klassen wieder!
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btHeightfieldTerrainShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import io.github.some_example_name.Terrain.HeightField; // Importiere deine HeightField-Klasse

import java.nio.FloatBuffer;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.Gdx; // Für Logging

/**
 * Erstellt und verwaltet einen statischen Bullet Physics Körper für ein Terrain,
 * basierend auf der benutzerdefinierten HeightField-Klasse.
 */
public class TerrainPhysics implements Disposable {

    // Mache sie final, aber initialisiere sie im Konstruktor
    public final btRigidBody body;
    private final btHeightfieldTerrainShape shape;
    private final btDefaultMotionState motionState;

    // Temporäre Vektoren, um Garbage zu vermeiden
    private static final Vector3 tmpV1 = new Vector3();
    private static final Vector3 tmpV2 = new Vector3();
    private static final Matrix4 tmpMat = new Matrix4(); // Für getTranslation

    /**
     * Erstellt das Physik-Terrain.
     *
     * @param physicsSystem Das Physik-System, zu dem der Körper hinzugefügt wird.
     * @param heightField Das HeightField-Objekt, das die visuellen und Höhendaten enthält.
     */
    public TerrainPhysics(PhysicsSystem physicsSystem, HeightField heightField) {

        Gdx.app.log("TerrainPhysics", "--- Starting TerrainPhysics Creation ---");

        // Deklariere Variablen hier, damit sie im ganzen Konstruktor gültig sind
        btHeightfieldTerrainShape tempShape = null; // Temporär
        btDefaultMotionState tempMotionState = null; // Temporär
        btRigidBody tempBody = null; // Temporär

        try {
            // 1. Hole Daten aus dem HeightField-Objekt
            int widthVertices = heightField.width;
            int depthVertices = heightField.height;
            float[] heightData = heightField.data;
            Gdx.app.log("TerrainPhysics", "Input HeightField dimensions: " + widthVertices + "x" + depthVertices);

            if (heightData == null || heightData.length != widthVertices * depthVertices) {
                Gdx.app.error("TerrainPhysics", "HeightField data array is NULL or has incorrect size! Expected: " + (widthVertices * depthVertices) + ", Got: " + (heightData != null ? heightData.length : "null"));
                throw new GdxRuntimeException("HeightField data array is null or has incorrect size!");
            }
            Gdx.app.log("TerrainPhysics", "Height data array size: " + heightData.length);

            // 2. Finde Min/Max-Höhe
            float minHeight = findMinHeight(heightData);
            float maxHeight = findMaxHeight(heightData);
            Gdx.app.log("TerrainPhysics", "Calculated Min/Max height from data: MinH=" + minHeight + ", MaxH=" + maxHeight);
            if (minHeight > maxHeight) {
                //Gdx.app.warn("TerrainPhysics", "MinHeight (" + minHeight + ") is greater than MaxHeight (" + maxHeight + ")! Check height data calculation.");
                // Hier nicht abbrechen, aber es ist ein Warnsignal
            }

            // 3. Erstelle einen DIREKTEN FloatBuffer
            FloatBuffer heightBuffer = BufferUtils.newFloatBuffer(heightData.length);
            heightBuffer.put(heightData);
            heightBuffer.flip();
            Gdx.app.log("TerrainPhysics", "Created direct FloatBuffer for height data. Capacity: " + heightBuffer.capacity() + ", Limit: " + heightBuffer.limit());

            // 4. Erstelle die Bullet Heightfield Shape
            tempShape = new btHeightfieldTerrainShape(
                    widthVertices,       // int heightStickWidth (Anzahl Vertices X)
                    depthVertices,       // int heightStickLength (Anzahl Vertices Z) <-- Korrigiert!
                    heightBuffer,        // Der direkte FloatBuffer
                    1.0f,                // float heightScale
                    minHeight,           // float minHeight
                    maxHeight,           // float maxHeight
                    1,                   // int upAxis
                    true                 // boolean useFloatData
            );
            Gdx.app.log("TerrainPhysics", "Created btHeightfieldTerrainShape with FloatBuffer (8 args)");


            // 5. Berechne Skalierung und Offset für die Physik-Shape
            float visualCorner00X = heightField.corner00.x;
            float visualCorner00Z = heightField.corner00.z;
            float visualCorner10X = heightField.corner10.x;
            float visualCorner01Z = heightField.corner01.z;
            Gdx.app.log("TerrainPhysics", "Visual Corners: corner00=(" + visualCorner00X + "," + visualCorner00Z +
                    "), corner10.x=" + visualCorner10X + ", corner01.z=" + visualCorner01Z);

            float terrainWorldWidth = visualCorner10X - visualCorner00X;
            float terrainWorldDepth = visualCorner01Z - visualCorner00Z;
            Gdx.app.log("TerrainPhysics", "Calculated visual terrain world size: Width=" + terrainWorldWidth + ", Depth=" + terrainWorldDepth);

            // Deklariere scaleX und scaleZ hier!
            float scaleX, scaleZ;
            if (widthVertices <= 1 || depthVertices <= 1) {
                Gdx.app.error("TerrainPhysics", "Invalid vertex count for scaling calculation (<= 1). Vertices: " + widthVertices + "x" + depthVertices + ". Using scale 1.0.");
                scaleX = 1.0f;
                scaleZ = 1.0f;
            } else {
                scaleX = terrainWorldWidth / (widthVertices - 1);
                scaleZ = terrainWorldDepth / (depthVertices - 1);
            }
            tempShape.setLocalScaling(new Vector3(scaleX, 30f, scaleZ)); // Verwende tempShape hier
            Gdx.app.log("TerrainPhysics", "Set local scaling: X=" + scaleX + ", Y=1.0, Z=" + scaleZ);


            // 6. Erstelle MotionState und setze die Transformation
            Matrix4 transform = new Matrix4();
            transform.setToTranslation(heightField.corner00); // Positioniere Ursprung auf corner00
            tempMotionState = new btDefaultMotionState(); // Verwende tempMotionState
            tempMotionState.setWorldTransform(transform);
            Vector3 initialPos = new Vector3();
            transform.getTranslation(initialPos);
            Gdx.app.log("TerrainPhysics", "Set initial MotionState world transform position to: " + initialPos);


            // 7. Erstelle RigidBody (statisch)
            float mass = 0f;
            Vector3 localInertia = new Vector3(0, 0, 0);
            // ConstructionInfo wird intern vom RigidBody gehalten, muss nicht disposed werden
            btRigidBody.btRigidBodyConstructionInfo constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(
                    mass, tempMotionState, tempShape, localInertia
            );
            tempBody = new btRigidBody(constructionInfo); // Verwende tempBody
            tempBody.setCollisionFlags(tempBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_STATIC_OBJECT);
            tempBody.setFriction(0.8f);
            Gdx.app.log("TerrainPhysics", "Created btRigidBody and set flags/friction.");


            // 8. Füge den Körper zur Physik-Welt hinzu
            physicsSystem.dynamicsWorld.addRigidBody(tempBody);
            Gdx.app.log("TerrainPhysics", "Static terrain rigid body added to the world. Total objects: " + physicsSystem.dynamicsWorld.getNumCollisionObjects());

            // Wenn alles erfolgreich war, weise die finalen Member-Variablen zu
            this.shape = tempShape;
            this.motionState = tempMotionState;
            this.body = tempBody;

            Gdx.app.log("TerrainPhysics", "--- Finished TerrainPhysics Creation Successfully ---");

        } catch (Exception e) {
            Gdx.app.error("TerrainPhysics", "EXCEPTION during TerrainPhysics creation!", e);
            // Aufräumen im Fehlerfall - nur disposen, wenn Objekt != null ist
            if (tempBody != null) tempBody.dispose();
            if (tempMotionState != null) tempMotionState.dispose();
            if (tempShape != null) tempShape.dispose();
            // Stelle sicher, dass die finalen Member null bleiben oder wirf eine Exception
            /*this.shape = null;
            this.motionState = null;
            this.body = null;
            */

            throw new GdxRuntimeException("Failed to create TerrainPhysics", e);
        }
        // Der FloatBuffer (heightBuffer) wird NICHT disposed, da Bullet ihn braucht.
    }

    // findMinHeight / findMaxHeight bleiben gleich...
    private float findMinHeight(float[] data) {
        if (data == null || data.length == 0) return 0f;
        float min = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] < min) min = data[i];
        }
        return min;
    }
    private float findMaxHeight(float[] data) {
        if (data == null || data.length == 0) return 0f;
        float max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) max = data[i];
        }
        return max;
    }

    @Override
    public void dispose() {
        // Prüfe vor dem Disposen, ob die Objekte überhaupt erfolgreich initialisiert wurden
        if (body != null) {
            // Optional: Aus der Welt entfernen, bevor disposed wird
            // if (physicsSystem != null && physicsSystem.dynamicsWorld != null) {
            //     physicsSystem.dynamicsWorld.removeRigidBody(body);
            // }
            body.dispose();
        }
        if (motionState != null) motionState.dispose();
        if (shape != null) shape.dispose();
        Gdx.app.log("TerrainPhysics", "Disposed terrain physics objects (if they were created).");
    }
}