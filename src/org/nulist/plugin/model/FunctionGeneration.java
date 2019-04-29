package org.nulist.plugin.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.grammatech.cs.*;
import org.nulist.plugin.model.action.ITTIAbstract;
import org.nulist.plugin.parser.CFABuilder;
import org.nulist.plugin.parser.CFGFunctionBuilder;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.nulist.plugin.model.action.ITTIAbstract.itti_send_to_task;
import static org.nulist.plugin.parser.CFGParser.ENB;
import static org.nulist.plugin.parser.CFGParser.MME;
import static org.nulist.plugin.util.ClassTool.printWARNING;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.createDummyLiteral;

/**
 * @ClassName FunctionGeneration
 * @Description generate function cfa with new content
 * @Author Yinbo Yu
 * @Date 4/15/19 11:01 AM
 * @Version 1.0
 **/
public class FunctionGeneration {
    public final static String ITTI_ALLOC_NEW_MESSAGE = "itti_alloc_new_message";
    public final static String ITTI_SEND_MSG_TO_TASKS = "itti_send_msg_to_task";
    public final static String CREATE_TASKS_UE = "create_tasks_ue";
    public final static String CREATE_TASKS = "create_tasks";


    private final LogManager logger;
    private final MachineModel machineModel;
    public CFABuilder cfaBuilder;
    public String projectName;

    public FunctionGeneration(CFABuilder builder, String projectName, LogManager logger, MachineModel machineModel){
        cfaBuilder = builder;
        this.projectName = projectName;
        this.logger = logger;
        this.machineModel = machineModel;
    }


    /**
     * @Description //initializer rrc
     * eNB_app_Initialize
     * rrc_enb_init
     * @Param [createTasksENB]
     * @return void
     **/
    public void generatCreateTasksENB(procedure createTasksENB)throws result{
        assert createTasksENB.name().equals(CREATE_TASKS);
        String funcName = CREATE_TASKS;
        CFunctionDeclaration functionDeclaration =
                (CFunctionDeclaration) cfaBuilder.expressionHandler.globalDeclarations.get(funcName.hashCode());
        if(functionDeclaration==null){
            printWARNING("Can not find"+ CREATE_TASKS);
            return;
        }
        if(cfaBuilder.cfgFunctionBuilderMap.containsKey(CREATE_TASKS)){
            printWARNING("Shall no builder of "+CREATE_TASKS);
            return;
        }

        CFGFunctionBuilder cfgFunctionBuilder = new CFGFunctionBuilder(logger,
                cfaBuilder.typeConverter,
                createTasksENB,
                CREATE_TASKS, createTasksENB.get_compunit().name(),
                cfaBuilder);
        functionDeclaration = cfgFunctionBuilder.handleFunctionDeclaration();

        cfaBuilder.expressionHandler.globalDeclarations.replace(funcName.hashCode(), functionDeclaration);
        // handle the function definition
        CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();
        cfaBuilder.functions.put(funcName, en);
        cfaBuilder.cfgFunctionBuilderMap.put(funcName,cfgFunctionBuilder);

        //
        constructionCreateTasksENB(cfgFunctionBuilder);
        cfgFunctionBuilder.finish();
    }

    /**
     * @Description //assign users and initializer users
     * @Param [createTasksUE]
     * @return void
     **/
    public void generateCreateTasksUE(procedure createTasksUE)throws result {
        assert createTasksUE.name().equals(CREATE_TASKS_UE);
        String funcName = CREATE_TASKS_UE;
        CFunctionDeclaration functionDeclaration =
                (CFunctionDeclaration) cfaBuilder.expressionHandler.globalDeclarations.get(funcName.hashCode());
        if(functionDeclaration==null){
            printWARNING("Can not find"+ CREATE_TASKS_UE);
            return;
        }
        if(cfaBuilder.cfgFunctionBuilderMap.containsKey(CREATE_TASKS_UE)){
            printWARNING("Shall no builder of "+CREATE_TASKS_UE);
            return;
        }

        CFGFunctionBuilder cfgFunctionBuilder = new CFGFunctionBuilder(logger,
                cfaBuilder.typeConverter,
                createTasksUE,
                CREATE_TASKS_UE, createTasksUE.get_compunit().name(),
                cfaBuilder);
        functionDeclaration = cfgFunctionBuilder.handleFunctionDeclaration();

        cfaBuilder.expressionHandler.globalDeclarations.replace(funcName.hashCode(), functionDeclaration);
        // handle the function definition
        CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();
        cfaBuilder.functions.put(funcName, en);
        cfaBuilder.cfgFunctionBuilderMap.put(funcName,cfgFunctionBuilder);

        //
        constructionCreateTasksUE(cfgFunctionBuilder);
        cfgFunctionBuilder.finish();
    }


