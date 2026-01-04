package com.tallybot.backend.tallybot_back.debtopt;

public class ThreeTuple<E, F, G> {
    private final E first;
    private final F second;
    private final G third;

    public ThreeTuple(E first, F second, G third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public E first() {
        return first;
    }

    public F second() {
        return second;
    }

    public G third() {
        return third;
    }

    @Override
    public String toString() {
        return "(" + first() + ", " + second() + ", " + third() + ")";
    }
}