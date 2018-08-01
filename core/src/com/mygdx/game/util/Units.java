package com.mygdx.game.util;

public class Units{
    
    /**
     *
     * BASE UNITS
     *
     * https://en.wikipedia.org/wiki/International_System_of_Units#Base_units
     *
     */
    
    
    /**
     * DISTANCE
     */
    
    public static float CM_TO_M = 1f/100f;
    public static float M_TO_KM = 1f/1000f;
    public static float AU_TO_KM = 149597870700f * M_TO_KM;
    
    public static float CM_TO_KM = CM_TO_M*M_TO_KM;
    public static float KM_TO_CM = 1f/(CM_TO_M*M_TO_KM);
    
    
    /**
     * MASS
     */
    
    public static float G_TO_KG = 1f/1000f;
    public static float KG_TO_MTONS = 1f/1000f;
    
    public static float G_TO_MTONS = G_TO_KG*KG_TO_MTONS;
    
    
    /**
     * TIME
     */

    public static float YR_TO_DAY = 365f;
    public static float DAY_TO_HR = 24f;
    public static float HR_TO_MIN = 60f;
    public static float MIN_TO_S = 60f;
    
    public static float YR_TO_S = YR_TO_DAY * DAY_TO_HR * HR_TO_MIN * MIN_TO_S;

    public static float CM_YR_TO_M_MA = 10000f;
    
    
    /**
     *
     * DERIVED UNITS
     *
     * https://en.wikipedia.org/wiki/International_System_of_Units#Derived_units
     *
     */
    
    
    
    
    
    
    
    /**
     * Energy
     * https://en.wikipedia.org/wiki/Newton_(unit)
     *
     */
    
    public static float N_TO_KN = 1000f;
    
}
