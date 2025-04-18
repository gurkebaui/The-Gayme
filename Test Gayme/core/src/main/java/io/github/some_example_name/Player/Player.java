package io.github.some_example_name.Player; // Stelle sicher, dass der Paketname stimmt

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import io.github.some_example_name.Settings; // Importiere deine Settings
import net.mgsx.gltf.scene3d.scene.Scene;

public class Player { // Name der Klasse ist jetzt Player

    private final Scene playerScene; // Feld zum Speichern der Szene
    private final float speed = Settings.PLAYER_MOVE_SPEED;
    private final float rotationSpeed = Settings.PLAYER_ROTATION_SPEED;

    private final Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    private final Vector3 currentPosition = new Vector3();
    private float angleBehindPlayer = 0f;

    // HIER IST DER WICHTIGE TEIL: Der Konstruktor
    public Player(Scene playerScene) { // Akzeptiert ein Scene-Objekt
        this.playerScene = playerScene; // Speichert die übergebene Szene im Feld
        // Initialposition holen
        this.playerScene.modelInstance.transform.getTranslation(currentPosition);
    }

    public void processInput(float deltaTime) {
        // Spieler-Transform holen
        playerTransform.set(playerScene.modelInstance.transform);

        // Bewegung zurücksetzen für diesen Frame
        moveTranslation.set(0, 0, 0);
        float rotationChange = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveTranslation.z += speed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveTranslation.z -= speed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            rotationChange = rotationSpeed * deltaTime;
            playerTransform.rotate(Vector3.Y, rotationChange);
            angleBehindPlayer += rotationChange; // Winkel für die Kamera anpassen
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            rotationChange = -rotationSpeed * deltaTime;
            playerTransform.rotate(Vector3.Y, rotationChange);
            angleBehindPlayer += rotationChange; // Winkel für die Kamera anpassen
        }

        // Winkel normalisieren (optional, aber kann helfen)
        angleBehindPlayer = angleBehindPlayer % 360;

        // Bewegungs-Translation anwenden
        playerTransform.translate(moveTranslation);

        // Modifizierte Transformation setzen
        playerScene.modelInstance.transform.set(playerTransform);

        // Aktuelle Position aktualisieren
        playerScene.modelInstance.transform.getTranslation(currentPosition);
    }

    // Getter für Daten, die andere Klassen (wie der CameraController) brauchen
    public Vector3 getCurrentPosition() {
        // Stelle sicher, dass die Position aktuell ist, bevor sie zurückgegeben wird
        // playerScene.modelInstance.transform.getTranslation(currentPosition); // Ist in processInput schon drin
        return currentPosition;
    }

    public float getAngleBehindPlayer() {
        return angleBehindPlayer;
    }

    // Optional: Getter für die Scene, falls andere Teile darauf zugreifen müssen
    public Scene getPlayerScene() {
        return playerScene;
    }
}