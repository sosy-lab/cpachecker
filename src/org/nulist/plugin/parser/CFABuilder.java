package org.nulist.plugin.parser;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.nulist.plugin.parser.CFGAST.getStorageClass;
import static org.nulist.plugin.parser.CFGAST.getVariableName;
import static org.nulist.plugin.parser.CFGNode.isExpression;
import static org.nulist.plugin.parser.CFGNode.isVariable_Initialization;
import static org.nulist.plugin.util.FileOperations.*;

/**
 * @ClassName CFABuilder
 * @Description For a C file
 * @Author Yinbo Yu
 * @Date 2/27/19 4:18 PM
 * @Version 1.0
 **/
public class CFABuilder {
    private final LogManager logger;
    private final MachineModel machineModel;

    private final CFGTypeConverter typeConverter;


    private final List<Path> parsedFiles = new ArrayList<>();

    // Function name -> Function declaration
    protected Map<String, CFunctionDeclaration> functionDeclarations;
    protected NavigableMap<String, FunctionEntryNode> functions;
    //protected List<Pair<ADeclaration, String>> globalVariableDeclarations;
    protected final Map<Integer, ADeclaration> globalVariableDeclarations = new HashMap<>();
    protected SortedSetMultimap<String, CFANode> cfaNodes;
    protected Map<String, CFGFunctionBuilder> cfgFunctionBuilderMap = new HashMap<>();
    public CFGHandleExpression expressionHandler;

    public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
        logger = pLogger;
        machineModel = pMachineModel;

        typeConverter = new CFGTypeConverter(machineModel,logger);

        functionDeclarations = new HashMap<>();