    public void generateITTI_ALLOC_NEW_MESSAGE(procedure ittiAllocNewMessage)throws result{
        assert ittiAllocNewMessage.name().equals(ITTI_ALLOC_NEW_MESSAGE);
        String funcName = ittiAllocNewMessage.name();
        CFunctionDeclaration functionDeclaration =
                (CFunctionDeclaration) cfaBuilder.expressionHandler.globalDeclarations.get(funcName.hashCode());
        if(functionDeclaration==null){
            printWARNING("Can not find"+ ITTI_ALLOC_NEW_MESSAGE);
            return;
        }
        if(!cfaBuilder.cfgFunctionBuilderMap.containsKey(funcName)){
            CFGFunctionBuilder cfgFunctionBuilder = new CFGFunctionBuilder(logger,
                    cfaBuilder.typeConverter,
                    ittiAllocNewMessage,
                    funcName, ittiAllocNewMessage.get_compunit().name(),
                    cfaBuilder);
            functionDeclaration = cfgFunctionBuilder.handleFunctionDeclaration();

            cfaBuilder.expressionHandler.globalDeclarations.replace(funcName.hashCode(), functionDeclaration);
            // handle the function definition
            CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();
            cfaBuilder.functions.put(funcName, en);
            cfaBuilder.cfgFunctionBuilderMap.put(funcName,cfgFunctionBuilder);
            cfgFunctionBuilder.visitFunction(true);
        }else {
            printWARNING("There exits function builder for"+ITTI_ALLOC_NEW_MESSAGE);

            CFGFunctionBuilder cfgFunctionBuilder = cfaBuilder.cfgFunctionBuilderMap.get(funcName);
            if(!cfgFunctionBuilder.isFinished)
                cfgFunctionBuilder.visitFunction(true);
        }
    }

    /**
     * @Description //generate cfa of itti_send_msg_to_task_abstract
     * @Param [itti_send_msg_to_task, itti_send_msg_to_task_abstract]
     * @return void
     **/
    public void generateITTI_SEND_TO_TASK(procedure itti_send_msg_to_task, procedure itti_send_msg_to_task_abstract)throws result{
        assert itti_send_msg_to_task.name().equals(ITTI_SEND_MSG_TO_TASKS);
        String functionName = ITTI_SEND_MSG_TO_TASKS;

        if(!cfaBuilder.cfgFunctionBuilderMap.containsKey(functionName)){

            //replace itti_send_msg_to_task_abstract entry by itti_send_msg_to_task
            CFGFunctionBuilder cfgFunctionBuilder = new CFGFunctionBuilder(logger,
                    cfaBuilder.typeConverter,
                    itti_send_msg_to_task_abstract,
                    functionName,itti_send_msg_to_task_abstract.get_compunit().name(),
                    cfaBuilder);
            CFunctionDeclaration functionDeclaration = cfgFunctionBuilder.handleFunctionDeclaration();

            cfaBuilder.expressionHandler.globalDeclarations.replace(functionName.hashCode(),functionDeclaration);
            cfaBuilder.functionDeclarations.put(functionName, functionDeclaration);
            CFunctionEntryNode en = cfgFunctionBuilder.handleFunctionDefinition();
            cfaBuilder.functions.put(functionName,en);

            cfgFunctionBuilder.visitFunction(false);

            postAssociateFunctions(cfgFunctionBuilder, functionName);
            //cfgFunctionBuilder.finish();
        }
    }

    public void postAssociateFunctions(CFGFunctionBuilder builder, String functionName){
        for(CFANode node:builder.cfaNodes){
            if(node.getNumLeavingEdges()>0)
                for(int i=0;i<node.getNumLeavingEdges();i++){
                    traverseEdges(builder,node.getLeavingEdge(i), functionName);
                }
        }
    }

