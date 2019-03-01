package org.nulist.plugin.parser;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;

import static org.nulist.plugin.util.ClassTool.getUnsignedInt;
import static org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.NO_ARGS_VOID_FUNCTION;

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

    private final CFGTypeConverter typeConverter;

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

        typeConverter = new CFGTypeConverter(machineModel,logger);

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
        boolean isUserDefinedFile = false;
        String pFileName = cu.normalized_name();

        // Iterate over all procedures in the compilation unit
        // procedure = function
        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            //only focus on the function defined by user
            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){
                isUserDefinedFile = true;
                point entryPoint = proc.entry_point();

                // handle the function definition
                String funcName = proc.name();

                FunctionEntryNode en = visitFunction(proc, pFileName);
            }
        }
        if(isUserDefinedFile){
            declareGlobalVariables(cu, pFileName);
        }

        return  null;
    }
    
    /**
     *@Description TODO
     *@Param [procedure, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode
     **/
    protected FunctionEntryNode visitFunction(procedure procedure, final String pFileName) throws result {

        logger.log(Level.FINE, "Creating function: " + procedure.name());

        String functionName = procedure.name();

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        addNode(functionName,functionExit);

        declareFunction(procedure, pFileName);

        point_set points= procedure.points();
        for(point_set_iterator point_it = points.cbegin();
            !point_it.at_end();point_it.advance()){
            point p = point_it.current();
            if(p.get_kind().equals(point_kind.getFORMAL_IN())||
                    p.get_kind().equals(point_kind.getFORMAL_OUT())
            ||p.get_kind().equals(point_kind.getACTUAL_IN())
            ||p.get_kind().equals(point_kind.getACTUAL_OUT())){

            }else if(p.get_kind().equals(point_kind.getBODY())){

            }
        }


        return null;
    }

    /**
     *@Description TODO
     *@Param [compunit, pFileName]
     *@return void
     **/
    private void declareGlobalVariables(compunit compunit, final String pFileName) throws result {

        for (compunit_procedure_iterator proc_it = compunit.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            //only focus on the function defined by user
            if(proc.get_kind().equals(procedure_kind.getFILE_INITIALIZATION())
                && proc.name().contains("Global_Initialization")){

                visitGlobalItem(proc, pFileName);
            }
        }
        
    }


    private void visitGlobalItem(procedure global_initialization, final String pFileName) throws result {

        point_set pointSet = global_initialization.points();
        CType cType = CNumericTypes.INT;
        for(point_set_iterator point_it = pointSet.cbegin();
            !point_it.at_end();point_it.advance()){
            point p = point_it.current();
            CInitializer initializer = null;
            if(p.get_kind().equals(point_kind.getVARIABLE_INITIALIZATION()) ||
                    p.get_kind().equals(point_kind.getEXPRESSION())){
                ast nc_ast = p.get_ast(ast_family.getC_NORMALIZED());
                // for example: int i=0;
                // in nc_ast: children {i, 0}
                //            attributes {is_initialization: true, type: int}
                //has initialization
                if(nc_ast.get(ast_ordinal.getNC_IS_INITIALIZATION()).as_boolean()){

                }else {//// Declaration without initialization
                    initializer = null;
                }
                CDeclaration declaration = (CDeclaration) getAssignedVarDeclaration(p, "", initializer, pFileName);

                globalDeclarations.add(Pair.of(declaration, nc_ast.children().get(0).as_ast().pretty_print()));
            }

        }
    }


    /**
     *@Description TODO
     *@Param [point, pFunctionName, pInitializer, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration
     **/
    private CSimpleDeclaration getAssignedVarDeclaration(
            final point point,
            final String pFunctionName,
            final CInitializer pInitializer,
            final String pFileName) throws result {


        final long itemId = getUnsignedInt(point.hashCode());// point.get_address() is not implemented
        if (!variableDeclarations.containsKey(itemId)) {
            ast nc_ast = point.get_ast(ast_family.getC_NORMALIZED());
            ast un_ast = point.get_ast(ast_family.getC_UNNORMALIZED());

            String assignedVar = nc_ast.children().get(0).as_ast().pretty_print();//the 1st child field store the variable

            final boolean isGlobal = true;
            // TODO: Support static and other storage classes
            CStorageClass storageClass = CStorageClass.AUTO;
            if(un_ast.get(ast_ordinal.getBASE_STORAGE_CLASS()).as_enum_value_string().equals("static")){
                storageClass = CStorageClass.STATIC;
            }
            CType varType = typeConverter.getCType(nc_ast.get(ast_ordinal.getNC_TYPE()).as_ast());

            // We handle alloca not like malloc, which returns a pointer, but as a general
            // variable declaration. Consider that here by using the allocated type, not the
            // pointer of that type alloca returns.
//            if (pItem.isAllocaInst()) {
//                varType = typeConverter.getCType(pItem.getAllocatedType());
//            }

            if (isGlobal && varType instanceof CPointerType) {
                varType = ((CPointerType) varType).getType();
            }

            CSimpleDeclaration newDecl =
                    new CVariableDeclaration(
                            getLocation(point, pFileName),
                            isGlobal,
                            storageClass,
                            varType,
                            assignedVar,
                            assignedVar,
                            getQualifiedName(assignedVar, pFunctionName),
                            pInitializer);
            assert !variableDeclarations.containsKey(itemId);
            variableDeclarations.put(itemId, newDecl);
        }

        return variableDeclarations.get(itemId);
    }
    
    
    

    private CInitializer getConstantAggregateInitializer(
            point point, final String pFileName) throws result {

        return  null;
    }


    private CInitializer getZeroInitializer(
            point point , final CType pExpectedType, final String pFileName) throws result {

        return null;
    }

    /**
     *@Description function declaration
     *@Param [function, pFileName]
     *@return void
     **/
    private void declareFunction(procedure function, final String pFileName) throws result{
        String functionName = function.name();

        // Function return type
        point_set formal_outs = function.formal_outs();//get the formal out of function, if type is VOID, the set is empty

        CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
        if(!formal_outs.empty()){
            //Note that it is impossible that there are more than one formal out
            point p = formal_outs.cbegin().current();
            cFuncType = (CFunctionType) typeConverter.getCType(p.parameter_symbols().get(0));
        }

        // Parameters
        point_set formal_ins = function.formal_ins();// each point in formal_ins is an input parameter
        List<CParameterDeclaration> parameters = new ArrayList<>((int)formal_ins.size());
        if(!formal_ins.empty()){

            for(point_set_iterator point_it= formal_ins.cbegin();
                !point_it.at_end();point_it.advance())
            {
                point param_point = point_it.current();
                String paramName = param_point.parameter_symbols().get(0).get_ast().pretty_print();
                CType paramType = typeConverter.getCType(param_point.parameter_symbols().get(0));
                CParameterDeclaration parameter =
                        new CParameterDeclaration(getLocation(param_point,pFileName),paramType,paramName);

                parameter.setQualifiedName(getQualifiedName(paramName, functionName));
                variableDeclarations.put(function.file_line().get_second(),parameter);
                parameters.add(parameter);
            }
        }


        // Function declaration, exit
        CFunctionDeclaration functionDeclaration =
                new CFunctionDeclaration(
                        getLocation(function.entry_point(), pFileName), cFuncType, functionName, parameters);
        functionDeclarations.put(functionName, functionDeclaration);
    }

    private FileLocation getLocation(point point, final String pFileName) {
        assert point != null;
        return new FileLocation(pFileName, 0, 1, 0, 0);
    }

    private String getQualifiedName(String pVarName, String pFuncName) {
        return pFuncName + "::" + pVarName;
    }

}
