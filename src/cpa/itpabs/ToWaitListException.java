package cpa.itpabs;

import java.util.Collection;
import java.util.Collections;

import cpa.common.RefinementNeededException;
import cpa.common.interfaces.AbstractElementWithLocation;

/**
 * Specialized version of the RefinementNeededException used to put some more
 * elements into the waiting list (see the toProcess member of
 * ItpTransferRelation)
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ToWaitListException extends RefinementNeededException {

    private static Collection<AbstractElementWithLocation> toUnreach =
        Collections.emptyList();

    public ToWaitListException(Collection<AbstractElementWithLocation> toWaitlist) {
        super(toUnreach, toWaitlist);
    }

    /**
     * auto generated
     */
    private static final long serialVersionUID = 948567529332169605L;

}
