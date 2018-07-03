package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import static com.badlogic.gdx.math.MathUtils.cos;
import static com.badlogic.gdx.math.MathUtils.sin;

public class CameraInterface{
	
	private PerspectiveCamera cam;
	float u, v, du, dv;
	float ds = 1f; //u = azimuth; v = lat; ds = d-spherical_distance
	float orbitDistance;
	
	
	Vector3 center;
	
	boolean keyboardSelection;
	
	public CameraInterface(PerspectiveCamera cam, Planet planet){
		this.cam = cam;
		keyboardSelection = true;
		center = planet.position;
		orbitDistance = cam.position.len();
		
		float 	x = cam.position.x,
				y = cam.position.y,
				z = cam.position.z;
		
		
		u = MathUtils.atan2(z, x);
		v = MathUtils.atan2((float)Math.sqrt(x*x + z*z), y);
		
	}
	
	public void update(){
		input();
		cam.position.setFromSpherical(u, v).scl(orbitDistance);
		
		cam.lookAt(center);
		du=dv=0;
	}
	
	public void input(){
		if(Gdx.input.isButtonPressed(Input.Keys.A)){
			du -= ds;
		}
		if(Gdx.input.isKeyPressed(Input.Keys.D)){
			du += ds;
		}
		if(Gdx.input.isKeyPressed(Input.Keys.W)){
			dv += ds;
		}
		if(Gdx.input.isKeyPressed(Input.Keys.S)){
			du -= ds;
		}
		
		u += du;
		v += dv;
		
	}
	
	
	
	
	
}
