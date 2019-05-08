package org.nulist.plugin.model.channel;

import com.google.common.base.Optional;
import com.grammatech.cs.result;
import io.shiftleft.fuzzyc2cpg.ast.AstNode;
import io.shiftleft.fuzzyc2cpg.ast.CodeLocation;
import io.shiftleft.fuzzyc2cpg.parser.TokenSubStream;
import io.shiftleft.fuzzyc2cpg.parser.functions.AntlrCFunctionParserDriver;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.nulist.plugin.parser.CFABuilder;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.nulist.plugin.model.ChannelBuildOperation.cn_channel_msg_cache;
import static org.nulist.plugin.model.ChannelBuildOperation.ue_channel_msg_cache;
import static org.nulist.plugin.model.channel.ChannelConstructer.*;
import static org.nulist.plugin.parser.CFGParser.*;

/**
 * @ClassName ChannelBuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/5/19 5:06 PM
 * @Version 1.0
 **/
public class ChannelBuilder {

    public Map<String, CFABuilder> builderMap;
    public CFABuilder channelBuilder;
    private String filename="";

    public ChannelBuilder (Map<String, CFABuilder> builderMap, String buildName){
        this.builderMap = builderMap;
        channelBuilder=new CFABuilder(null, MachineModel.LINUX64,buildName);
    }

    public void parseBuildFile(AntlrCFunctionParserDriver driver){

        buildChannelMessageType();

        AstNode top = driver.builderStack.peek().getItem();
        filename = driver.filename;
        Map<Integer, List<AstNode>> globalNodes = globalStatementMap(top);

        globalNodes.forEach(((integer, astNodes) -> {
            AstNode astNode = astNodes.get(astNodes.size()-1);
            switch (astNode.getTypeAsString()){
                case "IdentifierDeclStatement"://global variable
                    CDeclaration declaration = parseVariableDeclaration(null,true, astNode);
                    channelBuilder.expressionHandler.globalDeclarations.put(declaration.getName().hashCode(),declaration);
                    break;
                case "CompoundStatement"://function
                    functionDeclaration(astNodes);
                    break;
            }
        }));
    }

    public void buildChannelMessageType(){
        CFABuilder ueBuilder = builderMap.get(UE);
        CType nas = buildNASChannelMessageType(ueBuilder);
        CType rrc = buildRRCChannelMessageType(ueBuilder);
        buildChannelMessageType(nas,rrc,"ue");

        CFABuilder enbBuilder = builderMap.get(ENB);
        CFABuilder mmeBuilder = builderMap.get(MME);
        CType mmenas = buildNASChannelMessageType(mmeBuilder);
        CType enbrrc = buildRRCChannelMessageType(enbBuilder);
        buildChannelMessageType(mmenas,enbrrc,"cn");
    }

    public void buildChannelMessageType(CType nas,CType rrc, String typeside){
        String typename = typeside+"_channel_message_s";
        String defname = typeside+"_channel_message_t";
        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, typename, typename);
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>(2);


        CCompositeType.CCompositeTypeMemberDeclaration nasMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(nas, "nas_message");
        members.add(nasMessage);

        CCompositeType.CCompositeTypeMemberDeclaration rrcMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(rrc, "rrc_message");
        members.add(rrcMessage);

        cStructType.setMembers(members);

