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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.*;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExporter;
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
import static org.nulist.plugin.parser.CFGOperations.sortVectorByLineNo;
import static org.nulist.plugin.util.ClassTool.getUnsignedInt;
import static org.nulist.plugin.util.FileOperations.getLocation;
import static org.nulist.plugin.util.FileOperations.getQualifiedName;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.isReachableNode;
import static org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.NO_ARGS_VOID_FUNCTION;

public class CFGFunctionBuilder  {

    // Data structure for maintaining our scope stack in a function
    private final Deque<CFANode> locStack = new ArrayDeque<>();


    private final CBinaryExpressionBuilder binExprBuilder;

    // Data structures for handling goto
    private final Map<String, CLabelNode> labelMap = new HashMap<>();

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
    private boolean isSwitchBranch = false;

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

        //handle all variable declarations and get the last declared symbol;
        symbol lastDecSymbol = handleVariableDeclaration();

        //first visit: build nodes before traversing CFGs
        point_set pointSet = function.points();
        for(point_set_iterator point_it=pointSet.cbegin();!point_it.at_end();point_it.advance()){
            CFGNode node = (CFGNode) point_it.current();
            if(node.getKindName().equals(CALL_SITE)
                || node.getKindName().equals(CONTROL_POINT)
                || node.getKindName().equals(JUMP)
                || node.getKindName().equals(EXPRESSION)
                ){
                CFANode newCFAnode = newCFANode();
                cfaNodeMap.put(node.id(),newCFAnode);
            }else if(node.isGoToLabel()){
                String labelName = node.getLabelName();
                CLabelNode labelNode = new CLabelNode(functionName,labelName);
                cfaNodeMap.put(node.id(),labelNode);
            }
        }

        //second visit: build edges
        CFGNode entryNextNode = (CFGNode) function.entry_point().cfg_targets().cbegin().current().get_first();

