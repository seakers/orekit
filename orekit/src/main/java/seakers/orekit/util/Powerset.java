package seakers.orekit.util;

import java.util.ArrayList;

public class Powerset {
    public static <T> ArrayList<ArrayList<T>> powerSet(ArrayList<T> originalSet) {
        ArrayList<ArrayList<T>> sets = new ArrayList<>();
        if (originalSet.isEmpty()) {
            sets.add(new ArrayList<>());
            return sets;
        }
        ArrayList<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        ArrayList<T> rest = new ArrayList<>(list.subList(1, list.size()));
        for (ArrayList<T> set : powerSet(rest)) {
            ArrayList<T> newSet = new ArrayList<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
}
