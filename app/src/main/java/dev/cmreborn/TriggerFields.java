package dev.cmreborn;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Selects only uniquely typed instance fields, never the first non-null field. */
final class TriggerFields {
    private TriggerFields() {}

    static Field account(Class<?> owner, Class<?> accountType) {
        List<Field> matches = new ArrayList<>();
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == accountType) {
                matches.add(field);
            }
        }
        return ResolutionPolicy.unique(matches);
    }

    static Field peer(Class<?> searchBox, Class<?> peerType) {
        List<Field> matches = new ArrayList<>();
        for (Field field : searchBox.getDeclaredFields()) {
            Class<?> contract = field.getType();
            if (!Modifier.isStatic(field.getModifiers()) && contract.isInterface()
                    && contract.isAssignableFrom(peerType)) matches.add(field);
        }
        return ResolutionPolicy.unique(matches);
    }
}
