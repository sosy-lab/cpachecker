package org.nulist.plugin.translation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

/**
 * @ClassName CFABuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/27/19 4:18 PM
 * @Version 1.0
 **/
public class CFABuilder {
    private final LogManager logger;
    private final MachineModel machineModel;

    // Value address -> Variable declaration
    private final Map<Long, CSimpleDeclaration> variableDeclarations;
    // Function name -> Function declaration
    private Map<String, CFunctionDeclaration> functionDeclarations;

    // unnamed basic blocks will be named as 1,2,3,...
    private int basicBlockId;
    protected NavigableMap<String, FunctionEntryNode> functions;

    protected SortedSetMultimap<String, CFANode> cfaNodes;
    protected List<Pair<ADeclaration, String>> globalDeclarations;

    public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
        logger = pLogger;
        machineModel = pMachineModel;
        variableDeclarations = new HashMap<>();
        functionDeclarations = new HashMap<>();

        functions = new TreeMap<>();
        cfaNodes = TreeMultimap.create();
        globalDeclarations = new ArrayList<>();
    }

    protected void addNode(String funcName, CFANode nd) {
        cfaNodes.put(funcName, nd);
    }

    public ParseResult build(compunit cu) throws  result {

        // Iterate over all procedures in the compilation unit
        // procedure = function
        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            //only focus on the function defined by user
            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){

                point entryPoint = proc.entry_point();

                // handle the function definition
                String funcName = proc.name();

                FunctionEntryNode en = visitFunction(proc, cu.name());
            }
        }

        return  null;
    }

    protected FunctionEntryNode visitFunction(procedure procedure, final String pFileName) throws result {

        logger.log(Level.FINE, "Creating function: " + procedure.name());

        String functionName = procedure.name();

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        addNode(functionName,functionExit);
        


        return null;
    }

}