        functions = new TreeMap<>();
        cfaNodes = TreeMultimap.create();
        expressionHandler = new CFGHandleExpression(logger,typeConverter);
    }


    public List<Pair<ADeclaration, String>> getGlobalVariableDeclarations(){
        List<Pair<ADeclaration, String>> gvars = new ArrayList<>();
        for(ADeclaration gvar:expressionHandler.globalVariableDeclarations.values())
            gvars.add(Pair.of(gvar,gvar.getName()));
        return gvars;
    }


    protected void addNode(String funcName, CFANode nd) {
        cfaNodes.put(funcName, nd);
    }

    public void basicBuild(compunit cu)throws result{
        String pFileName = cu.normalized_name();

        // Iterate over all procedures in the compilation unit
        // procedure = function

        /* create global variable declaration*/
        declareGlobalVariables(cu, pFileName);
        //expressionHandler.setGlobalVariableDeclarations(globalVariableDeclarations);

        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();

            //only focus on the function defined by user
            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){

                String funcName = proc.name();
                CFGFunctionBuilder cfgFunctionBuilder =
                        new CFGFunctionBuilder(logger,machineModel, typeConverter,  proc,funcName, pFileName, this);
                // add function declaration
                functionDeclarations.put(funcName,cfgFunctionBuilder.handleFunctionDeclaration());

                // handle the function definition
                CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();
                addNode(funcName, en);
                addNode(funcName, en.getExitNode());
                //

                cfaNodes.put(funcName, en);
                functions.put(funcName, en);
                cfgFunctionBuilderMap.put(funcName,cfgFunctionBuilder);
            }
        }

        parsedFiles.add(Paths.get(pFileName));
    }

    /**
     *@Description input is a C file TODO
     *@Param [cu]
     *@return org.sosy_lab.cpachecker.cfa.ParseResult
     **/
    public void build(compunit cu) throws result {
        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){
                String funcName = proc.name();
                CFGFunctionBuilder cfgFunctionBuilder = cfgFunctionBuilderMap.get(funcName);
                cfgFunctionBuilder.visitFunction();
            }
        }
    }



    /**
     *@Description all global and file static variables are defined in File_Initialization-
     *              Global_Initialization_0 (no initializer) and Global_Initialization_1 (static initializer)
     *              all symbols of global and file static variables also can be obtained by compunit.global_symbols
     *                 and figured out by symbol.is_gobal() or symbol.is_file_static() (or directly is_user())
     *@Param [compunit, pFileName]
     *@return void
     **/
    private void declareGlobalVariables(compunit compunit, final String pFileName) throws result{

        for (compunit_procedure_iterator proc_it = compunit.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            if(proc.get_kind().equals(procedure_kind.getFILE_INITIALIZATION())
                && proc.name().contains("Global_Initialization")){
                visitGlobalItem(proc, pFileName);
            }
        }
        
    }


    /**
     *@Description global variables and their initialization
     * in Codesurfer, all global variables are initialized in the following procedures:
     *                            Global_Initialization_0==> no initialization
     *                            Global_Initialization_1==> static initialization
     *@Param [global_initialization, pFileName]
     *@return void
     **/
    private void visitGlobalItem(procedure global_initialization, final String pFileName) throws result {

        point_set pointSet = global_initialization.points();
        for(point_set_iterator point_it = pointSet.cbegin();
            !point_it.at_end(); point_it.advance()){
            //point p = point_it.current();
            point node =  point_it.current();
            CInitializer initializer = null;

            if(isVariable_Initialization(node)|| isExpression(node)){
                FileLocation fileLocation = getLocation(node,pFileName);
                ast no_ast = node.get_ast(ast_family.getC_NORMALIZED());
                CType varType = typeConverter.getCType(no_ast.get(ast_ordinal.getNC_TYPE()).as_ast());

                // for example: int i=0;
                // in nc_ast: children {i, 0}
                //            attributes {is_initialization: true, type: int}
                // has initialization
                initializer = expressionHandler.getInitializer(no_ast,fileLocation);

                String variableName =getVariableName(no_ast, true);

                //String assignedVar = no_ast.children().get(0).as_ast().pretty_print();//the 1st child field store the variable

                // Support static and other storage classes
                ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());
                CStorageClass storageClass= getStorageClass(un_ast);

                if ( varType instanceof CPointerType) {
                    varType = ((CPointerType) varType).getType();
                }

                CSimpleDeclaration newDecl =
                        new CVariableDeclaration(
                                fileLocation,
                                true,
                                storageClass,
                                varType,
                                variableName,
                                variableName,
                                variableName,
                                initializer);

                expressionHandler.globalVariableDeclarations.put(variableName.hashCode(),(ADeclaration) newDecl);
            }

        }
    }



    /**
     *@Description TODO
     *@Param [point, pFunctionName, pInitializer, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration
     **/
    private CDeclaration getAssignedVarDeclaration(
            final point node, CInitializer pInitializer, final FileLocation fileLocation) throws result {


        final long itemId = node.id();


        ast nc_ast = node.get_ast(ast_family.getC_NORMALIZED());
        ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());

        String assignedVar = nc_ast.children().get(0).as_ast().pretty_print();//the 1st child field store the variable

        final boolean isGlobal = node.declared_symbol().is_global();
        // Support static and other storage classes

        CStorageClass storageClass= getStorageClass(un_ast);

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
                        fileLocation,
                        isGlobal,
                        storageClass,
                        varType,
                        assignedVar,
                        assignedVar,
                        getQualifiedName(assignedVar, fileLocation.getFileName()),
                        pInitializer);
        return (CDeclaration) newDecl;
    }



    public boolean isFunction(ast value_ast) throws result{
//        ast no_ast=point.get_ast(ast_family.getC_NORMALIZED());
//        ast_field value_ast = no_ast.children().get(1);
//        try {
//
//            String value = value_ast.get(ast_ordinal.getBASE_NAME()).as_str();
//            if(value.contains("$result")){
//                String functionName = value.substring(0,value.indexOf("$result"));
//                if(functionDeclarations.containsKey(functionName))
//                    return true;
//            }
//            return false;
//        }catch (result r){
//            return false;
//        }
        try {
            symbol value_symbol = value_ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
            if(value_symbol.get_kind().equals(symbol_kind.getRESULT())){
                String value = value_ast.get(ast_ordinal.getBASE_NAME()).as_str();
                String functionName = value.substring(0,value.indexOf("$result"));
                if(functionDeclarations.containsKey(functionName))
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }



    /**
     * Returns whether the first param is a pointer of the type of the second parameter.<br>
     * Examples:
     *
     * <ul>
     *   <li>pointerOf(*int, int) -> true
     *   <li>pointerOf(**int, *int) -> true
     *   <li>pointerOf(int, int*) -> false
     *   <li>pointerOf(int, int) -> false
     * </ul>
     */
    public static boolean pointerOf(CType pPotentialPointer, CType pPotentialPointee) {
        if (pPotentialPointer instanceof CPointerType) {
            return ((CPointerType) pPotentialPointer)
                    .getType()
                    .getCanonicalType()
                    .equals(pPotentialPointee.getCanonicalType());
        } else {
            return false;
        }
    }




}
