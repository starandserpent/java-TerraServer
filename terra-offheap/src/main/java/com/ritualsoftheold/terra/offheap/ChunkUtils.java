package com.ritualsoftheold.terra.offheap;

public class ChunkUtils {
    
    private ChunkUtils() {}
    
    public static float distance(float x, float y, float z) {
        return x + y * 16 + z * 256;
    }
    
    /**
     * Some black magic to get 0.5m/0.25m block index from
     * coordinates relative to bigger block's center.
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static int getSmallBlockIndex(float x, float y, float z) {
        if (x <= 0) {
            if (y <= 0) {
                if (z <= 0) {
                    return 0;
                } else {
                    return 4;
                }
            } else {
                if (z <= 0) {
                    return 2;
                } else {
                    return 6;
                }
            }
        } else {
            if (y <= 0) {
                if (z <= 0) {
                    return 1;
                } else {
                    return 5;
                }
            } else {
                if (z <= 0) {
                    return 3;
                } else {
                    return 7;
                }
            }
        }
    }
    
    public static int get025BlockIndex(float x, float y, float z) {
        // TODO test this
        if (x <= -0.5f) {
            if (y <= -0.5f) {
                if (z <= -0.5f) {
                    return 0;
                } else if (z <= 0) {
                    return 16;
                } else if (z <= 0.5f) {
                    return 32;
                } else {
                    return 48;
                }
            } else if (y <= 0) {
                if (z <= -0.5f) {
                    return 4;
                } else if (z <= 0) {
                    return 20;
                } else if (z <= 0.5f) {
                    return 36;
                } else {
                    return 52;
                }
            } else if (y < 0.5f) {
                if (z <= -0.5f) {
                    return 8;
                } else if (z <= 0) {
                    return 24;
                } else if (z <= 0.5f) {
                    return 40;
                } else {
                    return 56;
                }
            } else {
                if (z <= -0.5f) {
                    return 12;
                } else if (z <= 0) {
                    return 28;
                } else if (z <= 0.5f) {
                    return 44;
                } else {
                    return 60;
                }
            }
        } else if (x <= 0) {
            if (y <= -0.5f) {
                if (z <= -0.5f) {
                    return 1;
                } else if (z <= 0) {
                    return 17;
                } else if (z <= 0.5f) {
                    return 33;
                } else {
                    return 49;
                }
            } else if (y <= 0) {
                if (z <= -0.5f) {
                    return 5;
                } else if (z <= 0) {
                    return 21;
                } else if (z <= 0.5f) {
                    return 37;
                } else {
                    return 53;
                }
            } else if (y < 0.5f) {
                if (z <= -0.5f) {
                    return 9;
                } else if (z <= 0) {
                    return 25;
                } else if (z <= 0.5f) {
                    return 41;
                } else {
                    return 57;
                }
            } else {
                if (z <= -0.5f) {
                    return 13;
                } else if (z <= 0) {
                    return 29;
                } else if (z <= 0.5f) {
                    return 45;
                } else {
                    return 61;
                }
            }
        } else if (x <= 0.5f) {
            if (y <= -0.5f) {
                if (z <= -0.5f) {
                    return 2;
                } else if (z <= 0) {
                    return 18;
                } else if (z <= 0.5f) {
                    return 34;
                } else {
                    return 50;
                }
            } else if (y <= 0) {
                if (z <= -0.5f) {
                    return 6;
                } else if (z <= 0) {
                    return 22;
                } else if (z <= 0.5f) {
                    return 38;
                } else {
                    return 54;
                }
            } else if (y < 0.5f) {
                if (z <= -0.5f) {
                    return 10;
                } else if (z <= 0) {
                    return 26;
                } else if (z <= 0.5f) {
                    return 42;
                } else {
                    return 58;
                }
            } else {
                if (z <= -0.5f) {
                    return 14;
                } else if (z <= 0) {
                    return 30;
                } else if (z <= 0.5f) {
                    return 46;
                } else {
                    return 62;
                }
            }
        } else {
            if (y <= -0.5f) {
                if (z <= -0.5f) {
                    return 3;
                } else if (z <= 0) {
                    return 19;
                } else if (z <= 0.5f) {
                    return 35;
                } else {
                    return 51;
                }
            } else if (y <= 0) {
                if (z <= -0.5f) {
                    return 7;
                } else if (z <= 0) {
                    return 23;
                } else if (z <= 0.5f) {
                    return 39;
                } else {
                    return 55;
                }
            } else if (y < 0.5f) {
                if (z <= -0.5f) {
                    return 11;
                } else if (z <= 0) {
                    return 27;
                } else if (z <= 0.5f) {
                    return 43;
                } else {
                    return 59;
                }
            } else {
                if (z <= -0.5f) {
                    return 15;
                } else if (z <= 0) {
                    return 31;
                } else if (z <= 0.5f) {
                    return 47;
                } else {
                    return 63;
                }
            }
        }
    }
}
