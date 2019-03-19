/**
 * @ClassName CFGFunctionBuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 5:27 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.google.common.base.Optional;
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
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.nulist.plugin.parser.CFABuilder.pointerOf;
import static org.nulist.plugin.parser.CFGAST.*;
import static org.nulist.plugin.parser.CFGNode.*;
import static org.nulist.plugin.parser.CFGNode.DECLARATION;
import static org.nulist.plugin.parser.CFGOperations.sortVectorByLineNo;
import static org.nulist.plugin.util.ClassTool.getUnsignedInt;
import static org.nulist.plugin.util.FileOperations.getLocation;
import static org.nulist.plugin.util.FileOperations.getQualifiedName;

public class CFGFunctionBuilder  {

    // Data structure for maintaining our scope stack in a function
    private final Deque<CFANode> locStack = new ArrayDeque<>();

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

    private final CFGHandleExpression expressionHandler;
    private final LogManager logger;
    private final procedure function;
    public final String functionName;
    private final CFGTypeConverter typeConverter;
    private CFunctionDeclaration functionDeclaration;
    private final String fileName;
    private final CFABuilder cfaBuilder;

    public CFGFunctionBuilder(
            LogManager pLogger,
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

        fileName = pFileName;
        this.cfaBuilder = cfaBuilder;
        expressionHandler = new CFGHandleExpression(pLogger,functionName,typeConverter);
        expressionHandler.setGlobalVariableDeclarations(cfaBuilder.expressionHandler.globalVariableDeclarations);
    }


    public FunctionEntryNode getCfa() {
        return cfa;
    }

    public FunctionEntryNode getFunctionEntryNode(point functionEntry) throws result{
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


        CType returnType = CVoidType.VOID;
        if(!formal_outs.empty()){
            //Note that it is impossible that there are more than one formal out
            point p = formal_outs.cbegin().current();

            ast un_ast = p.get_ast(ast_family.getC_UNNORMALIZED());
            ast type_ast = un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
            //for example: int functionName(int param_1, long param_2){...}
            //type_ast.pretty_print() == int (int, long)
            returnType = typeConverter.getCType(type_ast.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());

            // ast_field_vector params = un_ast.get(ast_ordinal.getUC_PARAM_TYPES()).as_ast().children();
            // param type: params.get(i).as_ast().get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print()
        }

        // Parameters
        point_set formal_ins = function.formal_ins();// each point in formal_ins is an input parameter
        List<CParameterDeclaration> parameters = new ArrayList<>((int)formal_ins.size());
        List<CType> paramsType = new ArrayList<>((int)formal_ins.size());

        if(!formal_ins.empty()){
            //left to right
            for(point_set_iterator point_it= formal_ins.cbegin();
                !point_it.at_end();point_it.advance())
            {
                point paramNode = point_it.current();
                ast un_ast = paramNode.get_ast(ast_family.getC_UNNORMALIZED());

                String paramName = un_ast.pretty_print();//param_point.parameter_symbols().get(0).get_ast().pretty_print();

                CType paramType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
                paramsType.add(paramType);
                CParameterDeclaration parameter =
                        new CParameterDeclaration(getLocation(paramNode,fileName),paramType,paramName);

                parameter.setQualifiedName(paramName);
                expressionHandler.variableDeclarations.put(paramName.hashCode(),parameter);
                parameters.add(parameter);
            }

        }
        CFunctionType cFuncType = new CFunctionType(checkNotNull(returnType),paramsType,false);

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
        //logger.log(Level.FINE, "Creating function: " + functionName);

        // Return variable : The return value is written to this
        Optional<CVariableDeclaration> returnVar = Optional.absent();
        FileLocation fileLocation =getLocation(function, fileName);

        if(!functionDeclaration.getType().getReturnType().equals(CVoidType.VOID)){
            point formal_out = function.formal_outs().cbegin().current();
            ast no_ast = formal_out.get_ast(ast_family.getC_NORMALIZED());
            String variabeName = no_ast.get(ast_ordinal.getNC_UNNORMALIZED()).as_ast().pretty_print();
            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            functionDeclaration.getType().getReturnType(),
                            variabeName,
                            variabeName,
                            variabeName,
                            null);
            expressionHandler.variableDeclarations.put(variabeName.hashCode(),newVarDecl);
            returnVar =Optional.of(newVarDecl);
        }

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        CFunctionEntryNode entry = new CFunctionEntryNode(
                        fileLocation, functionDeclaration, functionExit, returnVar);
        functionExit.setEntryNode(entry);

        cfa = entry;

        cfaNodeMap.put(function.entry_point().id(),cfa);

        return entry;
    }

    /**
     *@Description traverse CFG nodes and edges and transform them into CFA
     *@Param [function, pFileName]
     *@return void
     **/
    public void visitFunction() throws result {
        assert function!=null && function.get_kind().equals(procedure_kind.getUSER_DEFINED());

        //expressionHandler.setVariableDeclarations(variableDeclarations);
        //first visit: build nodes before traversing CFGs
        List<point> declSet = new ArrayList<>();
        point_set pointSet = function.points();
        for(point_set_iterator point_it=pointSet.cbegin();!point_it.at_end();point_it.advance()){
            point node = point_it.current();
            if(getKindName(node).equals(CALL_SITE)
                || getKindName(node).equals(CONTROL_POINT)
                || getKindName(node).equals(JUMP)
                || getKindName(node).equals(EXPRESSION)
                || getKindName(node).equals(SWITCH_CASE)
                || getKindName(node).equals(RETURN)
                || getKindName(node).endsWith(LABEL)){
                CFANode newCFAnode = newCFANode();
                cfaNodeMap.put(node.id(),newCFAnode);
            }else if(isGoToLabel(node)){
                String labelName = getLabelName(node);
                CLabelNode labelNode = new CLabelNode(functionName,labelName);
                cfaNodeMap.put(node.id(),labelNode);
            }else if(getKindName(node).equals(DECLARATION)){//static variable has no expression, but actually has initializer
                symbol s = node.declared_symbol();
                ast symbolAST = s.get_ast(ast_family.getC_UNNORMALIZED());
                if(s.is_local() &&
                        s.get_kind().equals(symbol_kind.getUSER()) &&
                        !s.is_formal()){
                    if(!symbolHasInitialization(symbolAST))
                        declSet.add(node);
                }else if(s.is_local_static() && staticSymbolHasInitialization(symbolAST))
                    declSet.add(node);
                else if(s.get_kind().equals(symbol_kind.getRETURN()))
                    declSet.add(node);
            }
        }

        //second visit: build edges
        point entryNextNode = function.entry_point().cfg_targets().cbegin().current().get_first();

        //handle all variable declarations and get the last declared symbol;
        handleVariableDeclaration(declSet, entryNextNode);
        //build edges between cfg nodes
        traverseCFGNode(entryNextNode);

        finish();
    }


    private void finish(){
        SortedSet<CFANode> nodes = Collections.unmodifiableSortedSet(cfaBuilder.cfaNodes.get(functionName));
        Iterator it = nodes.iterator();
        while (it.hasNext()){
            CFANode node = (CFANode) it.next();
            if(node.getNumLeavingEdges()==0 && node.getNumEnteringEdges()==0)
                it.remove();
        }
    }

    /**
     *@Description traverse local declared symbols in the function
     *@Param [declSet, entryNextNode]
     *@return null
     **/
    private void handleVariableDeclaration(List<point> declSet, point entryNextNode)throws result{

        symbol lastDecSymbol = null;
        boolean isFirstNode = true;

        CFANode prevNode = cfa;
        CVariableDeclaration preVar =null;
        FileLocation fileLocation =null;
        CFANode nextCFANode = cfaNodeMap.get(entryNextNode.id());
        if(declSet.isEmpty()){
            final BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                    prevNode, nextCFANode, "Function start edge");
            addToCFA(dummyEdge);
            //locStack.push(prevNode);
        }else {
            for(point del:declSet){
                symbol variable = del.declared_symbol();//normalized ast

                if(isFirstNode){
                    isFirstNode = false;
                    final CFANode nextNode = newCFANode();

                    final BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                            prevNode, nextNode, "Function start edge");
                    addToCFA(dummyEdge);
                    locStack.push(nextNode);

                }else {
                    prevNode = locStack.peek();
                    CFANode nextNode = newCFANode();
                    CDeclarationEdge edge = new CDeclarationEdge(getRawSignature(del),
                            fileLocation, prevNode, nextNode, preVar);
                    addToCFA(edge);
                    locStack.push(nextNode);
                    cfaNodeMap.put(del.id(),nextNode);
                }

                lastDecSymbol = variable;
                if(variable.get_kind().equals(symbol_kind.getRETURN()))
                    fileLocation = getLocation((int)function.entry_point().file_line().get_second()+1, fileName);
                else
                    fileLocation = getLocation((int)del.file_line().get_second(),fileName);
                preVar = expressionHandler.generateVariableDeclaration(variable, null, fileLocation);
            }
            prevNode = locStack.peek();
            CDeclarationEdge edge = new CDeclarationEdge(getRawSignature(lastDecSymbol.primary_declaration()),
                    fileLocation,prevNode,nextCFANode, preVar);
            addToCFA(edge);
        }

    }

    /**
     *@Description shall not generate nodes as traversing CFG node,
     *@Param [node, prevNode]
     *@return void
     **/
    private void traverseCFGNode(point cfgNode) throws result{
        CFANode cfaNode = cfaNodeMap.get(cfgNode.id());

        cfg_edge_set cfgEdgeSet = cfgNode.cfg_targets();
        FileLocation fileLocation = getLocation(cfgNode,fileName);//new FileLocation(fileName, 0, 1,getFileLineNumber(cfgNode),0);

        if(cfgEdgeSet.empty() && !isFunctionExit(cfgNode)){
            //throw new Exception("");
        }//check if the edge has been built
        else if(cfaNode.hasEdgeTo(cfaNodeMap.get(cfgEdgeSet.cbegin().current().get_first().id()))){
            cfaNode.setLoopStart();
            return;
        }

        if(cfgNode.is_inside_macro()&& !isExpression(cfgNode)){

        }else {
            String pointKind = getKindName(cfgNode);
            switch (pointKind){
                case CALL_SITE:
                    handleFunctionCall(cfgNode, fileLocation);
                    break;
                case CONTROL_POINT:
                    if(isIfControlPointNode(cfgNode))
                        handleIFPoint(cfgNode, fileLocation);
                    else if(isWhileControlPointNode(cfgNode))
                        handleWhilePoint(cfgNode, fileLocation);
                    else if(isDoControlPointNode(cfgNode))
                        handleDoWhilePoint(cfgNode,fileLocation);
                    else if(isForControlPointNode(cfgNode))
                        handleForPoint(cfgNode,fileLocation);
                    else if(isSwitchControlPointNode(cfgNode))
                        handleSwitchPoint(cfgNode,fileLocation);
                    else
                        throw new RuntimeException("other control point");
                    break;
                case JUMP:
                    if(isGotoNode(cfgNode)){
                        handleNormalPoint(cfgNode,
                                fileLocation, "Goto: "+ getGoToLabelName(cfgNode));
                        //handleGotoPoint(cfgNode, fileLocation);
                    }else if(isBreakNode(cfgNode)){
                        handleNormalPoint(cfgNode, fileLocation,"break");
                    }else
                        throw new RuntimeException("other jump node");
                    break;
                case EXPRESSION:
                    handleExpression(cfgNode,fileLocation);
                    break;
                case LABEL:
                    if(isGoToLabel(cfgNode))
                        handleNormalPoint(cfgNode, fileLocation,
                                "Label: "+getLabelName(cfgNode));
                        //handleLabelPoint(cfgNode, fileLocation);
                    else if(isElseLabel(cfgNode)){
                        handleNormalPoint(cfgNode, fileLocation, "else");
                    }else if(isDoLabel(cfgNode))
                        handleDoLabelPoint(cfgNode, fileLocation);
                    break;
                case RETURN:
                    handleReturnPoint(cfgNode,fileLocation);
                    break;

            }
        }

    }

    /**
     *@Description build the edge for expression node
     *@Param [exprNode, fileLocation]
     *@return void
     **/
    private void handleExpression(final point exprNode,
                                  FileLocation fileLocation)throws result{
        assert isExpression(exprNode);

        CFANode prevNode = cfaNodeMap.get(exprNode.id());

        point nextCFGNode = exprNode.cfg_targets().cbegin().current().get_first();
        CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());

        ast un_ast = exprNode.get_ast(ast_family.getC_UNNORMALIZED());

        if(un_ast.is_a(ast_class.getUC_INIT())){
            CVariableDeclaration variableDeclaration = expressionHandler.generateInitVarDeclFromUC(un_ast,fileLocation);
            CDeclarationEdge edge = new CDeclarationEdge(getRawSignature(exprNode),
                    fileLocation,
                    prevNode,
                    nextNode,
                    variableDeclaration);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode);
        }else if(isReturn(nextCFGNode) && hasReturnVariable()){
            CType type = functionDeclaration.getType().getReturnType();
            CLeftHandSide leftHandSide = (CLeftHandSide) expressionHandler.
                        getAssignedIdExpression((CVariableDeclaration)cfa.getReturnVariable().get(),
                                type, fileLocation);

            CExpression rightHandSide ;

            rightHandSide = expressionHandler.getExpressionFromUC(un_ast,type,fileLocation);
            CStatement statement = new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);

            CReturnStatement returnStatement = new CReturnStatement(fileLocation,
                                                                    Optional.of(rightHandSide),
                                                                    Optional.of((CAssignment) statement));
            String rawString = getRawSignature(nextCFGNode);
            CReturnStatementEdge edge = new CReturnStatementEdge(rawString,
                                            returnStatement, fileLocation,
                                            prevNode, cfa.getExitNode());
            addToCFA(edge);
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_OPERATION())){
            CStatement statement = expressionHandler.getAssignStatementFromUC(un_ast, fileLocation);
            CStatementEdge edge = new CStatementEdge(getRawSignature(exprNode), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode);
        }
    }

    /**
     *@Description TODO need to add inter-edge of function call
     *@Param [cfgNode, prevNode, fileLocation]
     *@return void
     **/
    private void handleFunctionCall(final point cfgNode,
                                    FileLocation fileLocation)throws result{
        assert isCall_Site(cfgNode);
        point_set actuals_in = cfgNode.actuals_in();
        point_set actuals_out = cfgNode.actuals_out();
        CFANode prevNode = cfaNodeMap.get(cfgNode.id());
        CFANode nextNode;

        CFunctionEntryNode pFunctionEntry = null;
        point functionCallNode, nextCFGNode;
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
                functionCallNode = cfgNode.cfg_inter_targets()
                            .cbegin().current().get_first();

                nextCFGNode = cfgNode.cfg_targets().cbegin()
                        .current().get_first();
                rawCharacters = getRawSignature(cfgNode);
            }else {
                // functionA(param_1, param_2, param_3)
                // call site: functionA()
                // actual_in: param_3
                // actual_in: param_2
                // actual_in: param_1 --> inter_edge to functionA entry point

                functionCallNode = actuals_in.to_vector().get((int)actuals_in.size()-1)
                        .cfg_inter_targets().cbegin().current().get_first();
                //combine call-site and actual in

                nextCFGNode = actuals_in.to_vector().get((int)actuals_in.size()-1)
                        .cfg_targets().cbegin().current().get_first();

                point_vector pv = actuals_in.to_vector();

                StringBuilder sb = new StringBuilder(cfgNode.characters().replace(")",""));
                for(int i=((int)pv.size())-1;i>=0;i--){
                    point actual_in = pv.get(i);
                    ast inAST  = actual_in.get_ast(ast_family.getC_UNNORMALIZED());

                    if(inAST.is_a(ast_class.getUC_EXPR_VARIABLE())){
                        ast variable_ast = inAST.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                        String variableName = normalizingVariableName(variable_ast);
                        sb.append(variableName).append(", ");
                    }else{
                        sb.append(actual_in.characters()).append(", ");
                    }

                    CType type = typeConverter.getCType(inAST.get(ast_ordinal.getBASE_TYPE()).as_ast());
                    CExpression param = expressionHandler.getExpressionFromUC(inAST,type,fileLocation);
                    params.add(param);
                }
                rawCharacters=sb.toString().replace(", ",")");
            }

            nextNode = handleSwitchCasePoint(nextCFGNode);
            //nextNode = cfaNodeMap.get(nextCFGNode.id());
            CFunctionDeclaration calledFunctionDeclaration =
                    cfaBuilder.functionDeclarations.get(functionCallNode.get_procedure().name());

            pFunctionEntry = (CFunctionEntryNode)cfaBuilder.functions.get(functionCallNode.get_procedure().name());
            if(pFunctionEntry!=null){
                pFunctionEntry = (CFunctionEntryNode)getFunctionEntryNode(functionCallNode);
                cfaBuilder.functions.put(functionCallNode.get_procedure().name(), pFunctionEntry);
                cfaBuilder.addNode(functionCallNode.get_procedure().name(), pFunctionEntry);
            }

            CIdExpression funcNameExpr =
                    new CIdExpression(
                            fileLocation,
                            calledFunctionDeclaration.getType(),
                            pFunctionEntry.getFunctionName(),
                            pFunctionEntry.getFunctionDefinition());

            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    calledFunctionDeclaration.getType(), funcNameExpr,
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

            traverseCFGNode(nextCFGNode);

        }else {
            // if the return result of the function call is used, there should have one actual_out node.
            // functionCallStatement ==> CFunctionCallAssignmentStatement, include, lefthandside and righthandside

            point actualoutCFGNode = actuals_out.cbegin().current();

            if(actuals_in.empty()){
                //shall have target nodes including actual_out and other node who using the actual out
                functionCallNode = cfgNode.cfg_inter_targets().cbegin().current().get_first();
            }else {
                functionCallNode = actuals_in.to_vector().get((int)actuals_in.size()-1)
                        .cfg_inter_targets().cbegin().current().get_first();

                point_vector pv = actuals_in.to_vector();
                StringBuilder sb = new StringBuilder(cfgNode.characters().replace(")",""));
                //default param order is right to left
                for(int i=((int)pv.size())-1;i>=0;i--){
                    point actual_in = pv.get(i);

                    ast inAST  = actual_in.get_ast(ast_family.getC_UNNORMALIZED());

                    if(inAST.is_a(ast_class.getUC_EXPR_VARIABLE())){
                        ast variable_ast = inAST.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                        String variableName = normalizingVariableName(variable_ast);
                        sb.append(variableName).append(", ");
                    }else{
                        sb.append(actual_in.characters()).append(", ");
                    }

                    CType type = typeConverter.getCType(inAST.get(ast_ordinal.getBASE_TYPE()).as_ast());
                    CExpression param = expressionHandler.getExpressionFromUC(inAST,type,fileLocation);
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

            nextCFGNode = actualoutCFGNode.cfg_targets().cbegin().current().get_first();

            CFunctionDeclaration calledFunctionDeclaration =
                    cfaBuilder.functionDeclarations.get(functionCallNode.get_procedure().name());

            //shall have an assignment expression
            // condition 1: variable = function();
            // condition 2: no real variable, for example, if(function(p))
            // in this condition, CodeSurfer built a temporary variable
            // and use the variable for following instruction;
            // thus, CFG always has a variable that is assigned with the return value of the called function
            //need a temporary variable to convert the operation as a binary operation,
            // e.g., if(function(p))--> temporary_var = function(p), if(temporary_var)
            //insert a decl node here

            //functionName$result+uid==>tempory result name
            ast un_ast = actualoutCFGNode.get_ast(ast_family.getC_UNNORMALIZED());

            symbol uc_symbol = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast()//operands
                    .children().get(0).get(ast_ordinal.getUC_ROUTINE())//routine
                    .get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();

            String name = expressionHandler.getFunctionCallResultName(un_ast);

            CVariableDeclaration declaration =
                    new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            calledFunctionDeclaration.getType().getReturnType(),
                            name,
                            uc_symbol.name(),
                            name,
                            null);

            expressionHandler.variableDeclarations.put(name.hashCode(),declaration);
            String rawString = declaration.toString();
            CFANode declNode = newCFANode();

            CDeclarationEdge declarationEdge = new CDeclarationEdge(rawString,
                    fileLocation,
                    prevNode,
                    declNode,
                    declaration);
            addToCFA(declarationEdge);

            //function call assign
            CLeftHandSide assignedVarExp = new CIdExpression(fileLocation, declaration);

            CIdExpression funcNameExpr =
                    new CIdExpression(
                            fileLocation,
                            calledFunctionDeclaration.getType(),
                            pFunctionEntry.getFunctionName(),
                            pFunctionEntry.getFunctionDefinition());

            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    calledFunctionDeclaration.getType(),
                    funcNameExpr, params,
                    pFunctionEntry.getFunctionDefinition());

            functionCallStatement = new CFunctionCallAssignmentStatement(
                    fileLocation, assignedVarExp,
                    functionCallExpression);

            //nextNode = cfaNodeMap.get(nextCFGNode.id());
            nextNode = handleSwitchCasePoint(nextCFGNode);
            edge = new CFunctionSummaryEdge(rawCharacters, fileLocation,
                    declNode, nextNode, functionCallStatement, pFunctionEntry);


            callEdge = new CFunctionCallEdge(rawCharacters,fileLocation,
                    declNode, pFunctionEntry,functionCallStatement, edge);
            returnEdge = new CFunctionReturnEdge(fileLocation,pFunctionEntry.getExitNode(),nextNode,edge);

            addToCFA(edge);
            addToCFA(callEdge);
            addToCFA(returnEdge);

            traverseCFGNode(nextCFGNode);
        }
    }

    /**
     *@Description build edge for label node
     *@Param [labelNode, fileloc]
     *@return void
     **/
    private void handleLabelPoint(final point labelNode, FileLocation fileloc) throws result{

        assert isLabel(labelNode);

        String labelName = getLabelName(labelNode);
        CLabelNode prevNode = (CLabelNode) cfaNodeMap.get(labelNode.id());

        point nextCFGNode = labelNode.cfg_targets().cbegin().current().get_first();
        CFANode nextNode = handleSwitchCasePoint(nextCFGNode);// cfaNodeMap.get(nextCFGNode.id());
        BlankEdge blankEdge =
                new BlankEdge(
                        getRawSignature(labelNode),
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
    private void handleGotoPoint(final point gotoNode, FileLocation fileloc) throws result{
        assert isJump(gotoNode);
        CFANode prevNode = cfaNodeMap.get(gotoNode.id());

        String gotoLabelName = getGoToLabelName(gotoNode);

        point nextCFGNode = gotoNode.cfg_targets().cbegin().current().get_first();
        assert getLabelName(nextCFGNode).equals(gotoLabelName);

        CFANode nextCFANode = cfaNodeMap.get(nextCFGNode.id());

        BlankEdge gotoEdge = new BlankEdge(getRawSignature(gotoNode),
                fileloc, prevNode, nextCFANode, "Goto: " + gotoLabelName);
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode);
    }


    //node that has only one processor
    private void handleNormalPoint(final point node, FileLocation fileLocation,
                                   String description) throws result{
        CFANode prevNode = cfaNodeMap.get(node.id());

        point nextCFGNode = node.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = handleSwitchCasePoint(nextCFGNode);//cfaNodeMap.get(nextCFGNode.id());


        BlankEdge gotoEdge = new BlankEdge(getRawSignature(node),
                    fileLocation, prevNode, nextCFANode, description);
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode);
    }

    //BUG!!!
    private void handleReturnPoint(point returnNode,
                                       FileLocation fileloc)throws result {
        //only have unnormalized ast
        CFANode prevNode = cfaNodeMap.get(returnNode.id());
        if(cfa.getReturnVariable().isPresent()){
            throw new RuntimeException("Return node that has the return variable shall have been processed");
        }

        CReturnStatement returnStatement = new CReturnStatement(fileloc, Optional.absent(), Optional.absent());

        CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(getRawSignature(returnNode),
                returnStatement, fileloc, prevNode, cfa.getExitNode());
        addToCFA(returnStatementEdge);
    }

    private void handleWhilePoint(point whileNode, FileLocation fileLocation)throws result{
        assert isWhileControlPointNode(whileNode);
        CFANode prevNode = cfaNodeMap.get(whileNode.id());

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode = cfaNodeMap.get(trueCFGNode.id());

        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = cfaNodeMap.get(falseCFGNode.id());

        CFANode whileCFANode = newCFANode();
        BlankEdge blankEdge = new BlankEdge(getRawSignature(whileNode), fileLocation,
                prevNode, whileCFANode, "while");
        addToCFA(blankEdge);
        whileCFANode.setLoopStart();
        cfaNodeMap.replace(whileNode.id(), whileCFANode);

        ast uc_condition = whileNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression conditionExpr=expressionHandler.getExpressionFromUC(uc_condition, type, fileLocation);

        createConditionEdges(whileCFANode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, fileLocation);
    }

    private void handleDoWhilePoint(point whileNode, FileLocation fileLocation)throws result{
        assert isDoControlPointNode(whileNode);
        CFANode prevNode = cfaNodeMap.get(whileNode.id());

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode = cfaNodeMap.get(trueCFGNode.id()).getLeavingEdge(0).getSuccessor();

        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = cfaNodeMap.get(falseCFGNode.id());

        ast uc_condition = whileNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression conditionExpr=expressionHandler.getExpressionFromUC(uc_condition, type, fileLocation);

        createConditionEdges(prevNode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, fileLocation);
    }

    private void handleForPoint(point forNode, FileLocation fileLocation)throws result{
        assert isForControlPointNode(forNode);
        CFANode prevNode = cfaNodeMap.get(forNode.id());
        prevNode.setLoopStart();
        cfg_edge_set cfgEdgeSet = forNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();

        CFANode trueNode = cfaNodeMap.get(trueCFGNode.id());

        CFANode falseNode = cfaNodeMap.get(falseCFGNode.id());

        ast uc_condition = forNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression conditionExpr=expressionHandler.getExpressionFromUC(uc_condition, type, fileLocation);

        createConditionEdges(prevNode,trueNode,trueCFGNode,falseNode,falseCFGNode, conditionExpr,fileLocation);

    }

    private void handleSwitchPoint(point switchNode, FileLocation fileLocation)throws result{
        assert  isSwitchControlPointNode(switchNode);

        CFANode prevNode = cfaNodeMap.get(switchNode.id());

        cfg_edge_vector cfgEdgeVector = sortVectorByLineNo(switchNode.cfg_targets().to_vector());

        ast variableAST = switchNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType variableType = typeConverter.getCType(variableAST
                .get(ast_ordinal.getBASE_TYPE()).as_ast());

        CExpression switchExpr = expressionHandler.getExpressionFromUC(variableAST, variableType, fileLocation);

        String rawSignature = "switch (" + getRawSignature(switchNode) + ")";
        String description = "switch (" + getRawSignature(switchNode) + ")";

        // firstSwitchNode is first Node of switch-Statement.
        point firstSwitchCFGNode = cfgEdgeVector.get(0).get_first();
        CFANode firstSwitchNode = cfaNodeMap.get(firstSwitchCFGNode.id());
        addToCFA(new BlankEdge(rawSignature, fileLocation, prevNode, firstSwitchNode, description));

        if(cfgEdgeVector.size()>2){
            for(int i=0;i<cfgEdgeVector.size()-1;i++){
                CExpression conditionExpr = handleSwitchCase(cfgEdgeVector.get(i).get_first(), switchExpr);
                String conditionString = conditionExpr.toASTString();
                CFANode case1 = cfaNodeMap.get(cfgEdgeVector.get(i).get_first().id());
                CFANode case2 = cfaNodeMap.get(cfgEdgeVector.get(i+1).get_first().id());
                FileLocation fileLoc = getLocation(cfgEdgeVector.get(i).get_first(),fileName);
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
                    FileLocation fileLoc2 = getLocation(cfgEdgeVector.get(i+1).get_first(),fileName);
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
                    traverseCFGNode(cfgEdgeVector.get(i+1).get_first());
                }
            }
        }

        point defaultCFGNode = cfgEdgeVector.get((int)(cfgEdgeVector.size()-1)).get_first();

        if(cfgEdgeVector.get((int)(cfgEdgeVector.size()-1)).get_second().name().equals("default")){
            CFANode defaultNode = cfaNodeMap.get(defaultCFGNode.id());
            point nextCFGNode = defaultCFGNode.cfg_targets().cbegin().current().get_first();
            FileLocation fileLocation1 = getLocation(defaultCFGNode,fileName);
            CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());
            BlankEdge blankEdge = new BlankEdge("default", fileLocation1,
                    defaultNode, nextNode,"default");
            addToCFA(blankEdge);
            traverseCFGNode(nextCFGNode);
        }

    }

    private CExpression handleSwitchCase(point caseNode, CExpression switchExpr)throws result{
        CFANode caseCFANode = cfaNodeMap.get(caseNode.id());

        point nextCFGNode = caseNode.cfg_targets().cbegin().current().get_first();

        FileLocation fileLocation = getLocation(caseNode,fileName);
        //case node: no normalized ast
        ast condition = caseNode.get_ast(ast_family.getC_UNNORMALIZED());
        ast valueAST = condition.get(ast_ordinal.getBASE_VALUE()).as_ast()
                    .get(ast_ordinal.getUC_CONSTANT()).as_ast()
                    .get(ast_ordinal.getBASE_VALUE()).as_ast();

        //in c, the case type can only be Integer or Char
        CType valueType = typeConverter.getCType(valueAST.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CExpression caseExpr = null;
        if(valueAST.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().equals("int") && !hasRadixField(valueAST)){
                char value = valueAST.get(ast_ordinal.getUC_TEXT()).as_str().charAt(1);
                valueType = CNumericTypes.CHAR;
                caseExpr = new CCharLiteralExpression(fileLocation,valueType,value);
        }else {
            BigInteger value = BigInteger.valueOf(valueAST.get(ast_ordinal.getBASE_VALUE()).as_int32());
            caseExpr = new CIntegerLiteralExpression(fileLocation,valueType, value);
        }

        CBinaryExpression conditionExpr = expressionHandler.buildBinaryExpression(
                switchExpr, caseExpr, CBinaryExpression.BinaryOperator.EQUALS);

        CFANode nextCFANode = handleSwitchCasePoint(nextCFGNode);

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
        if(!isSwitchCase(nextCFGNode))
            traverseCFGNode(nextCFGNode);

        return conditionExpr;
    }

    private CFANode handleSwitchCasePoint(point caseNode)throws result{
        //if a non-switch control point has a inter edge to a switch case point, this means
        //in the case of the non-switch control point, there is no break point, thus, shall have
        //a fall through edge to the new switch case
        //fall through

        if(!isSwitchCase(caseNode)){
           return cfaNodeMap.get(caseNode.id());
        }
        FileLocation fileLocation = getLocation(caseNode,fileName);

        CFANode caseCFANode = cfaNodeMap.get(caseNode.id());
        CFANode fallNode = newCFANode();


        final BlankEdge blankEdge =
                new BlankEdge("", fileLocation, fallNode, caseCFANode, "fall through");
        fallNode.addLeavingEdge(blankEdge);
        caseCFANode.addEnteringEdge(blankEdge);
        return fallNode;
    }

    private void handleDoLabelPoint(point doWhileNode, FileLocation fileLocation)throws result{
        assert isDoControlPointNode(doWhileNode);
        CFANode prevNode = cfaNodeMap.get(doWhileNode.id());

        point nextCFGNode = doWhileNode.cfg_targets().cbegin().current().get_first();

        CFANode nextCFANode = cfaNodeMap.get(nextCFGNode.id());
        nextCFANode.setLoopStart();

        BlankEdge gotoEdge = new BlankEdge("",
                    fileLocation,
                    prevNode,
                    nextCFANode,
                 "do");
        addToCFA(gotoEdge);

        traverseCFGNode(nextCFGNode);
    }

    private void handleIFPoint(point ifNode, FileLocation fileLocation)throws result{
        assert isIfControlPointNode(ifNode);

        CFANode prevNode = cfaNodeMap.get(ifNode.id());

        cfg_edge_set cfgEdgeSet = ifNode.cfg_targets();

        point thenCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        point elseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();

        CFANode thenNode = cfaNodeMap.get(thenCFGNode.id());

        // elseNode is the start of the else branch,
        // or the node after the loop if there is no else branch
        CFANode elseNode = cfaNodeMap.get(elseCFGNode.id());


        ast uc_condition = ifNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression conditionExpr=expressionHandler.getExpressionFromUC(uc_condition, type, fileLocation);

        createConditionEdges(prevNode, thenNode, thenCFGNode,
                elseNode, elseCFGNode, conditionExpr, fileLocation);

    }

    private void createConditionEdges(CFANode rootNode, CFANode thenNode, point thenCFGNode, CFANode elseNode,
                                      point elseCFGNode, CExpression conditionExp, FileLocation fileLocation) throws result {

        String conditionString = conditionExp.toString();

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

    private void addToCFA(CFAEdge edge) {
        CFACreationUtils.addEdgeToCFA(edge, logger);
    }

    /**
     * @category helper
     */
    private CFANode newCFANode() {
        assert cfa != null;
        CFANode nextNode = new CFANode(cfa.getFunctionName());
        cfaBuilder.addNode(functionName,nextNode);
        return nextNode;
    }

    private boolean hasReturnVariable(){
        assert cfa != null;
        return cfa.getReturnVariable().isPresent();
    }
}
