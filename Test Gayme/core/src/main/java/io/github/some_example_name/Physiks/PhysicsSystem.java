package io.github.some_example_name.Physiks;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.utils.Disposable;

public class PhysicsSystem implements Disposable {

    public btCollisionConfiguration collisionConfiguration;
    public btCollisionDispatcher dispatcher;
    public btBroadphaseInterface broadphase;
    public btConstraintSolver solver;
    public btDiscreteDynamicsWorld dynamicsWorld;

    private final int MAX_SUBSTEPS = 5;
    private final float FIXED_TIME_STEP = 1f / 60f;

    public PhysicsSystem() {
        // Konfiguration für Kollisionen
        collisionConfiguration = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfiguration);
        // Effiziente Verwaltung von Kollisionsobjekten (DBVT = Dynamic Bounding Volume Tree)
        broadphase = new btDbvtBroadphase();
        // Löst Kollisionen und Constraints auf
        solver = new btSequentialImpulseConstraintSolver();
        // Die eigentliche Physik-Welt
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        // Schwerkraft setzen (typischerweise negativ auf der Y-Achse)
        dynamicsWorld.setGravity(new Vector3(0, -9.81f, 0));
    }

    public void update(float deltaTime) {
        // Physiksimulation fortschreiten lassen
        dynamicsWorld.stepSimulation(deltaTime, MAX_SUBSTEPS, FIXED_TIME_STEP);
    }

    @Override
    public void dispose() {
        dynamicsWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfiguration.dispose();
    }
}
