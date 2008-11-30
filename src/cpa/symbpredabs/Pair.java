package cpa.symbpredabs;


/**
 * A generic Pair class. Code borrowed from here:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6229146
 * @author alb
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A, B> {
    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        return
        other instanceof Pair &&
        equals(first, ((Pair)other).first) &&
        equals(second, ((Pair)other).second);
    }

    public int hashCode() {
        if (first == null) return (second == null) ? 0 : second.hashCode() + 1;
        else if (second == null) return first.hashCode() + 2;
        else return first.hashCode() * 17 + second.hashCode();
    }
}
