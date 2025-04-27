package io.github.some_example_name.Player; // Stelle sicher, dass der Paketname stimmt

import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.scene.Scene;

public class Player { // Name der Klasse ist jetzt Player

    private final Scene playerScene; // Feld zum Speichern der Szene

    private final Vector3 currentPosition = new Vector3();
    private float angleBehindPlayer = 0f;

    // HIER IST DER WICHTIGE TEIL: Der Konstruktor
    public Player(Scene playerScene) { // Akzeptiert ein Scene-Objekt
        this.playerScene = playerScene; // Speichert die übergebene Szene im Feld
        // Initialposition holen
        this.playerScene.modelInstance.transform.getTranslation(currentPosition);
    }



    // Getter für Daten, die andere Klassen (wie der CameraController) brauchen


    public float getAngleBehindPlayer() {
        return angleBehindPlayer;
    }

    // Optional: Getter für die Scene, falls andere Teile darauf zugreifen müssen
    public Scene getPlayerScene() {
        return playerScene;
    }
}