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
import static org.nulist.plugin.parser.CFGAST.dumpASTWITHClass;
import static org.nulist.plugin.parser.CFGNode.*;
import static org.nulist.plugin.parser.CFGParser.*;
import static org.nulist.plugin.util.CFGDumping.dumpCFG2Dot;
import static org.nulist.plugin.util.FileOperations.*;
import static org.nulist.plugin.util.ClassTool.*;
import static org.nulist.plugin.FunctionTest.*;
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
    protected NavigableMap<String, FunctionEntryNode> systemFunctions;
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
        systemFunctions = new TreeMap<>();
        cfaNodes = TreeMultimap.create();
        globalVariableDeclarations = new ArrayList<>();
        expressionHandler = new CFGHandleExpression(logger,"",typeConverter);
    }


    //TODO shall add all function declarations
    public List<Pair<ADeclaration, String>> getGlobalVariableDeclarations(){
        if(globalVariableDeclarations.isEmpty() ||
                globalVariableDeclarations.size()!= expressionHandler.globalDeclarations.size()){
            globalVariableDeclarations = new ArrayList<>();
            for(ADeclaration gvar:expressionHandler.globalDeclarations.values())
                globalVariableDeclarations.add(Pair.of(gvar,gvar.getName()));
        }
        return globalVariableDeclarations;
    }


    protected void addNode(String funcName, CFANode nd) {
        cfaNodes.put(funcName, nd);
    }

    public void basicBuild(compunit cu, String projectName)throws result{

        String pFileName = cu.normalized_name();
        //System.out.println(cu.name());
        // Iterate over all procedures in the compilation unit
        // procedure = function

        //expressionHandler.setGlobalVariableDeclarations(globalVariableDeclarations);
        /* create global variable and function declaration*/
        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();

            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())||
                    proc.get_kind().equals(procedure_kind.getLIBRARY())){
                String funcName = proc.name();
                if((funcName.equals("main") && !isProjectMainFunction(cu.name(),projectName)) ||
                        funcName.equals("cmpint") ||
                        funcName.equals("ASN__STACK_OVERFLOW_CHECK") ||
                        functionDeclarations.containsKey(funcName)) //oai has inline functions and asn generated codes have several same functions
                    continue;

                //System.out.println(funcName);
                CFGFunctionBuilder cfgFunctionBuilder =
                        new CFGFunctionBuilder(logger, typeConverter, proc,funcName, pFileName, this);
                // add function declaration
                CFunctionDeclaration functionDeclaration = cfgFunctionBuilder.handleFunctionDeclaration();

                functionDeclarations.put(funcName, functionDeclaration);
                expressionHandler.globalDeclarations.put(funcName.hashCode(), functionDeclaration);
                // handle the function definition
                CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();

                if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){
                    functions.put(funcName, en);
                    cfgFunctionBuilderMap.put(funcName,cfgFunctionBuilder);
                }else
                    systemFunctions.put(funcName, en);

            }else if(cu.is_user() && proc.get_kind().equals(procedure_kind.getFILE_INITIALIZATION())
                    && proc.name().contains("Global_Initialization")){
                visitGlobalItem(proc,projectName);
            }
        }

        parsedFiles.add(Paths.get(pFileName));
    }

    /**
     *@Description input is a C file
     *@Param [cu]
     *@return org.sosy_lab.cpachecker.cfa.ParseResult
     **/
    public void build(compunit cu) throws result {
        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){
                String funcName = proc.name();
                if(funcName.equals("cmpint") ||
                        funcName.equals("ASN__STACK_OVERFLOW_CHECK") ||
                        funcName.equals("rrc_control_socket_init") ||
                        funcName.startsWith("dump_") ||
                        funcName.startsWith("memb_"))
                    continue;
                System.out.println(funcName);
                CFGFunctionBuilder cfgFunctionBuilder = cfgFunctionBuilderMap.get(funcName);
                //functionTest(cfgFunctionBuilder);
                if(!cfgFunctionBuilder.isFinished)
                    cfgFunctionBuilder.visitFunction();
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
    private void visitGlobalItem(procedure global_initialization, String projectName) throws result {

        point_set pointSet = global_initialization.points();
        List<String> variableList = new ArrayList<>();
        for(point_set_iterator point_it = pointSet.cbegin();
            !point_it.at_end(); point_it.advance()){
            //point p = point_it.current();
            point node =  point_it.current();

            //f
            if(isVariable_Initialization(node)|| isExpression(node)){

                try {
                    if(!fileFilter(node.file_line().get_first().name(), projectName))
                        continue;
                }catch (result r){
                    System.out.println(node.get_procedure().get_compunit().name()+":"+node.toString());
                    throw new RuntimeException(r);
                }
                ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());
                symbol variableSym = un_ast.get(ast_ordinal.getUC_ABS_LOC()).as_symbol();
                String variableName =variableSym.name();

                String pFileName = node.file_line().get_first().name();
                FileLocation fileLocation = getLocation(node, pFileName);

                // Support static and other storage classes
                CStorageClass storageClass= getStorageClass(un_ast);
                String normalizedName = variableName;

                if (storageClass == CStorageClass.STATIC) {
                    //file static
                    normalizedName = "static__"+normalizedName;
                    storageClass = CStorageClass.AUTO;
                }
                if(variableList.contains(normalizedName) && node.get_ast(ast_family.getC_NORMALIZED()).is_a(ast_class.getNC_BLOCKASSIGN())){
                    continue;
                }

                if(expressionHandler.globalDeclarations.containsKey(normalizedName.hashCode()))
                    continue;

                //global variable is initialized static
                ast init = un_ast.get(ast_ordinal.getUC_STATIC_INIT()).as_ast();

                CType varType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                variableList.add(normalizedName);

                // void (*funA)(int)=&myFun;
                if(typeConverter.isFunctionPointerType(varType)){
                    varType = typeConverter.convertCFuntionType(varType, variableName, fileLocation);
                }


                CInitializer initializer = null;
                if(init.is_a(ast_class.getUC_STATIC_INITIALIZER())){
                    //System.out.println(un_ast.toString());
                    if(variableName.equals("mme_app_desc")){
                        ast no_ast = node.get_ast(ast_family.getC_NORMALIZED());
                        ast orginal = no_ast.get(ast_ordinal.getNC_ORIGINAL()).as_ast();
                        initializer = expressionHandler.getInitializerFromOriginal(orginal.children().get(1).as_ast(),varType,fileLocation);
                    } else if(un_ast.pretty_print().equals("mcc_mnc_list")){//can only get error from codesurfer CFG
                        //read from txt
                        initializer = expressionHandler.getInitializerFromTXT(varType,fileLocation);
                    }else{
                        initializer = expressionHandler.getInitializerFromUC(init,varType,fileLocation);
                    }
                }

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

                expressionHandler.globalDeclarations.put(normalizedName.hashCode(),(ADeclaration) newDecl);
            }
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
