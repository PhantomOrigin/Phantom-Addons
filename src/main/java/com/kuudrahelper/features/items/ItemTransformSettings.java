package com.kuudrahelper.features.items;

public class ItemTransformSettings {
    public boolean enabled         = false;
    public float   posX            = 0f, posY = 0f, posZ = 0f;
    public float   rotX            = 0f, rotY = 0f, rotZ = 0f;
    public float   scale           = 1f;
    public float   swingSpeed      = 1f;
    public float   proximity       = 0f;
    public boolean noEquipAnimation = false;
    public boolean inPlaceSwing    = false;
    public boolean staticPosition  = false;

    public boolean isDefault() {
        return posX == 0 && posY == 0 && posZ == 0
                && rotX == 0 && rotY == 0 && rotZ == 0
                && (scale == 0 || scale == 1f)
                && (swingSpeed == 0 || swingSpeed == 1f)
                && proximity == 0
                && !noEquipAnimation && !inPlaceSwing && !staticPosition;
    }
}