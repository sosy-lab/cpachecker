package exceptions;

import java.util.Collection;
import java.util.Collections;

import exceptions.RefinementNeededException;
import cpa.common.interfaces.AbstractElement;

/**
 * Specialized version of the RefinementNeededException used to put some more
 * elements into the waiting list (see the toProcess member of
 * ItpTransferRelation)
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ToWaitListException extends RefinementNeededException {

    private static Collection<AbstractElement> toUnreach =
        Collections.emptyList();

    public ToWaitListException(Collection<AbstractElement> toWaitlist) {
        super(toUnreach, toWaitlist);
    }

    /**
     * auto generated
     */
    private static final long serialVersionUID = 948567529332169605L;

}
