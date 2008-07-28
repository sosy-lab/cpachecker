package cpaplugin.cpa.cpas.symbpredabs;

import java.util.LinkedList;
import java.util.Map;

import cpaplugin.cfa.objectmodel.CFANode;

/**
 * A concrete execution trace is simply a list of pairs 
 * (location, assignment to variables)
 * @author alb
 */
public class ConcreteTrace extends
        LinkedList<Pair<CFANode, Map<String, String>>> {

    /**
     * auto-generated 
     */
    private static final long serialVersionUID = -8396031231056156326L;

}
