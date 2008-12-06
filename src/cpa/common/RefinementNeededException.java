package cpa.common;

import java.util.Collection;

import cpa.common.interfaces.AbstractElementWithLocation;

public class RefinementNeededException extends CPATransferException {

    /**
     * auto generated
     */
    private static final long serialVersionUID = -141927893977460824L;

    private final Collection<AbstractElementWithLocation> toUnreach;
    private final Collection<AbstractElementWithLocation> toWaitlist;

    public RefinementNeededException(
            Collection<AbstractElementWithLocation> toUnreach,
            Collection<AbstractElementWithLocation> toWaitlist) {
        super();
        this.toUnreach = toUnreach;
        this.toWaitlist = toWaitlist;
    }

    public Collection<AbstractElementWithLocation> getReachableToUndo() {
        return toUnreach;
    }

    public Collection<AbstractElementWithLocation> getToWaitlist() {
        return toWaitlist;
    }

}
