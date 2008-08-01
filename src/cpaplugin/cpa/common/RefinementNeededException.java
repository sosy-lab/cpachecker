package cpaplugin.cpa.common;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractElement;

public class RefinementNeededException extends CPATransferException {

    /**
     * auto generated
     */
    private static final long serialVersionUID = -141927893977460824L;
    
    private Collection<AbstractElement> toUnreach;
    private Collection<AbstractElement> toWaitlist;
    
    public RefinementNeededException(
            Collection<AbstractElement> toUnreach,
            Collection<AbstractElement> toWaitlist) {
        super();
        this.toUnreach = toUnreach;
        this.toWaitlist = toWaitlist;
    }
    
    public Collection<AbstractElement> getReachableToUndo() {
        return toUnreach;
    }
    
    public Collection<AbstractElement> getToWaitlist() {
        return toWaitlist;
    }

}
