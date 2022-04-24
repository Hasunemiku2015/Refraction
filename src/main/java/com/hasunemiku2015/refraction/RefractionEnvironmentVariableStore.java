package com.hasunemiku2015.refraction;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class RefractionEnvironmentVariableStore {
    private static final Map<String, String> e = new HashMap<>();

    public static void put(String key, String value){
        String f = "${" + key + "}";
        if (!e.containsKey(f)) {
            e.put(f, value);
        }
    }

    public static String a(String b){
        String c = b;
        for (String d : e.keySet()){
            c = b.replace(d, e.get(d));
        }
        return c;
    }
}
