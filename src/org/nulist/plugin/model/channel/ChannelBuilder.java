package org.nulist.plugin.model.channel;

import com.google.common.base.Optional;
import com.grammatech.cs.result;
import io.shiftleft.fuzzyc2cpg.ast.AstNode;
import io.shiftleft.fuzzyc2cpg.ast.CodeLocation;
import io.shiftleft.fuzzyc2cpg.ast.declarations.IdentifierDecl;
import io.shiftleft.fuzzyc2cpg.ast.expressions.*;
import io.shiftleft.fuzzyc2cpg.ast.langc.expressions.CallExpression;
import io.shiftleft.fuzzyc2cpg.ast.langc.expressions.SizeofExpression;
import io.shiftleft.fuzzyc2cpg.ast.langc.statements.blockstarters.ElseStatement;
import io.shiftleft.fuzzyc2cpg.ast.langc.statements.blockstarters.IfStatement;
import io.shiftleft.fuzzyc2cpg.ast.logical.statements.BreakOrContinueStatement;
import io.shiftleft.fuzzyc2cpg.ast.logical.statements.CompoundStatement;
import io.shiftleft.fuzzyc2cpg.ast.logical.statements.Label;
import io.shiftleft.fuzzyc2cpg.ast.logical.statements.Statement;
import io.shiftleft.fuzzyc2cpg.ast.statements.ExpressionStatement;
import io.shiftleft.fuzzyc2cpg.ast.statements.IdentifierDeclStatement;
import io.shiftleft.fuzzyc2cpg.ast.statements.blockstarters.*;
import io.shiftleft.fuzzyc2cpg.ast.statements.jump.BreakStatement;
import io.shiftleft.fuzzyc2cpg.ast.statements.jump.ContinueStatement;
import io.shiftleft.fuzzyc2cpg.ast.statements.jump.ReturnStatement;
import io.shiftleft.fuzzyc2cpg.parser.TokenSubStream;
import io.shiftleft.fuzzyc2cpg.parser.functions.AntlrCFunctionParserDriver;
import jdk.nashorn.internal.runtime.arrays.ArrayIndex;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.nulist.plugin.parser.CFABuilder;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.nulist.plugin.model.ChannelBuildOperation.*;
import static org.nulist.plugin.model.channel.ChannelConstructer.*;
import static org.nulist.plugin.parser.CFGParser.*;
import static org.sosy_lab.cpachecker.cfa.CFACreationUtils.removeEdgeFromNodes;

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
    private String project="";
    private Map<String, AntlrCFunctionParserDriver> driverList = new HashMap<>();
    private FileLocation fileLocation = null;
    private boolean useSingeFilelocation = false;

    public ChannelBuilder (Map<String, CFABuilder> builderMap, String buildName){
        this.builderMap = builderMap;
        channelBuilder=new CFABuilder(builderMap.get(UE).logger, MachineModel.LINUX64,buildName);
    }

    public void putDriver(String filePath, AntlrCFunctionParserDriver driver){
        channelBuilder.addParsedFile(Paths.get(filePath));
        String fileName = filePath.replace(".c","");
        fileName = fileName.split("/")[fileName.split("/").length-1];
        driverList.put(fileName,driver);
    }

    public void parseFile(){
        if(!driverList.isEmpty()){
            buildChannelMessageType();
            //channel related models
            if(driverList.containsKey("cnside_message_channel")){
                this.project=Channel;

                parseBuildFile("cnside_message_channel",driverList.get("cnside_message_channel"));
                System.out.println("Parse "+ "cnside_message_channel");
            }
            if(driverList.containsKey("ueside_message_channel")){
                this.project=Channel;
                parseBuildFile("ueside_message_channel",driverList.get("ueside_message_channel"));
                System.out.println("Parse "+ "ueside_message_channel");
            }

            if(driverList.containsKey("EMMMessageTranslation")){
                this.project=Channel;
                parseBuildFile("EMMMessageTranslation",driverList.get("EMMMessageTranslation"));
                System.out.println("Parse "+ "EMMMessageTranslation");
            }
//            if(driverList.containsKey("ESMMessageTranslation")){
//                this.project=Channel;
//                parseBuildFile("ESMMessageTranslation",driverList.get("ESMMessageTranslation"));
//                System.out.println("Parse "+ "ESMMessageTranslation");
//            }

            if(driverList.containsKey("channel_operation")){
                this.project=Channel;
                parseBuildFile("channel_operation",driverList.get("channel_operation"));
                System.out.println("Parse "+ "channel_operation");
            }

            //composition related models
            if(driverList.containsKey("enb_rrc_task_abstract")){
                this.project=ENB;
                parseBuildFile("enb_rrc_task_abstract",driverList.get("enb_rrc_task_abstract"));
                System.out.println("Parse "+ "enb_rrc_task_abstract");
            }
            if(driverList.containsKey("ue_rrc_task_abstract")){
                this.project=UE;
                parseBuildFile("ue_rrc_task_abstract",driverList.get("ue_rrc_task_abstract"));
                System.out.println("Parse "+ "ue_rrc_task_abstract");
            }
            if(driverList.containsKey("itti_task_abstract")){
                this.project=Channel;
                parseBuildFile("itti_task_abstract",driverList.get("itti_task_abstract"));
                System.out.println("Parse "+ "itti_task_abstract");
            }
            if(driverList.containsKey("s1ap_enb_message_deliver")){
                this.project=MME;
                useSingeFilelocation = true;
                parseInsert2Function("s1ap_enb_message_deliver",driverList.get("s1ap_enb_message_deliver"));
                System.out.println("Parse "+ "s1ap_enb_message_deliver");
            }
            if(driverList.containsKey("s1ap_mme_message_deliver")){
                this.project=ENB;
                useSingeFilelocation = true;
                parseInsert2Function("s1ap_mme_message_deliver",driverList.get("s1ap_mme_message_deliver"));
                System.out.println("Parse "+ "s1ap_mme_message_deliver");
            }
            useSingeFilelocation = false;
        }
    }

    public void parseInsert2Function(String filename, AntlrCFunctionParserDriver driver){
        AstNode top = driver.builderStack.peek().getItem();
        this.filename = filename;

        Map<Integer, List<AstNode>> globalNodes = globalStatementMap(top);

        globalNodes.forEach(((integer, astNodes) -> {
            AstNode astNode = astNodes.get(1);
            if(astNode.getEscapedCodeStr().equals("s1ap_eNB_handle_nas_first_req")){
                CFGFunctionBuilder functionBuilder = builderMap.get(ENB).cfgFunctionBuilderMap.get("s1ap_eNB_handle_nas_first_req");
                insert2Function((CompoundStatement)astNodes.get(astNodes.size()-1),functionBuilder);
            }else if(astNode.getEscapedCodeStr().equals("s1ap_eNB_nas_uplink")){
                CFGFunctionBuilder functionBuilder = builderMap.get(ENB).cfgFunctionBuilderMap.get("s1ap_eNB_nas_uplink");
                insert2Function((CompoundStatement)astNodes.get(astNodes.size()-1),functionBuilder);
            }else if(astNode.getEscapedCodeStr().equals("s1ap_eNB_nas_non_delivery_ind")){
                CFGFunctionBuilder functionBuilder = builderMap.get(ENB).cfgFunctionBuilderMap.get("s1ap_eNB_nas_non_delivery_ind");
                insert2Function((CompoundStatement)astNodes.get(astNodes.size()-1),functionBuilder);
            }else if(astNode.getEscapedCodeStr().equals("s1ap_generate_downlink_nas_transport")){
                CFGFunctionBuilder functionBuilder = builderMap.get(MME).cfgFunctionBuilderMap.get("s1ap_generate_downlink_nas_transport");
                insert2Function((CompoundStatement)astNodes.get(astNodes.size()-1),functionBuilder);
            }
        }));
    }

    public void insert2Function(CompoundStatement astNode, CFGFunctionBuilder functionBuilder){
        CFAEdge targetEdge = findIttiSendSctpDataReq(functionBuilder);
        this.fileLocation = targetEdge.getFileLocation();
        CFANode prevNode = targetEdge.getPredecessor();
        CFANode nextNode = targetEdge.getSuccessor();
        removeEdgeFromNodes(targetEdge);
        treeVisitor(functionBuilder,astNode,prevNode,nextNode);
        functionBuilder.finish();
    }

    private CFAEdge findIttiSendSctpDataReq(CFGFunctionBuilder functionBuilder){
        String name = functionBuilder.cfaBuilder.projectName.equals(ENB)?"s1ap_eNB_itti_send_sctp_data_req":"s1ap_mme_itti_send_sctp_request";

        CFAEdge edge = backTraceEdge(functionBuilder.cfa.getExitNode(),name);
        if(edge==null)
            throw new RuntimeException("No such function: "+name+" is called in that location "+functionBuilder.functionName);
        return edge;
    }

    private CFAEdge backTraceEdge(FunctionExitNode node, String targetFunctionName){
        if(node.getNumEnteringEdges()!=0){
            for(int i=0;i<node.getNumEnteringEdges();i++){
                CFAEdge edge = node.getEnteringEdge(i);
                if(edge instanceof CReturnStatementEdge && ((CReturnStatementEdge) edge).getExpression().get() instanceof CIntegerLiteralExpression){
                    int returnValue = ((CIntegerLiteralExpression) ((CReturnStatementEdge) edge).getExpression().get()).getValue().intValue();
                    if(returnValue==0){
                        CFANode node1 = edge.getPredecessor();
                        CFAEdge edge1 = node1.getEnteringEdge(0);
                        if(edge1 instanceof CStatementEdge && ((CStatementEdge) edge1).getStatement() instanceof CFunctionCallStatement){
                            if(((CFunctionCallStatement) ((CStatementEdge) edge1).getStatement()).
                                    getFunctionCallExpression().getDeclaration().getName().equals(targetFunctionName))
                                return edge1;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void parseBuildFile(String filename, AntlrCFunctionParserDriver driver){
        AstNode top = driver.builderStack.peek().getItem();
        this.filename = filename;

        Map<Integer, List<AstNode>> globalNodes = globalStatementMap(top);

        globalNodes.forEach(((integer, astNodes) -> {
            AstNode astNode = astNodes.get(astNodes.size()-1);
            if(astNode instanceof IdentifierDeclStatement){

                for(int i=0;i<((IdentifierDeclStatement) astNode).getIdentifierDeclList().size();i++) {
                    IdentifierDecl decl = (IdentifierDecl) ((IdentifierDeclStatement) astNode).getIdentifierDeclList().get(i);
                    CDeclaration declaration = parseVariableDeclaration(null, true, decl);
                    channelBuilder.expressionHandler.globalDeclarations.put(declaration.getName().hashCode(),declaration);

                }
            }else if(astNode instanceof CompoundStatement){
                functionDeclaration(astNodes);
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
        CFGFunctionBuilder functionBuilder = new CFGFunctionBuilder(channelBuilder.logger,
                channelBuilder.typeConverter, null,
                functionName,
                filename,
                channelBuilder);
        functionBuilder.setDirectAddEdge(true);

        String returnTypeName = astNodes.get(0).getEscapedCodeStr();
        CType returnType = getType(getProjectForType(functionName,1),returnTypeName);
        List<CType> paramTypes = new ArrayList<>();
        List<CParameterDeclaration> parameterDeclarations = new ArrayList<>();
        boolean isTakeArgs = false;


        if(astNodes.size()>5){
            String typename="", param="";
            for(int i=3;i<astNodes.size()-2;i++){
                FileLocation fileLocation = getFileLocation(astNodes.get(i).getLocation());
                boolean isConst = false;
                typename = astNodes.get(i).getEscapedCodeStr();
                if(typename.equals("...")){
                    isTakeArgs = true;
                    continue;
                }else if(typename.equals("const")){
                    isConst = true;
                    i++;
                    typename = astNodes.get(i).getEscapedCodeStr();
                }
                i++;
                param = astNodes.get(i).getEscapedCodeStr();
                i++;
                boolean isArray = false;
                if(!astNodes.get(i).getEscapedCodeStr().equals(",") && !astNodes.get(i).getEscapedCodeStr().equals(")")){
                    if(astNodes.get(i).getEscapedCodeStr().equals("[") && astNodes.get(i+1).getEscapedCodeStr().equals("]")){
                        i+=3;
                        isArray = true;
                    }else {
                        typename +=" "+param;
                        param = astNodes.get(i).getEscapedCodeStr();
                        i++;
                    }
                }
                CType type = getType(getProjectForType(functionName,2),typename);
                if(isArray)
                    type = new CArrayType(type.isConst(), type.isVolatile(), type, null);
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
        CompoundStatement compoundNode = (CompoundStatement)astNodes.get(astNodes.size()-1);
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

    public void functionVisitor(CFGFunctionBuilder functionBuilder, CompoundStatement node){

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

    public CFANode treeVisitor(CFGFunctionBuilder functionBuilder, CompoundStatement node, CFANode startNode1, CFANode endNode1){
        if(node.getChildCount()==0){
            FileLocation fileLocation = getFileLocation(node.getLocation());
            functionBuilder.addToCFA(new BlankEdge("",fileLocation,startNode1,endNode1,""));
            return endNode1;
        }
        Iterator<AstNode> iterator = node.getChildIterator();
        CFANode startNode , endNode= startNode1;

        while (iterator.hasNext()){
            AstNode astNode = iterator.next();
            startNode = endNode;
            endNode = iterator.hasNext()?functionBuilder.newCFANode():endNode1;
            handleStatement(functionBuilder,(Statement)astNode,startNode,endNode);
        }
        return endNode;
    }

    public void handleStatement(CFGFunctionBuilder functionBuilder, Statement astNode, CFANode startNode, CFANode endNode){
        if(astNode instanceof IdentifierDeclStatement){
            variableDeclaration(functionBuilder, (IdentifierDeclStatement) astNode,startNode,endNode);
        }else if(astNode instanceof ExpressionStatement){
            expressionStatement(functionBuilder,(ExpressionStatement) astNode,startNode,endNode);
        }else if(astNode instanceof IfStatement){
            ifStatement(functionBuilder,(IfStatement) astNode,startNode,endNode);
        }else if(astNode instanceof ElseStatement){
            elseStatement(functionBuilder,(ElseStatement) astNode,startNode,endNode);
        }else if(astNode instanceof SwitchStatement){
            switchStatement(functionBuilder,(SwitchStatement) astNode,startNode,endNode);
        }else if(astNode instanceof Label){

        }else if(astNode instanceof ForStatement){
            forStatement(functionBuilder,(ForStatement) astNode,startNode,endNode);
        }else if(astNode instanceof WhileStatement){
            whileStatement(functionBuilder,(WhileStatement)astNode,startNode,endNode);
        }else if(astNode instanceof ReturnStatement){
            returnStatement(functionBuilder,(ReturnStatement)astNode,startNode,endNode);
        }else if(astNode instanceof DoStatement){

        }else if(astNode instanceof BreakStatement){

        }else if(astNode instanceof ContinueStatement){

        }else if(astNode instanceof CompoundStatement){
            treeVisitor(functionBuilder,(CompoundStatement)astNode,startNode,endNode);
        }
    }

    public void returnStatement(CFGFunctionBuilder functionBuilder, ReturnStatement astNode, CFANode startNode, CFANode endNode){
        Expression node = astNode.getReturnExpression();
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CExpression expression = (CExpression)getExpression(functionBuilder,node);

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

    public void elseStatement(CFGFunctionBuilder functionBuilder, ElseStatement astNode, CFANode startNode, CFANode endNode){
        AstNode node = astNode.getChild(0);
        handleStatement(functionBuilder,(Statement)node,startNode,endNode);
    }

    public void ifStatement(CFGFunctionBuilder functionBuilder, IfStatement astNode, CFANode startNode, CFANode endNode){
        Condition condition = (Condition) astNode.getCondition();
        Statement statement =  astNode.getStatement();

        //CBinaryExpression conditionExpr = conditionExpression(functionBuilder,condition);
        CFANode ifnode = functionBuilder.newCFANode();

        if(astNode.getElseNode()!=null){
            ElseStatement elseStatement = astNode.getElseNode();
            CFANode elseCFANode = functionBuilder.newCFANode();
            handleIfCondition(functionBuilder,condition,startNode,ifnode,elseCFANode);
            handleStatement(functionBuilder,elseStatement, elseCFANode, endNode);

        }else {
            handleIfCondition(functionBuilder,condition,startNode,ifnode,endNode);
            handleStatement(functionBuilder, statement, ifnode,endNode);
        }
    }

    private void handleIfCondition(CFGFunctionBuilder functionBuilder, Expression condition, CFANode startNode, CFANode ifNode, CFANode endNode){

        if(condition instanceof AndExpression){
            CFANode andNode = functionBuilder.newCFANode();
            Expression left = ((AndExpression) condition).getLeft();
            handleIfCondition(functionBuilder,left,startNode,andNode,endNode);
            Expression right = ((AndExpression) condition).getRight();
            handleIfCondition(functionBuilder,right,andNode,ifNode,endNode);
        }else if(condition instanceof OrExpression){
            CFANode orNode = functionBuilder.newCFANode();
            Expression left = ((OrExpression) condition).getLeft();
            handleIfCondition(functionBuilder,left,startNode,ifNode,orNode);
            Expression right = ((OrExpression) condition).getRight();
            handleIfCondition(functionBuilder,right,orNode,ifNode,endNode);
        }else if(condition instanceof EqualityExpression){
            CBinaryExpression binaryExpression = (CBinaryExpression) getExpression(functionBuilder,condition);
            createTrueFalseEdge(functionBuilder,startNode,ifNode,endNode, condition.getEscapedCodeStr(),binaryExpression);
        }else if(condition instanceof Identifier){
            CExpression expression = (CExpression)getExpression(functionBuilder,condition);
            createTrueFalseEdge(functionBuilder,startNode,ifNode,endNode, condition.getEscapedCodeStr(),expression);
        }else if(condition instanceof Condition){
            handleIfCondition(functionBuilder, (Expression) condition.getChild(0), startNode, ifNode, endNode);
        }
    }


    public CRightHandSide getExpression(CFGFunctionBuilder functionBuilder, Expression expressionNode){
        FileLocation fileLocation = getFileLocation(expressionNode.getLocation());
        if(expressionNode instanceof RelationalExpression){
            CBinaryExpression.BinaryOperator operator = getBinaryOperator(expressionNode.getOperator());
            CExpression operand1 = (CExpression) getExpression(functionBuilder,(Expression)expressionNode.getChild(0));
            CExpression operand2 = (CExpression) getExpression(functionBuilder,(Expression)expressionNode.getChild(1));

            return  functionBuilder.expressionHandler.buildBinaryExpression(
                    operand1,
                    operand2,
                    operator,
                    CNumericTypes.BOOL);
        }else if(expressionNode instanceof UnaryOperationExpression){
            CExpression operand1 = (CExpression)getExpression(functionBuilder,(Expression)expressionNode.getChild(1));
            CType type = operand1.getExpressionType();
            if(expressionNode.getChild(0).getEscapedCodeStr().equals("*")){
                return new CPointerExpression(fileLocation, type, operand1);
            }

            CUnaryExpression.UnaryOperator unaryOperator = getUnaryOperator(expressionNode.getChild(0).getEscapedCodeStr());
            if(unaryOperator.equals(CUnaryExpression.UnaryOperator.AMPER))
                type = new CPointerType(false,false,type);
            else if(unaryOperator.equals(CUnaryExpression.UnaryOperator.SIZEOF))
                type = CNumericTypes.INT;
            return new CUnaryExpression(fileLocation,
                    type,
                    operand1,
                    unaryOperator);
        }else if(expressionNode instanceof CallExpression){
            return handleCallExpression(functionBuilder, (CallExpression)expressionNode);
        }else if(expressionNode instanceof SizeofExpression){
            String SizeofOperandStr = expressionNode.getChild(1).getEscapedCodeStr();
            CType sizeofType = getType(getProjectForType(functionBuilder.functionName,3),SizeofOperandStr);
            if(sizeofType!=null){
                return new CIntegerLiteralExpression(fileLocation,
                        CNumericTypes.INT,
                        MachineModel.LINUX64.getSizeof(sizeofType));
            }else {
                throw new RuntimeException("Unsupport sizeof expression: "+expressionNode.getEscapedCodeStr());
            }
        }else if(expressionNode instanceof EqualityExpression){
            CBinaryExpression.BinaryOperator operator = getBinaryOperator(expressionNode.getOperator());
            CExpression operand1 = (CExpression)getExpression(functionBuilder,(Expression)expressionNode.getChild(0));
            CExpression operand2 = (CExpression)getExpression(functionBuilder,(Expression)expressionNode.getChild(1));
            return  functionBuilder.expressionHandler.buildBinaryExpression(
                    operand1,
                    operand2,
                    operator,
                    CNumericTypes.BOOL);
        }else if(expressionNode instanceof CastExpression){
            Expression castExpression = ((CastExpression) expressionNode).getCastExpression();
            Expression castTarget = ((CastExpression) expressionNode).getCastTarget();
            String castType = castTarget.getEscapedCodeStr();

            CExpression cExpression = (CExpression)getExpression(functionBuilder,castExpression);
            CType type;
            if(castType.endsWith("*")){
                type =getType(getProjectForType(functionBuilder.functionName,4),castType.replace("*","").trim());
                type = new CPointerType(false,false,type);
            }else
                type = getType(getProjectForType(functionBuilder.functionName,4),castType);

            return new CCastExpression(fileLocation, type, cExpression);
        }else if(expressionNode instanceof Identifier ||
                expressionNode instanceof Constant ||
                expressionNode instanceof PtrMemberAccess||
                expressionNode instanceof MemberAccess ||
                expressionNode instanceof ArrayIndexing){
            return getExpressionFromString(functionBuilder,expressionNode.getEscapedCodeStr());
        }else if(expressionNode instanceof Argument){
            return getExpression(functionBuilder,(Expression)expressionNode.getChild(0));
        }else if(expressionNode instanceof PostfixExpression){
            AstNode identifier = expressionNode.getChild(0);
            AstNode operation = expressionNode.getChild(1);
            CSimpleDeclaration variableDeclaration = (CSimpleDeclaration)
                    functionBuilder.expressionHandler.variableDeclarations.get(identifier.getEscapedCodeStr().hashCode());
            CIdExpression idExpression = new CIdExpression(fileLocation,variableDeclaration);
            CBinaryExpression.BinaryOperator operator = operation.getTypeAsString().equals("++")?
                    CBinaryExpression.BinaryOperator.PLUS:
                    CBinaryExpression.BinaryOperator.MINUS;

            return functionBuilder.expressionHandler.buildBinaryExpression(
                    idExpression,
                    CIntegerLiteralExpression.ONE,
                    operator,
                    idExpression.getExpressionType());
        } else if(expressionNode instanceof BinaryOperationExpression){
            CExpression operand1 = (CExpression)getExpression(functionBuilder,(Expression)((BinaryOperationExpression) expressionNode).getLeft());
            CExpression operand2 = (CExpression)getExpression(functionBuilder,(Expression)((BinaryOperationExpression) expressionNode).getRight());
            CBinaryExpression.BinaryOperator operator = getBinaryOperator(expressionNode.getOperator());
            if(operator.getOperator().equals("*")||
                    operator.getOperator().equals("/")||
                    operator.getOperator().equals("+")||
                    operator.getOperator().equals("-")||
                    operator.getOperator().equals("%"))
                return functionBuilder.expressionHandler.buildBinaryExpression(operand1,operand2,operator, operand1.getExpressionType());
            else
                return functionBuilder.expressionHandler.buildBinaryExpression(operand1,operand2,operator,CNumericTypes.BOOL);
        }
        else {
            throw new RuntimeException("Unsupport expression node: "+ expressionNode.getTypeAsString());
        }
    }

    private CFunctionCallExpression handleCallExpression(CFGFunctionBuilder functionBuilder, CallExpression expressionNode){
        AstNode callee = expressionNode.getChild(0);
        List<CExpression> paramLists = new ArrayList<>();
        if(expressionNode.getChildCount()>1){
            AstNode agrumentList = expressionNode.getChild(1);
            for(int i=0;i<agrumentList.getChildCount();i++){
                CExpression expression = (CExpression)getExpression(functionBuilder,(Expression) agrumentList.getChild(i));
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
            throw new RuntimeException("No such function:"+functionName + " in "+ project);
        else
            return functionDeclaration;
    }

    //locationType= 0:internal variable, 1:global variable in channel, 2 global variable in UE, 3 global variable in eNB, 4 global variable in MME
    private CSimpleDeclaration getVariableDeclaration(CFGFunctionBuilder functionBuilder, String variableName){
        CSimpleDeclaration variableDel = null;
        if(filename.startsWith("s1ap")){
            for(CSimpleDeclaration simpleDeclaration: functionBuilder.expressionHandler.variableDeclarations.values()){
                if(simpleDeclaration.getOrigName().equals(variableName))
                    return simpleDeclaration;
            }
        }
        if(functionBuilder.expressionHandler.globalDeclarations.containsKey(variableName.hashCode()))
            variableDel = (CSimpleDeclaration)
                    functionBuilder.expressionHandler.globalDeclarations.get(variableName.hashCode());
        else
            variableDel = (CSimpleDeclaration)
                    functionBuilder.expressionHandler.variableDeclarations.get(variableName.hashCode());
        if(variableDel==null){
            switch (project){
                case UE:
                    variableDel = (CSimpleDeclaration) builderMap.get(UE).expressionHandler.globalDeclarations.get(variableName.hashCode());
                    break;
                case ENB:
                    variableDel = (CSimpleDeclaration) builderMap.get(ENB).expressionHandler.globalDeclarations.get(variableName.hashCode());
                    break;
                case MME:
                    variableDel = (CSimpleDeclaration) builderMap.get(MME).expressionHandler.globalDeclarations.get(variableName.hashCode());
                    break;
            }
        }
        if(variableDel==null && filename.equals("itti_task_abstract")){
            if(functionBuilder.functionName.contains("ue")) {
                variableDel = (CSimpleDeclaration) builderMap.get(UE).expressionHandler.globalDeclarations.get(variableName.hashCode());
            }
            else if(functionBuilder.functionName.contains("enb"))
                variableDel = (CSimpleDeclaration) builderMap.get(ENB).expressionHandler.globalDeclarations.get(variableName.hashCode());
            else
                variableDel = (CSimpleDeclaration) builderMap.get(MME).expressionHandler.globalDeclarations.get(variableName.hashCode());
        }

        if(variableDel==null)
            throw new RuntimeException("There is no such variable: "+variableName+" in the project: "+ project);
        return variableDel;
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

    public void expressionStatement(CFGFunctionBuilder functionBuilder, ExpressionStatement astNode, CFANode startNode, CFANode endNode){
        AstNode expressionNode = astNode.getChild(0);
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CStatement statement;
        if(expressionNode instanceof AssignmentExpression){
            String operatorString = ((AssignmentExpression) expressionNode).getOperator();
            CBinaryExpression.BinaryOperator operator = null;
            if(!operatorString.equals("=")){
                operator = getBinaryOperator(operatorString.replace("=","").trim());
            }
            CExpression operand1 = (CExpression)getExpression(functionBuilder,(Expression) expressionNode.getChild(0));
            CRightHandSide operand2;
            if(expressionNode.getChild(1) instanceof CallExpression){
                operand2 = handleCallExpression(functionBuilder, (CallExpression) expressionNode.getChild(1));
            }else
                 operand2 = getExpression(functionBuilder, (Expression) expressionNode.getChild(1));
            if(operand2 instanceof CFunctionCallExpression){
                if(operator!=null){

                    CVariableDeclaration variableDeclaration = new CVariableDeclaration(
                            fileLocation,
                            false,
                            CStorageClass.AUTO,
                            operand2.getExpressionType(),
                            "tempVar"+fileLocation.getStartingLineNumber(),
                            "tempVar"+fileLocation.getStartingLineNumber(),
                            "tempVar"+fileLocation.getStartingLineNumber(),
                            null
                    );
                    CFANode delNode = functionBuilder.newCFANode();
                    CDeclarationEdge declarationEdge = new CDeclarationEdge(variableDeclaration.getType().toString()+" tempVar"+fileLocation.getStartingLineNumber(),
                            fileLocation,
                            startNode,delNode,variableDeclaration);
                    functionBuilder.expressionHandler.variableDeclarations.put(variableDeclaration.getName().hashCode(),variableDeclaration);
                    functionBuilder.addToCFA(declarationEdge);
                    CIdExpression idExpression = new CIdExpression(fileLocation,variableDeclaration);
                    CFunctionCallAssignmentStatement assignmentStatement = new CFunctionCallAssignmentStatement(
                            fileLocation,
                            (CLeftHandSide) idExpression,
                            (CFunctionCallExpression) operand2);
                    String rawStrings = "tempVar"+fileLocation.getStartingLineNumber()+" = "+operand2.toString();
                    CFANode cfaNode = functionBuilder.newCFANode();
                    CStatementEdge statementEdge = new CStatementEdge(rawStrings,assignmentStatement,fileLocation,delNode,cfaNode);
                    functionBuilder.addToCFA(statementEdge);
                    startNode = cfaNode;
                    statement = new CExpressionAssignmentStatement(
                            fileLocation,
                            (CLeftHandSide)operand1,
                            idExpression);
                }else
                    statement = new CFunctionCallAssignmentStatement(
                    fileLocation,
                    (CLeftHandSide) operand1,
                        (CFunctionCallExpression) operand2);
            }
            else{
                if(operator!=null){
                    operand2 = functionBuilder.expressionHandler.buildBinaryExpression(operand1,(CExpression) operand2,operator,operand1.getExpressionType());
                }
                statement = new CExpressionAssignmentStatement(
                        fileLocation,
                        (CLeftHandSide) operand1,
                        (CExpression) operand2);
            }
        }else if(expressionNode instanceof PostIncDecOperationExpression){
            AstNode identifier = expressionNode.getChild(0);
            AstNode operation = expressionNode.getChild(1);

            //CVariableDeclaration variableDeclaration = (CVariableDeclaration) getVariableDeclaration(functionBuilder, identifier.getEscapedCodeStr());//functionBuilder.expressionHandler.variableDeclarations.get(identifier.getEscapedCodeStr().hashCode());
            CExpression idExpression = (CExpression) getExpression(functionBuilder, (Expression) identifier);
            CBinaryExpression.BinaryOperator operator = operation.getTypeAsString().equals("++")?
                    CBinaryExpression.BinaryOperator.PLUS:
                    CBinaryExpression.BinaryOperator.MINUS;

            CBinaryExpression binaryExpression = functionBuilder.expressionHandler.buildBinaryExpression(
                    idExpression,
                    CIntegerLiteralExpression.ONE,
                    operator,
                    idExpression.getExpressionType());
            statement = new CExpressionAssignmentStatement(fileLocation,(CLeftHandSide)idExpression,binaryExpression);
        }else if(expressionNode instanceof UnaryExpression){
            AstNode operation = expressionNode.getChild(0);
            AstNode identifier = expressionNode.getChild(1);
            CExpression idExpression = (CExpression)getExpression(functionBuilder, (Expression) identifier);

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
        }else if(expressionNode instanceof CallExpression){
            CFunctionCallExpression callExpression = handleCallExpression(functionBuilder, (CallExpression) expressionNode);
            statement = new CFunctionCallStatement(fileLocation,callExpression);
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
        return (CBinaryExpression) getExpression(functionBuilder, (Expression) astNode);
    }

    public void switchStatement(CFGFunctionBuilder functionBuilder, SwitchStatement astNode, CFANode startNode, CFANode endNode){
        String switchString = astNode.getEscapedCodeStr();
        AstNode condition = astNode.getChild(0);
        AstNode statement = astNode.getChild(1);

        String variableString = condition.getEscapedCodeStr();
        CFANode switchNode = functionBuilder.newCFANode();
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        functionBuilder.addToCFA(new BlankEdge(switchString, fileLocation, startNode, switchNode, switchString));

        CExpression variableExpr = getExpressionFromString(functionBuilder,variableString);

        CFANode trueNode, falseNode, lastnode=switchNode;
        for(int i=0;i<statement.getChildCount();i++){
            AstNode node = statement.getChild(i);
            if(node instanceof Label){
                FileLocation fileLocation1 = getFileLocation(node.getLocation());
                String labelName = "";
                if(node.getChildCount()==0){
                    labelName = node.getEscapedCodeStr().replace(":","").replace("case","").trim();
                }else
                    labelName = node.getChild(0).getEscapedCodeStr();

                if(labelName.equals("default")){
                    trueNode = functionBuilder.newCFANode();
                    functionBuilder.addToCFA(new BlankEdge(labelName, fileLocation1, lastnode,trueNode,labelName));
                    lastnode = functionBuilder.newCFANode();
                }else {
                    //String caseString = "case "+labelName;//node.getEscapedCodeStr().replace(":","").replace("case","").trim();
                    int value;
                    try{
                        value = Integer.valueOf(labelName);;
                    }catch (Exception e){
                        value = getTaskOrMsgIDbyName(variableExpr.getExpressionType(),labelName);
                    }
                    CExpression caseValueExpr;
                    if(variableExpr.getExpressionType().getCanonicalType().equals(CNumericTypes.CHAR)){
                        caseValueExpr = new CCharLiteralExpression(fileLocation,CNumericTypes.CHAR, (char)value);
                    }else{
                        caseValueExpr = new CIntegerLiteralExpression(fileLocation,CNumericTypes.INT, BigInteger.valueOf(value));
                    }
                    trueNode = functionBuilder.newCFANode();
                    falseNode =functionBuilder.newCFANode();
                    CBinaryExpression binaryExpression = functionBuilder.expressionHandler.buildBinaryExpression(variableExpr,caseValueExpr, CBinaryExpression.BinaryOperator.EQUALS,CNumericTypes.BOOL);
                    String rawString = variableString+"=="+labelName;
                    createTrueFalseEdge(functionBuilder,lastnode,trueNode,falseNode,rawString,binaryExpression);
                    lastnode=falseNode;
                }

                while (true){
                    i++;
                    AstNode nextNode = statement.getChild(i);
                    if(nextNode instanceof BreakStatement){// || (nextNode.getTypeAsString().equals("Statement") && nextNode.getEscapedCodeStr().equals("break"))){
                        fileLocation1 = getFileLocation(nextNode.getLocation());
                        functionBuilder.addToCFA(new BlankEdge("break", fileLocation1, trueNode,endNode,"default"));
                        break;
                    }else if(nextNode instanceof Label){
                        i--;
                        fileLocation1 = getFileLocation(nextNode.getLocation());
                        functionBuilder.addToCFA(new BlankEdge("fall through", fileLocation1, trueNode,lastnode,"fall through"));
                        break;
                    }else {
                        CFANode breakNode = functionBuilder.newCFANode();
                        handleStatement(functionBuilder, (Statement) nextNode, trueNode, breakNode);
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

    public void forStatement(CFGFunctionBuilder functionBuilder, ForStatement astNode, CFANode startNode, CFANode endNode){


        AstNode condition1 = astNode.getForInitExpression();//ForInit
        AstNode condition2 = astNode.getCondition();//Condition
        AstNode condition3 = astNode.getForLoopExpression();//OperationExpression
        AstNode statement = astNode.getChild(3);

        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        CFANode forNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge("for",fileLocation,startNode,forNode,"for"));
        AstNode initNode = condition1.getChild(0);
        CFANode initCFANode = functionBuilder.newCFANode();
        handleStatement(functionBuilder,(Statement) initNode,forNode, initCFANode);
        CFANode conditionNode = functionBuilder.newCFANode();
        String conditionString = condition2.getEscapedCodeStr();
        initCFANode.setLoopStart();
        CBinaryExpression conditionExpr = conditionExpression(functionBuilder,condition2);
        createTrueFalseEdge(functionBuilder,initCFANode,conditionNode,endNode, conditionString,conditionExpr);
        CFANode stateNode = functionBuilder.newCFANode();
        handleStatement(functionBuilder, (Statement) statement, conditionNode,stateNode);
        CBinaryExpression expression= (CBinaryExpression) getExpression(functionBuilder,(Expression)condition3);
        CExpressionAssignmentStatement assignmentStatement = new CExpressionAssignmentStatement(
                fileLocation,
                (CLeftHandSide) expression.getOperand1(),
                expression.getOperand2());
        CStatementEdge statementEdge = new CStatementEdge(expression.getOperand1().toString()+((Expression) condition3).getOperator(),
                assignmentStatement,
                fileLocation,
                stateNode,initCFANode);
        functionBuilder.addToCFA(statementEdge);
        //expressionStatement(functionBuilder, (ExpressionStatement) condition3, stateNode, initCFANode);
        //handleStatement(functionBuilder, (Statement) condition3, stateNode, initCFANode);
    }

    public void whileStatement(CFGFunctionBuilder functionBuilder, WhileStatement astNode, CFANode startNode, CFANode endNode){
        Condition condition = (Condition) astNode.getCondition();
        Statement statement = astNode.getStatement();
        FileLocation fileLocation = getFileLocation(astNode.getLocation());
        //CBinaryExpression conditionExpr = conditionExpression(functionBuilder,condition);
        CFANode whileNode = functionBuilder.newCFANode();
        functionBuilder.addToCFA(new BlankEdge("while", fileLocation, startNode,whileNode, "while" ));
        whileNode.setLoopStart();
        CFANode internalNode = functionBuilder.newCFANode();
        handleIfCondition(functionBuilder, condition,whileNode,internalNode,endNode);
        //createTrueFalseEdge(functionBuilder,startNode,internalNode, endNode, condition.getEscapedCodeStr(),conditionExpr);

        handleStatement(functionBuilder, statement,internalNode,whileNode);
    }

    public void variableDeclaration(CFGFunctionBuilder functionBuilder, IdentifierDeclStatement astNode, CFANode startNode, CFANode endNode){
        CFANode cfaNode = startNode;
        for(int i=0;i<astNode.getIdentifierDeclList().size();i++) {
            IdentifierDecl decl = (IdentifierDecl) astNode.getIdentifierDeclList().get(i);
            CDeclaration declaration = parseVariableDeclaration(functionBuilder, false, decl);

            functionBuilder.expressionHandler.variableDeclarations.put(declaration.getName().hashCode(), declaration);
            CDeclarationEdge declarationEdge;
            if (i <= astNode.getIdentifierDeclList().size() - 1) {
                declarationEdge = new CDeclarationEdge(decl.getEscapedCodeStr(),
                        declaration.getFileLocation(),
                        cfaNode,
                        endNode,
                        declaration);
            } else {
                CFANode nextNode = functionBuilder.newCFANode();
                declarationEdge = new CDeclarationEdge(decl.getEscapedCodeStr(),
                        declaration.getFileLocation(),
                        cfaNode,
                        nextNode,
                        declaration);
                cfaNode = nextNode;
            }

            functionBuilder.addToCFA(declarationEdge);
        }
    }

    public CType getType(String projectName, String typename){
        if(typename.endsWith("*")){
            CType type =getType(projectName,typename.replace("*","").trim());
            return new CPointerType(type.isConst(),type.isVolatile(),type);
        }else if(typename.startsWith("const")){
            CType type =getType(projectName,typename.replace("const","").trim());
            if(type.isConst())
                return type;
            //TODO
            //return new CPointerType(type.isConst(),type.isVolatile(),type);
        }

        if(builderMap.containsKey(projectName)){
            return builderMap.get(projectName).typeConverter.typeCache.get(typename.hashCode());
        }else if(channelBuilder.typeConverter.typeCache.containsKey(typename.hashCode()))
            return channelBuilder.typeConverter.typeCache.get(typename.hashCode());

        throw new RuntimeException("No existing type "+typename+" in project:"+ projectName);
    }

    public CDeclaration parseVariableDeclaration(CFGFunctionBuilder functionBuilder, boolean isGlobal, IdentifierDecl node){
        String variableName = node.getName().getEscapedCodeStr();
        CType type;
        if(functionBuilder==null){
            type = getType(getProjectForType("",0), node.getType().completeType);
        }else
            type = getType(getProjectForType(functionBuilder.functionName,3), node.getType().completeType);

        FileLocation fileLocation = getFileLocation(node.getLocation());
        CInitializer initializer = null;
        if(node.getAssignment()!=null){
            AssignmentExpression assignmentExpression = node.getAssignment();
            Expression initial = assignmentExpression.getRight();
            initializer = getInitializer(functionBuilder, initial);
        }

        CVariableDeclaration variableDeclaration = new CVariableDeclaration(
                fileLocation,
                isGlobal,
                CStorageClass.AUTO,
                type,
                variableName,
                variableName,
                variableName,
                initializer);

        return variableDeclaration;
    }

    public CInitializer getInitializer(CFGFunctionBuilder functionBuilder, Expression initializer){
        CInitializer cInitializer = null;
        FileLocation fileLocation = getFileLocation(initializer.getLocation());
        if(initializer instanceof InitializerList){
            List<CInitializer> elementInitializers = new ArrayList<>(initializer.getChildCount());
            for(int i=0;i<initializer.getChildCount();i++){
                CInitializer elementInitializer = getInitializer(functionBuilder, (Expression) initializer.getChild(i));
                elementInitializers.add(elementInitializer);
            }
            cInitializer = new CInitializerList(fileLocation, elementInitializers);
        }else{
            CExpression expression = (CExpression)getExpression(functionBuilder, initializer);
            cInitializer = new CInitializerExpression(fileLocation,expression);
        }

        return cInitializer;
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
                i++;
            }
        }
        return astNodeMap;
    }

    private List<List<AstNode>> globalStatementArray(AstNode top){
        List<List<AstNode>> stateList = new ArrayList<>();
        Iterator<AstNode> iterator = top.getChildIterator();
        List<AstNode> astNodes = new ArrayList<>();
        while (iterator.hasNext()){
            AstNode node = iterator.next();
            astNodes.add(node);
            if(!node.getTypeAsString().equals("Statement")){
                stateList.add(astNodes);
                astNodes = new ArrayList<>();
            }
        }
        return stateList;
    }

    private FileLocation getFileLocation(CodeLocation location){
        if(useSingeFilelocation)
            return fileLocation;
        return new FileLocation(filename, 0,1,location.startLine,location.startLine);
    }

    //locationType: global variable: 0, function return type: 1, function paramtype:2, variable del: 3, cast type: 4
    private String getProjectForType(String functionName, int locationType){
        if(filename.endsWith("side_message_channel")){
            if(functionName.equals("ueChannelMessageInit")||
                    functionName.equals("cnChannelMessageInit")||
                    locationType==0)
                return Channel;
            else if(functionName.startsWith("ue"))
                return UE;
            else if(functionName.contains("AS"))
                return MME;
            else
                return ENB;
        }else if(filename.endsWith("channel_operation")){
            if(functionName.contains("RRC")){
                if(functionName.startsWith("DL")){
                    return UE;
                }else
                    return ENB;
            }else if(functionName.equals("DLNASEMMMessageTranslation")){
                return MME;
            }else if(functionName.equals("ULNASEMMMessageTranslation"))
                return UE;
            else if(functionName.equals("DLNASMessageDeliver")){
                if(locationType==2)
                    return MME;
                else
                    return UE;
            }else if(functionName.equals("ULNASMessageDeliver"))
                if(locationType==2)
                    return UE;
                else
                    return MME;
        }else if(filename.equals("EMMMessageTranslation")||filename.equals("ESMMessageTranslation")){
            if(locationType==4)
                return functionName.contains("UL")?MME:UE;
            else if(locationType == 3)
                return functionName.contains("UL")?UE:MME;
            else
                return "";
                //throw new RuntimeException(filename+" "+functionName+" "+ locationType);
        }else if(filename.endsWith("rrc_task_abstract")){
            return functionName.startsWith("enb")?ENB:UE;
        }else if(filename.equals("itti_task_abstract")){
            switch (locationType){
                case 1:
                case 2:
                    return functionName.endsWith("ue")?UE:(functionName.endsWith("eNB")?ENB:MME);
                case 3:
                case 4:
                    return functionName.endsWith("ue")?ENB:(functionName.endsWith("eNB")?UE:MME);
            }
        }else if(filename.startsWith("s1ap_"))
            return project;

        return "";

    }

    public CType getRealCompositeType(CType type){
        if(type instanceof CPointerType)
            return getRealCompositeType(((CPointerType) type).getType());
        if(type instanceof CTypedefType)
            return getRealCompositeType(((CTypedefType) type).getRealType());
        if(type instanceof CElaboratedType)
            return getRealCompositeType(((CElaboratedType) type).getRealType());
        if(type instanceof CCompositeType)
            return type;
        return null;
    }

    private CType getMemberType(CCompositeType type, String memeber){
        for(CCompositeType.CCompositeTypeMemberDeclaration memberParam: type.getMembers()){
            if(memberParam.getName().equals(memeber))
                return memberParam.getType();
        }
        throw new RuntimeException(memeber+" does not belong to the type: " +type.toString());
    }

    public CExpression expressionParser(CFGFunctionBuilder functionBuilder, String[] exprElements){
        FileLocation fileLocation = useSingeFilelocation? this.fileLocation:FileLocation.DUMMY;
        if(exprElements.length==1){
            if(exprElements[0].toLowerCase().equals("null")){
                CType voidpointer = new CPointerType(false,false, CVoidType.VOID);
                return new CCastExpression(fileLocation,  voidpointer,  CIntegerLiteralExpression.ZERO);
            }else if(exprElements[0].toLowerCase().equals("true")){
                return CIntegerLiteralExpression.ONE;
            }else if(exprElements[0].toLowerCase().equals("false")){
                return CIntegerLiteralExpression.ZERO;
            }else{
                try {
                    int caseID = Integer.valueOf(exprElements[0]);
                    return new CIntegerLiteralExpression(fileLocation, CNumericTypes.UNSIGNED_INT, BigInteger.valueOf(caseID));
                }catch (Exception e){
                    if(exprElements[0].startsWith("TASK_")){
                        CType type = null;
                        if(functionBuilder.functionName.contains("ue"))
                            type = builderMap.get(UE).typeConverter.typeCache.get("task_id_t".hashCode());
                        else if(functionBuilder.functionName.contains("eNB"))
                            type = builderMap.get(ENB).typeConverter.typeCache.get("task_id_t".hashCode());
                        else
                            type = builderMap.get(MME).typeConverter.typeCache.get("task_id_t".hashCode());

                        int value  = getTaskOrMsgIDbyName(type, exprElements[0]);
                        if(value!=-1)
                            return new CIntegerLiteralExpression(fileLocation, type, BigInteger.valueOf(value));
                        else
                            throw  new RuntimeException("No such id :"+ exprElements[0]+" in "+ functionBuilder.functionName);
                    }else {
                        CSimpleDeclaration variableDeclaration = getVariableDeclaration(functionBuilder, exprElements[0]);
                        //functionBuilder.expressionHandler.variableDeclarations.get(exprElements[0].hashCode());
                        if(variableDeclaration!=null)
                            return new CIdExpression(fileLocation,variableDeclaration);
                        else
                            throw new RuntimeException("No such variable: "+ exprElements[0]);
                    }

                }
            }
        }else {
            CSimpleDeclaration variableDeclaration = getVariableDeclaration(functionBuilder, exprElements[0]);
            CExpression expression = new CIdExpression(fileLocation,variableDeclaration);
            for(int i=1;i<exprElements.length;i+=2){
                if(i+1==exprElements.length)
                    System.out.println(Arrays.toString(exprElements));
                String memberName = exprElements[i+1];
                if(exprElements[i].equals("[") && exprElements[i+2].equals("]")){
                    CExpression indexExpr = getExpressionFromString(functionBuilder,memberName);
                    CType expressionType = null;
                    if(expression.getExpressionType() instanceof CPointerType)
                        expressionType = ((CPointerType) expression.getExpressionType()).getType();
                    else if(expression.getExpressionType() instanceof CArrayType)
                        expressionType = ((CArrayType) expression.getExpressionType()).getType();
                    else
                        throw new RuntimeException(expression.getExpressionType().toString() +" is not a pointer or array type! in "+ Arrays.toString(exprElements));
                    expression = new CArraySubscriptExpression(fileLocation,
                            expressionType,
                            expression,
                            indexExpr);
                    i++;
                }else {
                    CCompositeType cCompositeType = (CCompositeType) getRealCompositeType(expression.getExpressionType());
                    CType memberType = getMemberType(cCompositeType, memberName);
                    boolean ispointerder = exprElements[i].equals("->");
                    expression = new CFieldReference(fileLocation,
                            memberType,
                            memberName,
                            expression,
                            ispointerder);
                }
            }
            return expression;
        }
    }

}
