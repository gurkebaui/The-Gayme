package io.github.some_example_name.Physiks; // Oder dein Paket

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.Gdx;

public class StaticObjectPhysics implements Disposable {

    public final btRigidBody body;
    private final btCollisionShape shape;
    private final btDefaultMotionState motionState;
    // Optional: Referenz zur ModelInstance, wenn du sie hier verwalten willst
    // public ModelInstance modelInstance;

    /**
     * Erstellt einen statischen Physik-Körper für ein Objekt mit einer Box-Form.
     *
     * @param physicsSystem Das Physik-System.
     * @param halfExtents Die halben Ausmaße der Box (Hälfte der Breite, Höhe, Tiefe).
     * @param worldTransform Die Transformationsmatrix (Position, Rotation) in der Welt.
     */
    public StaticObjectPhysics(PhysicsSystem physicsSystem, Vector3 halfExtents, Matrix4 worldTransform) {
        // this.modelInstance = modelInstance; // Falls du es hier speicherst

        // 1. Kollisionsform erstellen (Box)
        // halfExtents ist die Hälfte der Breite, Höhe und Tiefe der Box vom Zentrum aus.
        this.shape = new btBoxShape(halfExtents);
        Gdx.app.log("StaticObjectPhysics", "Created btBoxShape with halfExtents: " + halfExtents);

        // 2. MotionState erstellen und Welt-Transformation setzen
        this.motionState = new btDefaultMotionState();
        this.motionState.setWorldTransform(worldTransform);
        Vector3 pos = new Vector3();
        worldTransform.getTranslation(pos);
        Gdx.app.log("StaticObjectPhysics", "Set initial MotionState world transform position to: " + pos);


        // 3. RigidBody Konstruktionsinfo (statisch)
        float mass = 0f; // Masse 0 bedeutet statisch
        Vector3 localInertia = new Vector3(0, 0, 0); // Keine Trägheit für statische Objekte
        btRigidBody.btRigidBodyConstructionInfo constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(
                mass, motionState, shape, localInertia
        );

        // 4. RigidBody erstellen
        this.body = new btRigidBody(constructionInfo);
        this.body.setCollisionFlags(this.body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_STATIC_OBJECT);
        this.body.setFriction(0.9f); // Häuser haben oft hohe Reibung
        // Optional: UserData setzen, falls du später auf das spezifische Haus zugreifen musst
        // this.body.userData = "House"; // Oder eine komplexere ID

        Gdx.app.log("StaticObjectPhysics", "Created static btRigidBody.");

        // 5. Körper zur Physik-Welt hinzufügen
        physicsSystem.dynamicsWorld.addRigidBody(this.body);
        Gdx.app.log("StaticObjectPhysics", "Static object rigid body added to the world. Total objects: " + physicsSystem.dynamicsWorld.getNumCollisionObjects());
    }

    /**
     * Alternative: Erstelle das Physik-Objekt basierend auf einer ModelInstance.
     * Die Transformation wird von der ModelInstance übernommen.
     * Die halfExtents müssen trotzdem angegeben werden oder aus der BoundingBox der ModelInstance berechnet werden.
     */
    public StaticObjectPhysics(PhysicsSystem physicsSystem, Vector3 halfExtents, ModelInstance modelInstanceToMatch) {
        this(physicsSystem, halfExtents, modelInstanceToMatch.transform);
        // this.modelInstance = modelInstanceToMatch; // Speichern für Referenz
    }


    @Override
    public void dispose() {
        // Wichtig: In umgekehrter Reihenfolge der Erstellung (oder zumindest Body vor Shape/MotionState)
        if (body != null) {
            // Optional: Vor dem Disposen aus der Welt entfernen
            // if (physicsSystem != null && physicsSystem.dynamicsWorld != null) {
            //     physicsSystem.dynamicsWorld.removeRigidBody(body);
            // }
            body.dispose();
        }
        if (motionState != null) motionState.dispose();
        if (shape != null) shape.dispose();
        Gdx.app.log("StaticObjectPhysics", "Disposed static object physics.");
    }
}
