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

import static org.nulist.plugin.parser.CFGAST.*;
import static org.nulist.plugin.parser.CFGNode.*;
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
    protected List<Pair<ADeclaration, String>> globalVariableDeclarations ;
    //protected final Map<Integer, ADeclaration> globalVariableDeclarations = new HashMap<>();
    protected SortedSetMultimap<String, CFANode> cfaNodes;
    protected Map<String, CFGFunctionBuilder> cfgFunctionBuilderMap = new HashMap<>();
    public CFGHandleExpression expressionHandler;

    public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
        logger = pLogger;
        machineModel = pMachineModel;

        typeConverter = new CFGTypeConverter(logger);

        functionDeclarations = new HashMap<>();

        functions = new TreeMap<>();
        cfaNodes = TreeMultimap.create();
        globalVariableDeclarations = new ArrayList<>();
        expressionHandler = new CFGHandleExpression(logger,"",typeConverter);
    }


    public List<Pair<ADeclaration, String>> getGlobalVariableDeclarations(){
        if(globalVariableDeclarations.isEmpty() ||
                globalVariableDeclarations.size()!= expressionHandler.globalVariableDeclarations.size()){
            globalVariableDeclarations = new ArrayList<>();
            for(ADeclaration gvar:expressionHandler.globalVariableDeclarations.values())
                globalVariableDeclarations.add(Pair.of(gvar,gvar.getName()));
        }
        return globalVariableDeclarations;
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
                        new CFGFunctionBuilder(logger, typeConverter,  proc,funcName, pFileName, this);
                // add function declaration
                functionDeclarations.put(funcName,cfgFunctionBuilder.handleFunctionDeclaration());

                // handle the function definition
                CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();
                addNode(funcName, en);
                //addNode(funcName, en.getExitNode());
                //

                //cfaNodes.put(funcName, en);
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
                //cfaNodes.replaceValues(funcName,cfgFunctionBuilder.getCfa());
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

            //f
            if(isVariable_Initialization(node)|| isExpression(node)){

                FileLocation fileLocation = getLocation(node,pFileName);
                ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());
                // Support static and other storage classes
                CStorageClass storageClass= getStorageClass(un_ast);

                //global variable is initialized static
                ast init = un_ast.get(ast_ordinal.getUC_STATIC_INIT()).as_ast();

                symbol variableSym = un_ast.get(ast_ordinal.getUC_ABS_LOC()).as_symbol();
                String variableName =variableSym.name();
                String normalizedName = variableName;

                //TODO: CPAChecker change all static variables to auto
                if (storageClass == CStorageClass.STATIC) {
                    //file static
                    normalizedName = expressionHandler.getSimpleFileName(pFileName)+"__static__"+normalizedName;
                    storageClass = CStorageClass.AUTO;
                }

                CType varType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
                CInitializer initializer = null;
                if(init.is_a(ast_class.getUC_STATIC_INITIALIZER()))
                    initializer = expressionHandler.getInitializerFromUC(init,varType,fileLocation);

                CSimpleDeclaration newDecl =
                        new CVariableDeclaration(
                                fileLocation,
                                true,
                                storageClass,
                                varType,
                                normalizedName,
                                variableName,
                                normalizedName,
                                initializer);

                expressionHandler.globalVariableDeclarations.put(variableName.hashCode(),(ADeclaration) newDecl);
            }
        }
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
