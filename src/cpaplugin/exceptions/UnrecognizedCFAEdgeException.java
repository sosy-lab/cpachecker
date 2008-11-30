package cpaplugin.exceptions;

import cpaplugin.exceptions.TransferRelationException;

/**
 * Exception thrown if (the operation corresponding to) an edge in the CFA
 * can not be encoded into a MathSAT formula.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */ 
public class UnrecognizedCFAEdgeException extends TransferRelationException {

    public UnrecognizedCFAEdgeException(String msg) {
        super(msg);
    }

    /**
     * auto-generated UID
     */
    private static final long serialVersionUID = -5106215499745787051L;
}