    /**
     * @Description //insert function call
     * @Param [edge]
     * @return void
     **/
    private void traverseEdges(CFGFunctionBuilder builder, CFAEdge edge, String functionName){
        if(edge instanceof CAssumeEdge && ((CAssumeEdge) edge).getTruthAssumption()){
            CExpression conditionExpr = ((CAssumeEdge) edge).getExpression();
            if(conditionExpr instanceof CBinaryExpression){
                int taskID = ((CIntegerLiteralExpression)((CBinaryExpression) conditionExpr).getOperand2()).getValue().intValue();
                CType type = ((CBinaryExpression) conditionExpr).getOperand1().getExpressionType();
                String taskName = getTaskOrMsgNameByID(type, taskID);
                CFunctionDeclaration functionDeclaration = itti_send_to_task(taskName,projectName,cfaBuilder.expressionHandler);
                if(functionDeclaration!=null){
                    CFANode caseNextNode = edge.getSuccessor();
                    CFAEdge breakEdge = caseNextNode.getLeavingEdge(0);
                    CFANode cfaNode = new CFANode(functionName);
                    caseNextNode.removeLeavingEdge(breakEdge);
                    CFANode breakNode = breakEdge.getSuccessor();
                    breakNode.removeEnteringEdge(breakEdge);

                    CParameterDeclaration input = builder.functionDeclaration.getParameters().get(2);
                    List<CExpression> params = new ArrayList<>();
                    FileLocation fileLocation = breakEdge.getFileLocation();
                    CExpression param = builder.expressionHandler.getAssignedIdExpression(input.asVariableDeclaration(),input.getType(),fileLocation);
                    params.add(param);
                    CExpression functionCallExpr = new CIdExpression(fileLocation,functionDeclaration.getType(), functionDeclaration.getName(), functionDeclaration);

                    CFunctionCallExpression expression = new CFunctionCallExpression(fileLocation,functionDeclaration.getType(), functionCallExpr, params, functionDeclaration);

                    CFunctionCallStatement cFunctionCallStatement = new CFunctionCallStatement(fileLocation, expression);
                    String rawCharacters = functionDeclaration.getName()+"("+param.toString()+");";
                    CStatementEdge statementEdge = new CStatementEdge(rawCharacters,cFunctionCallStatement,
                            fileLocation, caseNextNode, cfaNode);
                    builder.addToCFA(statementEdge);
                    BlankEdge blankEdge = new BlankEdge(breakEdge.getRawStatement(),breakEdge.getFileLocation(),cfaNode,breakNode,breakEdge.getDescription());
                    builder.addToCFA(blankEdge);
                    cfaBuilder.addNode(functionName,cfaNode);
                }
            }
        }
    }

    public static String getTaskOrMsgNameByID(CType type, int id){
        if(type instanceof CTypedefType && ((CTypedefType) type).getName().equals("task_id_t")){
            CType realType = ((CElaboratedType)(((CTypedefType) type).getRealType())).getRealType();
            if(realType instanceof CEnumType){
                ImmutableList<CEnumType.CEnumerator> enumerators = ((CEnumType) realType).getEnumerators();
                if(enumerators.size()>id)
                    return enumerators.get(id).getName();
                else {
                    printWARNING("There is no this task id:"+id+" in task_id_t");
                    return "";
                }
            }else{
                printWARNING("This is not a task_id_t type");
                return "";
            }
        }else if(type instanceof CTypedefType && ((CTypedefType) type).getName().equals("MessagesIds")){
            CType realType = ((CElaboratedType)(((CTypedefType) type).getRealType())).getRealType();
            if(realType instanceof CEnumType){
                ImmutableList<CEnumType.CEnumerator> enumerators = ((CEnumType) realType).getEnumerators();
                if(enumerators.size()>id)
                    return enumerators.get(id).getName();
                else {
                    printWARNING("There is no this message id:"+id+" in MessagesIds");
                    return "";
                }
            }else{
                printWARNING("This is not a task_id_t type");
                return "";
            }
        }else{
            printWARNING("This is not a task_id_t or MessagesIds type");
            return "";
        }
    }

    public static int getTaskOrMsgIDbyName(CType type, String name){
        if(type instanceof CTypedefType && ((CTypedefType) type).getName().equals("task_id_t")){
            CType realType = ((CElaboratedType)(((CTypedefType) type).getRealType())).getRealType();
            if(realType instanceof CEnumType){
                ImmutableList<CEnumType.CEnumerator> enumerators = ((CEnumType) realType).getEnumerators();
                for(int i=0;i<enumerators.size();i++){
                    CEnumType.CEnumerator enumerator = enumerators.get(i);
                    if(enumerator.getName().equals(name))
                        return i;
                }
                printWARNING("There is no this task :"+name+" in task_id_t");
                return -1;
            }else{
                printWARNING("This is not a task_id_t type");
                return -1;
            }
        }else if(type instanceof CTypedefType && ((CTypedefType) type).getName().equals("MessagesIds")){
            CType realType = ((CElaboratedType)(((CTypedefType) type).getRealType())).getRealType();
            if(realType instanceof CEnumType){
                ImmutableList<CEnumType.CEnumerator> enumerators = ((CEnumType) realType).getEnumerators();
                for(int i=0;i<enumerators.size();i++){
                    CEnumType.CEnumerator enumerator = enumerators.get(i);
                    if(enumerator.getName().equals(name))
                        return i;
                }
                printWARNING("There is no this message :"+name+" in MessagesIds");
                return -1;
            }else{
                printWARNING("This is not a task_id_t type");
                return -1;
            }
        }else{
            printWARNING("This is not a task_id_t or MessagesIds type");
            return -1;
        }
    }

