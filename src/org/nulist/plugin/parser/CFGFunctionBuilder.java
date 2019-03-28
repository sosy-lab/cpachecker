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
    private Set<CFANode> cfaNodes = new HashSet<>();
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

    private Map<String, point> noInitiVlaDeclarationPoint = new HashMap<>();


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
        expressionHandler.setGlobalVariableDeclarations(cfaBuilder.expressionHandler.globalDeclarations);
    }


    public FunctionEntryNode getCfa() {
        return cfa;
    }

    public FunctionEntryNode getFunctionEntryNode(point functionEntry) throws result{
        if(functionEntry.get_procedure().get_kind().equals(procedure_kind.getUSER_DEFINED()))
            return cfaBuilder.functions.get(functionEntry.get_procedure().name());
        else
            return cfaBuilder.systemFunctions.get(functionEntry.get_procedure().name());
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

        ast entryAST = function.entry_point().get_ast(ast_family.getC_UNNORMALIZED());
        CFunctionType cFunctionType = (CFunctionType) typeConverter
                .getCType(entryAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        FileLocation fileLocation = getLocation(function.entry_point(),fileName);

        // Parameters
        point_set formal_ins = function.formal_ins();// each point in formal_ins is an input parameter
        List<CParameterDeclaration> parameters = new ArrayList<>((int)cFunctionType.getParameters().size());

        if(!formal_ins.empty()){
            //left to right
            for(point_set_iterator point_it= formal_ins.cbegin();
                !point_it.at_end();point_it.advance())
            {
                point paramNode = point_it.current();
                ast un_ast = paramNode.get_ast(ast_family.getC_UNNORMALIZED());

                String paramName = un_ast.pretty_print();//param_point.parameter_symbols().get(0).get_ast().pretty_print();

                CType paramType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);

                CParameterDeclaration parameter =
                        new CParameterDeclaration(fileLocation,paramType,paramName);

                parameter.setQualifiedName(paramName);
                expressionHandler.variableDeclarations.put(paramName.hashCode(),parameter);
                if(!paramName.equals("__builtin_va_alist"))
                    parameters.add(parameter);
            }

        }

        CFunctionTypeWithNames cFuncType =
                new CFunctionTypeWithNames(checkNotNull(cFunctionType.getReturnType()),parameters,cFunctionType.takesVarArgs());

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
        cfaBuilder.addNode(functionName, functionExit);
        cfaBuilder.addNode(functionName, entry);

        return entry;
    }

    /**
     *@Description traverse CFG nodes and edges and transform them into CFA
     *@Param [function, pFileName]
     *@return void
     **/
    public void visitFunction() throws result {
        assert function!=null && function.get_kind().equals(procedure_kind.getUSER_DEFINED());

        if(functionName.equals("nas_proc_deactivate_pdn") || functionName.equals("nas_proc_get_pdn_status"))
            System.out.println();
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
                || getKindName(node).equals(INDIRECT_CALL)){
                CFANode newCFAnode = newCFANode();
                cfaNodeMap.put(node.id(),newCFAnode);
            }else if(isGoToLabel(node)){
                String labelName = getLabelName(node);
                CLabelNode labelNode = new CLabelNode(functionName,labelName);
                cfaNodes.add(labelNode);
                cfaNodeMap.put(node.id(),labelNode);
            }else if(getKindName(node).endsWith(LABEL)){
                CFANode newCFAnode = newCFANode();
                cfaNodeMap.put(node.id(),newCFAnode);
            }else if(getKindName(node).equals(DECLARATION)){//static variable has no expression, but actually has initializer
                symbol s = node.declared_symbol();
                ast symbolAST = s.get_ast(ast_family.getC_UNNORMALIZED());
                ast normalizedAST = s.get_ast();
                if(isVLAType(symbolAST.get(ast_ordinal.getBASE_TYPE()).as_ast()) &&
                        normalizedAST.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().contains("$temp")){
//                    symbol temp = normalizedAST.get(ast_ordinal.getBASE_TYPE())
//                            .get(ast_ordinal.getNC_NUM_ELEMENTS_ALOC()).as_symbol();
//                    String tempVarName = temp.get_ast().get(ast_ordinal.getBASE_NAME()).as_str();
//                    noInitiVlaDeclarationPoint.put(tempVarName,node);
                } else if(s.is_local() &&
                        s.get_kind().equals(symbol_kind.getUSER()) &&
                        !s.is_formal()){
                    if(!symbolHasInitialization(symbolAST))
                        declSet.add(node);
                }else if(s.is_local_static() && staticSymbolHasInitialization(symbolAST))
                    declSet.add(node);
                else if(s.get_kind().equals(symbol_kind.getRETURN()))
                    declSet.add(node);
                else if(s.is_global()||s.is_file_static()){//extern variable with no initialization is not built in cfabuilder
                    CType type = typeConverter.getCType(symbolAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                    checkOrInsertNewGlobalVarDeclarations(s, type, null, getLocation(node));
                }
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
        for(CFANode node:cfaNodes){
            if(node.getNumEnteringEdges()>0 || node.getNumLeavingEdges()>0)
                cfaBuilder.addNode(functionName, node);
        }
        noInitiVlaDeclarationPoint = new HashMap<>();
    }


    public void checkOrInsertNewGlobalVarDeclarations(symbol varSymbol, CType type,
                                                      CInitializer initializer,
                                                      FileLocation fileLocation) throws result{
        String varName = expressionHandler.getNormalizedVariableName(varSymbol, fileName);
        if(!expressionHandler.globalDeclarations.containsKey(varName.hashCode())){
            expressionHandler.generateVariableDeclaration(varSymbol, type, true, initializer, fileLocation);
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
                    prevNode, nextCFANode, "Function start dummy edge");
            addToCFA(dummyEdge);
            //locStack.push(prevNode);
        }else {
            for(point del:declSet){

                symbol variable = del.declared_symbol();//normalized ast

                CType type = typeConverter.getCType(variable.get_ast().get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                if(isFirstNode){
                    isFirstNode = false;
                    final CFANode nextNode = newCFANode();

                    final BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                            prevNode, nextNode, "Function start dummy edge");
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
                preVar = (CVariableDeclaration) expressionHandler
                        .generateVariableDeclaration(variable, type, false,
                                null, fileLocation);
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
        else if(cfaNode.getNumLeavingEdges()>0){
            return;
        }

        switch (getKindName(cfgNode)){
            case INDIRECT_CALL:
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
                else if(isNodeNode(cfgNode)){//ternary operation
                    handleIFPoint(cfgNode, fileLocation);
                }else
                    throw new RuntimeException("other control point");
                break;
            case JUMP:
                if(isGotoNode(cfgNode)){
//                        handleNormalPoint(cfgNode,
//                                fileLocation, "Goto: "+ getGoToLabelName(cfgNode));
                    handleGotoPoint(cfgNode, fileLocation);
                }else if(isBreakNode(cfgNode)){
                    handleNormalPoint(cfgNode, fileLocation,"break");
                }else if(isContinueNode(cfgNode)){
                    handleNormalPoint(cfgNode, fileLocation,"continue");
                }else
                    throw new RuntimeException("other jump node "+ cfaNode.toString());
                break;
            case EXPRESSION:
                handleExpression(cfgNode,fileLocation);
                break;
            case LABEL:
                if(isGoToLabel(cfgNode))
//                        handleNormalPoint(cfgNode, fileLocation,
//                                "Label: "+getLabelName(cfgNode));
                    handleLabelPoint(cfgNode, fileLocation);
                else if(isElseLabel(cfgNode)){
                    handleNormalPoint(cfgNode, fileLocation, "else");
                }else if(isDoLabel(cfgNode))
                    handleDoLabelPoint(cfgNode, fileLocation);
                else if(isNodeNode(cfgNode)){//

                }else
                    throw new RuntimeException("other label node "+ cfaNode.toString());
                break;
            case RETURN:
                handleReturnPoint(cfgNode,fileLocation);
                break;

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

        ast un_ast = exprNode.get_ast(ast_family.getC_UNNORMALIZED());
        ast no_ast = exprNode.get_ast(ast_family.getC_NORMALIZED());

        // assign a struct variable to another struct variable
        // for struct each member, CFG will have a block assign expr, but actually we only need one express
        // or a struct variable initialization
        if(no_ast.is_a(ast_class.getNC_BLOCKASSIGN())){
            CType varType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
            nextCFGNode = pointNextToBlockAssignmentExpr(exprNode, varType);
        }

        CFANode nextNode = cfaNodeMap.get(nextCFGNode.id());

        if(un_ast.is_a(ast_class.getUC_VLA_DECL())) {//(void)$temp314 = type array[$temp314];
            throw new RuntimeException("VLA DECL shall have been processed in the previous expression");
        }else if(isTempVariableAssignment(exprNode) && un_ast.is_a(ast_class.getUC_SET_VLA_SIZE())){
            //use temp variable as the function input
            String rawString;
            ast tempVar;
            CType type;
            if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_POINTEREXPR())){
                tempVar = no_ast.children().get(0).as_ast().children().get(0).as_ast();
                type =  typeConverter.getCType(tempVar.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                rawString = type.toString()+" "+no_ast.pretty_print();
                type = new CPointerType(false, false, type);
            }else if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE())){
                tempVar = no_ast.children().get(0).as_ast();
                type =  typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                rawString = type.toString()+" "+no_ast.pretty_print();
            }else{
                dumpAST(no_ast,0, no_ast.get_class().name());
                throw new RuntimeException("Not a pointer expr "+ exprNode.toString());
            }

            CExpression expression = expressionHandler.getExpressionFromUC(un_ast, type, fileLocation);
            CInitializer cInitializer = new CInitializerExpression(fileLocation, expression);
            String varName = tempVar.get(ast_ordinal.getNC_NAME()).as_str();
            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            type,
                            varName,
                            varName,
                            varName,
                            cInitializer);
            expressionHandler.variableDeclarations.put(varName.hashCode(),newVarDecl);

            CDeclarationEdge declarationEdge = new CDeclarationEdge(rawString,fileLocation,prevNode,nextNode,newVarDecl);
            addToCFA(declarationEdge);

            if(nextCFGNode.get_ast(ast_family.getC_UNNORMALIZED()).is_a(ast_class.getUC_VLA_DECL())){
                ast variable = un_ast.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                ast vartype = variable.get(ast_ordinal.getBASE_TYPE()).as_ast();
                String arrayName = variable.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name().replace("-","_");
                CType elementType = typeConverter.getCType(vartype.get(ast_ordinal.getUC_ELEMENT_TYPE()).as_ast(), expressionHandler);

                CExpression length = new CIdExpression(fileLocation, newVarDecl);
                CType arrayType = new CArrayType(false,false, elementType, length);
                CVariableDeclaration arraryDecl =
                        new CVariableDeclaration(
                                fileLocation,
                                false,
                                CStorageClass.AUTO,
                                arrayType,
                                arrayName,
                                variable.pretty_print(),
                                arrayName,
                                cInitializer);
                expressionHandler.variableDeclarations.put(arrayName.hashCode(),arraryDecl);
                nextCFGNode = nextCFGNode.cfg_targets().cbegin().current().get_first();

                CFANode nextnextNode = cfaNodeMap.get(nextCFGNode.id());
                CDeclarationEdge edge = new CDeclarationEdge(arraryDecl.toASTString(),
                        fileLocation,
                        nextNode,
                        nextnextNode,
                        arraryDecl);
                addToCFA(edge);
                traverseCFGNode(nextCFGNode);

            }else
                throw new RuntimeException("Issue in VLA");
        }else if(isTempVariableAssignment(exprNode)){
            ast tempVar;
            CType type;
            if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_POINTEREXPR())){
                tempVar = no_ast.children().get(0).as_ast().children().get(0).as_ast();
                type =  typeConverter.getCType(tempVar.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
               // type = new CPointerType(false, false, type);
            }else if(no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE())){
                tempVar = no_ast.children().get(0).as_ast();
                type =  typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
            }else{
                dumpAST(no_ast,0, no_ast.get_class().name());
                throw new RuntimeException("Not a pointer expr "+ exprNode.toString());
            }
            CExpression rightHandSide;
            if(un_ast.is_a(ast_class.getUC_ABSTRACT_ASSIGN()))
                rightHandSide = expressionHandler.getExpressionFromUC(
                        un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast(),
                        type,fileLocation);
            else
                rightHandSide = expressionHandler.getExpressionFromUC(un_ast,type,fileLocation);

            String variableName = tempVar.get(ast_ordinal.getNC_NAME()).as_str();
            CVariableDeclaration variableDeclaration;
            if(expressionHandler.variableDeclarations.containsKey(variableName.hashCode())){
                variableDeclaration = (CVariableDeclaration)
                        expressionHandler.variableDeclarations.get(variableName.hashCode());
                CIdExpression leftHandSide =
                        (CIdExpression) expressionHandler.getAssignedIdExpression(
                                variableDeclaration, variableDeclaration.getType(), fileLocation);
                CStatement statement  = new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
                CStatementEdge edge = new CStatementEdge(no_ast.pretty_print(), statement,
                        fileLocation, prevNode, nextNode);
                addToCFA(edge);
                traverseCFGNode(nextCFGNode);
            }else {
                CInitializer initializer = new CInitializerExpression(fileLocation, rightHandSide);
                variableDeclaration = new CVariableDeclaration(fileLocation,
                        false,
                        CStorageClass.AUTO,
                        type,
                        variableName,
                        variableName,
                        variableName,
                        initializer);
                expressionHandler.variableDeclarations.put(variableName.hashCode(),variableDeclaration);
                String rawstring = type.toString()+" "+no_ast.pretty_print();
                CDeclarationEdge edge = new CDeclarationEdge(rawstring, fileLocation, prevNode, nextNode, variableDeclaration);
                addToCFA(edge);
                traverseCFGNode(nextCFGNode);
            }
        }else if(un_ast.is_a(ast_class.getUC_INIT())){
            CVariableDeclaration variableDeclaration;
            if(useTempVariable(exprNode)){//init is the temp var
                variableDeclaration = (CVariableDeclaration)
                        expressionHandler.generateInitVarDecl(un_ast,no_ast,fileLocation);
            }else{
                variableDeclaration = (CVariableDeclaration)
                        expressionHandler.generateInitVarDeclFromUC(un_ast,fileLocation);
            }

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

            CExpression rightHandSide;

            //the expression shall find the part that use the temp var
            if (useTempVariable(exprNode)) {
                rightHandSide = expressionHandler.getExpression(no_ast.children().get(1).as_ast(),type, fileLocation);
            }else
                rightHandSide = expressionHandler.getExpressionFromUC(un_ast,type,fileLocation);

            CStatement statement = new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);

            CReturnStatement returnStatement = new CReturnStatement(fileLocation,
                                                                    Optional.of(rightHandSide),
                                                                    Optional.of((CAssignment) statement));

            String rawString = getRawSignature(exprNode);
            CReturnStatementEdge edge = new CReturnStatementEdge(returnStatement.toASTString(),
                                            returnStatement, fileLocation,
                                            prevNode, cfa.getExitNode());
            addToCFA(edge);
        }else if(useTempVariable(exprNode) && !isReturn(nextCFGNode)){
            //these expression points that uses a temp var
            // we need to noramlize the expression with the temp var, for example, a[i++]=10==> int temp=i; i=i+1; a[temp]=10;
            CStatement statement = expressionHandler.getAssignStatement(exprNode, fileLocation);
            CStatementEdge edge = new CStatementEdge(getRawSignature(exprNode), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode);
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_OPERATION())){
            CStatement statement = expressionHandler.getAssignStatementFromUC(un_ast, fileLocation);
            CStatementEdge edge = new CStatementEdge(getRawSignature(exprNode), statement,
                    fileLocation, prevNode, nextNode);
            addToCFA(edge);
            traverseCFGNode(nextCFGNode);
        }else {
            throw new RuntimeException("Not support expression: "+ exprNode.toString()+ " "+ un_ast.get_class().name());
        }
    }


    /**
     *@Description TODO need to add inter-edge of function call
     *@Param [cfgNode, prevNode, fileLocation]
     *@return void
     **/
    private void handleFunctionCallOld(final point cfgNode,
                                    FileLocation fileLocation)throws result{
        assert isCall_Site(cfgNode) || isIndirect_Call(cfgNode);
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

                    CType type = typeConverter.getCType(inAST.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);
                    CExpression param = expressionHandler.getExpressionFromUC(inAST,type,fileLocation);
                    params.add(param);
                }
                rawCharacters=sb.toString().replace(", ",")");
            }

            nextNode = handleSwitchCasePoint(nextCFGNode);
            //nextNode = cfaNodeMap.get(nextCFGNode.id());
            CFunctionDeclaration calledFunctionDeclaration =
                    cfaBuilder.functionDeclarations.get(functionCallNode.get_procedure().name());
            if(calledFunctionDeclaration==null)
                throw  new RuntimeException("No such function declaration:"+ functionCallNode.get_procedure().name());

            pFunctionEntry = (CFunctionEntryNode)getFunctionEntryNode(functionCallNode);

            if(pFunctionEntry==null)
                throw  new RuntimeException("No such function entry:"+ functionCallNode.get_procedure().name());


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

            CStatementEdge statementEdge= new CStatementEdge(rawCharacters,functionCallStatement,
                    fileLocation, prevNode, nextNode);
            addToCFA(statementEdge);
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

                    CType type = typeConverter.getCType(inAST.get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);
                    CExpression param = expressionHandler.getExpressionFromUC(inAST,type,fileLocation);
                    params.add(param);
                }
                rawCharacters=sb.toString().replace(", ",")");
            }

            CFunctionDeclaration calledFunctionDeclaration =
                    cfaBuilder.functionDeclarations.get(functionCallNode.get_procedure().name());
            if(calledFunctionDeclaration==null)
                throw  new RuntimeException("No such function declaration:"+ functionCallNode.get_procedure().name());

            pFunctionEntry = (CFunctionEntryNode)getFunctionEntryNode(functionCallNode);

            if(pFunctionEntry==null)
                throw  new RuntimeException("No such function entry:"+ functionCallNode.get_procedure().name());


            nextCFGNode = actualoutCFGNode.cfg_targets().cbegin().current().get_first();


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

            nextNode = handleSwitchCasePoint(nextCFGNode);

