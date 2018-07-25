package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import static java.lang.Math.log;

public class CameraInterface implements InputProcessor{

	private PerspectiveCamera cam;
	float u, v;
	
	Vector3 center;
	
	boolean up, down, left, right;
	
	float radius;
	float orbitSpeed = 40.0f; // should be faster farther away
	
	public CameraInterface(PerspectiveCamera cam){
		this.cam = cam;
		Gdx.input.setInputProcessor(this);
	}
	
	public void update(float dt){
		this.cam.rotateAround(center, cam.up, u*dt);
		this.cam.rotateAround(center, cam.up.cpy().crs(cam.direction), v*dt);
		cam.up.set(Vector3.Y);
		cam.lookAt(center);
		cam.update();
		
	}
	
	@Override
	public boolean keyDown(int keycode){
		
		switch(keycode){
			case Input.Keys.W:
				v = 1.0f * orbitSpeed;
				up = true;
				break;
			case Input.Keys.S:
				v = -1.0f * orbitSpeed;
				down = true;
				break;
			case Input.Keys.A:
				u = -1.0f * orbitSpeed;
				left = true;
				break;
			case Input.Keys.D:
				u = 1.0f * orbitSpeed;
				right = true;
				break;
			default: return false;
		}
		return true;
	}
	
	@Override
	public boolean keyUp(int keycode){
		switch(keycode){
			case Input.Keys.W:
				v = down ? -1.0f * orbitSpeed : 0.0f;
				up = false;
				break;
			case Input.Keys.S:
				v = up ? 1.0f * orbitSpeed : 0.0f;
				down = false;
				break;
			case Input.Keys.A:
				u = right ? 1.0f * orbitSpeed : 0.0f;
				left = false;
				break;
			case Input.Keys.D:
				u = left ? -1.0f * orbitSpeed : 0.0f;
				right = false;
				break;
			default: return false;
		}
		
		return true;
	}
	
	
	@Override
	public boolean keyTyped(char character){
		return false;
	}
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button){
		return false;
	}
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button){
		return false;
	}
	
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer){
		return false;
	}
	
	@Override
	public boolean mouseMoved(int screenX, int screenY){
		return false;
	}
	
	@Override
	public boolean scrolled(int amount){
		float amountMultiplier = 0.0125f; // should be faster farther away? to a limit (starting distance)
		cam.position.add((center.x - cam.position.x)*-amount*amountMultiplier,
				         (center.y - cam.position.y)*-amount*amountMultiplier,
                         (center.z - cam.position.z)*-amount*amountMultiplier);
		
		return false;
	}
}
