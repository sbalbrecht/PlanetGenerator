package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraInterface implements InputProcessor {

	private PerspectiveCamera cam;
	private float u, v;
	private Vector2 lastTouch = new Vector2();
	
	Vector3 center;
	
	private boolean up, down, left, right, leftMouseClicked, leftMouseHeld;
	
	float radius;
	private float orbitSpeed = 40.0f; // should be faster farther away
    private final int touchSpeedReductionFactor = 3;


    public CameraInterface(PerspectiveCamera cam) {
		this.cam = cam;
		Gdx.input.setInputProcessor(this);
	}
	
	public void update(float dt){
	    // TODO: Impose vertical limits on camera to avoid erratic spinning
		this.cam.rotateAround(center, cam.up, u*dt);
		this.cam.rotateAround(center, cam.up.cpy().crs(cam.direction), v*dt);
		cam.up.set(Vector3.Y);
		cam.lookAt(center);
		cam.update();
        if(leftMouseHeld) {
            u = 0;
            v = 0;
            leftMouseHeld = false;
        }
	}
	
	@Override
	public boolean keyDown(int keycode) {

		if (keycode == Input.Keys.W || keycode == Input.Keys.UP) {
			v = 1.0f * orbitSpeed;
			up = true;
		} else if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
			v = -1.0f * orbitSpeed;
			down = true;
		} else if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
			u = -1.0f * orbitSpeed;
			left = true;
		} else if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
			u = 1.0f * orbitSpeed;
			right = true;
		} else {
			return false;
		}

		return true;
	}
	
	@Override
	public boolean keyUp(int keycode) {

		if (keycode == Input.Keys.W || keycode == Input.Keys.UP) {
			v = down ? -1.0f * orbitSpeed : 0.0f;
			up = false;
		} else if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
			v = up ? 1.0f * orbitSpeed : 0.0f;
			down = false;
		} else if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
			u = right ? 1.0f * orbitSpeed : 0.0f;
			left = false;
		} else if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
			u = left ? -1.0f * orbitSpeed : 0.0f;
			right = false;
		} else {
			return false;
		}

		return true;
	}
	
	
	@Override
	public boolean keyTyped(char character) {
		return false;
	}
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		leftMouseClicked = (button == Input.Buttons.LEFT);
		if(!leftMouseClicked) return false;

		lastTouch.set(screenX, screenY);
		return true;
	}
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		Vector2 newTouch = new Vector2(screenX, screenY);
		Vector2 delta = newTouch.cpy().sub(lastTouch);
		if(delta.x < 0) {
			u = right ? delta.x * orbitSpeed/touchSpeedReductionFactor : 0.0f;
			left = false;
		} else {
			u = left ? -delta.x * orbitSpeed/touchSpeedReductionFactor : 0.0f;
			right = false;
		}

		if(delta.y < 0) {
			v = down ? delta.y * orbitSpeed/touchSpeedReductionFactor : 0.0f;
			up = false;
		} else {
			v = up ? -delta.y * orbitSpeed/touchSpeedReductionFactor : 0.0f;
			down = false;
		}

		lastTouch = newTouch;

		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if(!leftMouseClicked) {
		    return false;
        }

		Vector2 newTouch = new Vector2(screenX, screenY);
		Vector2 delta = newTouch.cpy().sub(lastTouch);

		u = -delta.x * orbitSpeed/touchSpeedReductionFactor;
		v = delta.y * orbitSpeed/touchSpeedReductionFactor;

		lastTouch = newTouch;
		leftMouseHeld = true;
		return true;
	}
	
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}
	
	@Override
	public boolean scrolled(int amount) {
		float scaledAmount = -amount*0.025f; // should be faster farther away? to a limit (starting distance)
        Vector3 translation = new Vector3((center.x - cam.position.x)*scaledAmount,
                                          (center.y - cam.position.y)*scaledAmount,
                                          (center.z - cam.position.z)*scaledAmount);
        float newDistanceFromCenter = cam.position.cpy().add(translation).dst(center);
        if(newDistanceFromCenter < 10.1f || newDistanceFromCenter > 36f)
            translation = new Vector3(0,0,0);
		cam.position.add(translation);
		
		return false;
	}
}
