package com.tallybot.backend.tallybot_back.debtopt;


import java.util.List;

public class InfiniteIterator<E> {
    private final List<E> list;
    private final int idx;

    private InfiniteIterator(List<E> list, int idx) {
        this.list = list;
        this.idx = idx;
    }

    public static <E> InfiniteIterator<E> begin(List<E> list) {
        if (list == null || list.isEmpty()){
            throw new IllegalArgumentException("List must not be null or empty");
        }
        return new InfiniteIterator<>(list, 0);
    }

    public InfiniteIterator<E> increment() {
        int next = (idx+1)%list.size();
        return new InfiniteIterator<>(list, next);
    }

    public E value() {
        return list.get(idx);
    }

    public List<E> getInnerList() {
        return List.copyOf(list);
    }
}
