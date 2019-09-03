package de.dytanic.cloudnet.lib.utility;

/**
 * Created by Tareko on 20.01.2018.
 */
public class Quad<F, S, T, FF> {

    private F first;

    private S second;

    private T third;

    private FF fourth;

    public Quad() {
    }

    public Quad(final F first, final S second, final T third, final FF fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public F getFirst() {
        return first;
    }

    public void setFirst(final F first) {
        this.first = first;
    }

    public FF getFourth() {
        return fourth;
    }

    public void setFourth(final FF fourth) {
        this.fourth = fourth;
    }

    public S getSecond() {
        return second;
    }

    public void setSecond(final S second) {
        this.second = second;
    }

    public T getThird() {
        return third;
    }

    public void setThird(final T third) {
        this.third = third;
    }
}