    private void constructionCreateTasksENB(CFGFunctionBuilder builder){
        //if (enb_nb > 0) {
        //    eNB_app_Initialize();
        //    rrc_enb_init();
        //}
        //return 0;
        CVariableDeclaration returnVariable = (CVariableDeclaration) builder.cfa.getReturnVariable().get();
        CFANode cfaNode = builder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                builder.cfa, cfaNode, "Function start dummy edge");
        builder.addToCFA(dummyEdge);

        CFANode nextCFANode = builder.newCFANode();
        int startNumber = builder.cfa.getFileLocation().getStartingLineNumber()+1;
        FileLocation fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);

        CDeclarationEdge declarationEdge = new CDeclarationEdge(returnVariable.toASTString(),
                fileLocation,cfaNode,nextCFANode, returnVariable);
        builder.addToCFA(declarationEdge);

        //if (enb_nb>0)
        CFANode thenNode = builder.newCFANode();
        CFANode elseNode = builder.newCFANode();
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CVariableDeclaration inputVar = builder.functionDeclaration.getParameters().get(0).asVariableDeclaration();
        CIdExpression enb_na = (CIdExpression) builder.expressionHandler.getAssignedIdExpression(inputVar,inputVar.getType(),fileLocation);
        CExpression conditionExpr= builder.expressionHandler.buildBinaryExpression(enb_na,
                CIntegerLiteralExpression.ZERO,
                CBinaryExpression.BinaryOperator.GREATER_THAN,
                CNumericTypes.BOOL);

        String conditionString = conditionExpr.toString();
        CAssumeEdge trueEdge =
                new CAssumeEdge(
                        conditionString,
                        fileLocation,
                        nextCFANode,
                        thenNode,
                        conditionExpr,
                        true,
                        false,
                        false);
        builder.addToCFA(trueEdge);

        //eNB_app_Initialize
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CFANode init = builder.newCFANode();
        CFunctionDeclaration enbinit = (CFunctionDeclaration)
                cfaBuilder.expressionHandler.globalDeclarations.get("eNB_app_Initialize".hashCode());
        if(enbinit==null)
            throw new RuntimeException("No eNB_app_Initialize function");

        CExpression functionName = new CIdExpression(fileLocation,enbinit.getType(), enbinit.getName(), enbinit);

        CFunctionCallExpression callExpression =
                new CFunctionCallExpression(fileLocation,enbinit.getType(), functionName, new ArrayList<>(), enbinit);

        CFunctionCallStatement cFunctionCallStatement = new CFunctionCallStatement(fileLocation, callExpression);

        CStatementEdge statementEdge = new CStatementEdge("eNB_app_Initialize();",
                cFunctionCallStatement,fileLocation,thenNode,init);
        builder.addToCFA(statementEdge);

        //rrc_enb_init
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CFANode rrcinit = builder.newCFANode();
        CFunctionDeclaration rrcinitD = (CFunctionDeclaration)
                cfaBuilder.expressionHandler.globalDeclarations.get("rrc_enb_init".hashCode());
        if(rrcinitD==null)
            throw new RuntimeException("No rrc_enb_init function");

        functionName = new CIdExpression(fileLocation,rrcinitD.getType(), rrcinitD.getName(), rrcinitD);

        callExpression = new CFunctionCallExpression(fileLocation,rrcinitD.getType(), functionName, new ArrayList<>(), rrcinitD);

        cFunctionCallStatement = new CFunctionCallStatement(fileLocation, callExpression);

        statementEdge = new CStatementEdge("rrc_enb_init();",
                cFunctionCallStatement,fileLocation,init,rrcinit);
        builder.addToCFA(statementEdge);

        //
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        dummyEdge = new BlankEdge("", fileLocation,
                rrcinit, elseNode, "");
        builder.addToCFA(dummyEdge);

        //else
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CAssumeEdge falseEdge =
                new CAssumeEdge(
                        "!(" + conditionString + ")",
                        fileLocation,
                        nextCFANode,
                        elseNode,
                        conditionExpr,
                        false,
                        false,
                        false);
        builder.addToCFA(falseEdge);

        //return 0;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                builder.functionDeclaration.getFileLocation().getEndingLineNumber()-1,
                builder.functionDeclaration.getFileLocation().getEndingLineNumber()-1);
        CExpression returnExpr = CIntegerLiteralExpression.ZERO;
        CExpression returnVarExpr = new CIdExpression(fileLocation,
                returnVariable.getType(),
                returnVariable.getName(),
                returnVariable);
        CExpressionAssignmentStatement assignmentStatement =
                new CExpressionAssignmentStatement(fileLocation,
                        (CLeftHandSide) returnVarExpr,
                        CIntegerLiteralExpression.ZERO);
        CReturnStatement returnStatement = new CReturnStatement(fileLocation,
                Optional.of(returnExpr),
                Optional.of(assignmentStatement));
        CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge("return 0;",
                returnStatement,
                fileLocation,
                elseNode,
                builder.cfa.getExitNode());
        builder.addToCFA(returnStatementEdge);
    }

    private void constructionCreateTasksUE(CFGFunctionBuilder builder){
        //if (ue_nb > 0) {
        //    users = calloc(1, sizeof(*users));
        //    if (users == NULL) abort();
        //    users->count = ue_nb;
        //    nas_ue_user_initialize();
        //}
        //return 0;

        CVariableDeclaration returnVariable = (CVariableDeclaration) builder.cfa.getReturnVariable().get();
        CFANode cfaNode = builder.newCFANode();
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY,
                builder.cfa, cfaNode, "Function start dummy edge");
        builder.addToCFA(dummyEdge);

        CFANode nextCFANode = builder.newCFANode();

        int startNumber = builder.cfa.getFileLocation().getStartingLineNumber()+1;
        FileLocation fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);

        CDeclarationEdge declarationEdge = new CDeclarationEdge(returnVariable.toASTString(),
                fileLocation,cfaNode,nextCFANode, returnVariable);

        builder.addToCFA(declarationEdge);

        //if (ue_nb>0)
        CFANode thenNode = builder.newCFANode();
        CFANode elseNode = builder.newCFANode();
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CVariableDeclaration inputVar = builder.functionDeclaration.getParameters().get(0).asVariableDeclaration();
        CIdExpression ue_na = (CIdExpression) builder.expressionHandler.getAssignedIdExpression(inputVar,inputVar.getType(),fileLocation);
        CExpression conditionExpr= builder.expressionHandler.buildBinaryExpression(ue_na,
                CIntegerLiteralExpression.ZERO,
                CBinaryExpression.BinaryOperator.GREATER_THAN,
                CNumericTypes.BOOL);

        String conditionString = conditionExpr.toString();
        CAssumeEdge trueEdge =
                new CAssumeEdge(
                        conditionString,
                        fileLocation,
                        nextCFANode,
                        thenNode,
                        conditionExpr,
                        true,
                        false,
                        false);
        builder.addToCFA(trueEdge);

        // users = calloc(1, sizeof(*users));
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CFANode callocNode = builder.newCFANode();
        CVariableDeclaration users = (CVariableDeclaration) cfaBuilder.expressionHandler.globalDeclarations.get("users".hashCode());
        if(users==null)
            throw new RuntimeException("No users variable");

        CFunctionDeclaration calloc = (CFunctionDeclaration) cfaBuilder.expressionHandler.globalDeclarations.get("calloc".hashCode());
        if(calloc==null)
            throw new RuntimeException("No calloc function");
        List<CExpression> params = new ArrayList<>();
        params.add(CIntegerLiteralExpression.ONE);
        params.add(createDummyLiteral(15112L, CNumericTypes.INT));//sizeof(*users)
        CExpression functionNameExpr = new CIdExpression(fileLocation,calloc.getType(), calloc.getName(), calloc);

        CFunctionCallExpression expression = new CFunctionCallExpression(fileLocation,calloc.getType(), functionNameExpr, params, calloc);
        CIdExpression usersExpr = (CIdExpression) builder.expressionHandler.getAssignedIdExpression(users,users.getType(),fileLocation);
        CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement = new CFunctionCallAssignmentStatement(
                fileLocation,
                (CLeftHandSide) usersExpr,
                expression);

        String rawCharacters = "users = "+calloc.getName()+"(1, sizeof(*users));";
        CStatementEdge statementEdge = new CStatementEdge(rawCharacters,cFunctionCallAssignmentStatement,
                fileLocation, thenNode, callocNode);
        builder.addToCFA(statementEdge);

        //    users->count = ue_nb;
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CFANode countnode = builder.newCFANode();
        String fieldName = "count";
        CType memberType = builder.typeConverter.typeCache.get("size_t".hashCode());


        CLeftHandSide leftHandSide = new CFieldReference(fileLocation, memberType, fieldName, usersExpr, true);
        CExpressionAssignmentStatement assignmentStatement = new CExpressionAssignmentStatement(fileLocation,leftHandSide,ue_na);
        statementEdge = new CStatementEdge("users->count = ue_nb;",assignmentStatement,fileLocation,callocNode,countnode);
        builder.addToCFA(statementEdge);

        //    nas_ue_user_initialize();
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CFANode init = builder.newCFANode();
        CFunctionDeclaration nasiniti = (CFunctionDeclaration)
                cfaBuilder.expressionHandler.globalDeclarations.get("nas_ue_user_initialize".hashCode());
        if(nasiniti==null)
            throw new RuntimeException("No nas_ue_user_initialize function");

        functionNameExpr = new CIdExpression(fileLocation,nasiniti.getType(), nasiniti.getName(), nasiniti);

        CFunctionCallExpression callExpression =
                new CFunctionCallExpression(fileLocation,nasiniti.getType(), functionNameExpr, new ArrayList<>(), nasiniti);

        CFunctionCallStatement cFunctionCallStatement = new CFunctionCallStatement(fileLocation, callExpression);

        statementEdge = new CStatementEdge("nas_ue_user_initialize();",
                cFunctionCallStatement,fileLocation,countnode,init);
        builder.addToCFA(statementEdge);
        //
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        dummyEdge = new BlankEdge("", fileLocation,
                init, elseNode, "");
        builder.addToCFA(dummyEdge);

        //else
        startNumber++;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                startNumber,
                startNumber);
        CAssumeEdge falseEdge =
                new CAssumeEdge(
                        "!(" + conditionString + ")",
                        fileLocation,
                        nextCFANode,
                        elseNode,
                        conditionExpr,
                        false,
                        false,
                        false);
        builder.addToCFA(falseEdge);

        //return 0;
        fileLocation = new FileLocation(
                builder.cfa.getFileLocation().getFileName(),
                0,
                1,
                builder.functionDeclaration.getFileLocation().getEndingLineNumber()-1,
                builder.functionDeclaration.getFileLocation().getEndingLineNumber()-1);
        CExpression returnExpr = CIntegerLiteralExpression.ZERO;
        CExpression returnVarExpr = new CIdExpression(fileLocation,
                returnVariable.getType(),
                returnVariable.getName(),
                returnVariable);
        assignmentStatement =
                new CExpressionAssignmentStatement(fileLocation,
                        (CLeftHandSide) returnVarExpr,
                        CIntegerLiteralExpression.ZERO);
        CReturnStatement returnStatement = new CReturnStatement(fileLocation,
                Optional.of(returnExpr),
                Optional.of(assignmentStatement));
        CReturnStatementEdge returnStatementEdge = new CReturnStatementEdge("return 0;",
                returnStatement,
                fileLocation,
                elseNode,
                builder.cfa.getExitNode());
        builder.addToCFA(returnStatementEdge);
    }


    public static void doComposition(Map<String, CFABuilder> builderMap){
        if(builderMap.containsKey(ENB) && builderMap.containsKey(MME))
            buildSecureChannelBetweeneNBandMME(builderMap.get(ENB),builderMap.get(MME));
    }

    /**
     * @Description //build the secure channel with S1AP protocol
     * //once mme send msg (message id=NAS_DOWNLINK_DATA_REQ)to S1AP protocol task, we abstract it and directly sent it to eNB
     *   The content need to be translated including:   --mme_app_handle_nas_dl_req
     *      NAS_DOWNLINK_DATA_REQ (message_p).ue_id                  = nas_dl_req_pP->ue_id;
     *      NAS_DOWNLINK_DATA_REQ (message_p).nas_msg                = nas_dl_req_pP->nas_msg;
     *   to S1AP_DOWNLINK_NAS   -- s1ap_eNB_itti_send_nas_downlink_ind
     *      s1ap_downlink_nas->ue_initial_id  = ue_initial_id;
     *      s1ap_downlink_nas->nas_pdu.buffer = malloc(sizeof(uint8_t) * nas_pdu_length);
     *      memcpy(s1ap_downlink_nas->nas_pdu.buffer, nas_pdu, nas_pdu_length);
     *      s1ap_downlink_nas->nas_pdu.length = nas_pdu_length;
     * //once enb send msg (message id=S1AP_INITIAL_CONTEXT_SETUP_RESP|S1AP_UPLINK_NAS), we abstract it and send it to mme
     * @Param [enbBuilder, mmeBuilder]
     * @return void
     **/


    public static void buildSecureChannelBetweeneNBandMME(CFABuilder enbBuilder, CFABuilder mmeBuilder){
        CFGFunctionBuilder enbITTIFunctionBuilder = enbBuilder.cfgFunctionBuilderMap.get(ITTI_SEND_MSG_TO_TASKS);
        CFGFunctionBuilder mmeITTIFunctionBuilder = mmeBuilder.cfgFunctionBuilderMap.get(ITTI_SEND_MSG_TO_TASKS);
        if(enbITTIFunctionBuilder!=null && mmeITTIFunctionBuilder!=null){
            CFANode enbS1APNode = findOutS1APConditionNode(ENB, enbITTIFunctionBuilder);
            CFANode mmeS1APNpde = findOutS1APConditionNode(MME, mmeITTIFunctionBuilder);
            CVariableDeclaration enbMsg =  enbITTIFunctionBuilder.functionDeclaration.getParameters().get(2).asVariableDeclaration();
            CType enbMessageDef = enbMsg.getType();
            CVariableDeclaration mmeMsg =  mmeITTIFunctionBuilder.functionDeclaration.getParameters().get(2).asVariableDeclaration();
            CType mmeMessageDef = mmeMsg.getType();

            CFunctionDeclaration mmeMSGAlloc = mmeBuilder.cfgFunctionBuilderMap.get(ITTI_ALLOC_NEW_MESSAGE).functionDeclaration;
            CFunctionDeclaration s1ap_eNB_itti_send_nas_downlink_ind = enbBuilder.functionDeclarations.get("s1ap_eNB_itti_send_nas_downlink_ind");

            CFunctionDeclaration enbMSGAlloc = mmeBuilder.cfgFunctionBuilderMap.get(ITTI_ALLOC_NEW_MESSAGE).functionDeclaration;
        }
    }

    // call s1ap_eNB_itti_send_nas_downlink_ind in eNB
    //  params:
    //    instance_t instance,
    //    uint16_t ue_initial_id,
    //    uint32_t eNB_ue_s1ap_id,
    //    uint8_t *nas_pdu,
    //    uint32_t nas_pdu_length

    public static void buildSecureChannelInMME(CFGFunctionBuilder mmeITTIFunctionBuilder,
                                               CFABuilder mmeBuilder,
                                               CFABuilder enbBuilder,
                                               CFANode mmeS1APNpde,
                                               CVariableDeclaration mmeMsg,
                                               CType mmeMessageDef,
                                               CType enbMessageDef,
                                               CFunctionDeclaration s1ap_eNB_itti_send_nas_downlink_ind){
        FileLocation fileLocation = mmeS1APNpde.getLeavingEdge(0).getFileLocation();

        CFAEdge edge = mmeS1APNpde.getLeavingEdge(0);

        CFANode caseNextNode = edge.getSuccessor();
        CFAEdge breakEdge = caseNextNode.getLeavingEdge(0);
        caseNextNode.removeLeavingEdge(breakEdge);
        CFANode breakNode = breakEdge.getSuccessor();
        breakNode.removeEnteringEdge(breakEdge);

        List<CExpression> params = new ArrayList<>();
        params.add(CIntegerLiteralExpression.ZERO);

        CExpression mmeMSGExpr = new CIdExpression(fileLocation, mmeMsg);
        CType msg_t = mmeBuilder.typeConverter.typeCache.get("msg_t".hashCode());
        CFieldReference ittiMsg = new CFieldReference(fileLocation,msg_t,"ittiMsg",mmeMSGExpr,true);

        CType itti_nas_dl_data_req_t = mmeBuilder.typeConverter.typeCache.get("itti_nas_dl_data_req_t".hashCode());
        CFieldReference nas_dl_data_req = new CFieldReference(fileLocation,itti_nas_dl_data_req_t,"nas_dl_data_req", ittiMsg,false);

        CType mme_ue_s1ap_id_t = mmeBuilder.typeConverter.typeCache.get("mme_ue_s1ap_id_t".hashCode());
        CFieldReference ue_id = new CFieldReference(fileLocation,mme_ue_s1ap_id_t,"ue_id",nas_dl_data_req,false);
        CType uint16_t = enbBuilder.typeConverter.typeCache.get("uint16_t".hashCode());
        CCastExpression ue_id_cast = new CCastExpression(fileLocation,uint16_t,ue_id);

        CType enb_ue_s1ap_id_t = mmeBuilder.typeConverter.typeCache.get("enb_ue_s1ap_id_t".hashCode());
        CFieldReference enb_ue_s1ap_id = new CFieldReference(fileLocation,enb_ue_s1ap_id_t,"enb_ue_s1ap_id",nas_dl_data_req,false);
        CType uint32_t = enbBuilder.typeConverter.typeCache.get("uint32_t".hashCode());
        CCastExpression enb_ue_s1ap_id_cast = new CCastExpression(fileLocation,uint32_t,enb_ue_s1ap_id);

        CType bstring = mmeBuilder.typeConverter.typeCache.get("bstring".hashCode());
        CFieldReference nas_msg = new CFieldReference(fileLocation,bstring,"nas_msg",nas_dl_data_req,false);

//        CFANode msgDeclarationNode = mmeITTIFunctionBuilder.newCFANode();
//        CVariableDeclaration msgVarDec = new CVariableDeclaration(fileLocation,
//                false,
//                CStorageClass.AUTO,
//                enbMessageDef,
//                "enbMSG",
//                "enbMSG",
//                "enbMSG",
//                null
//        );
//        CDeclarationEdge declarationEdge = new CDeclarationEdge("MessageDef *enbMSG;",
//                fileLocation,caseNextNode,msgDeclarationNode,msgVarDec);
//        mmeITTIFunctionBuilder.addToCFA(declarationEdge);
//
//        String taskName = "TASK_S1AP";
//        String messageName = "S1AP_DOWNLINK_NAS";
//
//
//        BigInteger value = BigInteger.valueOf(getTaskOrMsgIDbyName(enbMSGAlloc.getParameters().get(0).getType(),taskName));
//        CExpression taskIDExpr = new CIntegerLiteralExpression(fileLocation,
//                enbMSGAlloc.getParameters().get(0).getType(),
//                value); //taskID,
//        params.add(taskIDExpr);
//        value = BigInteger.valueOf(getTaskOrMsgIDbyName(enbMSGAlloc.getParameters().get(1).getType(),messageName));
//        CExpression messageIDExpr = new CIntegerLiteralExpression(fileLocation,
//                enbMSGAlloc.getParameters().get(1).getType(),
//                value);; //messageID,
//
//        params.add(messageIDExpr);
//        CExpression functionName = new CIdExpression(fileLocation, enbMSGAlloc);
//
//        CFunctionCallExpression messageAllocation = new CFunctionCallExpression(fileLocation,
//                enbMSGAlloc.getType(),
//                functionName,
//                params,
//                enbMSGAlloc);
//
//        CExpression msgVarExpr = new CIdExpression(fileLocation,msgVarDec);
//        CFunctionCallAssignmentStatement assignmentStatement = new CFunctionCallAssignmentStatement(fileLocation,
//                (CLeftHandSide) msgVarExpr,
//                messageAllocation);
//        String rawString = "enbMSG= itti_alloc_new_message(TASK_S1AP, S1AP_DOWNLINK_NAS);";
//
//        CFANode msgAssignNode = mmeITTIFunctionBuilder.newCFANode();
//        CStatementEdge statementEdge = new CStatementEdge(rawString,
//                assignmentStatement,
//                fileLocation,
//                msgDeclarationNode,
//                msgAssignNode);
//        mmeITTIFunctionBuilder.addToCFA(statementEdge);


    }

    public static void buildSecureChannelInENB(CFGFunctionBuilder enbITTIFunctionBuilder,
                                               CFANode enbS1APNode,
                                               CVariableDeclaration enbMsg,
                                               CType enbMessageDef,
                                               CType mmeMessageDef){

    }

    public static CFANode findOutS1APConditionNode(String projectName, CFGFunctionBuilder builder){
        for(CFANode node:builder.cfaNodes){
            if(node.getLeavingEdge(0) instanceof CAssumeEdge && ((CAssumeEdge) node.getLeavingEdge(0)).getTruthAssumption()){
                CFAEdge edge = node.getLeavingEdge(0);
                CExpression conditionExpr = ((CAssumeEdge) edge).getExpression();
                if(conditionExpr instanceof CBinaryExpression){
                    int taskID = ((CIntegerLiteralExpression)((CBinaryExpression) conditionExpr).getOperand2()).getValue().intValue();
                    CType type = ((CBinaryExpression) conditionExpr).getOperand1().getExpressionType();
                    String task = getTaskOrMsgNameByID(type,taskID);
                    if(projectName.equals(ENB)){
                        task=task+"_ENB";
                        if(task.equals(ITTIAbstract.TASK_S1AP_ENB)){
                            return node;
                        }
                    }
                    else if(projectName.equals(MME)){
                        task=task+"_MME";
                        if(task.equals(ITTIAbstract.TASK_S1AP_ENB)){
                            return node;
                        }
                    }
                }
            }
        }

        return null;
    }
}
