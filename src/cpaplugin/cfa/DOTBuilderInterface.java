package cpaplugin.cfa;

import java.io.IOException;
import java.util.Collection;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;

/**
 * An interface for generating DOT representations of CFAs
 * @author alb
 */
public interface DOTBuilderInterface {
    public void generateDOT(Collection<CFAFunctionDefinitionNode> cfasMapList, 
            CFAFunctionDefinitionNode cfa, String fileName) throws IOException;
}
