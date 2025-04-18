package io.github.some_example_name.Player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import io.github.some_example_name.enums.CameraMode;
import io.github.some_example_name.Settings;

public class PlayerCameraController {

    private final PerspectiveCamera camera;
    private CameraMode cameraMode = CameraMode.BEHIND_PLAYER;

    private float camPitch = Settings.CAMERA_START_PITCH;
    private float distanceFromPlayer = 35f;
    private float angleAroundPlayer = 0f; // Aktueller Winkel um den Spieler

    public PlayerCameraController(PerspectiveCamera camera) {
        this.camera = camera;
    }

    public void update(Vector3 playerPosition, float angleBehindPlayer, float deltaTime) {
        // Maus-Input für Kamerarotation und Pitch
        calculatePitch();
        calculateAngleAroundPlayer(angleBehindPlayer);

        // Kamera-Position berechnen
        float horDistance = calculateHorizontalDistance(distanceFromPlayer);
        float vertDistance = calculateVerticalDistance(distanceFromPlayer);
        calculateCameraPosition(playerPosition, horDistance, vertDistance);

        // Kamera ausrichten
        camera.up.set(Vector3.Y);
        camera.lookAt(playerPosition);
        camera.update();
    }

    private void calculateCameraPosition(Vector3 playerPosition, float horDistance, float vertDistance) {
        float theta = angleAroundPlayer; // Winkel verwenden, der in calculateAngleAroundPlayer berechnet wurde
        float offsetX = (float) (horDistance * Math.sin(Math.toRadians(theta)));
        float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(theta)));

        camera.position.x = playerPosition.x - offsetX;
        camera.position.z = playerPosition.z - offsetZ;
        camera.position.y = playerPosition.y + vertDistance;
    }

    private void calculateAngleAroundPlayer(float angleBehindPlayer) {
        if (cameraMode == CameraMode.FREE_LOOK) {
            float angleChange = Gdx.input.getDeltaX() * Settings.CAMERA_ANGLE_AROUND_PLAYER_FACTOR;
            angleAroundPlayer -= angleChange; // Direkt den aktuellen Winkel ändern
        } else {
            // Im BEHIND_PLAYER Modus folgt die Kamera dem Spielerwinkel
            angleAroundPlayer = angleBehindPlayer;
        }
        // Winkel normalisieren (optional)
        angleAroundPlayer = angleAroundPlayer % 360;
    }

    private void calculatePitch() {
        // Nur Pitch ändern, wenn Maus bewegt wird (oder im FREE_LOOK?)
        // Im Moment wird es immer geändert, wenn die Maus vertikal bewegt wird.
        float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
        camPitch -= pitchChange;

        // Pitch begrenzen
        if (camPitch < Settings.CAMERA_MIN_PITCH)
            camPitch = Settings.CAMERA_MIN_PITCH;
        else if (camPitch > Settings.CAMERA_MAX_PITCH)
            camPitch = Settings.CAMERA_MAX_PITCH;
    }

    private float calculateVerticalDistance(float distance) {
        return (float) (distance * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateHorizontalDistance(float distance) {
        return (float) (distance * Math.cos(Math.toRadians(camPitch)));
    }

    // Methode für Zoom über das Scrollrad
    public boolean scrolled(float amountY) {
        float zoomLevel = amountY * Settings.CAMERA_ZOOM_LEVEL_FACTOR;
        distanceFromPlayer += zoomLevel;
        if (distanceFromPlayer < Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER)
            distanceFromPlayer = Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER;
        // Gib true zurück, um anzuzeigen, dass das Event behandelt wurde (optional)
        return true;
    }

    // Methode zum Umschalten des Kameramodus
    public void toggleCameraMode(float currentAngleBehindPlayer) {
        switch (cameraMode) {
            case FREE_LOOK:
                cameraMode = CameraMode.BEHIND_PLAYER;
                // Beim Wechsel zurück, setze den Kamerawinkel auf den Spielerwinkel
                angleAroundPlayer = currentAngleBehindPlayer;
                break;
            case BEHIND_PLAYER:
                cameraMode = CameraMode.FREE_LOOK;
                // Beim Wechsel zu Free Look bleibt der aktuelle Winkel erhalten
                break;
        }
        System.out.println("Camera Mode switched to: " + cameraMode); // Debugging
    }

    // Getter falls benötigt
    public CameraMode getCameraMode() {
        return cameraMode;
    }
}
