package org.sosy_lab.cpachecker.core.algorithm.acsl;

//TODO: Just a wrapper for predicate, could be removed
public class LoopInvariant {

    private ACSLPredicate predicate;

    public LoopInvariant(ACSLPredicate p) {
        predicate = p.toPureC().simplify();
    }

    public ACSLPredicate getPredicate() {
        return predicate;
    }

    public LoopInvariant and(ACSLPredicate p) {
        predicate = predicate.and(p);
        return this;
    }

    @Override
    public String toString() {
        return "loop invariant " + predicate.toString();
    }
}
