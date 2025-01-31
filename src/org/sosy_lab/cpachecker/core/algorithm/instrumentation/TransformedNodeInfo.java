package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

public class TransformedNodeInfo {
    private final int index;
    private final int loopLocation;

    public TransformedNodeInfo(int index, int loopLocation) {
        this.index = index;
        this.loopLocation = loopLocation;
    }

    public int getIndex() {
        return index;
    }

    public int getLoopLocation() {
        return loopLocation;
    }
}
