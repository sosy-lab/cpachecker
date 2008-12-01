package cfa.objectmodel;

import cfa.objectmodel.CFANode;

/**
 * A special CFANode to record Error locations
 * @author alb
 */
public class CFAErrorNode extends CFANode {

    public CFAErrorNode(int lineNumber) {
        super(lineNumber);
    }

}
