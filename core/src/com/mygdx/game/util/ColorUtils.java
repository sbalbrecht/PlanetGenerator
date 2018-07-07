package com.mygdx.game.util;

import com.badlogic.gdx.graphics.Color;

public final class ColorUtils {
    public static Color getComplementary(Color colorToInvert) {
        float[] hsv = {
                colorToInvert.r,
                colorToInvert.g,
                colorToInvert.b};

        hsv = colorToInvert.toHsv(hsv);

        hsv[0] = (hsv[0] + 180) % 360;

        if(hsv[1] < .6 && hsv[2] > .6)
            hsv[2] -= .4;
        else if(hsv[2] < .5)
            hsv[2] += .4;

        return new Color().fromHsv(hsv);
    }
}
