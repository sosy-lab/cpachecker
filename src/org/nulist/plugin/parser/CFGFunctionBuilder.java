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
    // Variable hashCode(getUnsignedInt(point.hashCode());) -> Variable declaration
    private final Map<Long, CSimpleDeclaration> variableDeclarations = new HashMap<>();

    private final LogManager logger;
    private boolean encounteredAsm = false;
    private final procedure function;
    public final String functionName;
    private final CFGTypeConverter typeConverter;
    private CFunctionDeclaration functionDeclaration;
    private final String fileName;
    private final CFABuilder cfaBuilder;

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
                    handleFunctionCall(node);
                    break;
                case CONTROL_POINT:
                    handleControlPoint(node);
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

    private void handleFunctionCall(final CFGNode cfgNode, CFANode prevNode, FileLocation fileLocation)throws result{
        assert cfgNode.isCall_Site();
        point_set actuals_in = cfgNode.actuals_in();
        point_set actuals_out = cfgNode.actuals_out();
        CFANode nextNode = newCFANode();

        FunctionEntryNode pFunctionEntry = null;
        CFGNode functionCall, nextCFGNode;
        FunctionSummaryEdge edge;

        CFunctionCallExpression functionCallExpression;

        if(actuals_out.empty()){
            List<CExpression> params = new ArrayList<>();
            if(actuals_in.empty()){
                functionCall = (CFGNode) cfgNode.cfg_inter_targets().cbegin().current().get_first();

                nextCFGNode = (CFGNode) cfgNode.cfg_targets().cbegin().current().get_first();

            }else {
                functionCall = (CFGNode) actuals_in.cend().current().cfg_inter_targets().cbegin().current().get_first();
                //combine call-site and actual in

                nextCFGNode = (CFGNode) actuals_in.cend().current().cfg_targets().cbegin().current().get_first();

                point_vector pv = actuals_in.to_vector();
                for(int i=((int)pv.size())-1;i>=0;i--){
                    CFGNode actual_in = (CFGNode) pv.get(i);
                    CFGAST inAST  = (CFGAST) actual_in.get_ast(ast_family.getC_NORMALIZED());
                    if(inAST.children().get(1).as_ast().get_class().equals(ast_class.getNC_VARIABLE())){
                        String variableName = normalizingVariableName((CFGAST) inAST.children().get(1).as_ast());


                    }else {

                    }
                }



            }

            CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
            if(!functionCall.get_procedure().formal_outs().empty()){
                point p = functionCall.get_procedure().formal_outs().cbegin().current();
                ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
                ast type_ast = un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
                cFuncType = (CFunctionType) typeConverter.
                        getCType((CFGAST) type_ast.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());
            }



            functionCallExpression = new CFunctionCallExpression(fileLocation, cFuncType,, params,pDeclaration);

            edge = new FunctionSummaryEdge(, fileLocation,
                    prevNode, nextNode, pExpression, pFunctionEntry);

            prevNode.addEnteringSummaryEdge(edge);
            nextNode.addLeavingSummaryEdge(edge);
            cfaNodeMap.put(nextCFGNode.id(),nextNode);
            traverseCFGNode(nextCFGNode,nextNode);
        }else {
            CFGNode actualoutCFGNode = (CFGNode) actuals_out.cbegin().current();

            if(actuals_in.empty()){
                //shall have target nodes including actual_out and other node who using the actual out
                functionCall = (CFGNode) cfgNode.cfg_inter_targets().cbegin().current().get_first();
            }else {
                functionCall = (CFGNode) actuals_in.cend().current().cfg_inter_targets().cbegin().current().get_first();
            }

            pFunctionEntry = cfaBuilder.functions.get(functionCall.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = getFunctionEntryNode(functionCall);
                cfaBuilder.functions.put(functionCall.get_procedure().name(), pFunctionEntry);
                cfaBuilder.addNode(functionCall.get_procedure().name(), pFunctionEntry);
            }

            nextCFGNode = (CFGNode) actualoutCFGNode.cfg_targets().cbegin().current().get_first();
            if(nextCFGNode.isExpression()){

            }else {
                //need a temporary variable to store the actual out==> if(function(p))

            }

        }




        if(actuals_in.empty()){
            CFGNode functionCall = (CFGNode) cfgNode.cfg_inter_targets().cbegin().current().get_first();
            pFunctionEntry = getFunctionEntryNode(functionCall);

            if(actuals_out.empty()){
                //
                CFGNode nextCFGNode = (CFGNode) cfgNode.cfg_targets().cbegin().current().get_first();

                CFGNode functionReturn = (CFGNode) nextCFGNode.cfg_inter_sources().cbegin().current().get_first();

                FunctionSummaryEdge edge = new FunctionSummaryEdge(cfgNode.characters(), fileLocation,
                        prevNode, nextNode, pExpression, pFunctionEntry);
                prevNode.addEnteringSummaryEdge(edge);
                nextNode.addLeavingSummaryEdge(edge);
                cfaNodeMap.put(nextCFGNode.id(),nextNode);
                traverseCFGNode(nextCFGNode,nextNode);
            }else {
                //shall have target nodes including actual_out and other node who using the actual out
                CFGNode actualoutCFGNode = (CFGNode) actuals_out.cbegin().current();
                CFGNode functionReturn = (CFGNode) actualoutCFGNode.cfg_inter_sources().cbegin().current().get_first();
                FunctionSummaryEdge edge = new FunctionSummaryEdge(cfgNode.characters(), fileLocation,
                        prevNode, nextNode, pExpression, pFunctionEntry);

                CFGNode nextCFGNode = (CFGNode) actualoutCFGNode.cfg_targets().cbegin().current().get_first();
                if(nextCFGNode.isExpression()){

                }else {
                    //need a temporary variable to store the actual out==> if(function(p))

                }

            }
        }else {
            CFGNode functionCall = (CFGNode) actuals_in.cend().current().cfg_inter_targets().cbegin().current().get_first();
            pFunctionEntry = getFunctionEntryNode(functionCall);

            if(actuals_out.empty()){
                CFGNode nextCFGNode = (CFGNode) actuals_in.cend().current().cfg_targets().cbegin().current().get_first();
                CFGNode functionReturn = (CFGNode) nextCFGNode.cfg_inter_sources().cbegin().current().get_first();
                FunctionSummaryEdge edge = new FunctionSummaryEdge(cfgNode.characters(), fileLocation,
                        prevNode, nextNode, pExpression, pFunctionEntry);
                prevNode.addEnteringSummaryEdge(edge);
                nextNode.addLeavingSummaryEdge(edge);
                cfaNodeMap.put(nextCFGNode.id(),nextNode);
                traverseCFGNode(nextCFGNode,nextNode);
            }else {
                //shall have target nodes including, actual_in, actual_out, and other node who using the actual out
                CFGNode actualoutCFGNode = (CFGNode) actuals_out.cbegin().current();
                CFGNode functionReturn = (CFGNode) actualoutCFGNode.cfg_inter_sources().cbegin().current().get_first();

                CFGNode nextCFGNode = (CFGNode) actualoutCFGNode.cfg_targets().cbegin().current().get_first();
                if(nextCFGNode.isExpression()){
                    FunctionSummaryEdge edge = new FunctionSummaryEdge(nextCFGNode.characters(), fileLocation,
                            prevNode, nextNode, pExpression, pFunctionEntry);
                }else {
                    //need a temporary variable to store the actual out==> if(function(p))

                }
            }
        }



    }

    public FunctionEntryNode getFunctionEntryNode(CFGNode functionEntry){

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
                variableDeclarations.put(function.file_line().get_second(),parameter);
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
    public FunctionEntryNode handleFunctionDefinition() throws result{

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
        FunctionEntryNode entry =
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
            variableDeclarations.put((long)assignedVar.hashCode(),variableDeclaration);
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
            variableDeclarations.put((long)assignedVar.hashCode(),newVarDecl);
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
                CDeclarationEdge edge = new CDeclarationEdge(variable.name(), fileLocation,//signature??
                        prevNode, nextNode, variableDeclaration);
                addToCFA(edge);
                locStack.push(nextNode);
                cfaNodeMap.put(variable.primary_declaration().id(),nextNode);
            }

        }

    }

    private void handleLabelNode(final CFGNode labelNode, FileLocation fileloc) throws result{
        assert labelNode.isLabel();
        String labelName = labelNode.getLabelName();

        if(labelMap.containsKey(labelName)){
            System.out.println("Duplicate label " + labelName + " in function " + cfa.getFunctionName());
        }

        CFANode prevNode = locStack.pop();

        CVariableDeclaration localLabel = scope.lookupLocalLabel(labelName);
        if (localLabel != null) {
            labelName = localLabel.getName();
        }

        CLabelNode labelCFANode = new CLabelNode(cfa.getFunctionName(), labelName);

        locStack.push(labelCFANode);


        if (localLabel == null) {
            labelMap.put(labelName, labelCFANode);
            nodeIDMap.put(labelCFANode.toString(),labelNode.id());
        } else {
            if (scope.containsLabelCFANode(labelCFANode)){
                System.out.println("Duplicate label " + labelName + " in function " + cfa.getFunctionName());
            }
            scope.addLabelCFANode(labelCFANode);
        }

        boolean isPrevNodeReachable = isReachableNode(prevNode);
        if (isPrevNodeReachable) {
            BlankEdge blankEdge =
                    new BlankEdge(
                            labelNode.getRawSignature(),
                            onlyFirstLine(fileloc),
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
                            onlyFirstLine(gotoNode.getSecond()),
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
     *@Description normalize variables to avoid same name
     *@Param [varAST]
     *@return void
     **/
    private String normalizingVariableName(CFGAST varAST)throws result{
        assert varAST!=null;
        return varAST.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name().replace("-","_");
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

        CFANode prevNode = locStack.pop();
        FunctionExitNode functionExitNode = cfa.getExitNode();

        // a return statement leaves all available scopes at once.
        for (Collection<CSimpleDeclaration> vars : scope.getVariablesOfMostLocalScopes()) {
            functionExitNode.addOutOfScopeVariables(vars);
        }

        CReturnStatement returnstmt = astCreator.convert(returnStatement);
        prevNode = handleAllSideEffects(prevNode, fileloc, returnNode.getRawSignature(), true);

        if (returnstmt.getReturnValue().isPresent()) {
            returnstmt.getReturnValue().get().accept(checkBinding);
        }
        CReturnStatementEdge edge = new CReturnStatementEdge(returnNode.getRawSignature(),
                returnstmt, fileloc, prevNode, functionExitNode);
        addToCFA(edge);

        CFANode nextNode = newCFANode();
        locStack.push(nextNode);
    }

    private void handleExpressionStatement(final CFGNode exprNode,
                                           FileLocation fileloc) {

        CFANode prevNode = locStack.pop();
        CFANode lastNode = null;
        String rawSignature = exprNode.getRawSignature();

        if (exprStatement.getExpression() instanceof IASTExpressionList) {
            for (IASTExpression exp : ((IASTExpressionList) exprStatement.getExpression()).getExpressions()) {
                CStatement statement = astCreator.convertExpressionToStatement(exp);
                lastNode = createIASTExpressionStatementEdges(rawSignature, fileloc, prevNode, statement);
                prevNode = lastNode;
            }
            assert lastNode != null;
        } else {
            CStatement statement = astCreator.convert(exprStatement);
            lastNode = createIASTExpressionStatementEdges(rawSignature, fileloc, prevNode, statement);
        }
        locStack.push(lastNode);
    }

    private CFANode createIASTExpressionStatementEdges(String rawSignature, FileLocation fileloc,
                                                       CFANode prevNode, CStatement statement) {

        CFANode lastNode;
        boolean resultIsUsed = true;

        if (sideAssignmentStack.hasConditionalExpression()) {
            if (statement == null) {
                // if return-type of expression-list (side-effect-statement) is Void, statement is Null.
                // Example: void f(){}; int main(){(0, f());}
                resultIsUsed = false;

            } else if (statement instanceof CExpressionStatement) {
                // this may be code where the resulting value of a ternary operator is not used, e.g. (x ? f() : g())
                List<Pair<IASTExpression, CIdExpression>> tempVars =
                        sideAssignmentStack.getConditionalExpressions();
                if ((tempVars.size() == 1)
                        && (tempVars.get(0).getSecond()
                        == ((CExpressionStatement) statement).getExpression())) {
                    resultIsUsed = false;
                }
            }
        }

        prevNode = handleAllSideEffects(prevNode, fileloc, rawSignature, resultIsUsed);

        if (statement == null) {
            return prevNode;
        }

        statement.accept(checkBinding);
        if (resultIsUsed) {
            lastNode = newCFANode();

            CStatementEdge edge = new CStatementEdge(rawSignature, statement,
                    fileloc, prevNode, lastNode);
            addToCFA(edge);
        } else {
            lastNode = prevNode;
        }
        return lastNode;
    }


    /**
     * Returns the id expression to an already declared variable. Returns it as a cast, if necessary
     * to match the expected type.
     */
    private CExpression getAssignedIdExpression(
            final CFGNode node, final CType pExpectedType, final FileLocation fileLocation) throws result, CFGException{
        logger.log(Level.FINE, "Getting var declaration for point");

        if(!variableDeclarations.containsKey(getUnsignedInt(node.hashCode()))) {
            throw new CFGException("ID expression has no declaration: " + node.declared_symbol().name());
        }

        CSimpleDeclaration assignedVarDeclaration = variableDeclarations.get(getUnsignedInt(node.hashCode()));
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

    /**
     *@Description TODO
     *@Param [point, pFunctionName, pInitializer, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration
     **/
    private CDeclaration getAssignedVarDeclaration(
            final CFGNode node, final FileLocation fileLocation) throws result {


        final long itemId = node.id();
        if (!variableDeclarations.containsKey(itemId)) {
            CFGAST nc_ast = (CFGAST) node.get_ast(ast_family.getC_NORMALIZED());
            CFGAST un_ast = (CFGAST) node.get_ast(ast_family.getC_UNNORMALIZED());

            String assignedVar = nc_ast.children().get(0).as_ast().pretty_print();//the 1st child field store the variable

            final boolean isGlobal = node.declared_symbol().is_global();
            // Support static and other storage classes
            CStorageClass storageClass;
            switch (un_ast.get(ast_ordinal.getBASE_STORAGE_CLASS()).as_enum_value_string()){
                case "static":
                    storageClass = CStorageClass.STATIC;
                    break;
                case "extern":
                    storageClass = CStorageClass.EXTERN;
                    break;
                case "typedef":
                    storageClass = CStorageClass.TYPEDEF;
                    break;
                default:
                    storageClass = CStorageClass.AUTO;
                    break;
            }

            CType varType = typeConverter.getCType((CFGAST) nc_ast.get(ast_ordinal.getNC_TYPE()).as_ast());

            // We handle alloca not like malloc, which returns a pointer, but as a general
            // variable declaration. Consider that here by using the allocated type, not the
            // pointer of that type alloca returns.
//            if (pItem.isAllocaInst()) {
//                varType = typeConverter.getCType(pItem.getAllocatedType());
//            }

            if (isGlobal && varType instanceof CPointerType) {
                varType = ((CPointerType) varType).getType();
            }

            CInitializer initializer = null;
            ast no_ast = node.get_ast(ast_family.getC_NORMALIZED());
            // for example: int i=0;
            // in nc_ast: children {i, 0}
            //            attributes {is_initialization: true, type: int}
            // has initialization
            if(no_ast.get(ast_ordinal.getNC_IS_INITIALIZATION()).as_boolean()){
                ast_field type = no_ast.get(ast_ordinal.getNC_TYPE());
                CFGAST type_ast = (CFGAST)type.as_ast();
                if(isConstantArrayOrVector(type) || type_ast.isStructType() || type_ast.isEnumType()){
                    initializer = getConstantAggregateInitializer(no_ast, fileLocation);
                } else if (isConstantAggregateZero(type)) {
                    CType expressionType = typeConverter.getCType(initializerRaw.typeOf());
                    initializer = getZeroInitializer(initializerRaw, expressionType);
                } else {
                    initializer = new CInitializerExpression(fileLocation,
                            (CExpression) getConstant(node, fileLocation));
                }
            }else {//// Declaration without initialization
                initializer = null;
            }



            CSimpleDeclaration newDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            isGlobal,
                            storageClass,
                            varType,
                            assignedVar,
                            assignedVar,
                            getQualifiedName(assignedVar, functionName),
                            initializer);
            assert !variableDeclarations.containsKey(itemId);
            variableDeclarations.put(itemId, newDecl);
        }

        return variableDeclarations.get(itemId);
    }

    /**
     *@Description handle the aggregate initialization (normalized ast), e.g., int array[5]={1,2,3,4,5};
     *@Param [no_ast, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    private CInitializer getConstantAggregateInitializer(ast no_ast,
                                                         final FileLocation fileLoc) throws result {

        //ast no_ast = initialPoint.get_ast(ast_family.getC_NORMALIZED());
        ast_field value = no_ast.children().get(1);
        ast_field_vector elements = value.as_ast().children();

        int length = (int)elements.size();
        List<CInitializer> elementInitializers = new ArrayList<>(length);
        for(int i=0;i<length;i++){
            ast_field element = elements.get(i);
            CInitializer elementInitializer;
            ast_field elementType = element.as_ast().get(ast_ordinal.getBASE_TYPE());
            CFGAST elementType_ast = (CFGAST) elementType.as_ast();
            if(isConstantArrayOrVector(elementType) || elementType_ast.isStructType() || elementType_ast.isEnumType()){
                elementInitializer = getConstantAggregateInitializer(element.as_ast(), fileLoc);
            }else if(isConstantAggregateZero(elementType)){
                elementInitializer =
                        getZeroInitializer(element.as_ast(), typeConverter.getCType(elementType_ast), fileLoc);
            } else {
                elementInitializer =
                        new CInitializerExpression(
                                fileLoc, (CExpression) getConstant((CFGAST)element.as_ast()));
            }
            elementInitializers.add(elementInitializer);
        }

        CInitializerList aggregateInitializer =
                new CInitializerList(fileLoc, elementInitializers);
        return aggregateInitializer;
    }


    private CInitializer getZeroInitializer(
            final ast init_ast, final CType pExpectedType, final FileLocation fileLoc) throws result {

        CInitializer init;
        CType canonicalType = pExpectedType.getCanonicalType();
        if (canonicalType instanceof CArrayType) {
            int length = ((CArrayType) canonicalType).getLengthAsInt().getAsInt();
            CType elementType = ((CArrayType) canonicalType).getType().getCanonicalType();
            CInitializer zeroInitializer = getZeroInitializer(init_ast, elementType, fileLoc);
            List<CInitializer> initializers = Collections.nCopies(length, zeroInitializer);
            init = new CInitializerList(fileLoc, initializers);
        } else if (canonicalType instanceof CCompositeType) {

            List<CCompositeType.CCompositeTypeMemberDeclaration> members = ((CCompositeType) canonicalType).getMembers();
            List<CInitializer> initializers = new ArrayList<>(members.size());
            for (CCompositeType.CCompositeTypeMemberDeclaration m : members) {
                CType memberType = m.getType();
                CInitializer memberInit = getZeroInitializer(init_ast, memberType, fileLoc);
                initializers.add(memberInit);
            }
            init = new CInitializerList(fileLoc, initializers);

        } else {
            CExpression zeroExpression;
            if (canonicalType instanceof CSimpleType) {
                CBasicType basicType = ((CSimpleType) canonicalType).getType();
                if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
                    // use expected type for float, not canonical
                    zeroExpression = new CFloatLiteralExpression(fileLoc, pExpectedType, BigDecimal.ZERO);
                } else {
                    zeroExpression = CIntegerLiteralExpression.ZERO;
                }
            } else {
                // use expected type for cast, not canonical
                zeroExpression = new CCastExpression(fileLoc, pExpectedType, CIntegerLiteralExpression.ZERO);
            }
            init = new CInitializerExpression(fileLoc, zeroExpression);
        }

        return init;
    }

    private CRightHandSide getConstant(final CFGNode exprNode, final FileLocation fileLoc)
            throws result, CFGException {
        ast no_ast =exprNode.get_ast(ast_family.getC_NORMALIZED());
        //ast value_ast = no_ast.children().get(1).as_ast();

        CType expectedType = typeConverter.getCType((CFGAST) no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        return getConstant(exprNode, expectedType, fileLoc);
    }

    private CRightHandSide getConstant(final CFGNode exprNode, CType pExpectedType, final FileLocation fileLoc)
            throws result, CFGException {

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
            throws result,CFGException {
        CFGAST un_ast = (CFGAST) exprNode.get_ast(ast_family.getC_UNNORMALIZED());
        CFGAST no_ast = (CFGAST) exprNode.get_ast(ast_family.getC_NORMALIZED());

        if (no_ast.isConstantExpression()) {
            return createFromOpCode(exprNode, pFileName);
        } else if (no_ast.isConstant() && !exprNode.isGlobalVariable()) {
            return (CExpression) getConstant(un_ast, pExpectedType, fileLocation);
        } else {
            return getAssignedIdExpression(exprNode, pExpectedType);
        }
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

    private void handleControlPoint(final CFGNode cfgNode)throws result{

        assert cfgNode.isControl_Point();

        point_syntax_kind psk = cfgNode.get_syntax_kind();
        point_syntax_element pse = cfgNode.get_syntax_element();

        if(psk.equals(point_syntax_kind.getIF())){

        }else if(psk.equals(point_syntax_kind.getELSE())){

        }else if(psk.equals(point_syntax_kind.getWHILE())){

        }
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
