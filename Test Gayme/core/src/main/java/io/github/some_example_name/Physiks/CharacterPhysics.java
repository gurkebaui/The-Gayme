package io.github.some_example_name.Physiks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Disposable;

public class CharacterPhysics implements Disposable {

    public btRigidBody body;
    public btCollisionShape shape; // Kapsel ist oft gut für Charaktere
    public btDefaultMotionState motionState;
    public ModelInstance modelInstance; // Referenz zum visuellen Modell

    private final float mass = 80f; // Beispielmasse in kg
    private final Vector3 inertia = new Vector3();
    private final Vector3 tempVelocity = new Vector3();
    private final Vector3 tmpVec = new Vector3();
    // Wird berechnet

    public CharacterPhysics(PhysicsSystem physicsSystem, ModelInstance modelInstance, float radius, float height, float CHARACTER_MASS, Vector3 initialPosition) {
        this.modelInstance = modelInstance;

        // Kollisionsform: Kapsel ist oft am besten für Charaktere
        // Alternativen: btBoxShape, btSphereShape
        // Verwende KEINE komplexe Mesh-Form (btBvhTriangleMeshShape) für dynamische Objekte,
        // das ist langsam und oft instabil.
        shape = new btCapsuleShape(radius, height); // Y-Achse ist die Höhe

        // MotionState: Verbindet Physik-Transformation mit Grafik-Transformation
        motionState = new btDefaultMotionState();
        Matrix4 initialTransform = new Matrix4().setToTranslation(initialPosition);
        motionState.setWorldTransform(initialTransform);
        modelInstance.transform.set(initialTransform); // Setze auch die initiale Grafik-Position

        // Berechne die lokale Trägheit basierend auf Masse und Form
        shape.calculateLocalInertia(mass, inertia);

        // Konstruktionsinfo
        btRigidBody.btRigidBodyConstructionInfo constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(
                mass, motionState, shape, inertia
        );

        // Erstelle den RigidBody
        body = new btRigidBody(constructionInfo);


        // Im CharacterPhysics Konstruktor:
        body.setAngularFactor(0f); // Sperrt Rotation um ALLE Achsen
        body.setAngularVelocity(Vector3.Zero);
        Gdx.app.log("CharacterPhysics", "Set AngularFactor to 0 and AngularVelocity to Zero.");// Setze initiale Rotationsgeschwindigkeit auf Null

        // 2. Verhindern, dass der Körper "einschläft" und nicht mehr auf Kräfte reagiert
        body.setActivationState(Collision.DISABLE_DEACTIVATION);

        // Füge den Körper zur Physik-Welt hinzu
        physicsSystem.dynamicsWorld.addRigidBody(body);
    }

    // Methode zum Aktualisieren der Grafik basierend auf der Physik
    public void updateGraphicsTransform() {
        if (modelInstance != null && motionState != null) {
            motionState.getWorldTransform(modelInstance.transform);
        }
    }

    // Methoden zur Steuerung (Beispiele)
    public void move(Vector3 moveDirection, float speed) {
        if (body == null) return; // Sicherheitscheck

        // 1. Hole die AKTUELLE Geschwindigkeit vom Physik-Körper
        //    Wir brauchen besonders die aktuelle Y-Geschwindigkeit (velocity.y)
        //    Verwende getLinearVelocity(Vector3) um Garbage zu vermeiden
        Vector3 currentVelocity = body.getLinearVelocity();// Füllt tempVelocity mit der aktuellen Geschwindigkeit


        // 2. Berechne die gewünschte XZ-Geschwindigkeit
        //    Kopiere die Bewegungsrichtung, um sie nicht zu verändern
        //    und skaliere sie mit der Geschwindigkeit.
        Vector3 desiredXZVelocity = tempVelocity.set(moveDirection).scl(speed); // tempVelocity wird hier wiederverwendet

        // 3. Setze den wiederverwendbaren Vektor auf die finale gewünschte Geschwindigkeit
        //    Nimm die berechneten X und Z Werte und die AKTUELLE Y Geschwindigkeit.
        tempVelocity.set(desiredXZVelocity.x, currentVelocity.y, desiredXZVelocity.z); // tempVelocity wird nochmal gesetzt

        // 4. Übergebe das EINE Vector3-Objekt an setLinearVelocity
        body.setLinearVelocity(tempVelocity);

        // Körper aufwecken, falls er eingeschlafen ist
        body.activate();
    }

    public void jump(float jumpForce) {
        if (body == null) return;
        // Optional: Prüfen, ob der Charakter am Boden ist (z.B. mit Raycast)
        // Einfache Prüfung: Nur springen, wenn vertikale Geschwindigkeit klein ist
        Vector3 currentVelocity = body.getLinearVelocity();
        if (Math.abs(currentVelocity.y) < 0.5f) { // Kleine Toleranz
            body.applyCentralImpulse(tmpVec.set(0, jumpForce, 0)); // Verwende tmpVec für Impuls
            body.activate();
        }
    }
    // Stelle sicher, dass tmpVec auch als Member deklariert ist (für jump etc.)
     // Für temporäre Berechnungen wie Sprungimpuls


    @Override
    public void dispose() {
        // Wichtig: Objekte in der richtigen Reihenfolge disposen
        // physicsSystem.dynamicsWorld.removeRigidBody(body); // Optional

        if (body != null) body.dispose();
        if (motionState != null) motionState.dispose();
        if (shape != null) shape.dispose();
        // ConstructionInfo wird nicht separat disposed.
    }
}