//            CStatementEdge statementEdge= new CStatementEdge(rawCharacters,functionCallStatement, fileLocation,
//                    prevNode, nextNode);
//            addToCFA(statementEdge);

            CStatementEdge statementEdge= new CStatementEdge(rawCharacters,functionCallStatement,
                    fileLocation, declNode, nextNode);
            addToCFA(statementEdge);
            traverseCFGNode(nextCFGNode);
        }
    }


    private void handleFunctionCall(final point cfgNode,
                                    FileLocation fileLocation)throws result{
        assert isCall_Site(cfgNode) || isIndirect_Call(cfgNode);

        ast callAST = cfgNode.get_ast(ast_family.getC_UNNORMALIZED());
        ast operands = callAST.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        CType type = typeConverter.getCType(callAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        //the 1st child is the function, others are inputs,
        CExpression funcNameExpr = expressionHandler
                .getExpressionFromUC(operands.children().get(0).as_ast(),type,fileLocation);
        String rawCharacters="";
        point actualoutCFGNode = null, nextCFGNode = null;
        point_set actuals_in = cfgNode.actuals_in();
        point_set actuals_out = cfgNode.actuals_out();

        List<CExpression> params = new ArrayList<>();
        if(operands.children().size()>1){
            StringBuilder sb = new StringBuilder(cfgNode.characters().replace(")",""));
            point_vector actualIns = sortActualInVectorByID(actuals_in.to_vector());

            if(actualIns.size()!=operands.children().size()-1)
                throw new RuntimeException("Not consistent param in "+cfgNode.toString());
            for(int i=1;i<operands.children().size();i++){
                ast actual_in = actualIns.get(i-1).get_ast(ast_family.getC_NORMALIZED());
                CExpression param;
                if(actual_in.children().get(1).as_ast().pretty_print().contains("$temp")){
                    CType paramType = typeConverter.getCType(actual_in.get(ast_ordinal.getBASE_TYPE()).as_ast(),
                            expressionHandler);
                    param = expressionHandler
                            .getExpression(actual_in.children().get(1).as_ast(), paramType, fileLocation);
                }else {
                    ast oper = operands.children().get(i).as_ast();
                    CType paramType = typeConverter.getCType(oper.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                    param = expressionHandler.getExpressionFromUC(oper, paramType, fileLocation);
                }
                sb.append(param.toASTString()).append(", ");
                params.add(param);
            }
            rawCharacters=sb.toString().replace(", ",")");
            if(actuals_out.empty()){
                for(int i=0;i<actuals_in.to_vector().size();i++){
                    String param = actuals_in.to_vector().get(i).get_ast(ast_family.getC_NORMALIZED())
                            .children().get(0).as_ast().pretty_print();
                    if(param.equals("$param_1")){
                        nextCFGNode = actuals_in.to_vector().get(i).cfg_targets()
                                .cbegin().current().get_first();
                        break;
                    }
                }
                assert nextCFGNode!=null;
            }
            else {
                actualoutCFGNode = actuals_out.cbegin().current();
                nextCFGNode = actualoutCFGNode.cfg_targets().cbegin().current().get_first();
            }
        }else {
            rawCharacters = getRawSignature(cfgNode);
            if(actuals_out.empty())
                nextCFGNode = cfgNode.cfg_targets().cbegin()
                    .current().get_first();
            else {
                actualoutCFGNode = actuals_out.cbegin().current();
                nextCFGNode = actualoutCFGNode.cfg_targets().cbegin().current().get_first();
            }
        }

        CFunctionCallExpression functionCallExpression;
        if(funcNameExpr instanceof CPointerExpression){
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, funcNameExpr, params, null);
        }else if(typeConverter.isFunctionPointerType(funcNameExpr.getExpressionType())){
            CType funcType  = typeConverter.getFuntionTypeFromFunctionPointer(funcNameExpr.getExpressionType());
            CPointerExpression pointerExpression = new CPointerExpression(fileLocation, funcType, funcNameExpr);
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, pointerExpression, params, null);
        } else if(funcNameExpr instanceof CFieldReference){
            CFieldReference fieldReference = (CFieldReference)funcNameExpr;
            CType refType = fieldReference.getExpressionType();

            if(refType instanceof CTypedefType){
                refType = ((CTypedefType) refType).getRealType();
            }
            CFunctionType functionType = (CFunctionType) ((CPointerType)refType).getType();

            CPointerExpression pointerExpression = new CPointerExpression(fileLocation,
                    functionType, funcNameExpr);
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, pointerExpression, params, null);
        }else
            functionCallExpression = new CFunctionCallExpression(fileLocation,
                    type, funcNameExpr, params,
                    (CFunctionDeclaration) ((CIdExpression)funcNameExpr).getDeclaration());

        CFunctionCall functionCallStatement;

        CFANode prevNode = cfaNodeMap.get(cfgNode.id());
        CFANode nextNode;

        nextNode = handleSwitchCasePoint(nextCFGNode);
        CStatementEdge statementEdge;

        if(actualoutCFGNode==null){
            if(nextNode==null)
                System.out.println(nextCFGNode.get_kind().name());
            functionCallStatement = new CFunctionCallStatement(fileLocation,functionCallExpression);
            statementEdge= new CStatementEdge(rawCharacters,functionCallStatement,
                    fileLocation, prevNode, nextNode);
        }else {

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
                            functionCallExpression.getDeclaration().getType().getReturnType(),
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

            functionCallStatement = new CFunctionCallAssignmentStatement(
                    fileLocation, assignedVarExp,
                    functionCallExpression);
            statementEdge= new CStatementEdge(rawCharacters,functionCallStatement,
                    fileLocation, declNode, nextNode);
        }

        addToCFA(statementEdge);
        traverseCFGNode(nextCFGNode);
    }
    /**
     *@Description build edge for label node
     *@Param [labelNode, fileloc]
     *@return void
     **/
    private void handleLabelPoint(final point labelNode, FileLocation fileloc) throws result{

        assert isLabel(labelNode);

        String labelName = getLabelName(labelNode);
        CFANode prevNode = cfaNodeMap.get(labelNode.id());

        point nextCFGNode = labelNode.cfg_targets().cbegin().current().get_first();
        CFANode nextNode = handleSwitchCasePoint(nextCFGNode);// cfaNodeMap.get(nextCFGNode.id());
        BlankEdge blankEdge =
                new BlankEdge(
                        getRawSignature(labelNode),
                        fileloc,
                        prevNode,
                        nextNode,
                        "Label: " + labelName);
        nextNode.setLoopStart();
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
        //goto the node next to the label node
        nextCFGNode = nextCFGNode.cfg_targets().cbegin().current().get_first();

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
    private void handleReturnPoint(point returnNode, FileLocation fileloc)throws result {
        //only have unnormalized ast
        CFANode prevNode = cfaNodeMap.get(returnNode.id());
        //if there is the return variable, it should be processed in its processor
        //However, sometime, there is no return semantic, e.g., int a(){};
        if(cfa.getReturnVariable().isPresent() && !isExpression(returnNode.cfg_sources().cbegin().current().get_first())){

            ast un_ast = returnNode.get_ast(ast_family.getC_UNNORMALIZED());

            if(un_ast.has_field(ast_ordinal.getUC_RETURN_VALUE())){
                ast returnValue = un_ast.get(ast_ordinal.getUC_RETURN_VALUE()).as_ast();

                CType expectedType = typeConverter.getCType(returnValue.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
                CExpression returnExp = expressionHandler.getExpressionFromUC(returnValue,expectedType, fileloc);

                Optional<CExpression> returnExpression = Optional.of(returnExp);

                CSimpleDeclaration returnVarDecl = (CSimpleDeclaration) cfa.getReturnVariable();

                CIdExpression returnVar = new CIdExpression(fileloc, returnVarDecl);

                CAssignment returnVarAssignment =
                        new CExpressionAssignmentStatement(
                                fileloc, returnVar, returnExp);
                Optional<CAssignment> returnAssignment = Optional.of(returnVarAssignment);
                CReturnStatement returnStatement = new CReturnStatement(fileloc, returnExpression, returnAssignment);

                CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(getRawSignature(returnNode),
                        returnStatement, fileloc, prevNode, cfa.getExitNode());
                addToCFA(returnStatementEdge);
            }else {
                BlankEdge edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "default return");
                addToCFA(edge);
            }
        }else {
            BlankEdge edge = new BlankEdge("",fileloc, prevNode, cfa.getExitNode(), "default return");
            addToCFA(edge);
        }
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
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        CExpression conditionExpr=expressionHandler.getExpressionFromUC(uc_condition, type, fileLocation);

        createConditionEdges(whileCFANode, trueCFANode, trueCFGNode, falseCFANode, falseCFGNode, conditionExpr, fileLocation);
    }

    private void handleDoWhilePoint(point whileNode, FileLocation fileLocation)throws result{
        assert isDoControlPointNode(whileNode);
        CFANode prevNode = cfaNodeMap.get(whileNode.id());
        prevNode.setLoopStart();

        cfg_edge_set cfgEdgeSet = whileNode.cfg_targets();

        point trueCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        CFANode trueCFANode = cfaNodeMap.get(trueCFGNode.id()).getLeavingEdge(0).getSuccessor();

        point falseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();
        CFANode falseCFANode = cfaNodeMap.get(falseCFGNode.id());

        ast uc_condition = whileNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
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
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
        CExpression conditionExpr=expressionHandler.getExpressionFromUC(uc_condition, type, fileLocation);

        createConditionEdges(prevNode,trueNode,trueCFGNode,falseNode,falseCFGNode, conditionExpr,fileLocation);

    }

    private void handleSwitchPoint(point switchNode, FileLocation fileLocation)throws result{
        assert  isSwitchControlPointNode(switchNode);

        CFANode prevNode = cfaNodeMap.get(switchNode.id());

        cfg_edge_vector cfgEdgeVector = sortVectorByLineNo(switchNode.cfg_targets().to_vector());

        ast variableAST = switchNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType variableType = typeConverter.getCType(variableAST
                .get(ast_ordinal.getBASE_TYPE()).as_ast(),expressionHandler);

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
                    .get(ast_ordinal.getUC_CONSTANT()).as_ast();

        //in c, the case type can only be Integer or Char
        CType valueType = typeConverter.getCType(valueAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);

        CExpression caseExpr = expressionHandler.getExpressionFromUC(valueAST, valueType, fileLocation);

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
        //assert isIfControlPointNode(ifNode) ;

        CFANode prevNode = cfaNodeMap.get(ifNode.id());

        cfg_edge_set cfgEdgeSet = ifNode.cfg_targets();

        point thenCFGNode = cfgEdgeSet.to_vector().get(0).get_first();
        point elseCFGNode = cfgEdgeSet.to_vector().get(1).get_first();

        CFANode thenNode = cfaNodeMap.get(thenCFGNode.id());

        // elseNode is the start of the else branch,
        // or the node after the loop if there is no else branch
        CFANode elseNode = cfaNodeMap.get(elseCFGNode.id());


        ast uc_condition = ifNode.get_ast(ast_family.getC_UNNORMALIZED());
        CType type = typeConverter.getCType(uc_condition.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionHandler);
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
        cfaNodes.add(nextNode);
        return nextNode;
    }

    private boolean hasReturnVariable(){
        assert cfa != null;
        return cfa.getReturnVariable().isPresent();
    }
}
