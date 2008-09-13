package symbpredabstraction;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;

/**
 * InnerCFANode for function definitions
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsInnerFunctionDefinitionNode extends FunctionDefinitionNode
    implements SymbPredAbsInnerCFANode {

    private SymbPredAbsCFANode summary;
    
    public SymbPredAbsInnerFunctionDefinitionNode(int lineNumber, 
                                       IASTFunctionDefinition def) {
        super(lineNumber, def);
        summary = null;
    }
    
    public SymbPredAbsCFANode getSummaryNode() { return summary; }

    public void setSummaryNode(SymbPredAbsCFANode s) {
        summary = s;
    }
}
