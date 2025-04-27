package io.github.some_example_name.Player; // Oder dein bevorzugtes Paket

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import io.github.some_example_name.Physiks.CharacterPhysics;

public class PlayerInputHandler {

    // Abhängigkeiten, die von außen kommen
    private final CharacterPhysics characterPhysics;
    private final PerspectiveCamera camera;

    // Konstanten (könnten auch aus einer Settings-Klasse kommen)
    private final float MOVE_SPEED;
    private final float JUMP_FORCE;

    // Hilfsvektoren (um Garbage zu vermeiden)
    private final Vector3 tmpVec = new Vector3();
    private final Vector3 moveDirection = new Vector3(); // Wird hier wiederverwendet
    private final Vector3 newVelocity = new Vector3();
    private final Vector3 stopHorizontalVelocity = new Vector3();
    // currentLinearVelocity wird jetzt direkt geholt

    public PlayerInputHandler(CharacterPhysics characterPhysics, PerspectiveCamera camera, float moveSpeed, float jumpForce) {
        this.characterPhysics = characterPhysics;
        this.camera = camera;
        this.MOVE_SPEED = moveSpeed;
        this.JUMP_FORCE = jumpForce;

        if (this.characterPhysics == null) {
            Gdx.app.error("PlayerInputHandler", "CharacterPhysics cannot be null!");
            // Optional: Fehler werfen oder Dummy-Objekt verwenden
        }
        if (this.camera == null) {
            Gdx.app.error("PlayerInputHandler", "Camera cannot be null!");
        }
    }

    /**
     * Verarbeitet die Eingaben für Bewegung und Sprung in jedem Frame.
     * Sollte in der render()-Methode der Hauptklasse aufgerufen werden.
     */
    public void processInput() {
        // Sicherheitscheck, falls characterPhysics zur Laufzeit null wird (sollte nicht passieren)
        if (characterPhysics == null || characterPhysics.body == null) {
            // Gdx.app.log("PlayerInputHandler", "CharacterPhysics or body is null, skipping input.");
            return;
        }

        // --- Bewegungsrichtung basierend auf Input und Kamera berechnen ---
        moveDirection.set(0, 0, 0); // Richtung für diesen Frame zurücksetzen

        // Kamera-relative Bewegung
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            tmpVec.set(camera.direction); // Holen der Kamerarichtung
            moveDirection.add(tmpVec.x, 0, tmpVec.z);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            tmpVec.set(camera.direction);
            moveDirection.sub(tmpVec.x, 0, tmpVec.z);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            tmpVec.set(camera.direction).crs(camera.up).nor(); // tmpVec = Links
            moveDirection.add(tmpVec.x, 0, tmpVec.z); // Addiere Links-Komponenten
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            tmpVec.set(camera.direction).crs(camera.up).nor(); // tmpVec = Links
            moveDirection.sub(tmpVec.x, 0, tmpVec.z);
            // Subtrahiere Links-Komponenten -> ergibt Rechts
        }


        // --- Bewegung anwenden oder stoppen ---
        if (!moveDirection.isZero()) { // Nur wenn eine Bewegungstaste gedrückt ist
            moveDirection.nor(); // Normalisieren der kombinierten Richtung

            Vector3 currentLinearVelocity = characterPhysics.body.getLinearVelocity(); // Erzeugt Garbage!
            newVelocity.set(moveDirection).scl(MOVE_SPEED);
            newVelocity.y = currentLinearVelocity.y; // Y-Geschwindigkeit beibehalten
            characterPhysics.body.setLinearVelocity(newVelocity);
            characterPhysics.body.activate(); // Sicherstellen, dass der Körper reagiert
            characterPhysics.body.setAngularVelocity(Vector3.Zero); // Unerwünschte Drehung verhindern

        } else { // Keine Bewegungstaste gedrückt -> Horizontale Bewegung stoppen
            Vector3 currentLinearVelocity = characterPhysics.body.getLinearVelocity(); // Erzeugt Garbage!
            // Nur stoppen, wenn der Charakter sich horizontal bewegt (kleiner Schwellenwert)
            if (Math.abs(currentLinearVelocity.x) > 0.1f || Math.abs(currentLinearVelocity.z) > 0.1f) {
                stopHorizontalVelocity.set(0, currentLinearVelocity.y, 0);
                characterPhysics.body.setLinearVelocity(stopHorizontalVelocity);
            }
            // Winkelgeschwindigkeit stoppen, wenn Charakter steht
            characterPhysics.body.setAngularVelocity(Vector3.Zero);
        }

        // --- Sprung ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Vector3 currentLinearVelocity = characterPhysics.body.getLinearVelocity(); // Erzeugt Garbage!
            // Einfacher Bodencheck (könnte durch Raycast verbessert werden)
            if (Math.abs(currentLinearVelocity.y) < 0.5f) {
                characterPhysics.jump(JUMP_FORCE);
                Gdx.app.log("PlayerInputHandler", "Jump initiated with force: " + JUMP_FORCE);
            } // else {
            // Gdx.app.log("PlayerInputHandler", "Jump blocked (in air). Current Y velocity: " + currentLinearVelocity.y);
            //}
        }
    }
}