        CElaboratedType cEStructType = new CElaboratedType(false,
                false,
                CComplexType.ComplexTypeKind.STRUCT,
                typename,
                typename,
                cStructType);
        CTypedefType cTypedefType = new CTypedefType(false,false, defname, cEStructType);
        channelBuilder.typeConverter.typeCache.put(defname.hashCode(),cTypedefType);
    }

    public CType buildRRCChannelMessageType(CFABuilder builder){

        String union = "rrc_message_t";
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>();

        CType LTE_UL_CCCH_Message_t= builder.typeConverter.typeCache.get("LTE_UL_CCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration1 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_UL_CCCH_Message_t, "ul_ccch_msg");
        members.add(memberDeclaration1);

        CType LTE_UL_DCCH_Message_t= builder.typeConverter.typeCache.get("LTE_UL_DCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration2 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_UL_DCCH_Message_t, "ul_dcch_msg");
        members.add(memberDeclaration2);

        CType LTE_DL_CCCH_Message_t= builder.typeConverter.typeCache.get("LTE_DL_CCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration3 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_DL_CCCH_Message_t, "dl_ccch_msg");
        members.add(memberDeclaration3);

        CType LTE_DL_DCCH_Message_t= builder.typeConverter.typeCache.get("LTE_DL_DCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration4 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_DL_DCCH_Message_t, "dl_dcch_msg");
        members.add(memberDeclaration4);

        CType LTE_BCCH_BCH_Message_t= builder.typeConverter.typeCache.get("LTE_BCCH_BCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration5 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_BCCH_BCH_Message_t, "bcch_bch_msg");
        members.add(memberDeclaration5);

        CType LTE_BCCH_DL_SCH_Message_t= builder.typeConverter.typeCache.get("LTE_BCCH_DL_SCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration6 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_BCCH_DL_SCH_Message_t, "bcch_dl_sch_msg");
        members.add(memberDeclaration6);

        CType LTE_PCCH_Message_t= builder.typeConverter.typeCache.get("LTE_PCCH_Message_t".hashCode());
        CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration7 =
                new CCompositeType.CCompositeTypeMemberDeclaration(LTE_PCCH_Message_t, "pcch_msg");
        members.add(memberDeclaration7);

        CCompositeType cCompositeType = new CCompositeType(false, false,
                CComplexType.ComplexTypeKind.UNION,
                members, union, union);

        CElaboratedType cElaboratedType= new CElaboratedType(false, false,
                CComplexType.ComplexTypeKind.UNION,
                union, union, cCompositeType);

        String structname = "rrc_channel_message_s";
        String defName = "rrc_channel_message_t";

        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, structname, structname);
        List<CCompositeType.CCompositeTypeMemberDeclaration> structMembers = new ArrayList<>(2);

        CType uint8 = CNumericTypes.UNSIGNED_INT;

        CCompositeType.CCompositeTypeMemberDeclaration msgID =
                new CCompositeType.CCompositeTypeMemberDeclaration(uint8, "msgID");
        structMembers.add(msgID);

        CCompositeType.CCompositeTypeMemberDeclaration message =
                new CCompositeType.CCompositeTypeMemberDeclaration(cElaboratedType, "message");
        structMembers.add(message);

        cStructType.setMembers(structMembers);

        CElaboratedType cEStructType = new CElaboratedType(false,
                false,
                CComplexType.ComplexTypeKind.STRUCT,
                structname,
                structname,
                cStructType);
        CTypedefType cTypedefType = new CTypedefType(false,false,defName, cEStructType);
        channelBuilder.typeConverter.typeCache.put(defName.hashCode(),cTypedefType);
        return cTypedefType;
    }

    public CType buildNASChannelMessageType(CFABuilder builder){
        String structname = "nas_channel_message_s";
        String defName = "nas_channel_message_t";
        CCompositeType cStructType =
                new CCompositeType(false, false, CComplexType.ComplexTypeKind.STRUCT, structname, structname);
        List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>(3);

        CType nas_message_t= builder.typeConverter.typeCache.get("nas_message_t".hashCode());
        CType as_message_t= builder.typeConverter.typeCache.get("as_message_t".hashCode());
        CType uint8 = CNumericTypes.UNSIGNED_INT;

        CCompositeType.CCompositeTypeMemberDeclaration msgID =
                new CCompositeType.CCompositeTypeMemberDeclaration(uint8, "msgID");
        members.add(msgID);

        CCompositeType.CCompositeTypeMemberDeclaration nasMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(nas_message_t, "nas_message");
        members.add(nasMessage);

        CCompositeType.CCompositeTypeMemberDeclaration asMessage =
                new CCompositeType.CCompositeTypeMemberDeclaration(as_message_t, "as_message");
        members.add(asMessage);

        cStructType.setMembers(members);

        CElaboratedType cEStructType = new CElaboratedType(false,
                false,
                CComplexType.ComplexTypeKind.STRUCT,
                structname,
                structname,
                cStructType);
        CTypedefType cTypedefType = new CTypedefType(false,false,defName, cEStructType);
        channelBuilder.typeConverter.typeCache.put(defName.hashCode(),cTypedefType);
        return cTypedefType;
    }

    public void functionDeclaration(List<AstNode> astNodes){

        String functionName = astNodes.get(1).getEscapedCodeStr();
        CFGFunctionBuilder functionBuilder = new CFGFunctionBuilder(null,
                channelBuilder.typeConverter, null,
                functionName,
                filename,
                channelBuilder);

        CType returnType = getType(astNodes.get(0).getEscapedCodeStr());
        List<CType> paramTypes = new ArrayList<>();
        List<CParameterDeclaration> parameterDeclarations = new ArrayList<>();
        boolean isTakeArgs = false;

        if(astNodes.size()>5){
            String typename="", param="";
            for(int i=3;i<astNodes.size()-2;i++){
                FileLocation fileLocation = getFileLocation(astNodes.get(i).getLocation());
                if(typename.equals("...")){
                    isTakeArgs = true;
                    continue;
                }
                typename = astNodes.get(i).getEscapedCodeStr();
                i++;
                param = astNodes.get(i).getEscapedCodeStr();
                i++;
                if(!astNodes.get(i).getEscapedCodeStr().equals(",") && !astNodes.get(i).getEscapedCodeStr().equals(")")){
                    typename +=" "+param;
                    param = astNodes.get(i).getEscapedCodeStr();
                    i++;
                }
                CType type = getType(typename);
                paramTypes.add(type);

                CParameterDeclaration parameterDeclaration = new CParameterDeclaration(fileLocation,type,param);
                parameterDeclarations.add(parameterDeclaration);
            }
        }
        CFunctionTypeWithNames functionTypeWithNames = new CFunctionTypeWithNames(returnType,
                parameterDeclarations,
                isTakeArgs);
        FileLocation fileLocation = getFileLocation(astNodes.get(astNodes.size()-1).getLocation());
        CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(fileLocation,
                functionTypeWithNames,
                astNodes.get(1).getEscapedCodeStr(),
                parameterDeclarations);

        functionBuilder.setFunctionDeclaration(functionDeclaration);

        FunctionExitNode functionExit = new FunctionExitNode(functionName);

        Optional<CVariableDeclaration> returnVar = Optional.absent();
        AstNode compoundNode = astNodes.get(astNodes.size()-1);
        if(!returnType.equals(CVoidType.VOID)){
            CVariableDeclaration variableDeclaration = new CVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    CStorageClass.AUTO,
                    returnType,
                    "returnVar",
                    "returnVar",
                    "returnVar",
                    null);
            functionBuilder.expressionHandler.variableDeclarations.put(variableDeclaration.getName().hashCode(),
                    variableDeclaration);
            returnVar = Optional.of(variableDeclaration);
        }

        CFunctionEntryNode entry = new CFunctionEntryNode(
                fileLocation, functionDeclaration, functionExit, returnVar);
        functionExit.setEntryNode(entry);

        functionBuilder.cfa = entry;

        channelBuilder.addNode(functionName, functionExit);
        channelBuilder.addNode(functionName, entry);
        channelBuilder.functionDeclarations.put(functionName,functionDeclaration);
        channelBuilder.expressionHandler.globalDeclarations.put(functionName.hashCode(), functionDeclaration);
        channelBuilder.functions.put(functionName,entry);

        functionVisitor(functionBuilder, compoundNode);
    }

    public void functionVisitor(CFGFunctionBuilder functionBuilder, AstNode node){

        CFANode cfaNode = functionBuilder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                functionBuilder.cfa, cfaNode, "Function start dummy edge");
        functionBuilder.addToCFA(dummyEdge);
        CFANode lastNode;
        if(functionBuilder.cfa.getReturnVariable().isPresent()){
            CVariableDeclaration variableDeclaration= (CVariableDeclaration) functionBuilder.cfa.getReturnVariable().get();
            CFANode decNode = functionBuilder.newCFANode();
            CDeclarationEdge declarationEdge = new CDeclarationEdge(
                    functionBuilder.functionDeclaration.getType().getReturnType().toString()+" returnVar;",
                    FileLocation.DUMMY,
                    cfaNode,
                    decNode,
                    variableDeclaration);
            functionBuilder.addToCFA(declarationEdge);
            lastNode = treeVisitor(functionBuilder,node, decNode,functionBuilder.cfa.getExitNode());
        }else
            lastNode = treeVisitor(functionBuilder,node, cfaNode,functionBuilder.cfa.getExitNode());

        if(!functionBuilder.cfa.getReturnVariable().isPresent() &&functionBuilder.cfa.getExitNode().getNumEnteringEdges()==0){

            functionBuilder.addToCFA(new BlankEdge("return;",
                    FileLocation.DUMMY,
                    lastNode,
                    functionBuilder.cfa.getExitNode(),
            ""));
        }

        functionBuilder.finish();
    }
    public CFANode treeVisitor(CFGFunctionBuilder functionBuilder, AstNode node, CFANode startNode1, CFANode endNode1){
        Iterator<AstNode> iterator = node.getChildIterator();
        CFANode startNode , endNode= startNode1;

        while (iterator.hasNext()){
            AstNode astNode = iterator.next();
            startNode = endNode;
            endNode = iterator.hasNext()?functionBuilder.newCFANode():endNode1;
            switch (astNode.getTypeAsString()){
                case "IdentifierDeclStatement":
                    variableDeclaration(functionBuilder, astNode,startNode,endNode);
                    break;
                case "ExpressionStatement":
                    expressionStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "IfStatement":
                    ifStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "ElseStatement":
                    elseStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "SwitchStatement":
                    switchStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "CaseStatement":
                    break;
                case "ForStatement":
                    forStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "WhileStatement":
                    whileStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                case "ReturnStatement":
                    returnStatement(functionBuilder,astNode,startNode,endNode);
                    break;
                    default:
                        throw new RuntimeException("Unsupport statement:"+ astNode.getTypeAsString());
            }
        }
        return endNode;
    }

    public void returnStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        AstNode node = astNode.getChild(0);
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CExpression expression = getExpression(functionBuilder,node);

        CIdExpression returnVarIdExpr = new CIdExpression(fileLocation,(CSimpleDeclaration) functionBuilder.cfa.getReturnVariable().get());
        CExpressionAssignmentStatement returnAssign = new CExpressionAssignmentStatement(
                fileLocation,
                returnVarIdExpr,
                expression);
        CReturnStatement returnStatement = new CReturnStatement(
                fileLocation,
                Optional.of(returnVarIdExpr),
                Optional.of(returnAssign));

        CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge(
                astNode.getEscapedCodeStr(),
                returnStatement,
                fileLocation,
                startNode,
                functionBuilder.cfa.getExitNode());
        functionBuilder.addToCFA(returnStatementEdge);
    }

    public void elseStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        AstNode node = astNode.getChild(0);

        treeVisitor(functionBuilder,node, startNode, endNode);
    }

    public void ifStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        AstNode condition = astNode.getChild(0);
        AstNode statement = astNode.getChild(1);
        CBinaryExpression conditionExpr = conditionExpression(functionBuilder,condition);
        CFANode ifnode = functionBuilder.newCFANode();

        if(astNode.getChildCount()>2){
            AstNode elseNode = astNode.getChild(2);
            CFANode elseCFANode = functionBuilder.newCFANode();
            createTrueFalseEdge(functionBuilder,startNode,ifnode,elseCFANode, condition.getEscapedCodeStr(),conditionExpr);
            treeVisitor(functionBuilder,elseNode,elseCFANode, endNode);
        }else {
            createTrueFalseEdge(functionBuilder,startNode,ifnode,endNode, condition.getEscapedCodeStr(),conditionExpr);
            treeVisitor(functionBuilder, statement, ifnode,endNode);
        }
    }

    public CExpression getExpression(CFGFunctionBuilder functionBuilder, AstNode expressionNode){
        FileLocation fileLocation = getFileLocation(expressionNode.getLocation());
        switch (expressionNode.getTypeAsString()){
            case "RelationalExpression":
                CBinaryExpression.BinaryOperator operator = getBinaryOperator(expressionNode.getOperatorCode());
                CExpression operand1 = getExpression(functionBuilder,expressionNode.getChild(0));
                CExpression operand2 = getExpression(functionBuilder,expressionNode.getChild(1));
                return  functionBuilder.expressionHandler.buildBinaryExpression(
                        operand1,
                        operand2,
                        operator,
                        CNumericTypes.BOOL);
            case "UnaryOperationExpression":
                CUnaryExpression.UnaryOperator unaryOperator = getUnaryOperator(expressionNode.getChild(0).getEscapedCodeStr());
                operand1 = getExpression(functionBuilder,expressionNode.getChild(1));
                CType type = operand1.getExpressionType();
                if(unaryOperator.equals(CUnaryExpression.UnaryOperator.AMPER))
                    type = new CPointerType(false,false,type);
                else if(unaryOperator.equals(CUnaryExpression.UnaryOperator.SIZEOF))
                    type = CNumericTypes.INT;
                return new CUnaryExpression(fileLocation,
                        type,
                        operand1,
                        unaryOperator);
            case "CallExpression":
                return (CExpression) handleCallExpression(functionBuilder, expressionNode);
            case "SizeofExpression":
                String SizeofOperandStr = expressionNode.getChild(1).getEscapedCodeStr();
                CType sizeofType = getType(SizeofOperandStr);
                if(sizeofType!=null){
                    return new CIntegerLiteralExpression(fileLocation,
                            CNumericTypes.INT,
                            MachineModel.LINUX64.getSizeof(sizeofType));
                }else {
                    throw new RuntimeException("Unsupport sizeof expression: "+expressionNode.getEscapedCodeStr());
                }
            case "Identifier":
            case "Constant":
                return getExpressionFromString(functionBuilder,expressionNode.getEscapedCodeStr());
                default:
                    throw new RuntimeException("Unsupport expression node: "+ expressionNode.getTypeAsString());

        }
    }

    private CFunctionCallExpression handleCallExpression(CFGFunctionBuilder functionBuilder,AstNode expressionNode){
        AstNode callee = expressionNode.getChild(0);
        List<CExpression> paramLists = new ArrayList<>();
        if(expressionNode.getChildCount()>1){
            AstNode agrumentList = expressionNode.getChild(1);
            for(int i=0;i<agrumentList.getChildCount();i++){
                CExpression expression = getExpression(functionBuilder,agrumentList.getChild(i));
                paramLists.add(expression);
            }
        }
        FileLocation fileLocation = getFileLocation(expressionNode.getLocation());
        CFunctionDeclaration functionDeclaration = getFunctionDelcaration(callee.getEscapedCodeStr());
        CExpression functionName = new CIdExpression(fileLocation,functionDeclaration);
        return new CFunctionCallExpression(fileLocation,
                functionDeclaration.getType().getReturnType(),
                functionName,
                paramLists,
                functionDeclaration);
    }

    private CExpression getExpressionFromString(CFGFunctionBuilder functionBuilder, String expressionString){
        String[] expressionElements = splitStringExpression(expressionString);
        return expressionParser(functionBuilder,expressionElements);
    }

    private CUnaryExpression.UnaryOperator getUnaryOperator(String operatorCode){
        switch (operatorCode){
            case "&":return CUnaryExpression.UnaryOperator.AMPER;
            case "~":return CUnaryExpression.UnaryOperator.TILDE;
            case "-":return CUnaryExpression.UnaryOperator.MINUS;
            case "sizeof":return CUnaryExpression.UnaryOperator.SIZEOF;
            case "__alignof__":return CUnaryExpression.UnaryOperator.ALIGNOF;
            default:throw new RuntimeException("Not a unary operator: " + operatorCode);
        }
    }

    private CFunctionDeclaration getFunctionDelcaration(String functionName){
        CFunctionDeclaration functionDeclaration;
        if(functionName.startsWith("UE_"))
            functionDeclaration= builderMap.get(UE).functionDeclarations.get(functionName.replace("UE_",""));
        else if(functionName.startsWith("ENB_"))
            functionDeclaration= builderMap.get(ENB).functionDeclarations.get(functionName.replace("ENB_",""));
        else if(functionName.startsWith("MME_"))
            functionDeclaration= builderMap.get(MME).functionDeclarations.get(functionName.replace("MME_",""));
        else
            functionDeclaration= channelBuilder.functionDeclarations.get(functionName);
        if(functionDeclaration==null)
            throw new RuntimeException("No such function:"+functionName);
        else
            return functionDeclaration;
    }

    private CBinaryExpression.BinaryOperator getBinaryOperator(String operatorCode){
        switch (operatorCode){
            case ">":return CBinaryExpression.BinaryOperator.GREATER_THAN;
            case ">=":return CBinaryExpression.BinaryOperator.GREATER_EQUAL;
            case "<":return CBinaryExpression.BinaryOperator.LESS_THAN;
            case "<=":return CBinaryExpression.BinaryOperator.LESS_EQUAL;
            case "==":return CBinaryExpression.BinaryOperator.EQUALS;
            case "!=":return CBinaryExpression.BinaryOperator.NOT_EQUALS;
            case "|":return CBinaryExpression.BinaryOperator.BINARY_OR;
            case "^":return CBinaryExpression.BinaryOperator.BINARY_XOR;
            case "&":return CBinaryExpression.BinaryOperator.BINARY_AND;
            case ">>":return CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
            case "<<":return CBinaryExpression.BinaryOperator.SHIFT_LEFT;
            case "-":return CBinaryExpression.BinaryOperator.MINUS;
            case "+":return CBinaryExpression.BinaryOperator.PLUS;
            case "*":return CBinaryExpression.BinaryOperator.MULTIPLY;
            case "/":return CBinaryExpression.BinaryOperator.DIVIDE;
            case "%":return CBinaryExpression.BinaryOperator.MODULO;
            default:throw new RuntimeException("Not a binary operator: " + operatorCode);
        }
    }

    public void expressionStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        AstNode expressionNode = astNode.getChild(0);
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CStatement statement;
        if(expressionNode.getTypeAsString().equals("AssignmentExpression")){
            CExpression operand1 = getExpression(functionBuilder,expressionNode.getChild(0));
            CExpression operand2 = getExpression(functionBuilder,expressionNode.getChild(1));
            if(operand2 instanceof CFunctionCallExpression)
                statement = new CFunctionCallAssignmentStatement(
                    fileLocation,
                    (CLeftHandSide) operand1,
                        (CFunctionCallExpression) operand2);
            else
                statement = new CExpressionAssignmentStatement(
                        fileLocation,
                        (CLeftHandSide) operand1,
                        operand2);
        }else if(expressionNode.getTypeAsString().equals("PostIncDecOperationExpression")){
            AstNode identifier = expressionNode.getChild(0);
            AstNode operation = expressionNode.getChild(1);
            CVariableDeclaration variableDeclaration = (CVariableDeclaration) functionBuilder.variableDeclarations.get(identifier.getEscapedCodeStr().hashCode());
            CIdExpression idExpression = new CIdExpression(fileLocation,variableDeclaration);
            CBinaryExpression.BinaryOperator operator = operation.getTypeAsString().equals("++")?
                    CBinaryExpression.BinaryOperator.PLUS:
                    CBinaryExpression.BinaryOperator.MINUS;

            CBinaryExpression binaryExpression = functionBuilder.expressionHandler.buildBinaryExpression(
                    idExpression,
                    CIntegerLiteralExpression.ONE,
                    operator,
                    idExpression.getExpressionType());
            statement = new CExpressionAssignmentStatement(fileLocation,(CLeftHandSide)idExpression,binaryExpression);
        }else if(expressionNode.getTypeAsString().equals("UnaryExpression")){
            AstNode operation = expressionNode.getChild(0);
            AstNode identifier = expressionNode.getChild(1);
            CExpression idExpression = getExpression(functionBuilder, identifier);

            if(operation.getTypeAsString().equals("IncDec")){
                CBinaryExpression.BinaryOperator operator = operation.getTypeAsString().equals("++")?
                        CBinaryExpression.BinaryOperator.PLUS:
                        CBinaryExpression.BinaryOperator.MINUS;

                CBinaryExpression binaryExpression = functionBuilder.expressionHandler.buildBinaryExpression(
                        idExpression,
                        CIntegerLiteralExpression.ONE,
                        operator,
                        idExpression.getExpressionType());
                statement = new CExpressionAssignmentStatement(fileLocation,(CLeftHandSide)idExpression,binaryExpression);
            }else
                throw  new RuntimeException("other unaryexpression: "+ operation.getTypeAsString());
        }else {
            throw  new RuntimeException("other expression: "+ expressionNode.getTypeAsString());
        }

        CStatementEdge edge = new CStatementEdge(astNode.getEscapedCodeStr(),
                statement,
                fileLocation,
                startNode,
                endNode);
        functionBuilder.addToCFA(edge);
    }

    private CBinaryExpression conditionExpression(CFGFunctionBuilder functionBuilder, AstNode conditionNode){
        AstNode astNode = conditionNode.getChild(0);
        return (CBinaryExpression) getExpression(functionBuilder,astNode);
    }

    public void switchStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        String switchString = astNode.getEscapedCodeStr();
        AstNode condition = astNode.getChild(0);
        AstNode statement = astNode.getChild(1);

        String variableString = condition.getEscapedCodeStr();
        CFANode switchNode = functionBuilder.newCFANode();
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        functionBuilder.addToCFA(new BlankEdge(switchString, fileLocation, startNode, switchNode, switchString));

        CExpression variableExpr = expressionParser(functionBuilder,variableString);

        CFANode trueNode, falseNode, lastnode=switchNode;
        for(int i=0;i<statement.getChildCount();i++){
            AstNode node = statement.getChild(i);
            if(node.getTypeAsString().equals("Label")){
                FileLocation fileLocation1 = getFileLocation(node.getLocation());
                if(node.getEscapedCodeStr().startsWith("default")){
                    trueNode = functionBuilder.newCFANode();
                    functionBuilder.addToCFA(new BlankEdge("default", fileLocation1, lastnode,trueNode,"default"));
                    lastnode = functionBuilder.newCFANode();
                }else {
                    String caseString = node.getEscapedCodeStr().replace(":","").replace("case","").trim();
                    int value = Integer.valueOf(caseString);
                    CExpression caseValueExpr;
                    if(variableExpr.getExpressionType().getCanonicalType().equals(CNumericTypes.CHAR)){
                        caseValueExpr = new CCharLiteralExpression(fileLocation,CNumericTypes.CHAR, (char)value);
                    }else{
                        caseValueExpr = new CIntegerLiteralExpression(fileLocation,CNumericTypes.INT, BigInteger.valueOf(value));
                    }
                    trueNode = functionBuilder.newCFANode();
                    falseNode =functionBuilder.newCFANode();
                    CBinaryExpression binaryExpression = functionBuilder.expressionHandler.buildBinaryExpression(variableExpr,caseValueExpr, CBinaryExpression.BinaryOperator.EQUALS,CNumericTypes.BOOL);
                    String rawString = variableString+"=="+caseString;
                    createTrueFalseEdge(functionBuilder,lastnode,trueNode,falseNode,rawString,binaryExpression);
                    lastnode=falseNode;
                }

                while (true){
                    i++;
                    AstNode nextNode = statement.getChild(i);
                    if(nextNode.getTypeAsString().equals("BreakStatement")|| (nextNode.getTypeAsString().equals("Statement") && nextNode.getEscapedCodeStr().equals("break"))){
                        fileLocation1 = getFileLocation(nextNode.getLocation());
                        functionBuilder.addToCFA(new BlankEdge("break", fileLocation1, trueNode,endNode,"default"));
                        break;
                    }else if(nextNode.getTypeAsString().equals("Label")){
                        i--;
                        fileLocation1 = getFileLocation(nextNode.getLocation());
                        functionBuilder.addToCFA(new BlankEdge("fall through", fileLocation1, trueNode,lastnode,"fall through"));
                        break;
                    }else {
                        CFANode breakNode = functionBuilder.newCFANode();
                        treeVisitor(functionBuilder, nextNode, trueNode, breakNode);
                        trueNode = breakNode;
                    }
                }
            }
        }


    }

    public void createTrueFalseEdge(CFGFunctionBuilder builder, CFANode rootNode, CFANode trueNode, CFANode falseNode, String conditionExpr, CExpression expression){
        CAssumeEdge trueEdge21 = new CAssumeEdge(
                conditionExpr,
                FileLocation.DUMMY,
                rootNode,
                trueNode,
                expression,
                true,
                false,
                false);
        builder.addToCFA(trueEdge21);



        CAssumeEdge falseEdge21 = new CAssumeEdge(
                "!("+conditionExpr+")",
                FileLocation.DUMMY,
                rootNode,
                falseNode,
                expression,
                false,
                false,
                false);
        builder.addToCFA(falseEdge21);
    }

    public void forStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){

        AstNode condition1 = astNode.getChild(0);//ForInit
        AstNode condition2 = astNode.getChild(1);//Condition
        AstNode condition3 = astNode.getChild(2);//OperationExpression
        AstNode statement = astNode.getChild(3);

        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CFANode forNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge("for",fileLocation,startNode,forNode,"for"));
        AstNode initNode = condition1.getChild(0);
        CFANode initCFANode = functionBuilder.newCFANode();
        treeVisitor(functionBuilder,initNode,forNode, initCFANode);
        CFANode conditionNode = functionBuilder.newCFANode();
        String conditionString = condition2.getEscapedCodeStr();
        initCFANode.setLoopStart();
        CBinaryExpression conditionExpr = conditionExpression(functionBuilder,condition2);
        createTrueFalseEdge(functionBuilder,initCFANode,conditionNode,endNode, conditionString,conditionExpr);
        CFANode stateNode = functionBuilder.newCFANode();
        treeVisitor(functionBuilder, statement, conditionNode,stateNode);
        treeVisitor(functionBuilder,condition3, stateNode, initCFANode);
    }

    public void whileStatement(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        AstNode condition = astNode.getChild(0);
        AstNode statement = astNode.getChild(1);

        CBinaryExpression conditionExpr = conditionExpression(functionBuilder,condition);
        startNode.setLoopStart();
        CFANode internalNode = functionBuilder.newCFANode();
        createTrueFalseEdge(functionBuilder,startNode,internalNode, endNode, condition.getEscapedCodeStr(),conditionExpr);
        treeVisitor(functionBuilder,statement,internalNode,startNode);
    }

    public void variableDeclaration(CFGFunctionBuilder functionBuilder, AstNode astNode, CFANode startNode, CFANode endNode){
        CDeclaration declaration = parseVariableDeclaration(functionBuilder,false, astNode);
        functionBuilder.expressionHandler.variableDeclarations.put(declaration.getName().hashCode(),declaration);
        CDeclarationEdge declarationEdge = new CDeclarationEdge(astNode.getEscapedCodeStr(),
                declaration.getFileLocation(),
                startNode,
                endNode,
                declaration);
        functionBuilder.addToCFA(declarationEdge);
    }

    public CType getType(String typename){
        if(channelBuilder.typeConverter.typeCache.containsKey(typename.hashCode()))
            return channelBuilder.typeConverter.typeCache.get(typename.hashCode());
        else {
            for(CFABuilder cfaBuilder: builderMap.values()){
                if(cfaBuilder.typeConverter.typeCache.containsKey(typename.hashCode()))
                    return cfaBuilder.typeConverter.typeCache.get(typename.hashCode());
            }
            throw new RuntimeException("No existing type "+typename);
        }
    }

    public CDeclaration parseVariableDeclaration(CFGFunctionBuilder functionBuilder, boolean isGlobal, AstNode node){
        String expressionString = node.getEscapedCodeStr().replace("\\n","").replace(";","");
        FileLocation fileLocation = getFileLocation(node.getLocation());
        return declarationParser(functionBuilder, fileLocation, isGlobal, expressionString);
    }

    public Map<Integer, List<AstNode>> globalStatementMap(AstNode top){
        int i=0;
        Map<Integer, List<AstNode>> astNodeMap = new HashMap<>();
        Iterator<AstNode> iterator = top.getChildIterator();
        List<AstNode> astNodes = new ArrayList<>();
        while (iterator.hasNext()){
            AstNode node = iterator.next();
            astNodes.add(node);
            if(!node.getTypeAsString().equals("Statement")){
                astNodeMap.put(i, astNodes);
                astNodes = new ArrayList<>();
            }
            i++;
        }
        return astNodeMap;
    }

    private FileLocation getFileLocation(CodeLocation location){
        return new FileLocation(filename, 0,1,location.startLine,location.endLine);
    }

    public CDeclaration declarationParser(CFGFunctionBuilder functionBuilder, FileLocation fileLocation, boolean isGlobal, String declarationString){
        String typeString = (declarationString.split(" ")[0]).trim();
        CType type = getType(typeString);

        declarationString = declarationString.replace(typeString+" ","");
        if(declarationString.startsWith("*")){
            type = new CPointerType(false,false,type);
            declarationString = declarationString.replace("*","").trim();
        }
        String varName;
        CInitializer initializer = null;
        if(declarationString.contains("=")){
            varName = (declarationString.split("=")[0]).trim();
            String initializerString = (declarationString.split("=")[1]).trim();
            CExpression expression = getExpressionFromString(functionBuilder, initializerString);
            initializer = new CInitializerExpression(fileLocation,expression);
        }
        else
            varName = declarationString.trim();

        CVariableDeclaration variableDeclaration = new CVariableDeclaration(
                fileLocation,
                isGlobal,
                CStorageClass.AUTO,
                type,
                varName,
                varName,
                varName,
                initializer);

        return variableDeclaration;
    }

}