        CFGNode prevCFGNode = (CFGNode) lastDecSymbol.primary_declaration();
        FileLocation fileLocation = getLocation(prevCFGNode,fileName);
        CFANode prevCFAnode = locStack.peek();
        CFANode nextCFANode = cfaNodeMap.get(entryNextNode.id());
        if(prevCFAnode.equals(cfaNodeMap.get(prevCFGNode.id()))){
            CDeclarationEdge edge = new CDeclarationEdge(prevCFGNode.getRawSignature(),
                    fileLocation,prevCFAnode,nextCFANode,
                    (CDeclaration) variableDeclarations.get(getVariableDeclaration(lastDecSymbol).hashCode()));
            addToCFA(edge);
            traverseCFGNode(entryNextNode);
        }else {
            throw new RuntimeException("Problem in visitFunction");
        }

    }

    /**
     *@Description shall not generate nodes as traversing CFG node,
     *@Param [node, prevNode]
     *@return void
     **/
    private void traverseCFGNode(CFGNode cfgNode) throws result{
        CFANode cfaNode = cfaNodeMap.get(cfgNode.id());

        cfg_edge_set cfgEdgeSet = cfgNode.cfg_targets();
        FileLocation fileLocation = new FileLocation(fileName, 0, 1,cfgNode.getFileLineNumber(),0);

        if(cfgEdgeSet.empty() && !cfgNode.isFunctionExit()){
            //throw new Exception("");
        }//check if the edge has been built
        else if(cfaNode.hasEdgeTo(cfaNodeMap.get(cfgEdgeSet.cbegin().current().get_first().id()))){
            cfaNode.setLoopStart();
            return;
        }

        if(cfgNode.isFunctionExit()){

        }else if(cfgNode.is_inside_macro()){

        }else {
            switch (cfgNode.getKindName()){
                case CALL_SITE:
                    handleFunctionCall(cfgNode, fileLocation);
                    break;
                case CONTROL_POINT:
                    if(cfgNode.isIfControlPointNode())
                        handleIFPoint(cfgNode, fileLocation);
                    else if(cfgNode.isWhileControlPointNode())
                        handleWhilePoint(cfgNode, fileLocation);
                    else if(cfgNode.isDoControlPointNode())
                        handleDoWhilePoint(cfgNode,fileLocation);
                    else if(cfgNode.isForControlPointNode())
                        handleForPoint(cfgNode,fileLocation);
                    else if(cfgNode.isSwitchControlPointNode())
                        handleSwitchPoint(cfgNode,fileLocation);
                    else
                        throw new RuntimeException("other control point");
                    break;
                case JUMP:
                    if(cfgNode.isGotoNode()){
                        handleNormalPoint(cfgNode,CFAEdgeType.BlankEdge,
                                fileLocation, "Goto: "+ cfgNode.getGoToLabelName());
                        //handleGotoPoint(cfgNode, fileLocation);
                    }else if(cfgNode.isBreakNode()){
                        handleNormalPoint(cfgNode,CFAEdgeType.BlankEdge,
                                fileLocation,"break");
                    }else
                        throw new RuntimeException("other jump node");
                    break;
                case EXPRESSION:
                    handleNormalPoint(cfgNode, CFAEdgeType.StatementEdge,
                            fileLocation,"");
                    //handleExpression(cfgNode,fileLocation);
                    break;
                case LABEL:
                    if(cfgNode.isGoToLabel())
                        handleNormalPoint(cfgNode, CFAEdgeType.BlankEdge,
                                fileLocation, "Label: "+cfgNode.getLabelName());
                        //handleLabelPoint(cfgNode, fileLocation);
                    else if(cfgNode.isElseLabel()){
                        handleNormalPoint(cfgNode, CFAEdgeType.BlankEdge,
                                fileLocation, "else");
                    }else if(cfgNode.isDoLabel())
                        handleDoLabelPoint(cfgNode, fileLocation);

                    break;
                case RETURN:
                    handleReturnPoint(cfgNode,fileLocation);
                    break;
                case EXIT:
                    break;

            }
        }

    }

    /**
     *@Description build the edge for expression node
     *@Param [exprNode, fileLocation]
     *@return void
     **/
    private void handleExpression(final CFGNode exprNode,
                                  FileLocation fileLocation)throws result{
        assert exprNode.isExpression();
        CFANode prevNode = cfaNodeMap.get(exprNode.id());

        CFGNode nextCFGNode = (CFGNode) exprNode.cfg_targets().cbegin().current().get_first();
        CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());


        BlankEdge exprEdge = new BlankEdge(exprNode.getRawSignature(),
                fileLocation, prevNode, nextNode, "");
        addToCFA(exprEdge);

        traverseCFGNode(nextCFGNode);
    }

    /**
     *@Description TODO need to add inter-edge of function call
     *@Param [cfgNode, prevNode, fileLocation]
     *@return void
     **/
    private void handleFunctionCall(final CFGNode cfgNode,
                                    FileLocation fileLocation)throws result{
        assert cfgNode.isCall_Site();
        point_set actuals_in = cfgNode.actuals_in();
        point_set actuals_out = cfgNode.actuals_out();
        CFANode prevNode = cfaNodeMap.get(cfgNode.id());
        CFANode nextNode;

        CFunctionEntryNode pFunctionEntry = null;
        CFGNode functionCallNode, nextCFGNode;
        CFunctionSummaryEdge edge;
        CFunctionCallEdge callEdge;
        CFunctionReturnEdge returnEdge;
        String rawCharacters="";
        List<CExpression> params = new ArrayList<>();

        CFunctionCallExpression functionCallExpression;
        CFunctionCall functionCallStatement;
        if(actuals_out.empty()){
            //the return result of function call is not used in this function.
            // functionCallStatement ==> CFunctionCallStatement
            if(actuals_in.empty()){
                functionCallNode = (CFGNode) cfgNode.cfg_inter_targets()
                            .cbegin().current().get_first();

                nextCFGNode = (CFGNode) cfgNode.cfg_targets().cbegin()
                        .current().get_first();
                rawCharacters = cfgNode.characters();
            }else {
                // functionA(param_1, param_2, param_3)
                // call site: functionA()
                // actual_in: param_3
                // actual_in: param_2
                // actual_in: param_1 --> inter_edge to functionA entry point

                functionCallNode = (CFGNode) actuals_in.to_vector().get((int)actuals_in.size()-1)
                        .cfg_inter_targets().cbegin().current().get_first();
                //combine call-site and actual in

                nextCFGNode = (CFGNode) actuals_in.to_vector().get((int)actuals_in.size()-1)
                        .cfg_targets().cbegin().current().get_first();

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
                    CExpression param = getExpression(inAST,type,fileLocation);
                    params.add(param);
                    rawCharacters=sb.toString().replace(", ",")");
                }
            }

            nextNode = handleSwitchCasePoint(nextCFGNode);
            //nextNode = cfaNodeMap.get(nextCFGNode.id());

            CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
            if(!functionCallNode.get_procedure().formal_outs().empty()){
                point p = functionCallNode.get_procedure().formal_outs().cbegin().current();
                ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
                ast type_ast = un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
                cFuncType = (CFunctionType) typeConverter.
                        getCType((CFGAST) type_ast.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());
            }

            pFunctionEntry = (CFunctionEntryNode)cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = (CFunctionEntryNode)getFunctionEntryNode(functionCallNode);
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
                    params, pFunctionEntry.getFunctionDefinition());
            functionCallStatement = new CFunctionCallStatement(fileLocation,functionCallExpression);

            edge = new CFunctionSummaryEdge(rawCharacters, fileLocation,
                    prevNode, nextNode, functionCallStatement, pFunctionEntry);

            callEdge = new CFunctionCallEdge(rawCharacters,fileLocation,
                    prevNode, pFunctionEntry,functionCallStatement, edge);
            returnEdge = new CFunctionReturnEdge(fileLocation,pFunctionEntry.getExitNode(),nextNode,edge);

            addToCFA(edge);
            addToCFA(callEdge);
            addToCFA(returnEdge);


            /*pFunctionEntry.addEnteringEdge(callEdge);
            pFunctionEntry.getExitNode().addLeavingEdge(returnEdge);
            prevNode.addLeavingSummaryEdge(edge);
            prevNode.addLeavingEdge(callEdge);
            nextNode.addEnteringEdge(edge);
            nextNode.addEnteringEdge(returnEdge);*/
            traverseCFGNode(nextCFGNode);

        }else {
            // if the return result of the function call is used, there should have one actual_out node.
            // functionCallStatement ==> CFunctionCallAssignmentStatement, include, lefthandside and righthandside

            CFGNode actualoutCFGNode = (CFGNode) actuals_out.cbegin().current();

            if(actuals_in.empty()){
                //shall have target nodes including actual_out and other node who using the actual out
                functionCallNode = (CFGNode) cfgNode.cfg_inter_targets().cbegin().current().get_first();
            }else {
                functionCallNode = (CFGNode) actuals_in.to_vector().get((int)actuals_in.size()-1)
                        .cfg_inter_targets().cbegin().current().get_first();

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
                    CExpression param = getExpression(inAST,type,fileLocation);
                    params.add(param);
                    rawCharacters=sb.toString().replace(", ",")");
                }
            }

            pFunctionEntry = (CFunctionEntryNode) cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = (CFunctionEntryNode) getFunctionEntryNode(functionCallNode);
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

            pFunctionEntry = (CFunctionEntryNode) cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = (CFunctionEntryNode) getFunctionEntryNode(functionCallNode);
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
                cfaNodeMap.remove(nextCFGNode.id());
                nextCFGNode = (CFGNode) nextCFGNode.cfg_targets().cbegin().current().get_first();

            }else {
                //need a temporary variable to convert the operation as a binary operation,
                // e.g., if(function(p))--> temporary_var = function(p), if(temporary_var)
                variable_ast = (CFGAST) actualoutCFGNode.get_ast(ast_family.getC_NORMALIZED());
            }
            //nextNode = cfaNodeMap.get(nextCFGNode.id());
            nextNode = handleSwitchCasePoint(nextCFGNode);
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
                    params, pFunctionEntry.getFunctionDefinition());

            functionCallStatement = new CFunctionCallAssignmentStatement(fileLocation, assignedVarExp,
                    functionCallExpression);


            edge = new CFunctionSummaryEdge(rawCharacters, fileLocation,
                    prevNode, nextNode, functionCallStatement, pFunctionEntry);


            callEdge = new CFunctionCallEdge(rawCharacters,fileLocation,
                    prevNode, pFunctionEntry,functionCallStatement, edge);
            returnEdge = new CFunctionReturnEdge(fileLocation,pFunctionEntry.getExitNode(),nextNode,edge);

            addToCFA(edge);
            addToCFA(callEdge);
            addToCFA(returnEdge);

            /*pFunctionEntry.addEnteringEdge(callEdge);
            pFunctionEntry.getExitNode().addLeavingEdge(returnEdge);
            prevNode.addLeavingSummaryEdge(edge);
            prevNode.addLeavingEdge(callEdge);
            nextNode.addEnteringEdge(edge);
            nextNode.addEnteringEdge(returnEdge);*/

            traverseCFGNode(nextCFGNode);
        }
    }


    public FunctionEntryNode getFunctionEntryNode(CFGNode functionEntry) throws result{
        return cfaBuilder.functions.get(functionEntry.get_procedure().name());
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
     *@Description TODO
     *@Param [varInitPoint, operand, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/


    /**
     *@Description traverse local declared symbols in the function
     *@Param []
     *@return the last declaration node
     **/
    private symbol handleVariableDeclaration()throws result{
        assert (locStack.size() > 0) : "not in a function's scope";

        symbol_vector var_vector = function.declared_symbols();
        symbol lastDecSymbol = null;
        for(int i=0;i<var_vector.size();i++){
            symbol variable = var_vector.get(i);

            //focus on user defined local variables
            if(variable.get_kind().equals(symbol_kind.getUSER()) && !variable.is_formal()){
                lastDecSymbol = variable;
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

        return lastDecSymbol;

    }


    /**
     *@Description build edge for label node
     *@Param [labelNode, fileloc]
     *@return void
     **/
    private void handleLabelPoint(final CFGNode labelNode, FileLocation fileloc) throws result{

        assert labelNode.isLabel();

        String labelName = labelNode.getLabelName();
        CLabelNode prevNode = (CLabelNode) cfaNodeMap.get(labelNode.id());

        CFGNode nextCFGNode = (CFGNode) labelNode.cfg_targets().cbegin().current().get_first();
        CFANode nextNode = handleSwitchCasePoint(nextCFGNode);// cfaNodeMap.get(nextCFGNode.id());
        BlankEdge blankEdge =
                new BlankEdge(
                        labelNode.getRawSignature(),
                        fileloc,
                        prevNode,
                        nextNode,
                        "Label: " + labelName);
        addToCFA(blankEdge);

        traverseCFGNode(nextCFGNode);
    }



    /**
     *@Description build edge for goto jump node
     *@Param [gotoStatement, fileloc]
     *@return void
     **/
    private void handleGotoPoint(final CFGNode gotoNode, FileLocation fileloc) throws result{
        assert gotoNode.isJump();
        CFANode prevNode = cfaNodeMap.get(gotoNode.id());

        String gotoLabelName = gotoNode.getGoToLabelName();

        CFGNode nextCFGNode = (CFGNode) gotoNode.cfg_targets().cbegin().current().get_first();
        assert nextCFGNode.getLabelName().equals(gotoLabelName);

        CFANode nextCFANode = cfaNodeMap.get(nextCFGNode.id());

        BlankEdge gotoEdge = new BlankEdge(gotoNode.getRawSignature(),
                fileloc, prevNode, nextCFANode, "Goto: " + gotoLabelName);
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode);
    }


    //node that has only one processor
    private void handleNormalPoint(final CFGNode node, CFAEdgeType edgeType, FileLocation fileLocation,
                                   String description) throws result{
        CFANode prevNode = cfaNodeMap.get(node.id());

        CFGNode nextCFGNode = (CFGNode) node.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = handleSwitchCasePoint(nextCFGNode);//cfaNodeMap.get(nextCFGNode.id());

        if(edgeType.equals(CFAEdgeType.StatementEdge)){
            CFGAST no_ast = (CFGAST) node.get_ast(ast_family.getC_NORMALIZED());

            CStatement statement = getAssignStatement(no_ast,fileLocation);

            CStatementEdge edge = new CStatementEdge(node.getRawSignature(), statement,
                    fileLocation, prevNode, nextCFANode);
            addToCFA(edge);
        }else {
            BlankEdge gotoEdge = new BlankEdge(node.getRawSignature(),
                    fileLocation, prevNode, nextCFANode, description);
            addToCFA(gotoEdge);
        }

        traverseCFGNode(nextCFGNode);
    }

    private void handleReturnPoint(CFGNode returnNode,
                                       FileLocation fileloc)throws result {
        //only have unnormalized ast
        CFGAST un_ast = (CFGAST) returnNode.get_ast(ast_family.getC_UNNORMALIZED());
        Optional<CExpression> returnExpression;
        Optional<CAssignment> returnAssignment;
        try {

            CFGAST returnValue = (CFGAST) un_ast.get(ast_ordinal.getUC_RETURN_VALUE()).as_ast();

            CType expectedType = typeConverter.getCType((CFGAST)
                        returnValue.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression returnExp = null;

            CFGAST value_ast;
            if(returnValue.get_class().equals(ast_class.getUC_EXPR_CONSTANT())){//return 0; return true;
                value_ast = (CFGAST) returnValue.get(ast_ordinal.getUC_CONSTANT()).as_ast();
                if(expectedType.getCanonicalType().equals(CNumericTypes.CHAR)){
                    char value = (char)value_ast.get(ast_ordinal.getBASE_VALUE()).as_int8();
                    returnExp = new CCharLiteralExpression(fileloc, expectedType, value);
                }else if(expectedType.getCanonicalType().equals(CNumericTypes.INT)){
                    BigInteger value = BigInteger.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_int32());
                    returnExp = new CIntegerLiteralExpression(fileloc,expectedType,value);
                }else if(expectedType.getCanonicalType().equals(CNumericTypes.FLOAT)){
                    BigDecimal value = BigDecimal.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_flt32());
                    returnExp = new CFloatLiteralExpression(fileloc, expectedType,value);
                }

            }else if(returnValue.get_class().equals(ast_class.getUC_EXPR_VARIABLE())){//return a;
                value_ast = (CFGAST) returnValue.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                returnExp = getAssignedIdExpression(value_ast,expectedType,fileloc);
            }else if(returnValue.get_class().equals(ast_class.getUC_FUNCTION_CALL())){//return function();
                value_ast = (CFGAST) returnValue.get(ast_ordinal.getUC_OPERANDS()).as_ast();

            }else if(returnValue.get_class().equals(ast_class.getUC_SUBSCRIPT())){//return p[2]
                value_ast = (CFGAST) returnValue.get(ast_ordinal.getUC_OPERANDS()).as_ast();
                CExpression arrayExpr =;
                CExpression subscriptExpr = ;
                returnExp = new CArraySubscriptExpression(fileloc, expectedType,)
            }else {
                System.out.println(returnValue.get_class().name());
            }


            returnExpression = Optional.of(returnExp);

            CSimpleDeclaration returnVarDecl = (CSimpleDeclaration) cfa.getReturnVariable();

            CIdExpression returnVar = new CIdExpression(fileloc, returnVarDecl);

            CAssignment returnVarAssignment =
                    new CExpressionAssignmentStatement(
                            fileloc, returnVar, returnExp);
            returnAssignment = Optional.of(returnVarAssignment);


        }catch (result r){
            returnExpression = Optional.absent();
            returnAssignment = Optional.absent();
        }

        CFANode prevNode = cfaNodeMap.get(returnNode.id());

        CReturnStatement returnStatement = new CReturnStatement(fileloc, returnExpression, returnAssignment);

        CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(returnNode.getRawSignature(),
                returnStatement, fileloc, prevNode, cfa.getExitNode());
        addToCFA(returnStatementEdge);
    }


    private void handleWhilePoint(CFGNode whileNode, FileLocation fileLocation)throws result{
        assert whileNode.isWhileControlPointNode();
        CFANode prevNode = cfaNodeMap.get(whileNode.id());

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        CFGNode trueCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode = cfaNodeMap.get(trueCFGNode.id());

        CFGNode falseCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = cfaNodeMap.get(falseCFGNode.id());

        CFANode whileCFANode = newCFANode();
        BlankEdge blankEdge = new BlankEdge(whileNode.getRawSignature(), fileLocation,
                prevNode, whileCFANode, "while");
        addToCFA(blankEdge);
        whileCFANode.setLoopStart();
        cfaNodeMap.replace(whileNode.id(), whileCFANode);

        CFGAST condition = (CFGAST) whileNode.get_ast(ast_family.getC_NORMALIZED());
        CExpression conditionExpr=null;
        if(condition.get_class().equals(ast_class.getNC_VARIABLE())){
            String variableName = condition.normalizingVariableName();
            CSimpleDeclaration varDec = variableDeclarations.get(variableName.hashCode());
            conditionExpr = new CIdExpression(fileLocation, varDec.getType(),variableName, varDec);
        }else {
            conditionExpr = getBinaryExpression(condition,fileLocation);
        }
        createConditionEdges(whileCFANode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, fileLocation);
    }

    private void handleDoWhilePoint(CFGNode whileNode, FileLocation fileLocation)throws result{
        assert whileNode.isDoControlPointNode();
        CFANode prevNode = cfaNodeMap.get(whileNode.id());

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        CFGNode trueCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode = cfaNodeMap.get(trueCFGNode.id());

        CFGNode falseCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = cfaNodeMap.get(falseCFGNode.id());


        CFGAST condition = (CFGAST) whileNode.get_ast(ast_family.getC_NORMALIZED());
        CExpression conditionExpr=null;
        if(condition.get_class().equals(ast_class.getNC_VARIABLE())){
            String variableName = condition.normalizingVariableName();
            CSimpleDeclaration varDec = variableDeclarations.get(variableName.hashCode());
            conditionExpr = new CIdExpression(fileLocation, varDec.getType(),variableName, varDec);
        }else {
            conditionExpr = getBinaryExpression(condition,fileLocation);
        }
        createConditionEdges(prevNode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, fileLocation);
    }

    private void handleForPoint(CFGNode forNode, FileLocation fileLocation)throws result{
        assert forNode.isForControlPointNode();
        CFANode prevNode = cfaNodeMap.get(forNode.id());
        prevNode.setLoopStart();
        cfg_edge_set cfgEdgeSet = forNode.cfg_targets();

        CFGNode trueCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(0).get_first();
        CFGNode falseCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(1).get_first();

        CFANode trueNode = cfaNodeMap.get(trueCFGNode.id());

        CFANode falseNode = cfaNodeMap.get(falseCFGNode.id());

        CFGAST condition = (CFGAST) forNode.get_ast(ast_family.getC_NORMALIZED());
        CBinaryExpression conditionExpr = getBinaryExpression(condition,fileLocation);

        createConditionEdges(prevNode,trueNode,trueCFGNode,falseNode,falseCFGNode, conditionExpr,fileLocation);

    }

    private void handleSwitchPoint(CFGNode switchNode, FileLocation fileLocation)throws result{
        assert  switchNode.isSwitchControlPointNode();

        CFANode prevNode = cfaNodeMap.get(switchNode.id());

        cfg_edge_vector cfgEdgeVector = sortVectorByLineNo(switchNode.cfg_targets().to_vector());

        CFGAST variableAST = (CFGAST) switchNode.get_ast(ast_family.getC_NORMALIZED());
        CType variableType = typeConverter.getCType((CFGAST) variableAST
                .get(ast_ordinal.getBASE_TYPE()).as_ast());

        CExpression switchExpr = getAssignedIdExpression(variableAST, variableType, fileLocation);

        String rawSignature = "switch (" + switchNode.getRawSignature() + ")";
        String description = "switch (" + switchNode.getRawSignature() + ")";

        // firstSwitchNode is first Node of switch-Statement.
        CFGNode firstSwitchCFGNode = (CFGNode) cfgEdgeVector.get(0).get_first();
        CFANode firstSwitchNode = cfaNodeMap.get(firstSwitchCFGNode.id());
        addToCFA(new BlankEdge(rawSignature, fileLocation, prevNode, firstSwitchNode, description));

        if(cfgEdgeVector.size()>2){
            for(int i=0;i<cfgEdgeVector.size()-1;i++){
                CExpression conditionExpr = handleSwitchCase((CFGNode) cfgEdgeVector.get(i).get_first(), switchExpr);
                String conditionString = conditionExpr.toASTString();
                CFANode case1 = cfaNodeMap.get(cfgEdgeVector.get(i).get_first().id());
                CFANode case2 = cfaNodeMap.get(cfgEdgeVector.get(i+1).get_first().id());
                FileLocation fileLoc = getLocation((CFGNode)cfgEdgeVector.get(i).get_first(),fileName);
                if(!cfgEdgeVector.get(i+1).get_second().name().equals("implicit default")){
                    CAssumeEdge falseEdge =
                            new CAssumeEdge(
                                    "!(" + conditionString + ")",
                                    fileLoc,
                                    case1,
                                    case2,
                                    conditionExpr,
                                    false,
                                    false,
                                    false);
                    addToCFA(falseEdge);
                }else {
                    //no default branch
                    CFANode emptyNode = newCFANode();
                    FileLocation fileLoc2 = getLocation((CFGNode)cfgEdgeVector.get(i+1).get_first(),fileName);
                    CAssumeEdge falseEdge =
                            new CAssumeEdge(
                                    "!(" + conditionString + ")",
                                    fileLoc,
                                    case1,
                                    emptyNode,
                                    conditionExpr,
                                    false,
                                    false,
                                    false);
                    addToCFA(falseEdge);
                    BlankEdge blankEdge = new BlankEdge("",fileLoc2, emptyNode,case2,"");
                    addToCFA(blankEdge);
                    traverseCFGNode((CFGNode)cfgEdgeVector.get(i+1).get_first());
                }
            }
        }

        CFGNode defaultCFGNode = (CFGNode) cfgEdgeVector.get((int)(cfgEdgeVector.size()-1)).get_first();

        if(cfgEdgeVector.get((int)(cfgEdgeVector.size()-1)).get_second().name().equals("default")){
            CFANode defaultNode = cfaNodeMap.get(defaultCFGNode.id());
            CFGNode nextCFGNode = (CFGNode) defaultCFGNode.cfg_targets().cbegin().current().get_first();
            FileLocation fileLocation1 = getLocation(defaultCFGNode,fileName);
            CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());
            BlankEdge blankEdge = new BlankEdge("default", fileLocation1,
                    defaultNode, nextNode,"default");
            addToCFA(blankEdge);
            traverseCFGNode(nextCFGNode);
        }

    }

    private CExpression handleSwitchCase(CFGNode caseNode, CExpression switchExpr)throws result{
        CFANode caseCFANode = cfaNodeMap.get(caseNode.id());

        CFGNode nextCFGNode = (CFGNode) caseNode.cfg_targets().cbegin().current().get_first();
        CFANode nextCFANode = handleSwitchCasePoint(nextCFGNode);
        FileLocation fileLocation = getLocation(caseNode,fileName);
        //case node: no normalized ast
        CFGAST condition = (CFGAST) caseNode.get_ast(ast_family.getC_UNNORMALIZED());
        CFGAST valueAST = (CFGAST) condition.get(ast_ordinal.getBASE_VALUE()).as_ast()
                    .get(ast_ordinal.getUC_CONSTANT()).as_ast()
                    .get(ast_ordinal.getBASE_VALUE()).as_ast();

        //in c, the case type can only be Integer or Char
        CType valueType = typeConverter.getCType((CFGAST) valueAST.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CExpression caseExpr = null;
        if(valueAST.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().equals("int") && !valueAST.hasRadixField()){
                char value = valueAST.get(ast_ordinal.getUC_TEXT()).as_str().charAt(1);
                valueType = CNumericTypes.CHAR;
                caseExpr = new CCharLiteralExpression(fileLocation,valueType,value);
        }else {
            BigInteger value = BigInteger.valueOf(valueAST.get(ast_ordinal.getBASE_VALUE()).as_int32());
            caseExpr = new CIntegerLiteralExpression(fileLocation,valueType, value);
        }

        CBinaryExpression conditionExpr = buildBinaryExpression(
                switchExpr, caseExpr, CBinaryExpression.BinaryOperator.EQUALS);

        String conditionString = conditionExpr.toASTString();
        final CAssumeEdge trueEdge =
                new CAssumeEdge(
                        conditionString,
                        fileLocation,
                        caseCFANode,
                        nextCFANode,
                        conditionExpr,
                        true,
                        false,
                        false);
        addToCFA(trueEdge);
        traverseCFGNode(nextCFGNode);

        return conditionExpr;
    }

    private CFANode handleSwitchCasePoint(CFGNode caseNode)throws result{
        //if a non-switch control point has a inter edge to a switch case point, this means
        //in the case of the non-switch control point, there is no break point, thus, shall have
        //a fall through edge to the new switch case
        //fall through

        if(!caseNode.isSwitchCase()){
           return cfaNodeMap.get(caseNode.id());
        }
        FileLocation fileLocation = getLocation(caseNode,fileName);

        CFANode caseCFANode = cfaNodeMap.get(caseNode.id());
        CFANode fallNode = newCFANode();

        final BlankEdge blankEdge =
                new BlankEdge("", fileLocation, fallNode, caseCFANode, "fall through");
        addToCFA(blankEdge);

        return fallNode;
    }

    private void handleDoLabelPoint(CFGNode doWhileNode, FileLocation fileLocation)throws result{
        assert doWhileNode.isDoControlPointNode();
        CFANode prevNode = cfaNodeMap.get(doWhileNode.id());

        CFGNode nextCFGNode = (CFGNode) doWhileNode.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = cfaNodeMap.get(nextCFGNode.id());
        nextCFANode.setLoopStart();

        BlankEdge gotoEdge = new BlankEdge("",
                fileLocation, prevNode, nextCFANode, "do");
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode);
    }

    private void handleIFPoint(CFGNode ifNode, FileLocation fileLocation)throws result{
        assert ifNode.isIfControlPointNode();

        CFANode prevNode = cfaNodeMap.get(ifNode.id());

        cfg_edge_set cfgEdgeSet = ifNode.cfg_targets();

        CFGNode thenCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(0).get_first();
        CFGNode elseCFGNode = (CFGNode) cfgEdgeSet.to_vector().get(1).get_first();

        CFANode thenNode = cfaNodeMap.get(thenCFGNode.id());

        // elseNode is the start of the else branch,
        // or the node after the loop if there is no else branch
        CFANode elseNode = cfaNodeMap.get(elseCFGNode.id());

        CFGAST condition = (CFGAST) ifNode.get_ast(ast_family.getC_NORMALIZED());
        CExpression conditionExpr=null;
        if(condition.get_class().equals(ast_class.getNC_VARIABLE())){
            String variableName = condition.normalizingVariableName();
            CSimpleDeclaration varDec = variableDeclarations.get(variableName.hashCode());
            conditionExpr = new CIdExpression(fileLocation, varDec.getType(),variableName, varDec);
        }else {
            conditionExpr = getBinaryExpression(condition,fileLocation);
        }
        createConditionEdges(prevNode, thenNode, thenCFGNode, elseNode, elseCFGNode, conditionExpr, fileLocation);

    }


    private void createConditionEdges(CFANode rootNode, CFANode thenNode, CFGNode thenCFGNode, CFANode elseNode,
                                      CFGNode elseCFGNode, CExpression conditionExp, FileLocation fileLocation) throws result {

        String conditionString = conditionExp.toASTString();

        // edge connecting condition with thenNode
        final CAssumeEdge trueEdge =
                new CAssumeEdge(
                        conditionString,
                        fileLocation,
                        rootNode,
                        thenNode,
                        conditionExp,
                        true,
                        false,
                        false);
        addToCFA(trueEdge);
        traverseCFGNode(thenCFGNode);


        // edge connecting condition with elseNode
        final CAssumeEdge falseEdge =
                new CAssumeEdge(
                        "!(" + conditionString + ")",
                        fileLocation,
                        rootNode,
                        elseNode,
                        conditionExp,
                        false,
                        false,
                        false);
        addToCFA(falseEdge);
        traverseCFGNode(elseCFGNode);
    }


    private CBinaryExpression getBinaryExpression(final CFGAST condition,FileLocation fileLocation) throws result {
        // the only one supported now

        CBinaryExpression.BinaryOperator operator = condition.getBinaryOperator();

        CFGAST variable_ast = (CFGAST) condition.children().get(0).as_ast();
        CType op1Type = typeConverter.getCType((CFGAST) variable_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CFGAST value_ast = (CFGAST) condition.children().get(1).as_ast();
        CType op2Type = typeConverter.getCType((CFGAST) value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CCastExpression op1Cast = new CCastExpression(
                fileLocation,
                op1Type,
                getExpression(variable_ast, op1Type, fileLocation));
        CCastExpression op2Cast = new CCastExpression(
                fileLocation,
                op2Type,
                getExpression(value_ast, op2Type, fileLocation));

        return buildBinaryExpression(op1Cast, op2Cast, operator);
    }

    private CBinaryExpression buildBinaryExpression(
            CExpression operand1, CExpression operand2, CBinaryExpression.BinaryOperator op) {
        try {
            return binExprBuilder.buildBinaryExpression(operand1, operand2, op);
        } catch (UnrecognizedCodeException e) {
            e.getParentState();
        }
        return null;
    }

    private void addToCFA(CFAEdge edge) {
        CFACreationUtils.addEdgeToCFA(edge, logger);
    }

    /**
     * @category helper
     */
    private CFANode newCFANode() {
        assert cfa != null;
        CFANode nextNode = new CFANode(cfa.getFunctionName());
        return nextNode;
    }


    /**
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


    /**
     * Returns the id expression to an already declared variable.
     */
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
            assignedVarDeclaration = (CSimpleDeclaration) cfaBuilder.globalVariableDeclarations.get(assignedVarName.hashCode());
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

    private CRightHandSide getConstant(final CFGNode exprNode, final FileLocation fileLoc)
            throws result {
        ast no_ast =exprNode.get_ast(ast_family.getC_NORMALIZED());
        //ast value_ast = no_ast.children().get(1).as_ast();

        CType expectedType = typeConverter.getCType((CFGAST) no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        return getConstant(exprNode, expectedType, fileLoc);
    }

    private CRightHandSide getConstant(CFGAST value_ast, CType pExpectedType, FileLocation fileLoc)
            throws result{

        ast_field type = value_ast.get(ast_ordinal.getBASE_TYPE());

        if(type.as_ast().pretty_print().equals("int"))//const int
        {
            int constantValue = value_ast.get(ast_ordinal.getBASE_VALUE()).as_int32();
            return new CIntegerLiteralExpression(fileLoc, pExpectedType, BigInteger.valueOf(constantValue));
        }else if(value_ast.isNullPointer())//null pointer: e.g., p = 0; p= NULL;
        {
            return new CPointerExpression(fileLoc,pExpectedType,getNull(fileLoc,pExpectedType));
        }else if(value_ast.isConstantExpression()){
            return getExpression(value_ast,pExpectedType, fileLoc);
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
        } else if(cfaBuilder.isFunction(value_ast)){
            String value = value_ast.get(ast_ordinal.getBASE_NAME()).as_str();
            String functionName = value.substring(0,value.indexOf("$result"));
            CFunctionDeclaration funcDecl = cfaBuilder.functionDeclarations.get(functionName);
            CType functionType = funcDecl.getType();

            CIdExpression funcId = new CIdExpression(fileLoc, funcDecl);
            if (pointerOf(pExpectedType, functionType)) {
                return new CUnaryExpression(fileLoc, pExpectedType, funcId, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                return funcId;
            }
        }
        else if (value_ast.isGlobalConstant() && value_ast.isGlobalVariable()) {
            return getAssignedIdExpression(value_ast, pExpectedType, fileLoc);
        } else {
            throw new UnsupportedOperationException("CFG parsing does not support constant " + value_ast.as_string());
        }
    }

    private CRightHandSide getConstant(final CFGNode exprNode, CType pExpectedType, final FileLocation fileLoc)
            throws result {

        CFGAST no_ast =(CFGAST) exprNode.get_ast(ast_family.getC_NORMALIZED());
        CFGAST value_ast =(CFGAST) no_ast.children().get(1).as_ast();

        return getConstant(value_ast,pExpectedType,fileLoc);

    }

//    private CExpression getExpression(
//            final CFGNode exprNode, final CType pExpectedType, final FileLocation fileLocation)
//            throws result {
//        CFGAST no_ast = (CFGAST) exprNode.get_ast(ast_family.getC_NORMALIZED());
//
//        getExpression(no_ast,pExpectedType,fileLocation);
//    }





    private CExpression getNull(final FileLocation pLocation, final CType pType) {
        return new CIntegerLiteralExpression(pLocation, pType, BigInteger.ZERO);
    }



    //functionassignstatement has been handled in function call
    private CStatement getAssignStatement(CFGAST no_ast, FileLocation fileLocation)throws result{
        assert no_ast.get_class().equals(ast_class.getNC_NORMALASSIGN());

        CFGAST left_ast = (CFGAST) no_ast.children().get(0).as_ast();
        CType leftType = typeConverter.getCType(left_ast);
        CLeftHandSide leftHandSide  = (CLeftHandSide) getAssignedIdExpression(left_ast, leftType, fileLocation);

        CFGAST value_ast = (CFGAST) no_ast.children().get(1).as_ast();

        CType rightType = typeConverter.getCType(value_ast);
        CExpression rightHandSide = getExpression(value_ast, rightType, fileLocation);

        return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
    }


    private CExpression getExpression(CFGAST value_ast, CType valueType, FileLocation fileLoc)throws result{
        if(value_ast.isVariable()){//e.g., int a = b;
            return getAssignedIdExpression(value_ast, valueType, fileLoc);
        }else if(value_ast.isValue()){

        }else if(value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_ARITHMETIC()) ||//e.g., int i = a+1;
            value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_BITWISE())){// int i =a&1;
            return createFromArithmeticOp(value_ast, fileLoc);
        }else if(value_ast.isNormalExpression()){//const expression, e.g.,

        }else if(value_ast.isStructElementExpr()){//struct element, e.g., int p = astruct.a;

        }else if(value_ast.isPointerAddressExpr()){//pointer address, e.g., char p[30]="say hello", *p1 = &r;

        }else if(value_ast.isZeroInitExpr()){//zero initialization, e.g., char *p=NULL, p1[30]={};

        }else if(value_ast.isPointerExpr()){//pointer, e.g., int i = *(p+1);

        }

    }

    private CExpression createFromArithmeticOp(
            final CFGAST value_ast, final FileLocation fileLocation) throws result {

        CBinaryExpression.BinaryOperator operator = value_ast.getBinaryOperator();

        final CType expressionType = typeConverter.getCType((CFGAST) value_ast.children().get(0).as_ast());

        CFGAST operand1 = (CFGAST) value_ast.children().get(0).as_ast(); // First operand
        logger.log(Level.FINE, "Getting id expression for operand 1");
        CType op1type = typeConverter.getCType((CFGAST) operand1.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression operand1Exp = getExpression(operand1,op1type,fileLocation);

        CFGAST operand2 =  (CFGAST) value_ast.children().get(1).as_ast(); // Second operand
        CType op2type = typeConverter.getCType((CFGAST) operand2.get(ast_ordinal.getBASE_TYPE()).as_ast());
        logger.log(Level.FINE, "Getting id expression for operand 2");
        CExpression operand2Exp = getExpression(operand2, op2type, fileLocation);

        return new CBinaryExpression(
                fileLocation,
                expressionType,
                expressionType,
                operand1Exp,
                operand2Exp,
                operator);
    }



}
