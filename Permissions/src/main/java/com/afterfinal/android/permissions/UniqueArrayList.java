package com.afterfinal.android.permissions;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class UniqueArrayList extends ArrayList<String> {
    @Override
    public boolean add(String s) {
        if (contains(s)) {
            return false;
        }
        return super.add(s);
    }

    @Override
    public void add(int index, String element) {
        if (contains(element)) {
            remove(element);
        }
        super.add(index, element);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends String> c) {
        for (String s : c) {
            add(s);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends String> c) {
        Iterator<? extends String> iterator = c.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            add(index + i, iterator.next());
            i++;
        }
        return true;
    }
}