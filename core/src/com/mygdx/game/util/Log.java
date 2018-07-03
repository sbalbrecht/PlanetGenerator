package com.mygdx.game.util;

public class Log{

	long dt;
	String label;
	boolean logging;
	int indent;

	public Log(){
		dt = 0;
	}
	
	public static void log(String s){
		System.out.println(String.format("%s : %s", (System.currentTimeMillis()+"").substring(8), s));
	}
	
	public void start(String label){
		if(logging)
		    end();
		logging = true;
		this.label = label;
		dt = System.currentTimeMillis();
	}

	public void end(){
		logging = false;
		dt = System.currentTimeMillis()-dt;
		if(label != null) log(String.format(label + ": %d ms", dt));
	}

}
