/**
 * @ClassName CFGHandleExpression
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/12/19 5:27 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.nulist.plugin.parser.CFABuilder.pointerOf;
import static org.nulist.plugin.parser.CFGAST.*;
import static org.nulist.plugin.parser.CFGNode.*;
import static org.nulist.plugin.util.ClassTool.*;
import static org.nulist.plugin.util.FileOperations.readMCCMNCList;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.types.c.CVoidType.VOID;

public class CFGHandleExpression {
    private final CFGTypeConverter typeConverter;
    private final CBinaryExpressionBuilder binExprBuilder;
    public Map<Integer, CSimpleDeclaration> variableDeclarations;
    public Map<Integer, ADeclaration> globalDeclarations;
    private final String functionName;

    public CFGHandleExpression(LogManager pLogger,
                               String pFunctionName,
                               CFGTypeConverter typeConverter){
        this.typeConverter = typeConverter;
        this.variableDeclarations = new HashMap<>();
        this.globalDeclarations = new HashMap<>();
        functionName = pFunctionName;
        binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLogger);
    }

    public CFGHandleExpression(LogManager pLogger,
                               CFGTypeConverter typeConverter,
                               String pFunctionName,
                               Map<Integer, CSimpleDeclaration> variableDeclarations,
                               Map<Integer, ADeclaration> globalVariableDeclarations){
        this.typeConverter = typeConverter;
        this.variableDeclarations = variableDeclarations;
        this.globalDeclarations = globalVariableDeclarations;
        binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLogger);
        functionName = pFunctionName;
    }

    public void setGlobalVariableDeclarations(Map<Integer, ADeclaration> globalVariableDeclarations) {
        this.globalDeclarations = globalVariableDeclarations;
    }

    public void setVariableDeclarations(Map<Integer, CSimpleDeclaration> variableDeclarations) {
        this.variableDeclarations = variableDeclarations;
    }

    /**
     * @Description //get the binary operations, e.g., a!=b
     * @Param [condition, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression
     **/
    public CBinaryExpression getBinaryExpression(final ast condition, FileLocation fileLocation) throws result {
        // the only one supported now

        CBinaryExpression.BinaryOperator operator = getBinaryOperator(condition);

        ast variable_ast = condition.children().get(0).as_ast();
        CType op1Type = typeConverter.getCType(variable_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

        ast value_ast = condition.children().get(1).as_ast();
        CType op2Type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

        CCastExpression op1Cast = new CCastExpression(
                fileLocation,
                op1Type,
                getExpressionFromNO(variable_ast, op1Type, fileLocation));
        CCastExpression op2Cast = new CCastExpression(
                fileLocation,
                op2Type,
                getExpressionFromNO(value_ast, op2Type, fileLocation));
        CType resultType = typeConverter.getCType(condition.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);

        return buildBinaryExpression(op1Cast, op2Cast, operator, resultType);
    }

    public CBinaryExpression buildBinaryExpression(
            CExpression operand1, CExpression operand2, CBinaryExpression.BinaryOperator op, CType resultType) {
        try {
            return binExprBuilder.buildBinaryExpression(operand1, operand2, op);
        } catch (UnrecognizedCodeException e) {
            return  new CBinaryExpression(operand1.getFileLocation(), resultType, operand2.getExpressionType(), operand1, operand2, op);
            //throw new RuntimeException(e);
        }
    }

    /**
     * @Description Returns the id expression to an already declared variable. Returns it as a cast, if necessary
     * to match the expected type.
     * @Param [assignedVarDeclaration, pExpectedType, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getAssignedIdExpression(CSimpleDeclaration assignedVarDeclaration,
                                               final CType pExpectedType, final FileLocation fileLocation){

        String assignedVarName = assignedVarDeclaration.getName();

        CType expressionType = assignedVarDeclaration.getType();
        CIdExpression idExpression = new CIdExpression(fileLocation, expressionType, assignedVarName, assignedVarDeclaration);

        return idExpression;

//        if (expressionType.canBeAssignedFrom(pExpectedType)) {
//            return idExpression;
//        } else if (pointerOf(pExpectedType, expressionType)) {
//            CType typePointingTo = ((CPointerType) pExpectedType).getType().getCanonicalType();
//            if (expressionType.canBeAssignedFrom(typePointingTo)
//                    || expressionType.equals(typePointingTo)) {
//                return new CUnaryExpression(
//                        fileLocation, pExpectedType, idExpression, CUnaryExpression.UnaryOperator.AMPER);
//            } else {
//                throw new AssertionError("Unhandled type structure "+ assignedVarName+ " "+ expressionType.toString());
//            }
//        } else if (pExpectedType instanceof CPointerType) {
//            return new CPointerExpression(fileLocation, pExpectedType, idExpression);
//        }else {
//            throw new AssertionError("Unhandled types structure " + assignedVarName+ " "+ expressionType.toString());
//        }

    }

    /**
     * @Description Returns the id expression to an already declared variable.
     * @Param [variable_ast, pExpectedType, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getAssignedIdExpression(
            final ast variable_ast, final CType pExpectedType, final FileLocation fileLocation) throws result{
        //logger.log(Level.FINE, "Getting var declaration for point");
        //unnormalized
        symbol variableSymbol = variable_ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();

        String normalizedVarName = getNormalizedVariableName(variableSymbol, fileLocation.getFileName());

        boolean isGlobal = variableSymbol.is_global();
        boolean isFileStatic = variableSymbol.is_file_static();
        boolean isFunction = variableSymbol.is_function()||variableSymbol.is_static_function();
        CSimpleDeclaration assignedVarDeclaration;
        if(isGlobal || isFileStatic || isFunction){
            if(globalDeclarations.containsKey(normalizedVarName.hashCode())){
                if(isFunction)
                    assignedVarDeclaration = (CFunctionDeclaration) globalDeclarations.get(normalizedVarName.hashCode());
                else
                    assignedVarDeclaration = (CSimpleDeclaration) globalDeclarations.get(normalizedVarName.hashCode());
            }
            else{
                if(isFunction){
                    CFunctionType functionType;
                    CFunctionDeclaration functionDeclaration;
                    if(pExpectedType instanceof CTypedefType){
                        functionType = (CFunctionType) ((CTypedefType) pExpectedType).getRealType();
                        functionDeclaration = (CFunctionDeclaration)
                                generateVariableDeclaration(variableSymbol, functionType,
                                        true, null,  fileLocation);
                    }else if(pExpectedType instanceof CFunctionType){
                        functionType = (CFunctionType) pExpectedType;
                        functionDeclaration = generateFunctionDeclaration(variable_ast, functionType);
                    } else
                        throw new RuntimeException("Issue in the new function type "+ variable_ast.toString());

                    return new CIdExpression(fileLocation, pExpectedType, normalizedVarName, functionDeclaration);
                }else {
                    CVariableDeclaration variableDeclaration = (CVariableDeclaration)
                            generateVariableDeclaration(variableSymbol, pExpectedType,
                                    true, null, fileLocation);
                    CType expressionType = variableDeclaration.getType();
                    return new CIdExpression(fileLocation, expressionType, normalizedVarName, variableDeclaration);
                }
            }

                //throw new RuntimeException("Global variable has no declaration: " + normalizedVarName);
        }else {
            if(variableDeclarations.containsKey(normalizedVarName.hashCode()))
            {
                    assignedVarDeclaration =  variableDeclarations.get(normalizedVarName.hashCode());
            }
            else
            {
                CVariableDeclaration variableDeclaration = (CVariableDeclaration)
                        generateVariableDeclaration(variableSymbol, pExpectedType,
                                true, null, fileLocation);
                CType expressionType = variableDeclaration.getType();
                return new CIdExpression(fileLocation, expressionType, normalizedVarName, variableDeclaration);
            }
        }

        return getAssignedIdExpression(assignedVarDeclaration, pExpectedType, fileLocation);
    }


    /**
     * @Description //normalized variable name,
     * For example, for a variable 'a', CodeSurfer (CS) will the symbal named a-15. Here 15 is an id assigned by CS to distinguish with other variables
     * We the name a_15 to distinguish other variables that has the same name, e.g., a global variable 'a' and a temporal variable in a for loop
     * @Param [no_symbol, fileName]
     * @return java.lang.String
     **/
    public String getNormalizedVariableName(symbol no_symbol, String fileName)throws result{

        //symbol no_symbol = variable.primary_declaration().declared_symbol();

        String normalizedName;
        if(no_symbol.is_formal() ||
                no_symbol.is_global() || no_symbol.is_file_static() || no_symbol.is_local_static()||
                no_symbol.get_kind().equals(symbol_kind.getRETURN()))
            normalizedName = no_symbol.get_ast().pretty_print();
        else
            normalizedName = no_symbol.name().replace("-","_");

        if(no_symbol.is_file_static()){
            return "static__"+normalizedName;
        }else if(no_symbol.is_local_static()){
            return functionName+"__static__"+normalizedName;
        }else
            return normalizedName;
    }

    /**
     * @Description //get the file name without path
     * @Param [pFileNmae]
     * @return java.lang.String
     **/
    public String getSimpleFileName(String pFileNmae){
        return pFileNmae.substring(pFileNmae.lastIndexOf('/')+1).replace(".c","").replace("-","_");
    }

    /**
     * @Description //Return a null expression, C uses 0 as NULL
     * @Param [pLocation, pType]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getNull(final FileLocation pLocation, final CType pType) {
        return new CIntegerLiteralExpression(pLocation, pType, BigInteger.ZERO);
    }

    //functionassignstatement has been handled in function call
    public CStatement getAssignStatement(ast no_ast, FileLocation fileLocation)throws result{
        assert no_ast.get_class().equals(ast_class.getNC_NORMALASSIGN());

        ast left_ast = no_ast.children().get(0).as_ast();
        CType leftType = typeConverter.getCType(left_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        CLeftHandSide leftHandSide  = (CLeftHandSide) getExpressionFromNO(left_ast, leftType, fileLocation);

        ast value_ast = no_ast.children().get(1).as_ast();

        CType rightType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        CExpression rightHandSide = getExpressionFromNO(value_ast, rightType, fileLocation);

        return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
    }

    /**
     * @Description //using unnormalized node to generate the initializer
     * @Param [un_ast, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration
     **/
    public CDeclaration generateInitVarDeclFromUC(ast un_ast, FileLocation fileLocation) throws result{
        assert un_ast.get_class().is_subclass_of(ast_class.getUC_INIT());

        ast init = un_ast.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
        ast variable = init.get(ast_ordinal.getUC_VARIABLE()).as_ast();
        symbol varSymbol = variable.get(ast_ordinal.getUC_ABS_LOC()).as_symbol();
        CType cType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        if(typeConverter.isFunctionPointerType(cType)){
            cType = typeConverter.convertCFuntionType(cType,varSymbol.name(), fileLocation);
        }

        CInitializer initializer = getInitializerFromUC(init, cType, fileLocation);

        return generateVariableDeclaration(
                varSymbol.primary_declaration().declared_symbol(), cType, false,
                initializer,fileLocation);
    }
    //this is a specific generateInitVarDeclFromUC for these init using temp variable
    public CDeclaration generateInitVarDecl(ast un_ast, ast no_ast, FileLocation fileLocation) throws result{
        assert un_ast.get_class().is_subclass_of(ast_class.getUC_INIT()) ;

        ast init = un_ast.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
        ast variable = init.get(ast_ordinal.getUC_VARIABLE()).as_ast();
        symbol varSymbol = variable.get(ast_ordinal.getUC_ABS_LOC()).as_symbol();
        CType cType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        if(typeConverter.isFunctionPointerType(cType)){
            cType = typeConverter.convertCFuntionType(cType,varSymbol.name(), fileLocation);
        }

        CExpression expression = getExpressionWithTempVar(no_ast.children().get(1).as_ast(),
                init.get(ast_ordinal.getUC_EXPR()).as_ast(),fileLocation);

        CInitializer initializer = new CInitializerExpression(fileLocation, expression);

        return generateVariableDeclaration(
                varSymbol.primary_declaration().declared_symbol(), cType, false,
                initializer,fileLocation);
    }


    /**
     *@Description CodeSurfer splits declaration and initialization, e.g., int i=0;-->int i; i=0;
     *             We need to combine the declaration and initialization.
     *@Param [variable]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration
     **/
    public CDeclaration generateVariableDeclaration(symbol variable,
                                                            CType cType, boolean isGlobal,
                                                            CInitializer initializer,
                                                            FileLocation fileLocation)throws result{
        String assignedVar =getNormalizedVariableName(variable,fileLocation.getFileName());

        //if(variable.is_local() || variable.is_local_static())

        //if(variable.is_global() || variable.is_file_static())
        if(isGlobal)
            if(globalDeclarations.containsKey(assignedVar.hashCode()))
                return (CVariableDeclaration) globalDeclarations.get(assignedVar.hashCode());
        else
            if(variableDeclarations.containsKey(assignedVar.hashCode()))
                return (CVariableDeclaration) variableDeclarations.get(assignedVar.hashCode());

        if(variable.get_kind().equals(symbol_kind.getRETURN()) ||
                variable.get_kind().equals(symbol_kind.getUSER()) ||
                variable.get_kind().equals(symbol_kind.getRESULT())){
            CStorageClass storageClass;
            if(variable.get_kind().equals(symbol_kind.getRETURN()))
                storageClass = CStorageClass.AUTO;
            else
                storageClass = getStorageClass(variable.get_ast());

            String normalizedName = assignedVar;
            String originalName = variable.get_ast().pretty_print();
            if(storageClass==CStorageClass.STATIC){
                storageClass = CStorageClass.AUTO;
                //normalizedName = functionName+"__static__"+assignedVar;
            }


            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            variable.is_global(),
                            storageClass,
                            cType,
                            normalizedName,
                            originalName,
                            normalizedName,
                            initializer);
            variableDeclarations.put(normalizedName.hashCode(),newVarDecl);
            if(isGlobal)
                globalDeclarations.put(normalizedName.hashCode(),newVarDecl);
            else
                variableDeclarations.put(normalizedName.hashCode(),newVarDecl);
//            if(variable.is_local() || variable.is_local_static())
//                variableDeclarations.put(normalizedName.hashCode(),newVarDecl);
//            if(variable.is_global() || variable.is_file_static())
//                globalDeclarations.put(normalizedName.hashCode(),newVarDecl);
            return newVarDecl;
        }else if(variable.get_kind().equals(symbol_kind.getFUNCTION())){
            ast functionAST = variable.get_ast(ast_family.getC_UNNORMALIZED());
            return generateFunctionDeclaration(variable.get_ast(ast_family.getC_UNNORMALIZED()),(CFunctionType)cType);
        }else
            throw new RuntimeException("Incorrectly calling generateVariableDeclaration from "+ variable.get_kind()+" "+variable.as_string());
    }

    public CFunctionDeclaration generateFunctionDeclaration(ast function, CFunctionType functionType)throws result{
        try {
            ast functionScope;
            if(function.has_field(ast_ordinal.getUC_SCOPE()))
                functionScope = function.get(ast_ordinal.getUC_SCOPE()).as_ast();
            else
                functionScope = function.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol()
                        .get_ast(ast_family.getC_UNNORMALIZED())
                        .get(ast_ordinal.getUC_SCOPE()).as_ast();
            String functionName = function.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name();
            List<CParameterDeclaration> parameters = new ArrayList<>(functionType.getParameters().size());
            if(!functionType.getParameters().isEmpty()){
                ast params = functionScope.get(ast_ordinal.getUC_PARAMETERS()).as_ast();
                for(int i=0;i<params.children().size();i++){
                    ast param = params.children().get(i).as_ast();
                    String paramName = param.pretty_print();//param_point.parameter_symbols().get(0).get_ast().pretty_print();

                    CType paramType = typeConverter.getCType(param.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

                    CParameterDeclaration parameter =
                            new CParameterDeclaration(FileLocation.DUMMY,paramType,paramName);

                    parameter.setQualifiedName(paramName);
                    if(!paramName.equals("__builtin_va_alist"))
                        parameters.add(parameter);
                }
            }
            CFunctionTypeWithNames functionTypeWithNames = new CFunctionTypeWithNames(
                    checkNotNull(functionType.getReturnType()),
                    parameters,
                    functionType.takesVarArgs());
            functionTypeWithNames.setName(functionName);
            CFunctionDeclaration functionDeclaration =
                    new CFunctionDeclaration(FileLocation.DUMMY,functionTypeWithNames,functionName,parameters);
            globalDeclarations.put(functionName.hashCode(), functionDeclaration);
            return functionDeclaration;
        }catch (result r){
            String functionName = function.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name();

            List<CParameterDeclaration> parameters = new ArrayList<>(functionType.getParameters().size());
            if(!functionType.getParameters().isEmpty()){

                for(int i=0;i<functionType.getParameters().size();i++){
                    CType paramType = functionType.getParameters().get(i);

                    CParameterDeclaration parameter =
                            new CParameterDeclaration(FileLocation.DUMMY,paramType,"");
                    parameters.add(parameter);
                }
            }
            CFunctionTypeWithNames functionTypeWithNames = new CFunctionTypeWithNames(
                    checkNotNull(functionType.getReturnType()),
                    parameters,
                    functionType.takesVarArgs());
            functionTypeWithNames.setName(functionName);
            CFunctionDeclaration functionDeclaration =
                    new CFunctionDeclaration(FileLocation.DUMMY,functionTypeWithNames,functionName,parameters);
            printWARNING("A function has no scope: "+ functionName+":"+ functionDeclaration.toString());
            globalDeclarations.put(functionName.hashCode(), functionDeclaration);
            return functionDeclaration;
        }
    }


    /**
     * @Description //using unnormalized node to generate assign statement
     * @Param [un_ast, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CStatement
     **/
    public CStatement getAssignStatementFromUC(ast un_ast, FileLocation fileLocation)throws result{
        assert un_ast.is_a(ast_class.getUC_ABSTRACT_OPERATION());
        CLeftHandSide leftHandSide = null;
        CExpression rightHandSide = null;
        if(!un_ast.has_field(ast_ordinal.getUC_OPERANDS()))
            dumpAST(un_ast,0,un_ast.get_class().name());
        CType type = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        ast oper1 = operands.children().get(0).as_ast();
        CType leftType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        if(typeConverter.isFunctionPointerType(leftType)){
            leftType = typeConverter.convertCFuntionType(leftType,"",fileLocation);
        }

        CExpression variable = getExpressionFromUC(oper1, leftType, fileLocation);

        CExpression value = null;
        if(operands.children().size()==2){
            ast oper2 = operands.children().get(1).as_ast();
            CType rightType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            value = getExpressionFromUC(oper2, rightType, fileLocation);
        }

        if(un_ast.is_a(ast_class.getUC_GENERIC_ASSIGN())){//a=b+1, a=4
            if(operands.children().size()==2 && value!=null){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide =  value;
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_PRE_DECR())){//--a;
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.MINUS, type);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_PRE_INCR())){//++a
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.PLUS, type);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_POST_DECR())){//a--
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.MINUS, type);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_POST_INCR())){//a++
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.PLUS, type);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_CAST())){
            if(operands.children().size()==1){
                return new CExpressionStatement(fileLocation, variable);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_INDIRECT())){
            throw new RuntimeException("Issue in getAssignStatementFromUC with "+ functionName+ " "+ fileLocation.getStartingLineNumber());
        } else {
            CBinaryExpression.BinaryOperator operator = getBinaryOperatorFromUC(un_ast);
            if(operands.children().size()==2 && value!=null && operator!=null){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, value, operator, type);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                dumpAST(un_ast,0,un_ast.toString());
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }

    }

    public boolean isUnaryOperation(ast operator)throws result{
        if(operator.is_a(ast_class.getUC_ABSTRACT_POST_DECR())||
                operator.is_a(ast_class.getUC_ABSTRACT_PRE_DECR())||
                operator.is_a(ast_class.getUC_ABSTRACT_POST_INCR())||
                operator.is_a(ast_class.getUC_ABSTRACT_PRE_INCR()))
            return true;
        return false;
    }

    public CStatement getAssignStatement(point expr, FileLocation fileLocation)throws result{
        ast un_ast = expr.get_ast(ast_family.getC_UNNORMALIZED());
        ast no_ast = expr.get_ast(ast_family.getC_NORMALIZED());

        CLeftHandSide leftHandSide = null;
        CExpression rightHandSide = null;

        CType type = typeConverter.getCType(no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        int location = tempvarusedLocation(no_ast);
        ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        ast oper1 = operands.children().get(0).as_ast();
        ast oper2 = operands.children().get(1).as_ast();
        ast no_oper1 = no_ast.children().get(0).as_ast();
        ast no_oper2 = no_ast.children().get(1).as_ast();

        if(no_ast.is_a(ast_class.getNC_BLOCKASSIGN())){//un_ast.is_a(ast_class.getUC_BASSIGN())
            CStatement statement = getAssignStatementFromUC(un_ast,fileLocation);
            CExpression tempLeft = ((CExpressionAssignmentStatement)statement).getLeftHandSide();
            CExpression indexExpr = getTempVarIDExpression(no_oper1, fileLocation);
            if(tempLeft instanceof CArraySubscriptExpression){
                CArraySubscriptExpression cArraySubscriptExpression = new CArraySubscriptExpression(
                        fileLocation,
                        tempLeft.getExpressionType(),
                        ((CArraySubscriptExpression) tempLeft).getArrayExpression(),
                        indexExpr);
                leftHandSide  = cArraySubscriptExpression;
            }else {
                throw new RuntimeException("Block assign with temp:"+ tempLeft.getClass());
            }
            return new CExpressionAssignmentStatement(fileLocation, leftHandSide,
                    ((CExpressionAssignmentStatement) statement).getRightHandSide());
        }else if(!un_ast.is_a(ast_class.getUC_GENERIC_ASSIGN())){
            //ast no_oper21 = no_oper2.children().get(0).as_ast();
            //ast no_oper22 = no_oper2.children().get(1).as_ast();
            switch (location){
                case 2://two sides use temp vars
                    leftHandSide = (CLeftHandSide) getExpressionWithTempVar(no_oper1, oper1, fileLocation);
                    rightHandSide = getExpressionWithTempVar(no_oper2, un_ast, fileLocation);
                    break;
                case 1://right side use temp var

                    CType leftType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
                    leftHandSide = (CLeftHandSide) getExpressionFromUC(oper1, leftType, fileLocation);
                    rightHandSide = getExpressionWithTempVar(no_oper2, un_ast, fileLocation);
                    break;
                case 0://left side use temp var
                    leftHandSide = (CLeftHandSide) getExpressionWithTempVar(no_oper1, oper1, fileLocation);
                    CType rightType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
                    rightHandSide = getExpressionFromUC(un_ast,rightType,fileLocation);
                    break;
            }
        }else {

            CType leftType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            leftHandSide = (CLeftHandSide) getExpression(oper1, leftType, no_oper1, fileLocation);
            CType rightType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            rightHandSide = getExpression(oper2,rightType,no_oper2, fileLocation);
//            switch (location){
//                case 2://two sides use temp vars
//                    leftHandSide = (CLeftHandSide) getExpressionWithTempVar(no_oper1, oper1, fileLocation);
//                    rightHandSide = getExpressionWithTempVar(no_oper2, oper2, fileLocation);
//                    break;
//                case 1://right side use temp var
//
//                    CType leftType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
//                    leftHandSide = (CLeftHandSide) getExpressionFromUC(oper1, leftType, fileLocation);
//                    rightHandSide = getExpressionWithTempVar(no_oper2, oper2, fileLocation);
//                    break;
//                case 0://left side use temp var
//                    leftHandSide = (CLeftHandSide) getExpressionWithTempVar(no_oper1, oper1, fileLocation);
//                    CType rightType = typeConverter.getCType(oper2.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
//                    rightHandSide = getExpressionFromUC(oper2,rightType,fileLocation);
//                    break;
//            }
        }
        return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
    }

    public CExpression getTempVarIDExpression(ast no_expr, FileLocation fileLocation){
        try {
            if(no_expr.is_a(ast_class.getNC_VARIABLE())&& no_expr.pretty_print().startsWith("$temp")){
                CVariableDeclaration variableDeclaration =
                        (CVariableDeclaration) variableDeclarations.get(no_expr.pretty_print().hashCode());
                return getAssignedIdExpression(variableDeclaration, variableDeclaration.getType(), fileLocation);
            }else {
                if(!no_expr.children().isEmpty()){
                    for(int i=0;i<no_expr.children().size();i++){
                        if(no_expr.children().get(i).as_ast().pretty_print().contains("$temp"))
                            return getTempVarIDExpression(no_expr.children().get(i).as_ast(), fileLocation);
                    }
                }
            }
            return null;
        }catch (result r){
            throw new RuntimeException("There is no temp var in the expr: "+ no_expr.toString());
        }
    }

    public CExpression getExpression(ast un_expr, CType unType, ast no_expr, FileLocation fileLocation)throws result{
        if(!no_expr.pretty_print().contains("$temp")){
            return getExpressionFromUC(un_expr,unType,fileLocation);
        }else {
            return getExpressionWithTempVar(no_expr,un_expr,fileLocation);
        }
    }

    //for example, structvar[a++]= structvarb ==>
    // $temp1 = a++, structvar[$temp1].memb1 = structvarb.memb1, structvar[$temp1].memb2 = structvarb.memb2....
    //output: structvar[$temp1] = structvarb
    //TODO get type from noramlized
    public CExpression getExpressionWithTempVar(ast no_expr, ast un_expr, FileLocation fileLocation) throws  result{
        CType type = typeConverter.getCType(un_expr.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        if(no_expr.is_a(ast_class.getNC_VARIABLE())){
            CVariableDeclaration variableDeclaration = (CVariableDeclaration)
                    variableDeclarations.get(no_expr.pretty_print().hashCode());
            return getAssignedIdExpression(variableDeclaration, null, fileLocation);
        }else if(un_expr.is_a(ast_class.getUC_LAND())||un_expr.is_a(ast_class.getUC_LOR())){
            return getExpressionFromNO(no_expr,type,fileLocation);
        }else if(no_expr.is_a(ast_class.getNC_POINTEREXPR())){
            if(no_expr.pretty_print().startsWith("*$temp")){
                CVariableDeclaration variableDeclaration = (CVariableDeclaration)
                        variableDeclarations.get(no_expr.pretty_print().replace("*","").hashCode());
                CExpression expression= getAssignedIdExpression(variableDeclaration, null, fileLocation);
                return new CPointerExpression(fileLocation, type, expression);
            }else if(un_expr.is_a(ast_class.getUC_SUBSCRIPT())) {
                CExpression tempVarExpr = getTempVarIDExpression(no_expr,fileLocation);
                CExpression arraySubscript = getExpressionFromUC(un_expr,type, fileLocation);
                if(arraySubscript instanceof CArraySubscriptExpression){
                    return new CArraySubscriptExpression(
                            fileLocation,
                            arraySubscript.getExpressionType(),
                            ((CArraySubscriptExpression) arraySubscript).getArrayExpression(),
                            tempVarExpr);
                }else {//may have some problem
                    throw new RuntimeException("Block assign with temp:"+ arraySubscript.getClass());
                }
            }else if(no_expr.children().get(0).as_ast().is_a(ast_class.getNC_ABSTRACT_ARITHMETIC())){
                ast operand = no_expr.children().get(0).as_ast();
                ast operands = un_expr.get(ast_ordinal.getUC_OPERANDS()).as_ast()
                        .children().get(0).get(ast_ordinal.getUC_OPERANDS()).as_ast();
                CBinaryExpression.BinaryOperator operator = getBinaryOperator(operand);

                CExpression exper1 = getExpression(operands.children().get(0).as_ast(),
                        type,
                        operand.children().get(0).as_ast(),
                        fileLocation);

                CExpression exper2 = getExpression(operands.children().get(1).as_ast(),
                        type,
                        operand.children().get(1).as_ast(),
                        fileLocation);
//

                CCastExpression op1Cast = new CCastExpression(
                        fileLocation,
                        type,
                        exper1);
                CCastExpression op2Cast = new CCastExpression(
                        fileLocation,
                        type,
                        exper2);
                CExpression expression = buildBinaryExpression(op1Cast,op2Cast,operator, type);
                return  new CPointerExpression(fileLocation, type,expression);
            }else if(no_expr.children().get(0).as_ast().is_a(ast_class.getNC_CASTEXPR())){
                ast cast = no_expr.children().get(0).as_ast();
                ast uncast = un_expr.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();
                CType castType = typeConverter.getCType(uncast.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
                CExpression expression= getExpression(uncast, castType, cast, fileLocation);
                return new CPointerExpression(fileLocation, type,expression);
            }else {
                CExpression expression = getExpression(un_expr, type, no_expr.children().get(0).as_ast(), fileLocation);//TODO
                return  new CPointerExpression(fileLocation, type,expression);
            }
        }else if(no_expr.is_a(ast_class.getNC_ADDREXPR())){
            assert un_expr.is_a(ast_class.getUC_ADDRESS_OP());

            ast variable = no_expr.children().get(0).as_ast();
            ast operands = un_expr.get(ast_ordinal.getUC_OPERANDS()).as_ast();

            if(isVariable(variable)){//this variable shall be the temp
                if(variable.pretty_print().startsWith("$temp")){
                    CExpression tempVar = getTempVarIDExpression(no_expr,fileLocation);
                    return new CUnaryExpression(fileLocation, type, tempVar, CUnaryExpression.UnaryOperator.AMPER);
                }else {
                    throw new RuntimeException("Not support variable: "+ variable.toString());
                }
            } else if(variable.is_a(ast_class.getNC_POINTEREXPR())){
                CExpression operand = getExpressionWithTempVar(variable,operands.children().get(0).as_ast(),fileLocation);
                return new CUnaryExpression(fileLocation, type, operand, CUnaryExpression.UnaryOperator.AMPER);
            }else if(variable.is_a(ast_class.getNC_ARRAYREF())){//TODO
                ast pointedto = variable.children().get(0).as_ast();
                ast un_variable = operands.children().get(0).as_ast();
                ast suboperands = un_variable.get(ast_ordinal.getUC_OPERANDS()).as_ast();
                if(un_expr.is_a(ast_class.getUC_ARRAY_TO_POINTER_DECAY())){
                    //arraysubcript, d[index]
                    ast index = variable.children().get(1).as_ast();

                    ast array = suboperands.children().get(0).as_ast();

                    CType arrayType = typeConverter.getCType(array.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
                    CExpression arrayExpression = getExpression(array, arrayType, pointedto, fileLocation);

                    ast un_index = suboperands.children().get(1).as_ast();
                    CType indexType = typeConverter.getCType(un_index.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
                    CExpression indexExpression = getExpression(un_index,indexType,index,fileLocation);

                    return new CArraySubscriptExpression(fileLocation, type,arrayExpression, indexExpression);
                }else {
                    //unary &

                    ast pointer = suboperands.children().get(0).as_ast();
                    CType pointerType = typeConverter.getCType(pointer.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
                    CExpression variableExpression = getExpression(pointer,pointerType,pointedto,fileLocation);

                    return new CUnaryExpression(fileLocation, type, variableExpression, CUnaryExpression.UnaryOperator.AMPER);
                }
                    //throw new RuntimeException("Not support array ref: "+ variable.toString());
            } else
                throw new RuntimeException("Not support: "+ variable.toString());

        }else if(no_expr.is_a(ast_class.getNC_ABSTRACT_BITWISE()) ||
                no_expr.is_a(ast_class.getNC_ABSTRACT_ARITHMETIC()) ||
                no_expr.is_a(ast_class.getNC_ABSTRACT_LOGICAL())) {
            ast left = no_expr.children().get(0).as_ast();
            ast right = no_expr.children().get(1).as_ast();
            ast operands = un_expr.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            if(un_expr.is_a(ast_class.getUC_GENERIC_CAST())){
                operands = operands.children().get(0).get(ast_ordinal.getUC_OPERANDS()).as_ast();
            }
            ast leftOper = operands.children().get(0).as_ast();
            CBinaryExpression.BinaryOperator operator = getBinaryOperator(no_expr);
            CType typeLeft = typeConverter.getCType(leftOper.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
            CExpression leftExpession = getExpression(leftOper, typeLeft, left, fileLocation);

            if(operands.children().size()==1){
                return buildBinaryExpression(leftExpession,CIntegerLiteralExpression.ONE, operator, type);
            }
            ast rightOper = operands.children().get(1).as_ast();
            CType typeRight = typeConverter.getCType(rightOper.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
            CExpression rightExpression = getExpression(rightOper, typeRight,right,fileLocation);

            CBinaryExpression binaryExpression =  buildBinaryExpression(leftExpession,rightExpression, operator, type);
            if(un_expr.is_a(ast_class.getUC_GENERIC_CAST()))
                return new CCastExpression(fileLocation,type,binaryExpression);
            else
                return binaryExpression;
        }else if(no_expr.is_a(ast_class.getNC_STRUCTORUNIONREF())){
            ast operands = un_expr.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast dotfield = operands.children().get(0).as_ast();

            ast variable = dotfield.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();//EXPR_VARIABLE. variable

            ast member = dotfield.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast();//UC_EXPR_FIELD, field
            String fieldName = member.get(ast_ordinal.getUC_FIELD()).as_ast().pretty_print();
            CType memberType = typeConverter.getCType(member.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CType varType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CExpression variableExpr = getExpressionWithTempVar(no_expr.children().get(0).as_ast(),
                    variable, fileLocation);

            return new CFieldReference(fileLocation, memberType, fieldName, variableExpr,
                    dotfield.is_a(ast_class.getUC_POINTS_TO_FIELD())
                            ||dotfield.is_a(ast_class.getUC_POINTS_TO_STATIC()));
        }else if(no_expr.is_a(ast_class.getNC_CASTEXPR())){
            ast variable = no_expr.children().get(1).as_ast();
            ast unvariable = un_expr.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();
            CType varTYpe = typeConverter.getCType(unvariable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression expression = getExpression(unvariable, varTYpe, variable, fileLocation);
            return new CCastExpression(fileLocation, type,expression);
        }else {
            throw new RuntimeException("Not support "+ no_expr.get_class().name()+" in "+ no_expr.toString());
            //return getExpressionFromNO(no_expr, type, fileLocation);//TODO
        }
        //throw new RuntimeException("Not support "+ no_expr.get_class().name()+" in "+ no_expr.toString());
    }


    //normalized node
    /**
     * @Description //This a function to get expression from CodeSurfer expression ast
     * This is normalized version, unnormalized version see getExpressionFromUC
     * @Param [value_ast, valueType, fileLoc]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getExpressionFromNO(ast value_ast, CType valueType,FileLocation fileLoc)throws result{

        if(isVariable(value_ast)){//e.g., a
            CSimpleDeclaration variableDeclaration = variableDeclarations.get(value_ast.pretty_print().hashCode());
            return getAssignedIdExpression(variableDeclaration, valueType, fileLoc);
        }else if(isValue(value_ast)){//a=2;

            CBasicType basicType = ((CSimpleType) valueType).getType();
            if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
                //BigDecimal.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_flt32());
                BigDecimal value = getFloatValue(value_ast);
                return new CFloatLiteralExpression(fileLoc, valueType,value);
            }else if (basicType == CBasicType.BOOL) {
                long value = value_ast.get(ast_ordinal.getBASE_VALUE()).as_uint32();
                assert value==0||value==1;
                if(value==0)
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ZERO);//false
                else
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ONE);//true
            }else if(basicType == CBasicType.INT){
                BigInteger value = getIntegerValue(value_ast);
                if(value==null)
                    throw new RuntimeException("Problem in getting value of "+ value_ast.toString());
                return new CIntegerLiteralExpression(fileLoc,valueType,value);
            }else {
                throw  new RuntimeException("Unsupported type "+valueType.toString());
            }
        }else if(value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_ARITHMETIC()) ||//e.g., int i = a+1;
                value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_BITWISE())){// int i =a&1;
            return createFromArithmeticOp(value_ast,fileLoc);
        }else if(isStructElementExpr(value_ast)){//struct element, e.g., int p = astruct.a;

            ast variable = value_ast.children().get(0).as_ast();

            CType varType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression variableExpr = getExpressionFromNO(variable,varType, fileLoc);
            String fieldName = value_ast.children().get(1).get(ast_ordinal.getBASE_NAME()).as_str();

            return new CFieldReference(fileLoc, valueType, fieldName, variableExpr,
                    valueType instanceof CPointerType);

        }else if(isPointerAddressExpr(value_ast)){//pointer address, e.g., char p[30]="say hello", *p1 = &r;
            return getPointerAddrExpr(value_ast, fileLoc);
        }else if(isZeroInitExpr(value_ast)){//zero initialization, e.g., char *p=NULL, p1[30]={} (aggreate);
                                            // castexpr
                                            // for an array, child 0 *&p[0], child 1 (int[30])0
                                            // for a pointer, child 0: p, child 1: (void*)0
            CType castType = typeConverter.getCType(value_ast.get(ast_ordinal.getNC_TYPE()).as_ast(), this);
            ast operandAST = value_ast.children().get(1).as_ast();
            CType operandType = typeConverter.getCType(operandAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression operand = getExpressionFromNO(operandAST,operandType, fileLoc);
            return new CCastExpression(fileLoc, castType, operand);
        }else if(isPointerExpr(value_ast)){//pointer, e.g., int i = *(p+1);
            CType type  = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            ast variable = value_ast.children().get(0).as_ast();
            CExpression operand = getExpressionFromNO(variable, type, fileLoc);
            return new CPointerExpression(fileLoc, valueType, operand);
        }else if(value_ast.is_a(ast_class.getNC_CASTEXPR())){

        }
            throw new RuntimeException("Not support this expr: "+value_ast.toString());
    }


    /**
     *@Description get the expression from unnormalized ast
     *@Param [value_ast, valueType, fileLocation]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getExpressionFromUC(ast value_ast, CType valueType,FileLocation fileLoc)throws result{
        if(value_ast.is_a(ast_class.getUC_EXPR_VARIABLE())){//a

            ast variable = value_ast.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            //CType type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getExpressionFromUC(variable,valueType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_VARIABLE())){//a

            //CType type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getAssignedIdExpression(value_ast, valueType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_PARAMETER())){//param

            ast param = value_ast.get(ast_ordinal.getUC_PARAMETER()).as_ast();//
            CType paramType = typeConverter.getCType(param.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            return getAssignedIdExpression(param, paramType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_EXPR_CONSTANT())){//1, true

            ast constant = value_ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            return getConstantFromUC(constant, valueType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_ABSTRACT_DOT_EXPR())){
            //ast_class.getUC_POINTS_TO_FIELD())//r->member, r is pointer struct
            //ast_class.getUC_DOT_FIELD())////r.member
            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = operands.children().get(0).as_ast();//EXPR_VARIABLE. variable
            ast member = operands.children().get(1).as_ast();//UC_EXPR_FIELD, field
            String fieldName = member.get(ast_ordinal.getUC_FIELD()).as_ast().pretty_print();
            CType memberType = typeConverter.getCType(member.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CType varType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression variableExpr = getExpressionFromUC(variable,varType, fileLoc);
            return new CFieldReference(fileLoc, memberType, fieldName, variableExpr,
                    value_ast.is_a(ast_class.getUC_POINTS_TO_FIELD())
                    ||value_ast.is_a(ast_class.getUC_POINTS_TO_STATIC()));

        }else if(value_ast.is_a(ast_class.getUC_GENERIC_CAST())){//a+b, &a, operands

            ast cast = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();

            ast value = cast.children().get(0).as_ast();
            CType castType = typeConverter.getCType(value.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CExpression castExpr = getExpressionFromUC(value, castType, fileLoc);

            return new CCastExpression(fileLoc, valueType, castExpr);

        }else if(value_ast.is_a(ast_class.getUC_BOOL_CAST())){
            ast cast = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();

            ast value = cast.children().get(0).as_ast();
            CType castType = typeConverter.getCType(value.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CExpression castExpr = getExpressionFromUC(value, castType, fileLoc);

            return new CCastExpression(fileLoc, valueType, castExpr);
        }else if(value_ast.is_a(ast_class.getUC_EXPR_FIELD())){

            ast field = value_ast.get(ast_ordinal.getUC_FIELD()).as_ast();//UC_FIELD
            CType cType = typeConverter.getCType(field.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            return getExpressionFromUC(field, cType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_INDIRECT())){//*p, operands

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = operands.children().get(0).as_ast();
            CType cType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression operand = getExpressionFromUC(variable, cType, fileLoc);
            if(typeConverter.isFunctionPointerType(valueType)){
                //String functionName =getFunctionName(operand);
                CType funcType  = typeConverter.getFuntionTypeFromFunctionPointer(operand.getExpressionType());
                if(funcType instanceof CTypedefType){
                    funcType = ((CTypedefType) funcType).getRealType();
                }
                return new CPointerExpression(fileLoc, funcType, operand);
            }else
                return new CPointerExpression(fileLoc, valueType, operand);

        }else if(value_ast.is_a(ast_class.getUC_SUBSCRIPT())){//d[1], d[1][2] operands
            ast operand = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast array_ast = operand.children().get(0).as_ast();
            ast index_ast = operand.children().get(1).as_ast();

            CType arrayType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression arrayExpr = getExpressionFromUC(array_ast, arrayType, fileLoc);

            CType indexType = typeConverter.getCType(index_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CExpression subscriptExpr= getExpressionFromUC(index_ast,indexType, fileLoc);
            return new CArraySubscriptExpression(fileLoc, arrayType, arrayExpr, subscriptExpr);

        }else if(value_ast.is_a(ast_class.getUC_PADD())){//function$return=*(p+2), operands //TODO

            CBinaryExpression.BinaryOperator operator = CBinaryExpression.BinaryOperator.PLUS;
            ast operand = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast pointer = operand.children().get(0).as_ast();
            CExpression pointerExpr = getExpressionFromUC(pointer, valueType, fileLoc);
            ast index = operand.children().get(1).as_ast();
            CType indexType = typeConverter.getCType(index.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression indexExpr = getExpressionFromUC(index, indexType, fileLoc);
            return buildBinaryExpression(pointerExpr, indexExpr, operator, valueType);
            //return new CPointerExpression(fileLoc, valueType, binaryExpression);

        }else if(value_ast.is_a(ast_class.getUC_ARRAY_TO_POINTER_DECAY())){//d[1], d[1][2]

            ast operand = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();

            CType type  = typeConverter.getCType(operand.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
            CExpression expression = getExpressionFromUC(operand, type,fileLoc);
            if(expression instanceof CStringLiteralExpression)
                return expression;
            CType decay = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            CArraySubscriptExpression arraySubscriptExpression =
                    new CArraySubscriptExpression(fileLoc, decay, expression, CIntegerLiteralExpression.ZERO);
            return new CUnaryExpression(fileLoc, valueType,
                    arraySubscriptExpression, CUnaryExpression.UnaryOperator.AMPER);

        }else if(value_ast.is_a(ast_class.getUC_EXPR_ROUTINE())){//function$return=function1$result, routine

            ast routine = value_ast.get(ast_ordinal.getUC_ROUTINE()).as_ast();
            CType type = typeConverter.getCType(routine.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            return getAssignedIdExpression(routine,type,fileLoc);

//            String functionName = routine.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name();
//            if(type instanceof CTypedefType){
//                CFunctionType functionType = (CFunctionType) ((CTypedefType)type).getRealType();
//                CFunctionTypeWithNames cFunctionTypeWithNames =
//                        typeConverter.convertCFuntionType(functionType, functionName, fileLoc);
//                type = new CTypedefType(type.isConst(),type.isVolatile(),
//                        ((CTypedefType) type).getName(),cFunctionTypeWithNames);
//                return getAssignedIdExpression(routine,type,fileLoc);
//            }else {
//
//                CFunctionTypeWithNames cFunctionTypeWithNames =
//                        typeConverter.convertCFuntionType((CFunctionType) type, functionName, fileLoc);
//                return getAssignedIdExpression(routine,cFunctionTypeWithNames,fileLoc);
//            }
        }else if(value_ast.is_a(ast_class.getUC_ADDRESS_OP())){// &d;

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = operands.children().get(0).as_ast();
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression operand;
            //a function
            if(isFunction(variable)){
                ast routine = variable.get(ast_ordinal.getUC_ROUTINE()).as_ast();
                CFunctionTypeWithNames cFunctionTypeWithNames =
                        typeConverter.convertCFuntionType((CFunctionType) variableType, "", fileLoc);
                operand = getAssignedIdExpression(routine, cFunctionTypeWithNames, fileLoc);
            }else
                operand = getExpressionFromUC(variable, variableType, fileLoc);

            return new CUnaryExpression(fileLoc, valueType, operand, CUnaryExpression.UnaryOperator.AMPER);

        }else if(value_ast.is_a(ast_class.getUC_FUNCTION_CALL())){

            String functionName = getFunctionCallResultName(value_ast);
            if(variableDeclarations.containsKey(functionName.hashCode()))
                return new CIdExpression(fileLoc, variableDeclarations.get(functionName.hashCode()));
            else if(globalDeclarations.containsKey(functionName.hashCode())){
                return new CIdExpression(fileLoc, (CSimpleDeclaration) globalDeclarations.get(functionName.hashCode()));
            }else
                throw new RuntimeException("No existing function call result: "+ functionName);
        }else if(value_ast.is_a(ast_class.getUC_ABSTRACT_NEGATE())||//negate
                 value_ast.is_a(ast_class.getUC_COMPLEMENT())) //~
        {
            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast oper = operands.children().get(0).as_ast();
            CType operType = typeConverter.getCType(oper.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression expression = getExpressionFromUC(oper,operType,fileLoc);

            if(expression instanceof CIntegerLiteralExpression &&
                    value_ast.is_a(ast_class.getUC_ABSTRACT_NEGATE())){
                BigInteger value = ((CIntegerLiteralExpression) expression).getValue();
                return new CIntegerLiteralExpression(expression.getFileLocation(),
                        expression.getExpressionType(),value.not());
            } else {
                if(expression instanceof CFloatLiteralExpression ||
                        expression instanceof CCharLiteralExpression)
                    printWARNING("Issue in processing negate operation: "+ expression.toASTString());
                if(value_ast.is_a(ast_class.getUC_ABSTRACT_NEGATE()))
                    return new CUnaryExpression(fileLoc, operType, expression,
                        CUnaryExpression.UnaryOperator.MINUS);
                else
                    return new CUnaryExpression(fileLoc, operType, expression,
                            CUnaryExpression.UnaryOperator.TILDE);
            }
        }else if(value_ast.is_a(ast_class.getUC_NOT())){

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast oper = operands.children().get(0).as_ast();
            CType operType = typeConverter.getCType(oper.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression expression = getExpressionFromUC(oper,operType,fileLoc);
            return buildBinaryExpression(
                    CIntegerLiteralExpression.ZERO,
                    expression,
                    CBinaryExpression.BinaryOperator.EQUALS,
                    valueType);

        }else if(value_ast.is_a(ast_class.getUC_ABSTRACT_ASSIGN())){//actually assignment should have been processed in the previous expression,
            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast oper1 = operands.children().get(0).as_ast();
            return getExpressionFromUC(oper1,valueType,fileLoc);
        }else if(isUnaryOperation(value_ast)){
            ast variable = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();
            return getExpressionFromUC(variable, valueType, fileLoc);
        }else if(value_ast.is_a(ast_class.getUC_ABSTRACT_OPERATION())){//
            if(value_ast.is_a(ast_class.getUC_QUESTION()))
                return null;

            CBinaryExpression.BinaryOperator operator = getBinaryOperatorFromUC(value_ast);
            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            if(operands.children().size()==1){
                printINFO("Check if this is a index using temp variable! "+ value_ast.toString());
                return null;
            }

            ast oper1 = operands.children().get(0).as_ast();
            CType type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CType operType1 = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression right = getExpressionFromUC(oper1,operType1,fileLoc);
            ast oper2 = operands.children().get(1).as_ast();
            CType operType2 = typeConverter.getCType(oper2.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression left = getExpressionFromUC(oper2,operType2,fileLoc);

            return buildBinaryExpression(right,left,operator,type);

        } else if(value_ast.is_a(ast_class.getUC_STRING())){

            String value = value_ast.get(ast_ordinal.getBASE_VALUE()).as_str();
            CPointerType pointerType = new CPointerType(true, false, CNumericTypes.CHAR);
            return new CStringLiteralExpression(fileLoc, pointerType, value);

        }else if(value_ast.is_a(ast_class.getUC_INTEGER_VALUE())){
            if(value_ast.is_a(ast_class.getUC_ENUM())){
                long value = Long.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_ast().pretty_print());
                return new CIntegerLiteralExpression(fileLoc,CNumericTypes.SHORT_INT,BigInteger.valueOf(value));
            }else if(valueType.equals(CNumericTypes.BOOL)){
                long value = value_ast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_int32();
                assert value==0||value==1;
                if(value==0)
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ZERO);//false
                else
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ONE);//true
            }else if(valueType.getCanonicalType().equals(CNumericTypes.CHAR)){
                char value = (char)value_ast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_int32();
                CType type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
                return new CCharLiteralExpression(fileLoc, type, value);
            } else {
                BigInteger integer = getIntegerValue(value_ast);
                return new CIntegerLiteralExpression(fileLoc,valueType,integer);
            }
        }else if(value_ast.is_a(ast_class.getUC_FLOAT_VALUE())){
            if(valueType.getCanonicalType().equals(CNumericTypes.FLOAT)){
                float value = value_ast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_flt32();
                return new CFloatLiteralExpression(fileLoc, valueType, BigDecimal.valueOf(value));
            }else {
                double value = value_ast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_flt32();
                return new CFloatLiteralExpression(fileLoc, valueType, BigDecimal.valueOf(value));
            }
        }else if(value_ast.is_a(ast_class.getUC_STMT_EXPR())){
            ast expr = value_ast.get(ast_ordinal.getUC_EXPR()).as_ast();
            return getExpressionFromUC(expr,valueType,fileLoc);
        }else if(value_ast.is_a(ast_class.getUC_EXPR_STMT())){//block statement
            ast statments = value_ast.get(ast_ordinal.getUC_STATEMENT()).get(ast_ordinal.getUC_STATEMENTS()).as_ast();
            ast expr = statments.children().get((int)statments.children().size()-1).as_ast();
            return getExpressionFromUC(expr,valueType,fileLoc);
        }else if(value_ast.is_a(ast_class.getUC_SET_VLA_SIZE())){
            ast dimension = value_ast.get(ast_ordinal.getUC_VLA_DIMENSION()).as_ast();
            return getExpressionFromUC(dimension,valueType,fileLoc);
        }else if(value_ast.is_a(ast_class.getUC_VLA_DIMENSION())){
            ast expr = value_ast.get(ast_ordinal.getUC_DIMENSION_EXPR()).as_ast();
            return getExpressionFromUC(expr,valueType,fileLoc);
        }else
            throw new RuntimeException("Unsupport ast "+ value_ast.toString());
    }


    public String getFunctionName(CExpression expression){
        if(expression instanceof CFieldReference)
            return ((CFieldReference) expression).getFieldName();
        else if(expression instanceof CArraySubscriptExpression){
            return getFunctionName(((CArraySubscriptExpression) expression).getArrayExpression());
        }else if(expression instanceof CUnaryExpression)
            return getFunctionName(((CUnaryExpression) expression).getOperand());
        else if(expression instanceof CPointerExpression)
            return getFunctionName(((CPointerExpression) expression).getOperand());
        else if(expression instanceof CIdExpression)
            return ((CIdExpression)expression).getName();
        else
            throw new RuntimeException("Not support to get function name from expression "+ expression.toString());

    }

    /**
     * @Description //get function result variable from the unnormalized ast of an actual out point
     * to avoid name collision, we normalized the variable with the function name, the suffix "$result__", and its CS UID
     * @Param [function]
     * @return java.lang.String
     **/
    public String getFunctionCallResultName(ast function)throws result{
        ast routine = function.get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();
        if(routine.is_a(ast_class.getUC_EXPR_ROUTINE())){
            symbol result = routine.get(ast_ordinal.getUC_ROUTINE())//routine
                    .get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
            return result.name()+"$result__"+ function.get(ast_ordinal.getUC_UID()).as_uint32();
        }else {
            CType type = typeConverter.getCType(routine.get(ast_ordinal.getBASE_TYPE()).as_ast(),this);
            CExpression expression = getExpressionFromUC(routine,type, FileLocation.DUMMY);
            return expression.toString()+"$result__"+ function.get(ast_ordinal.getUC_UID()).as_uint32();
        }
    }

    //value_ast.get_class()==[c:addr]
    /**
     * @Description //pointer address, e.g., char p[30]="say hello", *p1 = &r; normalized ast
     * @Param [value_ast, fileloc]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getPointerAddrExpr(ast value_ast, FileLocation fileloc)throws  result{
        assert isPointerAddressExpr(value_ast);
        //addr->(array-ref -> variable | string) | variable
        ast variable = value_ast.children().get(0).as_ast();
        CType cType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

        if(isVariable(variable)){
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression operand = getExpressionFromNO(variable,variableType, fileloc);
            return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
        } else if(variable.is_a(ast_class.getNC_ARRAYREF())){
            ast pointedto = variable.children().get(0).as_ast();
            if(isVariable(pointedto)){
                CType variableType = typeConverter.getCType(pointedto.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
                CExpression operand = getExpressionFromNO(pointedto, variableType, fileloc);
                return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
            }else if(pointedto.is_a(ast_class.getNC_STRING())){
                String value = pointedto.get(ast_ordinal.getBASE_VALUE()).as_str();
                cType = new CPointerType(true,false,CNumericTypes.CHAR);
                return new CStringLiteralExpression(fileloc, cType, value);
            }
        }

        return null;
    }

    public BigDecimal getFloatValue(ast valueast)throws result{
        ast_field valueField;
        try {
            valueField= valueast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE());
        }catch (result r){
            valueField= valueast.get(ast_ordinal.getBASE_VALUE());
        }
        if(valueField.get_type().equals(ast_field_type.getFLT32()))
            return BigDecimal.valueOf(valueField.as_flt32());
        else if(valueField.get_type().equals(ast_field_type.getFLT64()))
            return BigDecimal.valueOf(valueField.as_flt64());
        else if(valueField.get_type().equals(ast_field_type.getFLT96()) || valueField.get_type().equals(ast_field_type.getFLT128()))
            return BigDecimal.valueOf(valueField.as_flt64());
        else
            throw new RuntimeException("Not support float value: "+ valueast.toString()+" "+ valueField.toString());
    }

    public BigInteger getIntegerValue(ast valueast)throws result{

        ast_field valueField;
        try {
            valueField= valueast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE());
        }catch (result r){
            valueField= valueast.get(ast_ordinal.getBASE_VALUE());
        }

        long value;
        if(valueField.get_type().equals(ast_field_type.getINT64()))
            value = valueField.as_int64();
        else if(valueField.get_type().equals(ast_field_type.getINT32()))
            value = valueField.as_int32();
        else if(valueField.get_type().equals(ast_field_type.getINT16()))
            value = valueField.as_int16();
        else if(valueField.get_type().equals(ast_field_type.getINT8()))
            value = valueField.as_int8();
        else if(valueField.get_type().equals(ast_field_type.getUINT64()))
            return valueField.as_uint64();
        else if(valueField.get_type().equals(ast_field_type.getUINT32()))
            value = valueField.as_uint32();
        else if(valueField.get_type().equals(ast_field_type.getUINT16()))
            value = valueField.as_uint16();
        else if(valueField.get_type().equals(ast_field_type.getUINT8()))
            value = valueField.as_uint8();
        else
            throw new RuntimeException("Not support basic type: "+ valueField.get_type().name());
        return BigInteger.valueOf(value);
    }

    /**
     * @Description //create expression in accordance with arithmetic operation, normalized ast
     * @Param [value_ast, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression createFromArithmeticOp(
            final ast value_ast, final FileLocation fileLocation) throws result {

        CBinaryExpression.BinaryOperator operator = getBinaryOperator(value_ast);

        final CType expressionType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

        ast operand1 = value_ast.children().get(0).as_ast(); // First operand
        //logger.log(Level.FINE, "Getting id expression for operand 1");
        CType op1type = typeConverter.getCType(operand1.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        CExpression operand1Exp = getExpressionFromNO(operand1,op1type, fileLocation);

        ast operand2 =  value_ast.children().get(1).as_ast(); // Second operand
        CType op2type = typeConverter.getCType(operand2.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
        //logger.log(Level.FINE, "Getting id expression for operand 2");
        CExpression operand2Exp = getExpressionFromNO(operand2, op2type, fileLocation);

        return new CBinaryExpression(
                fileLocation,
                expressionType,
                expressionType,
                operand1Exp,
                operand2Exp,
                operator);
    }


    // local: UC_INIT-->dynamic init-->const|expr...,
    // global: exprpoint-->un-ast-->static init-->
    /**
     * @Decription
     * local: UC_INIT-->dynamic init-->const|expr...,
     * global: exprpoint-->un-ast-->static init-->
     * input is dynamic init or static init
     **/
    public CInitializer getInitializerFromUC(ast init, CType type, FileLocation fileLocation) throws result{
        CInitializer initializer = null;
        if(init.is_a(ast_class.getUC_DYNAMIC_INIT_CONSTANT()) ||
                init.is_a(ast_class.getUC_STATIC_INITIALIZER())){// static init must be a constant

            if(!init.has_field(ast_ordinal.getUC_CONSTANT()))
                initializer = getZeroInitializer(type, fileLocation);
            else {
                ast constant = init.get(ast_ordinal.getUC_CONSTANT()).as_ast();
                if(isConstantAggreateZeroFromUC(constant)){
                    initializer = getZeroInitializer(type,fileLocation);
                }else if(constant.is_a(ast_class.getUC_AGGREGATE())){
                    initializer = getConstantAggregateInitializerFromUC(constant,type, fileLocation);
                }else{
                    CExpression expression = getConstantFromUC(constant, type, fileLocation);
                    initializer = new CInitializerExpression(fileLocation, expression);
                }
            }
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_EXPRESSION())){
            ast expr = init.get(ast_ordinal.getUC_EXPR()).as_ast();
            CExpression expression = getExpressionFromUC(expr, type, fileLocation);
            initializer = new CInitializerExpression(fileLocation, expression);
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_NONCONSTANT_AGGREGATE())){
            if(!init.has_field(ast_ordinal.getUC_CONSTANT()))
                initializer = getZeroInitializer(type, fileLocation);
            else {
                ast constant = init.get(ast_ordinal.getUC_CONSTANT()).as_ast();
                initializer = getConstantAggregateInitializerFromUC(constant,type,fileLocation);
            }
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_CONSTRUCTOR())){
            throw new RuntimeException("Unsupport initializer "+ init.toString());
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_BITWISE_COPY())){
            throw new RuntimeException("Unsupport initializer "+ init.toString());
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_ZERO())){
            initializer = getZeroInitializer(type, fileLocation);
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_NONE())){
            return null;
        }else if(init.is_a(ast_class.getUC_NO_INITIALIZER())){
            return null;
        }else
            throw new RuntimeException("Unsupport initializer "+ init.toString());

        return initializer;
    }


    public CInitializer getInitializerFromTXT(CType type, FileLocation fileLocation){
        if(type instanceof CArrayType){
            CType realType = ((CArrayType) type).getType();
            if(realType instanceof CTypedefType)
                realType = ((CTypedefType) realType).getRealType();
            if(realType instanceof CElaboratedType)
                realType = ((CElaboratedType) realType).getRealType();
            if(realType instanceof CCompositeType){
                if(!((CCompositeType) realType).getKind().equals(CComplexType.ComplexTypeKind.STRUCT))
                    return null;
                Map<Integer,String> mccMNCMap = readMCCMNCList();
                List<CInitializer> elementInitializers = new ArrayList<>(mccMNCMap.size());
                Iterator<Map.Entry<Integer,String>> iterator = mccMNCMap.entrySet().iterator();
                CType mccType = ((CCompositeType) realType).getMembers().get(0).getType();
                CType mncType = ((CCompositeType) realType).getMembers().get(1).getType();
                while (iterator.hasNext()){
                    Map.Entry<Integer,String> entry = iterator.next();

                    CIntegerLiteralExpression mcc = new CIntegerLiteralExpression(fileLocation, mccType, BigInteger.valueOf(entry.getKey()));
                    CInitializer mccInitializer = new CInitializerExpression(
                            fileLocation, mcc);
                    CStringLiteralExpression mnc = new CStringLiteralExpression(fileLocation, mncType, entry.getValue());
                    CInitializer mncInitializer = new CInitializerExpression(
                            fileLocation, mnc);
                    List<CInitializer> mccmncInitializer = new ArrayList<>(2);
                    mccmncInitializer.add(mccInitializer);
                    mccmncInitializer.add(mncInitializer);
                    elementInitializers.add(new CInitializerList(fileLocation, mccmncInitializer));
                }
                return new CInitializerList(fileLocation, elementInitializers);
            }
        }
        return null;
    }

    /**
     * @Description //generate the initializer from normalized ast
     * @Param [no_ast, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    public CInitializer getInitializer(ast no_ast, final FileLocation fileLocation) throws result{
        CInitializer initializer = null;

        if(isInitializationExpression(no_ast)){
            ast value_ast = no_ast.children().get(1).as_ast();
            ast type_ast = no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
            CType valueType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

            if (isConstantAggregateZero(value_ast, valueType)) {
                CType expressionType = typeConverter.getCType(type_ast, this);
                initializer = getZeroInitializer(expressionType, fileLocation);
            }else if(isArrayType(type_ast) ||
                    isStructType(type_ast) ||
                    isEnumType(type_ast)){
                initializer = getConstantAggregateInitializer(value_ast, fileLocation);
            } else if(isNullArrayInit(no_ast)){
                List<CInitializer> elementInitializers = new ArrayList<>();
                initializer = new CInitializerList(fileLocation, elementInitializers);
            }else  {
                initializer =  new CInitializerExpression(
                        fileLocation, getExpressionFromNO(value_ast, valueType, fileLocation));
            }
        }
        return initializer;
    }

    /**
     * @Description //get constant aggreate initializer, int a[3]={1,2,3}, unnormalized ast
     * @Param [constant, expectedType, fileLoc]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    public CInitializer getConstantAggregateInitializerFromUC(ast constant, CType expectedType,
                                                              final FileLocation fileLoc) throws result {
        ast constantList = constant.get(ast_ordinal.getUC_CONSTANT_LIST()).as_ast();
        int length = (int)constantList.children().size();
        List<CInitializer> elementInitializers = new ArrayList<>(length);
        int index =0;
        CType aggregateType = getAggregateType(expectedType);
        for(int i=0;i<length;i++){
            ast elementAST = constantList.children().get(i).as_ast();
            CInitializer elementInitializer;
            CType elementType;
            if(aggregateType instanceof CCompositeType)
                elementType = ((CCompositeType) aggregateType).getMembers().get(index).getType();
            else if(aggregateType instanceof CArrayType)
                elementType = ((CArrayType) aggregateType).getType();
            else
                throw new RuntimeException("Not support type");
            if(elementAST.is_a(ast_class.getUC_DESIGNATOR())){
                List<CDesignator> designatorList = new ArrayList<>();
                String field  = elementAST.get(ast_ordinal.getUC_FIELD()).as_ast().pretty_print();
                if(field.equals("")){
                    if(aggregateType instanceof CCompositeType)
                        field = ((CCompositeType) aggregateType).getMembers().get(index).getName();
                    else
                        throw new RuntimeException("Not support type for field");
                }
                designatorList.add(new CFieldDesignator(fileLoc, field));
                CInitializer rightInitializer = null;
                if(i+1<length){
                    i++;
                    ast subconstant = constantList.children().get(i).as_ast();
                    //CType type = typeConverter.getCType(subconstant.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
                    if(isConstantAggreateZeroFromUC(subconstant)){
                        rightInitializer = getZeroInitializer(elementType,fileLoc);
                    }else if(subconstant.is_a(ast_class.getUC_AGGREGATE())){
                        rightInitializer = getConstantAggregateInitializerFromUC(subconstant,elementType, fileLoc);
                    }else if(subconstant.is_a(ast_class.getUC_CONSTANT_DYNAMIC_INITIALIZATION())){
                        ast dynamicInit = subconstant.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
                        rightInitializer = getInitializerFromUC(dynamicInit, elementType, fileLoc);
                    }else {
                        CExpression expression = getConstantFromUC(subconstant, elementType, fileLoc);
                        rightInitializer = new CInitializerExpression(fileLoc, expression);
                    }
                }
                elementInitializer = new CDesignatedInitializer(fileLoc, designatorList, rightInitializer);
            }else {
                ast elementType_ast = elementAST.get(ast_ordinal.getBASE_TYPE()).as_ast();
                //CType elementType = typeConverter.getCType(elementType_ast, this);
                if(isConstantAggreateZeroFromUC(elementAST)){
                    elementInitializer =
                            getZeroInitializer(elementType, fileLoc);
                }else if(elementAST.is_a(ast_class.getUC_AGGREGATE())){
                    elementInitializer = getConstantAggregateInitializerFromUC(elementAST, elementType, fileLoc);
                } else if(elementAST.is_a(ast_class.getUC_CONSTANT_DYNAMIC_INITIALIZATION())){
                    ast dynamicInit = elementAST.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
                    elementInitializer = getInitializerFromUC(dynamicInit, elementType, fileLoc);
                }else {
                    elementInitializer = new CInitializerExpression(
                            fileLoc, getConstantFromUC(elementAST, elementType,fileLoc));
                }
            }
            index++;
            elementInitializers.add(elementInitializer);
        }

        return new CInitializerList(fileLoc, elementInitializers);
    }

    public CType getAggregateType(CType type){
        if(type instanceof CTypedefType)
            return getAggregateType(((CTypedefType) type).getRealType());
        if(type instanceof CElaboratedType)
            return getAggregateType(((CElaboratedType) type).getRealType());
        if(type instanceof CCompositeType || type instanceof CArrayType)
            return type;
        else
            throw new RuntimeException("Not a composite or array type:"+ type.toString());
    }

    /**
     *@Description handle the aggregate initialization (normalized ast), e.g., int array[5]={1,2,3,4,5};
     *@Param [no_ast, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    public CInitializer getConstantAggregateInitializer(ast no_ast,
                                                         final FileLocation fileLoc) throws result {

        //ast no_ast = initialPoint.get_ast(ast_family.getC_NORMALIZED());
       // ast_field value = no_ast.children().get(1);
        CType type = typeConverter.getCType(no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);

        ast_field_vector elements = no_ast.children();
        int length = (int)elements.size();
        if(isNormalExpression(no_ast) && type.equals(CNumericTypes.CHAR)){
            char[] items = new char[length];
            for(int i=0;i<length;i++){
                items[i]=(char) elements.get(i).as_ast().get(ast_ordinal.getBASE_VALUE()).as_int32();
            }
            CType cType = new CPointerType(true,false,CNumericTypes.CHAR);
            CStringLiteralExpression stringLiteralExpression =
                    new CStringLiteralExpression(fileLoc,cType, String.copyValueOf(items,0,items.length-1));
            return new CInitializerExpression(fileLoc,stringLiteralExpression);
        }


        List<CInitializer> elementInitializers = new ArrayList<>(length);
        for(int i=0;i<length;i++){
            ast_field element = elements.get(i);
            ast elementAST = element.as_ast();
            CInitializer elementInitializer;
            ast_field elementType = element.as_ast().get(ast_ordinal.getBASE_TYPE());
            ast elementType_ast = elementType.as_ast();
            if(isConstantAggregateZero(elementAST, type)){
                elementInitializer =
                        getZeroInitializer(typeConverter.getCType(elementType_ast, this), fileLoc);
            }else if(isArrayType(elementType_ast) ||
                    isStructType(elementType_ast) ||
                    isEnumType(elementType_ast)){
                elementInitializer = getConstantAggregateInitializer(element.as_ast(), fileLoc);
            } else {
                elementInitializer = new CInitializerExpression(
                        fileLoc, (CExpression) getConstant(element.as_ast(),
                                    typeConverter.getCType(elementType_ast, this),fileLoc));
            }
            elementInitializers.add(elementInitializer);
        }

        CInitializerList aggregateInitializer =
                new CInitializerList(fileLoc, elementInitializers);
        return aggregateInitializer;
    }


    /**
     * @Description //zero initializer, 0
     * @Param [pExpectedType, fileLoc]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    public CInitializer getZeroInitializer(final CType pExpectedType, final FileLocation fileLoc){

        CInitializer init;
        CType canonicalType = pExpectedType.getCanonicalType();
        if (canonicalType instanceof CArrayType) {
            int length = ((CArrayType) canonicalType).getLengthAsInt().getAsInt();
            CType elementType = ((CArrayType) canonicalType).getType().getCanonicalType();
            CInitializer zeroInitializer = getZeroInitializer(elementType, fileLoc);
            List<CInitializer> initializers = Collections.nCopies(length, zeroInitializer);
            init = new CInitializerList(fileLoc, initializers);
        } else if (canonicalType instanceof CCompositeType) {

            //List<CCompositeType.CCompositeTypeMemberDeclaration> members = ((CCompositeType) canonicalType).getMembers();
            List<CInitializer> initializers = new ArrayList<>();
//            for (CCompositeType.CCompositeTypeMemberDeclaration m : members) {
//                CType memberType = m.getType();
//                CInitializer memberInit = getZeroInitializer(memberType, fileLoc);
//                initializers.add(memberInit);
//            }
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
                CType type = new CPointerType(false,false,VOID);
                zeroExpression = new CCastExpression(fileLoc, type, CIntegerLiteralExpression.ZERO);
            }
            init = new CInitializerExpression(fileLoc, zeroExpression);
        }

        return init;
    }

    /**
     * @Description //TODO
     * @Param [value_ast, pExpectedType, fileLoc]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide
     **/
    public CRightHandSide getConstant(ast value_ast, CType pExpectedType, FileLocation fileLoc)
            throws result{

        ast typeAST = value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();

        if(isNullPointer(value_ast))//null pointer: e.g., p = 0; p= NULL;
        {
            return new CPointerExpression(fileLoc,pExpectedType,getNull(fileLoc,pExpectedType));
        }else if(isUndef(value_ast)){//TODO
            CType constantType = typeConverter.getCType(typeAST, this);
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
        } else if (isVariable(value_ast)) {
            return getAssignedIdExpression(value_ast, pExpectedType, fileLoc);
        } else
            return getExpressionFromNO(value_ast,pExpectedType, fileLoc);
    }

    /**
     * @Description //get constant, e.g., int a[20]="hello world!"
     * @Param [constant, pExpectedType, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getConstantFromUC(ast constant, CType pExpectedType, FileLocation fileLocation) throws result{

        if(constant.is_a(ast_class.getUC_CONSTANT_ADDRESS())){
            CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            return getConstantFromUC(constant.get(ast_ordinal.getUC_CONSTANT()).as_ast(),type, fileLocation);
        }else if(constant.is_a(ast_class.getUC_STRING())){
            String value = constant.get(ast_ordinal.getBASE_VALUE()).as_str();
            //CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CPointerType pointerType = new CPointerType(true, false, CNumericTypes.CHAR);
            return new CStringLiteralExpression(fileLocation, pointerType, value);
        }else if(constant.has_field(ast_ordinal.getBASE_VALUE())){
            ast typeAST = constant.get(ast_ordinal.getBASE_TYPE()).as_ast();
            CType type = typeConverter.getCType(typeAST, this);

            CBasicType cBasicType = returnValueRealType(typeAST,type);

            if(cBasicType.equals(CBasicType.INT)){
                if(typeAST.is_a(ast_class.getUC_ENUM())){
                    long value = Long.valueOf(constant.get(ast_ordinal.getBASE_VALUE()).as_ast().pretty_print());
                    return new CIntegerLiteralExpression(fileLocation,CNumericTypes.SHORT_INT,BigInteger.valueOf(value));
                }else if(pExpectedType.equals(CNumericTypes.BOOL)){
                    long value = constant.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_uint32();
                    assert value==0||value==1;
                    if(value==0)
                        return new CIntegerLiteralExpression(
                                fileLocation, CNumericTypes.BOOL, BigInteger.ZERO);//false
                    else
                        return new CIntegerLiteralExpression(
                                fileLocation, CNumericTypes.BOOL, BigInteger.ONE);//true
                }else if(pExpectedType.getCanonicalType().equals(CNumericTypes.CHAR)){
                    char value = (char)constant.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_int32();
                    return new CCharLiteralExpression(fileLocation, type, value);
                } else {
                    BigInteger integer = getIntegerValue(constant);
                    return new CIntegerLiteralExpression(fileLocation, type, integer);
                }
            }else if(cBasicType.equals(CBasicType.FLOAT)){
                return new CFloatLiteralExpression(fileLocation, type, getFloatValue(constant));
            }else if(cBasicType.equals(CBasicType.UNSPECIFIED)){//pointer type,,int *p=NULL;
                if (constant.has_field(ast_ordinal.getUC_EXPR())
                        && constant.get(ast_ordinal.getUC_EXPR()).as_ast().is_a(ast_class.getUC_GENERIC_CAST())) {
                    CType voidtype = new CPointerType(false,false,VOID);
                    return new CCastExpression(fileLocation, voidtype, CIntegerLiteralExpression.ZERO);
                }else
                    throw  new RuntimeException("Unsupported type "+pExpectedType.toString()+" in GETCONSTANT");
            }else
                throw  new RuntimeException("Unsupported type "+pExpectedType.toString()+" in GETCONSTANT");
        }else if(constant.is_a(ast_class.getUC_ROUTINE_ADDRESS_CONSTANT())){
            //function pointer
            ast expr = constant.get(ast_ordinal.getUC_EXPR()).as_ast();
            //functionType
            CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression operand = getExpressionFromUC(expr, type, fileLocation);
            return new CUnaryExpression(fileLocation, type, operand, CUnaryExpression.UnaryOperator.AMPER);
        }else if(constant.is_a(ast_class.getUC_VARIABLE_ADDRESS_CONSTANT())){
            ast variable = constant.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast(), this);
            CExpression operand = getAssignedIdExpression(variable, type, fileLocation);
            return new CUnaryExpression(fileLocation, type, operand, CUnaryExpression.UnaryOperator.AMPER);
        } else if(constant.is_a(ast_class.getUC_LABEL_ADDRESS())){

        }else if(constant.is_a(ast_class.getUC_TYPEID_ADDRESS())){

        }else if(constant.is_a(ast_class.getUC_UUIDOF_ADDRESS())){

        }else if(constant.is_a(ast_class.getUC_ERROR_VALUE())){
            dumpASTWITHClass(constant);
        }

        throw  new RuntimeException("Unsupported type "+constant.toString()+" GETCONSTANT");
    }




    public CBasicType returnValueRealType(ast typeAST, CType cType)throws result{
        if(typeAST.is_a(ast_class.getUC_FLOAT()))
            return CBasicType.FLOAT;
        else if(typeAST.is_a(ast_class.getUC_INTEGER()))
            return CBasicType.INT;
        else if(typeAST.is_a(ast_class.getUC_TYPEREF())){
            ast originTypeAST = typeAST.get(ast_ordinal.getBASE_TYPE()).as_ast();
            return returnValueRealType(originTypeAST,cType);
        }else if(typeAST.is_a(ast_class.getUC_POINTER()))
            return CBasicType.UNSPECIFIED;
        throw new RuntimeException("Unsupported type "+cType.toString());
    }


    public boolean isFunction(ast variable) throws result{

        try {
            if(variable.get(ast_ordinal.getBASE_TYPE()).as_ast().is_a(ast_class.getUC_ROUTINE_TYPE())
                && variable.has_field(ast_ordinal.getUC_ROUTINE()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

}
