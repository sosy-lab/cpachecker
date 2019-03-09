/**
 * @ClassName CFGFunctionBuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 5:27 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.grammatech.cs.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.model.FunctionDeclaration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.*;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.cpa.usage.storage.TemporaryUsageStorage;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;

import static org.nulist.plugin.parser.CFABuilder.pointerOf;
import static org.nulist.plugin.parser.CFGAST.isConstantAggregateZero;
import static org.nulist.plugin.parser.CFGAST.isConstantArrayOrVector;
import static org.nulist.plugin.parser.CFGNode.*;
import static org.nulist.plugin.parser.CFGNode.DECLARATION;
import static org.nulist.plugin.util.ClassTool.getUnsignedInt;
import static org.nulist.plugin.util.FileOperations.getLocation;
import static org.nulist.plugin.util.FileOperations.getQualifiedName;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.isReachableNode;
import static org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.NO_ARGS_VOID_FUNCTION;

public class CFGFunctionBuilder  {

    // Data structure for maintaining our scope stack in a function
    private final Deque<CFANode> locStack = new ArrayDeque<>();

    // Data structures for handling loops & else conditions
    private final Deque<CFANode> loopStartStack = new ArrayDeque<>();
    private final Deque<CFANode> loopNextStack  = new ArrayDeque<>(); // For the node following the current if / while block
    private final Deque<CFANode> elseStack      = new ArrayDeque<>();

    // Data structure for handling switch-statements
    private final Deque<CExpression> switchExprStack = new ArrayDeque<>();
    private final Deque<CFANode> switchCaseStack = new ArrayDeque<>();

    @SuppressWarnings("JdkObsolete") // ArrayDeque not possible because it does not allow null
    private final Deque<CFANode> switchDefaultStack = new LinkedList<>();

    @SuppressWarnings("JdkObsolete") // ArrayDeque not possible because it does not allow null
    private final Deque<FileLocation> switchDefaultFileLocationStack = new LinkedList<>();

    private final CBinaryExpressionBuilder binExprBuilder;

    // Data structures for handling goto
    private final Map<String, CLabelNode> labelMap = new HashMap<>();
    private final Map<String, Long> nodeIDMap = new HashMap<>();//<node.toString, id>
    private final Multimap<String, Pair<CFANode, FileLocation>> gotoLabelNeeded = ArrayListMultimap.create();


    // Data structures for handling function declarations
    private FunctionEntryNode cfa = null;
    private Set<CFANode> cfaNodes = null;
    private Map<Long, CFANode> cfaNodeMap = new HashMap<>();

    // There can be global declarations in a function
    // because we move some declarations to the global scope (e.g., static variables)
    private final List<Pair<ADeclaration, String>> globalDeclarations = new ArrayList<>();
    // Key: variable_name.hashcode, Value: variabledeclaration
    private final Map<Integer, CSimpleDeclaration> variableDeclarations = new HashMap<>();
    private List<String> temporaryVariableNameList = new ArrayList<>();


    private final LogManager logger;
    private boolean encounteredAsm = false;
    private final procedure function;
    public final String functionName;
    private final CFGTypeConverter typeConverter;
    private CFunctionDeclaration functionDeclaration;
    private final String fileName;
    private final CFABuilder cfaBuilder;
    private CBinaryExpressionBuilder binaryExpressionBuilder;

    public CFGFunctionBuilder(
            LogManager pLogger,
            MachineModel pMachine,
            CFGTypeConverter typeConverter,
            procedure pFunction,
            String functionName,
            String pFileName,
            CFABuilder cfaBuilder
            ) {
        logger = pLogger;
        this.typeConverter = typeConverter;
        function = pFunction;
        this.functionName = functionName;
        binExprBuilder = new CBinaryExpressionBuilder(pMachine, pLogger);
        fileName = pFileName;
        this.cfaBuilder = cfaBuilder;
        binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachine, logger);
    }


    public FunctionEntryNode getCfa() {
        return cfa;
    }

    /**
     *@Description traverse CFG nodes and edges and transform them into CFA
     *@Param [function, pFileName]
     *@return void
     **/
    public void visitFunction() throws result {
        assert function!=null && function.get_kind().equals(procedure_kind.getUSER_DEFINED());

        //handle all variable declarations;
        handleVariableDeclaration();

        function.entry_point();



    }

    private void traverseCFGNode(CFGNode node, CFANode prevNode) throws result{
        cfg_edge_set cfgEdgeSet = node.cfg_targets();
        FileLocation fileLocation = new FileLocation(fileName, 0, 1,node.getFileLineNumber(),0);

        if(cfgEdgeSet.empty() && !node.isFunctionExit()){
            //throw new Exception("");
        }else if(node.isFunctionExit()){

        }else if(node.is_inside_macro()){

        }else {
            switch (node.getKindName()){
                case CALL_SITE:
                    handleFunctionCall(node, prevNode, fileLocation);
                    break;
                case CONTROL_POINT:
                    handleControlPoint(node, prevNode, fileLocation);
                    break;
                case JUMP:
                    if(node.isGotoNode()){
                        handleGotoStatement(node,fileLocation);
                    }else if(node.isBreakNode()){

                    }
                    break;
                case EXPRESSION:

                    break;
                case LABEL:
                    if(node.isGoToLabel())
                        handleLabelNode(node, fileLocation);
                    else if(node.isElseLabel()){

                    }else if(node.isDoLabel()){

                    }
                    break;

            }
        }

    }

    /**
     *@Description TODO need to add inter-edge of function call
     *@Param [cfgNode, prevNode, fileLocation]
     *@return void
     **/
    private void handleFunctionCall(final CFGNode cfgNode, CFANode prevNode, FileLocation fileLocation)throws result{
        assert cfgNode.isCall_Site();
        point_set actuals_in = cfgNode.actuals_in();
        point_set actuals_out = cfgNode.actuals_out();
        CFANode nextNode = newCFANode();

        CFunctionEntryNode pFunctionEntry = null;
        CFGNode functionCallNode, nextCFGNode;
        CFunctionSummaryEdge edge;
        String rawCharacters="";
        List<CExpression> params = new ArrayList<>();

        CFunctionCallExpression functionCallExpression;
        CFunctionCall functionCallStatement;
        if(actuals_out.empty()){
            //the return result of function call is not used in this function.
            // functionCallStatement ==> CFunctionCallStatement
            if(actuals_in.empty()){
                functionCallNode = (CFGNode) cfgNode.cfg_inter_targets().cbegin().current().get_first();

                nextCFGNode = (CFGNode) cfgNode.cfg_targets().cbegin().current().get_first();
                rawCharacters = cfgNode.characters();
            }else {
                // functionA(param_1, param_2, param_3)
                // call site: functionA()
                // actual_in: param_3
                // actual_in: param_2
                // actual_in: param_1 --> inter_edge to functionA entry point

                functionCallNode = (CFGNode) actuals_in.cend().current().cfg_inter_targets()
                        .cbegin().current().get_first();
                //combine call-site and actual in

                nextCFGNode = (CFGNode) actuals_in.cend().current().cfg_targets().cbegin().current().get_first();

                point_vector pv = actuals_in.to_vector();

                StringBuilder sb = new StringBuilder(cfgNode.characters().replace(")",""));
                for(int i=((int)pv.size())-1;i>=0;i--){
                    CFGNode actual_in = (CFGNode) pv.get(i);
                    CFGAST inAST  = (CFGAST) actual_in.get_ast(ast_family.getC_NORMALIZED());
                    if(inAST.children().get(1).as_ast().get_class().equals(ast_class.getNC_VARIABLE())){
                        CFGAST variable_ast = (CFGAST) inAST.children().get(1).as_ast();
                        String variableName = variable_ast.normalizingVariableName();
                        sb.append(variableName).append(", ");
                    }else {
                        String value = inAST.children().get(i).as_ast().pretty_print();
                        sb.append(value).append(", ");
                    }

                    CType type = typeConverter.getCType((CFGAST) inAST.children()
                            .get(1).as_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());
                    CExpression param = getExpression(actual_in,type,fileLocation);
                    params.add(param);
                    rawCharacters=sb.toString().replace(", ",")");
                }
            }

            CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
            if(!functionCallNode.get_procedure().formal_outs().empty()){
                point p = functionCallNode.get_procedure().formal_outs().cbegin().current();
                ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
                ast type_ast = un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
                cFuncType = (CFunctionType) typeConverter.
                        getCType((CFGAST) type_ast.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());
            }

            pFunctionEntry = cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = getFunctionEntryNode(functionCallNode);
                cfaBuilder.functions.put(functionCallNode.get_procedure().name(), pFunctionEntry);
                cfaBuilder.addNode(functionCallNode.get_procedure().name(), pFunctionEntry);
            }

            CIdExpression funcNameExpr =
                    new CIdExpression(
                            fileLocation,
                            cFuncType,
                            pFunctionEntry.getFunctionName(),
                            pFunctionEntry.getFunctionDefinition());

            functionCallExpression = new CFunctionCallExpression(fileLocation, cFuncType, funcNameExpr,
                    params,pFunctionEntry.getFunctionDefinition());
            functionCallStatement = new CFunctionCallStatement(fileLocation,functionCallExpression);

            edge = new CFunctionSummaryEdge(rawCharacters, fileLocation,
                    prevNode, nextNode, functionCallStatement, pFunctionEntry);

            prevNode.addEnteringSummaryEdge(edge);
            nextNode.addLeavingSummaryEdge(edge);
            cfaNodeMap.put(nextCFGNode.id(),nextNode);
            traverseCFGNode(nextCFGNode,nextNode);

        }else {
            // if the return result of the function call is used, there should have one actual_out node.
            // functionCallStatement ==> CFunctionCallAssignmentStatement, include, lefthandside and righthandside

            CFGNode actualoutCFGNode = (CFGNode) actuals_out.cbegin().current();

            if(actuals_in.empty()){
                //shall have target nodes including actual_out and other node who using the actual out
                functionCallNode = (CFGNode) cfgNode.cfg_inter_targets().cbegin().current().get_first();
            }else {
                functionCallNode = (CFGNode) actuals_in.cend().current().cfg_inter_targets()
                        .cbegin().current().get_first();

                point_vector pv = actuals_in.to_vector();
                StringBuilder sb = new StringBuilder(cfgNode.characters().replace(")",""));
                for(int i=((int)pv.size())-1;i>=0;i--){
                    CFGNode actual_in = (CFGNode) pv.get(i);
                    CFGAST inAST  = (CFGAST) actual_in.get_ast(ast_family.getC_NORMALIZED());
                    if(inAST.children().get(1).as_ast().get_class().equals(ast_class.getNC_VARIABLE())){
                        CFGAST variable_ast = (CFGAST) inAST.children().get(1).as_ast();
                        String variableName = variable_ast.normalizingVariableName();
                        sb.append(variableName).append(", ");
                    }else {
                        String value = inAST.children().get(i).as_ast().pretty_print();
                        sb.append(value).append(", ");
                    }

                    CType type = typeConverter.getCType((CFGAST) inAST.children()
                            .get(1).as_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());
                    CExpression param = getExpression(actual_in,type,fileLocation);
                    params.add(param);
                    rawCharacters=sb.toString().replace(", ",")");
                }
            }

            pFunctionEntry = cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = getFunctionEntryNode(functionCallNode);
                cfaBuilder.functions.put(functionCallNode.get_procedure().name(), pFunctionEntry);
                cfaBuilder.addNode(functionCallNode.get_procedure().name(), pFunctionEntry);
            }

            nextCFGNode = (CFGNode) actualoutCFGNode.cfg_targets().cbegin().current().get_first();

            CLeftHandSide assignedVarExp =null;

            CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
            if(!functionCallNode.get_procedure().formal_outs().empty()){
                point p = functionCallNode.get_procedure().formal_outs().cbegin().current();
                ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
                ast type_ast = un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
                cFuncType = (CFunctionType) typeConverter.
                        getCType((CFGAST) type_ast.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());
            }

            pFunctionEntry = cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = getFunctionEntryNode(functionCallNode);
                cfaBuilder.functions.put(functionCallNode.get_procedure().name(), pFunctionEntry);
                cfaBuilder.addNode(functionCallNode.get_procedure().name(), pFunctionEntry);
            }

            //shall have an assignment expression
            // condition 1: variable = function();
            // condition 2: no real variable, for example, if(function(p))
            // in this condition, CodeSurfer built a temporary variable
            // and use the variable for following instruction;
            // thus, CFG always has a variable that is assigned with the return value of the called function

            CFGAST variable_ast;
            if(nextCFGNode.isExpression()){
                variable_ast = (CFGAST) nextCFGNode.get_ast(ast_family.getC_NORMALIZED()).get(0).as_ast();
            }else {
                //need a temporary variable to convert the operation as a binary operation,
                // e.g., if(function(p))--> temporary_var = function(p), if(temporary_var)
                variable_ast = (CFGAST) actualoutCFGNode.get_ast(ast_family.getC_NORMALIZED());
            }

            CType variable_type = typeConverter.getCType((CFGAST) variable_ast
                    .get(ast_ordinal.getBASE_TYPE()).as_ast());
            assignedVarExp = (CLeftHandSide) getAssignedIdExpression(variable_ast, variable_type, fileLocation);

            CIdExpression funcNameExpr =
                    new CIdExpression(
                            fileLocation,
                            cFuncType,
                            pFunctionEntry.getFunctionName(),
                            pFunctionEntry.getFunctionDefinition());

            functionCallExpression = new CFunctionCallExpression(fileLocation, cFuncType, funcNameExpr ,
                    params,pFunctionEntry.getFunctionDefinition());

            functionCallStatement = new CFunctionCallAssignmentStatement(fileLocation, assignedVarExp,
                    functionCallExpression);


            edge = new CFunctionSummaryEdge(rawCharacters, fileLocation,
                    prevNode, nextNode, functionCallStatement, pFunctionEntry);

            prevNode.addEnteringSummaryEdge(edge);
            nextNode.addLeavingSummaryEdge(edge);
            cfaNodeMap.put(nextCFGNode.id(),nextNode);
            traverseCFGNode(nextCFGNode,nextNode);
        }
    }


    public CFunctionEntryNode getFunctionEntryNode(CFGNode functionEntry){

    }

    /**
     *@Description function declaration, need to extract its return type and parameters
     *@Param [function, pFileName]
     *@return void
     **/
    public CFunctionDeclaration handleFunctionDeclaration() throws result{
        String functionName = function.name();

        //for struct example: struct test{int a, int b}
        //function example: test function(int c,int d)
        //routine type: test (c,d)

        // Function return type
        point_set formal_outs = function.formal_outs();//get the formal out of function, if type is VOID, the set is empty

        CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
        if(!formal_outs.empty()){
            //Note that it is impossible that there are more than one formal out
            point p = formal_outs.cbegin().current();
            ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
            ast type_ast = un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
            //for example: int functionName(int param_1, long param_2){...}
            //type_ast.pretty_print() == int (int, long)
            cFuncType = (CFunctionType) typeConverter.
                    getCType((CFGAST) type_ast.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());
            // ast_field_vector params = un_ast.get(ast_ordinal.getUC_PARAM_TYPES()).as_ast().children();
            // param type: params.get(i).as_ast().get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print()
        }

        // Parameters
        point_set formal_ins = function.formal_ins();// each point in formal_ins is an input parameter
        List<CParameterDeclaration> parameters = new ArrayList<>((int)formal_ins.size());
        if(!formal_ins.empty()){
            for(point_set_iterator point_it= formal_ins.cbegin();
                !point_it.at_end();point_it.advance())
            {
                CFGNode paramNode = (CFGNode) point_it.current();
                ast un_ast = paramNode.get_ast(ast_family.getC_UNNORMALIZED());

                String paramName = un_ast.pretty_print();//param_point.parameter_symbols().get(0).get_ast().pretty_print();

                CType paramType = typeConverter.getCType((CFGAST) un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
                CParameterDeclaration parameter =
                        new CParameterDeclaration(getLocation(paramNode,fileName),paramType,paramName);

                parameter.setQualifiedName(getQualifiedName(paramName, functionName));
                variableDeclarations.put(paramName.hashCode(),parameter);
                parameters.add(parameter);
            }
        }


        // Function declaration, exit
        functionDeclaration =
                new CFunctionDeclaration(
                        getLocation(function, fileName), cFuncType, functionName, parameters);
        return functionDeclaration;
    }

    /**
     *@Description function entry node
     *@Param [function, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode
     **/
    public CFunctionEntryNode handleFunctionDefinition() throws result{

        assert labelMap.isEmpty();
        assert gotoLabelNeeded.isEmpty();
        assert cfa == null;

        String functionName = function.name();
        logger.log(Level.FINE, "Creating function: " + functionName);

        // Return variable : The return value is written to this
        Optional<CVariableDeclaration> returnVar;

        returnVar = Optional.absent();
        for(procedure_locals_iterator prc_it= function.local_symbols();
            !prc_it.at_end();prc_it.advance()){
            symbol variable = prc_it.current();
            if(variable.get_kind().equals(symbol_kind.getRETURN())){
                returnVar =Optional.of(getVariableDeclaration(variable));
                break;
            }
        }


        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        CFunctionEntryNode entry =
                new CFunctionEntryNode(
                        getLocation(function, fileName), functionDeclaration, functionExit, returnVar);
        functionExit.setEntryNode(entry);

        cfa = entry;

        final CFANode nextNode = newCFANode();
        locStack.add(nextNode);

        final BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                entry, nextNode, "Function start edge");
        addToCFA(dummyEdge);

        cfaNodeMap.put(function.entry_point().id(),cfa);

        return entry;
    }

    /**
     *@Description CodeSurfer splits declaration and assignment, e.g., int i=0;-->int i; i=0;
     *@Param [variable]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration
     **/
    private CVariableDeclaration getVariableDeclaration(symbol variable)throws result{
        if(variable.get_kind().equals(symbol_kind.getRETURN())){

            FileLocation fileLocation = getLocation((int)function.file_line().get_second(),fileName);
            CStorageClass storageClass = CStorageClass.AUTO;

            CType varType = typeConverter.getCType((CFGAST)variable.get_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());
            String assignedVar = variable.get_ast(ast_family.getC_NORMALIZED()).pretty_print();//functionname$return

            CVariableDeclaration variableDeclaration = new CVariableDeclaration(
                            fileLocation,
                            variable.is_global(),
                            storageClass,
                            varType,
                            assignedVar,
                            assignedVar,
                            getQualifiedName(assignedVar, functionName),
                            null);
            variableDeclarations.put(assignedVar.hashCode(),variableDeclaration);
            return variableDeclaration;
        }else if(variable.get_kind().equals(symbol_kind.getUSER())){
            FileLocation fileLocation = getLocation((int)variable.file_line().get_second(),fileName);
            CStorageClass storageClass = ((CFGAST)variable.get_ast()).getStorageClass();

            CType varType = typeConverter.getCType((CFGAST)variable.get_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());
            String assignedVar = variable.name().replace("-","_");//name-id-->name_id

            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            variable.is_global(),
                            storageClass,
                            varType,
                            assignedVar,
                            assignedVar,
                            getQualifiedName(assignedVar, functionName),
                            null);
            variableDeclarations.put(assignedVar.hashCode(),newVarDecl);
            return newVarDecl;
        }
        return null;
    }

    /**
     *@Description traverse local declared symbols in the function
     *@Param []
     *@return void
     **/
    private void handleVariableDeclaration()throws result{
        assert (locStack.size() > 0) : "not in a function's scope";

        symbol_vector var_vector = function.declared_symbols();

        for(int i=0;i<var_vector.size();i++){
            symbol variable = var_vector.get(i);

            //focus on user defined local variables
            if(variable.get_kind().equals(symbol_kind.getUSER()) && !variable.is_formal()){
                FileLocation fileLocation = getLocation((int)variable.file_line().get_second(),fileName);
                CVariableDeclaration variableDeclaration = getVariableDeclaration(variable);
                CFANode prevNode = locStack.peek();
                CFANode nextNode = newCFANode();
                CDeclarationEdge edge = new CDeclarationEdge(variable.primary_declaration().characters(),
                        fileLocation, prevNode, nextNode, variableDeclaration);
                addToCFA(edge);
                locStack.push(nextNode);
                cfaNodeMap.put(variable.primary_declaration().id(),nextNode);
            }

        }

    }


    /**
     *@Description TODO need to modify
     *@Param [labelNode, fileloc]
     *@return void
     **/
    private void handleLabelNode(final CFGNode labelNode, FileLocation fileloc) throws result{
        assert labelNode.isLabel();
        String labelName = labelNode.getLabelName();

        if(labelMap.containsKey(labelName)){
            System.out.println("Duplicate label " + labelName + " in function " + cfa.getFunctionName());
        }

        CFANode prevNode = locStack.pop();


        CLabelNode labelCFANode = new CLabelNode(cfa.getFunctionName(), labelName);

        locStack.push(labelCFANode);
        labelMap.put(labelName,labelCFANode);

        boolean isPrevNodeReachable = isReachableNode(prevNode);
        if (isPrevNodeReachable) {
            BlankEdge blankEdge =
                    new BlankEdge(
                            labelNode.getRawSignature(),
                            fileloc,
                            prevNode,
                            labelCFANode,
                            "Label: " + labelName);
            addToCFA(blankEdge);
        }

        // Check if any goto's previously analyzed need connections to this label
        for (Pair<CFANode, FileLocation> gotoNode : gotoLabelNeeded.get(labelName)) {
            String description = "Goto: " + labelName;
            BlankEdge gotoEdge =
                    new BlankEdge(
                            description,
                            fileloc,
                            gotoNode.getFirst(),
                            labelCFANode,
                            description);
            addToCFA(gotoEdge);
        }
        gotoLabelNeeded.removeAll(labelName);

        if (!isPrevNodeReachable && isReachableNode(labelCFANode)) {
            locStack.pop();
            CFANode node = newCFANode();
            BlankEdge blankEdge =
                    new BlankEdge(
                            labelNode.getRawSignature(),
                            onlyFirstLine(fileloc),
                            labelCFANode,
                            node,
                            "Label: " + labelName);
            addToCFA(blankEdge);
            locStack.push(node);
        }
    }



    /**
     *@Description TODO
     *@Param [gotoStatement, fileloc]
     *@return void
     **/
    private void handleGotoStatement(final CFGNode gotoNode,
                                     FileLocation fileloc) throws result{
        assert gotoNode.isJump();

        String gotoLabelName = gotoNode.getGoToLabelName();

        CFANode prevNode = locStack.pop();
        CFANode labelNode = labelMap.get(gotoLabelName);

//        // check if label is local label
//        CVariableDeclaration localLabel = scope.lookupLocalLabel(gotoLabelName);
//        if (localLabel != null) {
//            gotoLabelName = localLabel.getName();
//            labelNode = scope.lookupLocalLabelNode(gotoLabelName);
//        }

        if (labelNode != null) {
            BlankEdge gotoEdge = new BlankEdge(gotoNode.getRawSignature(),
                    fileloc, prevNode, labelNode, "Goto: " + gotoLabelName);

            /* labelNode was analyzed before, so it is in the labelMap,
             * then there can be a jump backwards and this can create a loop.
             * If LabelNode has not been the start of a loop, Node labelNode can be
             * the start of a loop, so check if there is a path from labelNode to
             * the current Node through DFS-search */

            if (!labelNode.isLoopStart() && isPathFromTo(labelNode, prevNode)) {
                labelNode.setLoopStart();
            }
            addToCFA(gotoEdge);
        } else {
            gotoLabelNeeded.put(gotoLabelName, Pair.of(prevNode, fileloc));
        }

        CFANode nextNode = newCFANode();
        locStack.push(nextNode);
    }


    private void handleReturnStatement(CFGNode returnNode,
                                       FileLocation fileloc) {

    }

    private void handleExpressionStatement(final CFGNode exprNode,
                                           FileLocation fileloc) {


    }



    /**  TODO not complete
     * Returns the id expression to an already declared variable. Returns it as a cast, if necessary
     * to match the expected type.
     */

    private CExpression getAssignedIdExpression(CSimpleDeclaration assignedVarDeclaration,final CType pExpectedType, final FileLocation fileLocation){

        String assignedVarName = assignedVarDeclaration.getName();
        CType expressionType = assignedVarDeclaration.getType().getCanonicalType();
        CIdExpression idExpression =
                new CIdExpression(
                        fileLocation, expressionType, assignedVarName, assignedVarDeclaration);

        if (expressionType.canBeAssignedFrom(pExpectedType)) {
            return idExpression;
        } else if (pointerOf(pExpectedType, expressionType)) {
            CType typePointingTo = ((CPointerType) pExpectedType).getType().getCanonicalType();
            if (expressionType.canBeAssignedFrom(typePointingTo)
                    || expressionType.equals(typePointingTo)) {
                return new CUnaryExpression(
                        fileLocation, pExpectedType, idExpression, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                throw new AssertionError("Unhandled type structure");
            }
        } else if (expressionType instanceof CPointerType) {
            return new CPointerExpression(fileLocation, pExpectedType, idExpression);
        } else {
            throw new AssertionError("Unhandled types structure");
        }
    }


    private CExpression getAssignedIdExpression(
            final CFGAST variable_ast, final CType pExpectedType, final FileLocation fileLocation) throws result{
        logger.log(Level.FINE, "Getting var declaration for point");

        String assignedVarName =variable_ast.normalizingVariableName();
        boolean isGlobal = variable_ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global();

        if(isGlobal && !cfaBuilder.globalVariableDeclarations.containsKey(assignedVarName.hashCode())){
            throw new RuntimeException("Global variable has no declaration: " + assignedVarName);
        }

        if(!isGlobal && !variableDeclarations.containsKey(assignedVarName.hashCode())) {
            throw new RuntimeException("Local variable has no declaration: " + assignedVarName);
        }
        CSimpleDeclaration assignedVarDeclaration;
        if(isGlobal){
            assignedVarDeclaration = cfaBuilder.globalVariableDeclarations.get(assignedVarName.hashCode());
        }else {
            assignedVarDeclaration = variableDeclarations.get(assignedVarName.hashCode());
        }
        return getAssignedIdExpression(assignedVarDeclaration, pExpectedType, fileLocation);
    }



    /**
     *@Description TODO
     *@Param [point, pFunctionName, pInitializer, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration
     **/
    private CDeclaration getAssignedVarDeclaration(
            final CFGNode node) throws result {

        FileLocation fileLocation = getLocation(node,fileName);
        final int itemId = node.getVariableNameInNode().hashCode();
        if (!variableDeclarations.containsKey(itemId)) {
            CFGAST nc_ast = (CFGAST) node.get_ast(ast_family.getC_NORMALIZED());
            CFGAST un_ast = (CFGAST) node.get_ast(ast_family.getC_UNNORMALIZED());

            String assignedVar = nc_ast.children().get(0).as_ast().pretty_print();//the 1st child field store the variable

            final boolean isGlobal = node.declared_symbol().is_global();
            // Support static and other storage classes

            CStorageClass storageClass= un_ast.getStorageClass();

            CType varType = typeConverter.getCType((CFGAST) nc_ast.get(ast_ordinal.getNC_TYPE()).as_ast());

            if (isGlobal && varType instanceof CPointerType) {
                varType = ((CPointerType) varType).getType();
            }

            //note that in CodeSurfer CFGs of a function,
            // all local variable declarations do not have initializer, e.g., int var;
            // int var=0; --> [declaration] int var; [expression] var=0;

            CSimpleDeclaration newDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            isGlobal,
                            storageClass,
                            varType,
                            assignedVar,
                            assignedVar,
                            getQualifiedName(assignedVar, functionName),
                            null);
            assert !variableDeclarations.containsKey(itemId);
            variableDeclarations.put(itemId, newDecl);
        }

        return (CDeclaration) variableDeclarations.get(itemId);
    }

    private CDeclaration getAssignedVarDeclaration(
            final CFGNode node, final FileLocation fileLocation) throws result {

    }


    private CRightHandSide getConstant(final CFGNode exprNode, final FileLocation fileLoc)
            throws result {
        ast no_ast =exprNode.get_ast(ast_family.getC_NORMALIZED());
        //ast value_ast = no_ast.children().get(1).as_ast();

        CType expectedType = typeConverter.getCType((CFGAST) no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        return getConstant(exprNode, expectedType, fileLoc);
    }

    private CRightHandSide getConstant(final CFGNode exprNode, CType pExpectedType, final FileLocation fileLoc)
            throws result {

        CFGAST no_ast =(CFGAST) exprNode.get_ast(ast_family.getC_NORMALIZED());
        CFGAST value_ast =(CFGAST) no_ast.children().get(1).as_ast();
        ast_field type = no_ast.get(ast_ordinal.getBASE_TYPE());

        if(type.as_ast().pretty_print().equals("int"))//const int
        {
            int constantValue = value_ast.get(ast_ordinal.getBASE_VALUE()).as_int32();
            return new CIntegerLiteralExpression(fileLoc, pExpectedType, BigInteger.valueOf(constantValue));
        }else if(value_ast.isNullPointer())//null pointer: e.g., p = 0; p= NULL;
        {
            return new CPointerExpression(fileLoc,pExpectedType,getNull(fileLoc,pExpectedType));
        }else if(value_ast.isConstantExpression()){
            return getExpression(exprNode,pExpectedType);
        }else if(value_ast.isUndef()){//TODO
            CType constantType = typeConverter.getCType((CFGAST) type.as_ast());
            String undefName = "__VERIFIER_undef_" + constantType.toString().replace(' ', '_');
            CSimpleDeclaration undefDecl =
                    new CVariableDeclaration(
                            fileLoc,
                            true,
                            CStorageClass.AUTO,
                            pExpectedType,
                            undefName,
                            undefName,
                            undefName,
                            null);
            CExpression undefExpression = new CIdExpression(fileLoc, undefDecl);
            return undefExpression;
        } else if(isFunction(value_ast)){
            String value = value_ast.get(ast_ordinal.getBASE_NAME()).as_str();
            String functionName = value.substring(0,value.indexOf("$result"));
            CFunctionDeclaration funcDecl = functionDeclarations.get(functionName);
            CType functionType = funcDecl.getType();

            CIdExpression funcId = new CIdExpression(fileLoc, funcDecl);
            if (pointerOf(pExpectedType, functionType)) {
                return new CUnaryExpression(fileLoc, pExpectedType, funcId, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                return funcId;
            }
        }
        else if (exprNode.isGlobalConstant() && exprNode.isGlobalVariable()) {
            return getAssignedIdExpression(exprNode, pExpectedType, pFileName);
        } else {
            throw new UnsupportedOperationException("CFG parsing does not support constant " + exprNode.characters());
        }
    }

    private CExpression getExpression(
            final CFGNode exprNode, final CType pExpectedType, final FileLocation fileLocation)
            throws result {
        CFGAST no_ast = (CFGAST) exprNode.get_ast(ast_family.getC_NORMALIZED());

        getExpression(no_ast,pExpectedType,fileLocation);
    }


    private CExpression getExpression(final CFGAST ast, CType pExpectedType, FileLocation fileLocation) throws result{
        if(ast.isConstantExpression()){
            return createFromOpCode(exprNode, fileLocation);
        }else if(ast.isConstant()){
            return (CExpression) getConstant(ast, pExpectedType, fileLocation);
        }else
            return getAssignedIdExpression(ast,pExpectedType,fileLocation);
    }

    private boolean isPathFromTo(CFANode fromNode, CFANode toNode) throws result{

        // Optimization: do two DFS searches in parallel:
        // 1) search forwards from fromNode
        // 2) search backwards from toNode
        Deque<CFANode> toProcessForwards = new ArrayDeque<>();
        Deque<CFANode> toProcessBackwards = new ArrayDeque<>();
        Set<CFANode> visitedForwards = new HashSet<>();
        Set<CFANode> visitedBackwards = new HashSet<>();

        toProcessForwards.addLast(fromNode);
        visitedForwards.add(fromNode);

        toProcessBackwards.addLast(toNode);
        visitedBackwards.add(toNode);

        // if one of the queues is empty, the search has reached a dead end
        while (!toProcessForwards.isEmpty() && !toProcessBackwards.isEmpty()) {
            // step in forwards search
            CFANode currentForwards = toProcessForwards.removeLast();
            if (visitedBackwards.contains(currentForwards)) {
                // the backwards search already has seen the current node
                // so we know there's a path from fromNode to current and a path from
                // current to toNode
                return true;
            }

            for (CFANode successor : CFAUtils.successorsOf(currentForwards)) {
                if (visitedForwards.add(successor)) {
                    toProcessForwards.addLast(successor);
                }
            }

            // step in backwards search
            CFANode currentBackwards = toProcessBackwards.removeLast();
            if (visitedForwards.contains(currentBackwards)) {
                // the forwards search already has seen the current node
                // so we know there's a path from fromNode to current and a path from
                // current to toNode
                return true;
            }

            for (CFANode predecessor : CFAUtils.predecessorsOf(currentBackwards)) {
                if (visitedBackwards.add(predecessor)) {
                    toProcessBackwards.addLast(predecessor);
                }
            }
        }
        return false;
    }

    private void handleControlPoint(final CFGNode cfgNode, CFANode prevNode, FileLocation fileLocation)throws result{

        assert cfgNode.isControl_Point();

        point_syntax_kind psk = cfgNode.get_syntax_kind();

        if(psk.equals(point_syntax_kind.getIF())){
            handleIFPoint(cfgNode, prevNode, fileLocation);
        }else if(psk.equals(point_syntax_kind.getWHILE())){

        }
    }

    private void handleIFPoint(CFGNode ifNode, CFANode prevNode, FileLocation fileLocation)throws result{

        cfg_edge_set cfgEdgeSet = ifNode.cfg_targets();

        cfg_edge thenEdge = cfgEdgeSet.cbegin().current();
        cfg_edge elseEdge = cfgEdgeSet.cend().current();


        CFANode thenNode = newCFANode();

        // elseNode is the start of the else branch,
        // or the node after the loop if there is no else branch
        CFANode elseNode = newCFANode();

        CFGAST condition = (CFGAST) ifNode.get_ast(ast_family.getC_NORMALIZED());
        if(condition.get_class().equals(ast_class.getNC_VARIABLE())){
            String variableName = condition.normalizingVariableName();
            createConditionEdges(ifNode, prevNode, thenEdge, thenNode, elseEdge, elseNode, fileLocation);
        }else {
            CBinaryExpression binaryExpression = getBinaryExpression(condition,fileLocation);
        }

    }

    private void createConditionEdges(CFGNode ifNode, CFANode rootNode,
                                      cfg_edge thenEdge, CFANode thenNode,
                                      cfg_edge elseEdge, CFANode elseNode, FileLocation fileLocation) throws  result{


    }

    private CBinaryExpression getBinaryExpression(final CFGAST condition,FileLocation fileLocation) throws result {
        // the only one supported now

        CBinaryExpression.BinaryOperator operator = condition.getBinaryOperator();

        CFGAST variable_ast = (CFGAST) condition.children().get(0).as_ast();
        CType op1Type = typeConverter.getCType((CFGAST) variable_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CFGAST value_ast = (CFGAST) condition.children().get(1).as_ast();
        CType op2Type = typeConverter.getCType((CFGAST) value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        try {
            CCastExpression op1Cast = new CCastExpression(
                    fileLocation,
                    op1Type,
                    getExpression(variable_ast, op1Type, fileLocation));
            CCastExpression op2Cast = new CCastExpression(
                    fileLocation,
                    op2Type,
                    getExpression(value_ast, op2Type, fileLocation));

            return binaryExpressionBuilder.buildBinaryExpression(op1Cast, op2Cast, operator);
        }catch (UnrecognizedCodeException e){
            e.getParentState();
        }
        return null;
    }

    private void addToCFA(CFAEdge edge) {
        CFACreationUtils.addEdgeToCFA(edge, logger, false);
    }

    /**
     * @category helper
     */
    private CFANode newCFANode() {
        assert cfa != null;
        CFANode nextNode = new CFANode(cfa.getFunctionName());
        return nextNode;
    }

    // Keeping only first line of FileLocation. Temporary workaround to avoid considering
    // switch-case blocks and while blocks as covered when the BlankEdge entering these
    // blocks is traversed.
    private FileLocation onlyFirstLine(FileLocation f) {
        return new FileLocation(
                f.getFileName(),
                f.getNiceFileName(),
                f.getNodeOffset(),
                f.getNodeLength(),
                f.getStartingLineNumber(),
                f.getStartingLineNumber(),
                f.getStartingLineInOrigin(),
                f.getStartingLineInOrigin());
    }


}
