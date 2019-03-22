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

import static org.nulist.plugin.parser.CFABuilder.pointerOf;
import static org.nulist.plugin.parser.CFGAST.*;
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
        CType op1Type = typeConverter.getCType(variable_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        ast value_ast = condition.children().get(1).as_ast();
        CType op2Type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

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

    public CBinaryExpression buildBinaryExpression(
            CExpression operand1, CExpression operand2, CBinaryExpression.BinaryOperator op) {
        try {
            return binExprBuilder.buildBinaryExpression(operand1, operand2, op);
        } catch (UnrecognizedCodeException e) {
            e.getParentState();
        }
        return null;
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
        return new CIdExpression(fileLocation, expressionType, assignedVarName, assignedVarDeclaration);

//        if (expressionType.canBeAssignedFrom(pExpectedType)) {
//            return idExpression;
//        } else
        /*if (pointerOf(pExpectedType, expressionType)) {
            CType typePointingTo = ((CPointerType) pExpectedType).getType().getCanonicalType();
            if (expressionType.canBeAssignedFrom(typePointingTo)
                    || expressionType.equals(typePointingTo)) {
                return new CUnaryExpression(
                        fileLocation, pExpectedType, idExpression, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                throw new AssertionError("Unhandled type structure "+ assignedVarName+ " "+ expressionType.toString());
            }
        } else if (pExpectedType instanceof CPointerType) {
            return new CPointerExpression(fileLocation, pExpectedType, idExpression);
        }else
            return idExpression;*/
//        else {
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
            else
                throw new RuntimeException("Global variable has no declaration: " + normalizedVarName);
        }else {
            if(variableDeclarations.containsKey(normalizedVarName.hashCode()))
            {
                if(isFunction)
                    assignedVarDeclaration = variableDeclarations.get(normalizedVarName.hashCode());
                else
                    assignedVarDeclaration =  variableDeclarations.get(normalizedVarName.hashCode());
            }
            else
                throw new RuntimeException("Local variable has no declaration: " + normalizedVarName);
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
            return getSimpleFileName(fileName)+"__static__"+normalizedName;
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
        CType leftType = typeConverter.getCType(left_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CLeftHandSide leftHandSide  = (CLeftHandSide) getExpression(left_ast, leftType, fileLocation);

        ast value_ast = no_ast.children().get(1).as_ast();

        CType rightType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression rightHandSide = getExpression(value_ast, rightType, fileLocation);

        return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
    }

    /**
     * @Description //using unnormalized node to generate the initializer
     * @Param [un_ast, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration
     **/
    public CVariableDeclaration generateInitVarDeclFromUC(ast un_ast, FileLocation fileLocation) throws result{
        assert un_ast.get_class().is_subclass_of(ast_class.getUC_INIT());

        ast init = un_ast.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
        ast variable = init.get(ast_ordinal.getUC_VARIABLE()).as_ast();
        symbol varSymbol = variable.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
        CType cType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
        if(typeConverter.isFunctionPointerType(cType)){
            CPointerType pointerType = (CPointerType)cType;
            CFunctionTypeWithNames cFunctionTypeWithNames =
                    typeConverter.convertCFuntionType((CFunctionType)pointerType.getType(),varSymbol.name(),fileLocation);
            cType = new CPointerType(pointerType.isConst(),
                    pointerType.isVolatile(),
                    cFunctionTypeWithNames);
        }

        CInitializer initializer = getInitializerFromUC(init, cType, fileLocation);

        return generateVariableDeclaration(
                varSymbol.primary_declaration().declared_symbol(),
                initializer,fileLocation);
    }

    /**
     *@Description CodeSurfer splits declaration and initialization, e.g., int i=0;-->int i; i=0;
     *             We need to combine the declaration and initialization.
     *@Param [variable]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration
     **/
    public CVariableDeclaration generateVariableDeclaration(symbol variable,
                                                            CInitializer initializer,
                                                            FileLocation fileLocation)throws result{
        String assignedVar =getNormalizedVariableName(variable,fileLocation.getFileName());

        if(variable.is_local() || variable.is_local_static())
        if(variableDeclarations.containsKey(assignedVar.hashCode()))
            return (CVariableDeclaration) variableDeclarations.get(assignedVar.hashCode());

        if(variable.is_global() || variable.is_file_static())
            if(globalDeclarations.containsKey(assignedVar.hashCode()))
                return (CVariableDeclaration) globalDeclarations.get(assignedVar.hashCode());

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

            CType varType = typeConverter.getCType(variable.get_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());

            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            variable.is_global(),
                            storageClass,
                            varType,
                            normalizedName,
                            originalName,
                            normalizedName,
                            initializer);
            if(variable.is_local() || variable.is_local_static())
                variableDeclarations.put(normalizedName.hashCode(),newVarDecl);
            if(variable.is_global() || variable.is_file_static())
                globalDeclarations.put(normalizedName.hashCode(),newVarDecl);
            return newVarDecl;
        }else {
            throw new RuntimeException("Incorrectly calling generateVariableDeclaration from "+ variable.as_string());
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
        ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        ast oper1 = operands.children().get(0).as_ast();
        CType leftType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast());
        if(typeConverter.isFunctionPointerType(leftType)){
            CPointerType pointerType = (CPointerType)leftType;
            CFunctionTypeWithNames cFunctionTypeWithNames =
                    typeConverter.convertCFuntionType((CFunctionType)pointerType.getType(), "", fileLocation);
            leftType = new CPointerType(pointerType.isConst(),
                    pointerType.isVolatile(),
                    cFunctionTypeWithNames);
        }

        CExpression variable = getExpressionFromUC(oper1, leftType, fileLocation);

        CExpression value = null;
        if(operands.children().size()==2){
            ast oper2 = operands.children().get(1).as_ast();
            CType rightType = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast());
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
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.MINUS);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_PRE_INCR())){//++a
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.PLUS);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_POST_DECR())){//a--
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.MINUS);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_POST_INCR())){//a++
            if(operands.children().size()==1){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, ONE, CBinaryExpression.BinaryOperator.PLUS);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }else if(un_ast.is_a(ast_class.getUC_INDIRECT())){
            throw new RuntimeException("Issue in getAssignStatementFromUC with "+ functionName+ " "+ fileLocation.getStartingLineNumber());
        } else {
            CBinaryExpression.BinaryOperator operator = getBinaryOperatorFromUC(un_ast);
            if(operands.children().size()==2 && value!=null && operator!=null){
                leftHandSide = (CLeftHandSide) variable;
                rightHandSide = buildBinaryExpression(variable, value, operator);
                return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
            }else {
                throw new RuntimeException("Issue in getAssignStatementFromUC with "+ un_ast.toString());
            }
        }

    }

    //normalized node
    /**
     * @Description //This a function to get expression from CodeSurfer expression ast
     * This is normalized version, unnormalized version see getExpressionFromUC
     * @Param [value_ast, valueType, fileLoc]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getExpression(ast value_ast, CType valueType,FileLocation fileLoc)throws result{

        if(isVariable(value_ast)){//e.g., a
            return getAssignedIdExpression(value_ast, valueType, fileLoc);
        }else if(isValue(value_ast)){//a=2;

            CBasicType basicType = ((CSimpleType) valueType).getType();
            if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
                BigDecimal value = BigDecimal.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_flt32());
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
                BigInteger value = getBasicValue(value_ast);
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
            CType varType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression variableExpr = getAssignedIdExpression(variable,varType, fileLoc);
            String fieldName = value_ast.children().get(1).get(ast_ordinal.getBASE_NAME()).as_str();

            return new CFieldReference(fileLoc, valueType, fieldName, variableExpr,
                    valueType instanceof CPointerType);

        }else if(isPointerAddressExpr(value_ast)){//pointer address, e.g., char p[30]="say hello", *p1 = &r;
            return getPointerAddrExpr(value_ast, fileLoc);
        }else if(isZeroInitExpr(value_ast)){//zero initialization, e.g., char *p=NULL, p1[30]={} (aggreate);
                                            // castexpr
                                            // for an array, child 0 *&p[0], child 1 (int[30])0
                                            // for a pointer, child 0: p, child 1: (void*)0
            CType castType = typeConverter.getCType(value_ast.get(ast_ordinal.getNC_TYPE()).as_ast());
            ast operandAST = value_ast.children().get(1).as_ast();
            CType operandType = typeConverter.getCType(operandAST.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand = getExpression(operandAST,operandType, fileLoc);
            return new CCastExpression(fileLoc, castType, operand);
        }else if(isPointerExpr(value_ast)){//pointer, e.g., int i = *(p+1);
            CPointerType pointerType = new CPointerType(false,false,valueType);
            CBinaryExpression operand = getBinaryExpression(value_ast,fileLoc);
            return new CPointerExpression(fileLoc, pointerType, operand);
        }else
            throw new RuntimeException("");
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
            CType paramType = typeConverter.getCType(param.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getAssignedIdExpression(param, paramType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_EXPR_CONSTANT())){//1, true

            ast constant = value_ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            return getConstantFromUC(constant, valueType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_DOT_FIELD())){//r.member

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = operands.children().get(0).as_ast();//EXPR_VARIABLE. variable
            ast member = operands.children().get(1).as_ast();//UC_EXPR_FIELD, field

            CType varType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression variableExpr = getExpressionFromUC(variable,varType, fileLoc);

            String fieldName = member.get(ast_ordinal.getUC_FIELD()).as_ast().pretty_print();

            return new CFieldReference(fileLoc, valueType, fieldName, variableExpr,
                    valueType instanceof CPointerType);

        }else if(value_ast.is_a(ast_class.getUC_GENERIC_CAST())){//a+b, &a, operands

            ast cast = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();

            ast value = cast.children().get(0).as_ast();
            CType castType = typeConverter.getCType(value.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression castExpr = getExpressionFromUC(value, castType, fileLoc);

            return new CCastExpression(fileLoc, castType, castExpr);

        }else if(value_ast.is_a(ast_class.getUC_EXPR_FIELD())){

            ast field = value_ast.get(ast_ordinal.getUC_FIELD()).as_ast();//UC_FIELD
            CType cType = typeConverter.getCType(field.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getExpressionFromUC(field, cType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_INDIRECT())){//*p, operands

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = operands.children().get(0).as_ast();
            CType cType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand = getExpressionFromUC(variable, cType, fileLoc);
            if(typeConverter.isFunctionPointerType(operand.getExpressionType())){
                CType functionType = ((CPointerType)operand.getExpressionType()).getType();
                return new CPointerExpression(fileLoc, functionType, operand);
            }else
                return new CPointerExpression(fileLoc, operand.getExpressionType(), operand);

        }else if(value_ast.is_a(ast_class.getUC_SUBSCRIPT())){//d[1], d[1][2] operands
            ast operand = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast array_ast = operand.children().get(0).as_ast();
            ast index_ast = operand.children().get(1).as_ast();

            CType arrayType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression arrayExpr = getExpressionFromUC(array_ast, arrayType, fileLoc);

            CType indexType = typeConverter.getCType(index_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

            CExpression subscriptExpr = getExpressionFromUC(index_ast,indexType, fileLoc);
            return new CArraySubscriptExpression(fileLoc, arrayType, arrayExpr, subscriptExpr);

        }else if(value_ast.is_a(ast_class.getUC_PADD())){//function$return=*(p+2), operands

            CBinaryExpression.BinaryOperator operator = CBinaryExpression.BinaryOperator.PLUS;
            ast operand = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast pointer = operand.children().get(0).as_ast();
            CExpression pointerExpr = getExpressionFromUC(pointer, valueType, fileLoc);
            ast index = operand.children().get(1).as_ast();
            CType indexType = typeConverter.getCType(index.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression indexExpr = getExpressionFromUC(index, indexType, fileLoc);
            return buildBinaryExpression(pointerExpr, indexExpr, operator);
            //return new CPointerExpression(fileLoc, valueType, binaryExpression);

        }else if(value_ast.is_a(ast_class.getUC_ARRAY_TO_POINTER_DECAY())){//d[1], d[1][2]

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            //CType type  = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getExpressionFromUC(operands.children().get(0).as_ast(), valueType, fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_EXPR_ROUTINE())){//function$return=function1$result, routine

            ast routine = value_ast.get(ast_ordinal.getUC_ROUTINE()).as_ast();
            CType type = typeConverter.getCType(routine.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CFunctionTypeWithNames cFunctionTypeWithNames =
                    typeConverter.convertCFuntionType((CFunctionType) type, "", fileLoc);
            return getAssignedIdExpression(routine,cFunctionTypeWithNames,fileLoc);

        }else if(value_ast.is_a(ast_class.getUC_ADDRESS_OP())){// &d;

            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = operands.children().get(0).as_ast();
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand;
            //a function
            if(isFunction(variable)){
                ast routine = variable.get(ast_ordinal.getUC_ROUTINE()).as_ast();
                CFunctionTypeWithNames cFunctionTypeWithNames =
                        typeConverter.convertCFuntionType((CFunctionType) variableType, "", fileLoc);
                operand = getAssignedIdExpression(routine, cFunctionTypeWithNames, fileLoc);
            }else {
                ast variable_ast = variable.get(ast_ordinal.getUC_VARIABLE()).as_ast();
                operand = getExpressionFromUC(variable_ast,variableType, fileLoc);
            }
            return new CUnaryExpression(fileLoc, valueType, operand, CUnaryExpression.UnaryOperator.AMPER);

        }else if(value_ast.is_a(ast_class.getUC_FUNCTION_CALL())){

            String functionName = getFunctionCallResultName(value_ast);
            if(variableDeclarations.containsKey(functionName.hashCode()))
                return new CIdExpression(fileLoc, variableDeclarations.get(functionName.hashCode()));
            else
                throw new RuntimeException("No existing function call result: "+ functionName);
        }else if(value_ast.is_a(ast_class.getUC_ABSTRACT_OPERATION())){//

            CBinaryExpression.BinaryOperator operator = getBinaryOperatorFromUC(value_ast);
            ast operands = value_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast oper1 = operands.children().get(0).as_ast();
            CType operType1 = typeConverter.getCType(oper1.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression right = getExpressionFromUC(oper1,operType1,fileLoc);
            ast oper2 = operands.children().get(1).as_ast();
            CType operType2 = typeConverter.getCType(oper2.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression left = getExpressionFromUC(oper2,operType2,fileLoc);

            return buildBinaryExpression(right,left,operator);

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
                CType type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
                return new CCharLiteralExpression(fileLoc, type, value);
            } else {
                if(valueType.getCanonicalType().equals(CNumericTypes.INT)||
                        valueType.getCanonicalType().equals(CNumericTypes.SHORT_INT)||
                        valueType.getCanonicalType().equals(CNumericTypes.UNSIGNED_SHORT_INT)||
                        valueType.getCanonicalType().equals(CNumericTypes.SIGNED_INT)||
                        valueType.getCanonicalType().equals(CNumericTypes.UNSIGNED_INT)){
                    int value = Integer.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_ast().pretty_print());
                    return new CIntegerLiteralExpression(fileLoc,valueType, BigInteger.valueOf(value));
                }else {
                    long value = Long.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_ast().pretty_print());
                    return new CIntegerLiteralExpression(fileLoc,valueType, BigInteger.valueOf(value));
                }

            }
        }else if(value_ast.is_a(ast_class.getUC_FLOAT_VALUE())){
            if(valueType.getCanonicalType().equals(CNumericTypes.FLOAT)){
                float value = value_ast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_flt32();
                return new CFloatLiteralExpression(fileLoc, valueType, BigDecimal.valueOf(value));
            }else {
                double value = value_ast.get(ast_ordinal.getBASE_VALUE()).get(ast_ordinal.getBASE_VALUE()).as_flt32();
                return new CFloatLiteralExpression(fileLoc, valueType, BigDecimal.valueOf(value));
            }
        }else
            throw new RuntimeException("Unsupport ast "+ value_ast.as_string());
    }


    /**
     * @Description //get function result variable from the unnormalized ast of an actual out point
     * to avoid name collision, we normalized the variable with the function name, the suffix "$result__", and its CS UID
     * @Param [function]
     * @return java.lang.String
     **/
    public String getFunctionCallResultName(ast function)throws result{
        symbol result = function.get(ast_ordinal.getUC_OPERANDS()).as_ast()//operands
                .children().get(0).get(ast_ordinal.getUC_ROUTINE())//routine
                .get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
        return result.name()+"$result__"+ function.get(ast_ordinal.getUC_UID()).as_uint32();
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
        CType cType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        if(isVariable(variable)){
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand = getExpression(variable,variableType, fileloc);
            return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
        } else if(variable.is_a(ast_class.getNC_ARRAYREF())){
            ast pointedto = variable.children().get(0).as_ast();
            if(isVariable(pointedto)){
                CType variableType = typeConverter.getCType(pointedto.get(ast_ordinal.getBASE_TYPE()).as_ast());
                CExpression operand = getExpression(pointedto, variableType, fileloc);
                return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
            }else if(pointedto.is_a(ast_class.getNC_STRING())){
                String value = pointedto.get(ast_ordinal.getBASE_VALUE()).as_str();
                cType = new CPointerType(true,false,CNumericTypes.CHAR);
                return new CStringLiteralExpression(fileloc, cType, value);
            }
        }

        return null;
    }


    /**
     * @Description //create expression in accordance with arithmetic operation, normalized ast
     * @Param [value_ast, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression createFromArithmeticOp(
            final ast value_ast, final FileLocation fileLocation) throws result {

        CBinaryExpression.BinaryOperator operator = getBinaryOperator(value_ast);

        final CType expressionType = typeConverter.getCType(value_ast.children().get(0).as_ast());

        ast operand1 = value_ast.children().get(0).as_ast(); // First operand
        //logger.log(Level.FINE, "Getting id expression for operand 1");
        CType op1type = typeConverter.getCType(operand1.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression operand1Exp = getExpression(operand1,op1type, fileLocation);

        ast operand2 =  value_ast.children().get(1).as_ast(); // Second operand
        CType op2type = typeConverter.getCType(operand2.get(ast_ordinal.getBASE_TYPE()).as_ast());
        //logger.log(Level.FINE, "Getting id expression for operand 2");
        CExpression operand2Exp = getExpression(operand2, op2type, fileLocation);

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

            ast constant = init.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            if(constant.is_a(ast_class.getUC_AGGREGATE())){
                initializer = getConstantAggregateInitializerFromUC(constant,type, fileLocation);
            }else{
                CExpression expression = getConstantFromUC(constant, type, fileLocation);
                initializer = new CInitializerExpression(fileLocation, expression);
            }
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_EXPRESSION())){
            ast expr = init.get(ast_ordinal.getUC_EXPR()).as_ast();
            CExpression expression = getExpressionFromUC(expr, type, fileLocation);
            initializer = new CInitializerExpression(fileLocation, expression);
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_CONSTRUCTOR())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_BITWISE_COPY())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_NONCONSTANT_AGGREGATE())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_ZERO())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_NONE())){

        }else if(init.is_a(ast_class.getUC_NO_INITIALIZER())){
            return null;
        }else
            throw new RuntimeException("Unsupport initializer "+ init.toString());

        return initializer;
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
            CType valueType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

            if (isConstantAggregateZero(value_ast, valueType)) {
                CType expressionType = typeConverter.getCType(type_ast);
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
                        fileLocation, getExpression(value_ast, valueType, fileLocation));
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
        if(constant.has_field(ast_ordinal.getUC_CONSTANT_LIST())){
            ast constantList = constant.get(ast_ordinal.getUC_CONSTANT_LIST()).as_ast();
            int length = (int)constantList.children().size();
            List<CInitializer> elementInitializers = new ArrayList<>(length);
            for(int i=0;i<length;i++){
                ast elementAST = constantList.children().get(i).as_ast();
                CInitializer elementInitializer;
                ast elementType_ast = elementAST.get(ast_ordinal.getBASE_TYPE()).as_ast();
                CType elementType = typeConverter.getCType(elementType_ast);
                if(isConstantAggreateZeroFromUC(elementAST)){
                    elementInitializer =
                            getZeroInitializer(elementType, fileLoc);
                }else if(isArrayType(elementType_ast) ||
                        isStructType(elementType_ast) ||
                        isEnumType(elementType_ast) ||
                        elementType_ast.has_field(ast_ordinal.getUC_CONSTANT_LIST())){
                    elementInitializer = getConstantAggregateInitializerFromUC(elementAST, elementType, fileLoc);
                } else {
                    elementInitializer = new CInitializerExpression(
                            fileLoc, getConstantFromUC(elementAST,
                            typeConverter.getCType(elementAST.get(ast_ordinal.getBASE_TYPE()).as_ast()),fileLoc));
                }
                elementInitializers.add(elementInitializer);
            }

            return new CInitializerList(fileLoc, elementInitializers);
        }else {
            if(isArrayType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast())||
                    isStructType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast()) ||
                    isEnumType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast()) ){
                return new CInitializerList(fileLoc, new ArrayList<>());
            }else

                return getZeroInitializer(
                    typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast()),
                    fileLoc);
        }
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
        CType type = typeConverter.getCType(no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

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
                        getZeroInitializer(typeConverter.getCType(elementType_ast), fileLoc);
            }else if(isArrayType(elementType_ast) ||
                    isStructType(elementType_ast) ||
                    isEnumType(elementType_ast)){
                elementInitializer = getConstantAggregateInitializer(element.as_ast(), fileLoc);
            } else {
                elementInitializer = new CInitializerExpression(
                        fileLoc, (CExpression) getConstant(element.as_ast(),
                                    typeConverter.getCType(elementType_ast),fileLoc));
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

            List<CCompositeType.CCompositeTypeMemberDeclaration> members = ((CCompositeType) canonicalType).getMembers();
            List<CInitializer> initializers = new ArrayList<>(members.size());
            for (CCompositeType.CCompositeTypeMemberDeclaration m : members) {
                CType memberType = m.getType();
                CInitializer memberInit = getZeroInitializer(memberType, fileLoc);
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
            CType constantType = typeConverter.getCType(typeAST);
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
            return getExpression(value_ast,pExpectedType, fileLoc);
    }

    /**
     * @Description //get constant, e.g., int a[20]="hello world!"
     * @Param [constant, pExpectedType, fileLocation]
     * @return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getConstantFromUC(ast constant, CType pExpectedType, FileLocation fileLocation) throws result{

        if(constant.is_a(ast_class.getUC_CONSTANT_ADDRESS())){
            CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getConstantFromUC(constant.get(ast_ordinal.getUC_CONSTANT()).as_ast(),type, fileLocation);
        }else if(constant.is_a(ast_class.getUC_STRING())){
            String value = constant.get(ast_ordinal.getBASE_VALUE()).as_str();
            //CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CPointerType pointerType = new CPointerType(true, false, CNumericTypes.CHAR);
            return new CStringLiteralExpression(fileLocation, pointerType, value);
        }else if(constant.has_field(ast_ordinal.getBASE_VALUE())){
            ast typeAST = constant.get(ast_ordinal.getBASE_TYPE()).as_ast();
            CType type = typeConverter.getCType(typeAST);

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
                    long value = Long.valueOf(constant.get(ast_ordinal.getBASE_VALUE()).as_ast().pretty_print());
                    return new CIntegerLiteralExpression(fileLocation,pExpectedType, BigInteger.valueOf(value));
                }
            }else if(cBasicType.equals(CBasicType.FLOAT)){
                double value = Double.valueOf(constant.get(ast_ordinal.getBASE_VALUE()).as_ast().pretty_print());
                return new CFloatLiteralExpression(fileLocation, pExpectedType, BigDecimal.valueOf(value));
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
            CType type = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return getExpressionFromUC(expr, type, fileLocation);
        }else {
            throw  new RuntimeException("Unsupported type "+pExpectedType.toString()+" GETCONSTANT");
        }
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
