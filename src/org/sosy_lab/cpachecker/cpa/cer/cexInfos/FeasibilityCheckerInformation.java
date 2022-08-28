package org.sosy_lab.cpachecker.cpa.cer.cexInfos;

public class FeasibilityCheckerInformation implements CounterexampleInformation {

    public enum Status {
        UNKNOWN,
        SPURIOUS,
        FEASIBLE
    }

    private final Status status;

    public FeasibilityCheckerInformation(Status pStatus) {
        status = pStatus;
    }

    public Status getStatus() {
        return status;
    }

}
