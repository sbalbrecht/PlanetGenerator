package com.mygdx.game;

public class TileAttribute implements Comparable<TileAttribute>{
	private String name;
	private float value;
	
	public TileAttribute(){
		this.name = "";
		this.value = 0.0f;
	}
	
	public TileAttribute(float value){
		this.name = "";
		this.value = value;
	}
	public TileAttribute(String n){
		this.name = n;
		this.value = 0.0f;
	}
	
	
	public TileAttribute(String name, float value){
		this.name = name;
		this.value = value;
	}
	
	public void setValue(float value){
		this.value = value;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public float getValue(){
		return value;
	}
	
	public String getName(){
		return name;
	}
	
	@Override
	public String toString(){
		return String.format("%12s: %2.3f", name, value);
	}
	
	@Override
	public int compareTo(TileAttribute o){
		if (o.getValue() < this.value) return -1;
		else if (o.getValue() > this.value) return 1;
		else return 0;
	}
}
