package com.example.labxplorer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

public class ArActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ModelRenderable modelRenderable;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_simulate);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        progressBar = findViewById(R.id.progressBar);

        load3DModel();

        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (modelRenderable == null) {
                Toast.makeText(this, "Model not loaded yet!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                Toast.makeText(this, "Please tap on a horizontal plane", Toast.LENGTH_SHORT).show();
                return;
            }

            Anchor anchor = hitResult.createAnchor();
            place3DModel(anchor);
        });
    }

    private void load3DModel() {
        Log.d("ArActivity", "Starting model loading...");
        progressBar.setVisibility(View.VISIBLE);

        ModelRenderable.builder()
                .setSource(this, RenderableSource.builder()
                        .setSource(this, Uri.parse("file:///android_asset/chemsar.glb"), RenderableSource.SourceType.GLTF2)
                        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                        .build())

                .setRegistryId("chemar.glb")
                .build()
                .thenAccept(renderable -> {
                    modelRenderable = renderable;
                    progressBar.setVisibility(View.GONE);
                    Log.d("ArActivity", "Model loaded successfully");
                })
                .exceptionally(throwable -> {
                    Log.e("ArActivity", "Error loading 3D model", throwable);
                    Toast.makeText(this, "Failed to load 3D model", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return null;
                });
    }

    private void place3DModel(Anchor anchor) {
        if (modelRenderable == null) {
            Toast.makeText(this, "Model not loaded yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        Node modelNode = new Node();
        modelNode.setRenderable(modelRenderable);
        modelNode.setParent(anchorNode);

        // Optionally scale the model
        modelNode.setLocalScale(new com.google.ar.sceneform.math.Vector3(0.1f, 0.1f, 0.1f));
    }
}
