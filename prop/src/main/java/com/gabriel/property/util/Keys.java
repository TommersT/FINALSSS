package com.gabriel.property.util;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;

public final class Keys {

    private static ArrayList<Integer> invalidKeys = new ArrayList<>(Arrays.asList(
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_SHIFT,
            KeyEvent.VK_ALT,
            KeyEvent.VK_CONTROL
    ));

    public static boolean ignoreKey(int key) {
        return invalidKeys.contains(key);
    }
}
