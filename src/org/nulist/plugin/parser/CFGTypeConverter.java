/**
 * @ClassName CFGTypeConverter
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/28/19 5:42 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nulist.plugin.parser.CFGAST.*;

public class CFGTypeConverter {
    private final MachineModel machineModel;
    private final LogManager logger;
    private static final CSimpleType ARRAY_LENGTH_TYPE = CNumericTypes.LONG_LONG_INT;
    private final Map<Integer, CType> typeCache = new HashMap<>();

    public CFGTypeConverter(final MachineModel pMachineModel, final LogManager pLogger) {
        machineModel = pMachineModel;
        logger = pLogger;
        basicTypeInitialization();
    }

    //TODO
    //since codesurfer normalizes bool into unsigned char, which may confuse model checking
    // we need to input unnormalized type
    public CType getCType(ast type){
        try {
            String typeString = type.pretty_print();
            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            if(cType!=null)
                return cType;
            else if(isStructType(type)){//struct
                return createStructType(type);
            }else if(isPointerType(type)){
                ast pointedTo = type.get(ast_ordinal.getBASE_POINTED_TO()).as_ast();
                cType = getCType(pointedTo);
                CPointerType cPointerType = new CPointerType(isConstantType(type), false, cType);
                typeCache.put(typeString.hashCode(),cPointerType);
                return cPointerType;
            }else if(isArrayType(type)){
                long length = type.get(ast_ordinal.getBASE_NUM_ELEMENTS()).as_uint32();
                ast elementType = type.get(ast_ordinal.getBASE_ELEMENT_TYPE()).as_ast();
                cType = getCType(elementType);
                CIntegerLiteralExpression arrayLength =
                        new CIntegerLiteralExpression(
                                FileLocation.DUMMY,
                                ARRAY_LENGTH_TYPE,
                                BigInteger.valueOf(length));
                CArrayType cArrayType =  new CArrayType(
                        isConstantType(type), false, cType, arrayLength);
                typeCache.put(typeString.hashCode(),cArrayType);
                return cArrayType;
            }else if(isTypeRef(type)){

            }else if(isEnumType(type)){

            }

            return null;
        }catch (result r){
            return null;
        }

    }


    /**
     *@Description basic type initialization
     *@Param []
     *@return void
     **/
    private void basicTypeInitialization(){
        typeCache.put("bool".hashCode(),CNumericTypes.BOOL);
        typeCache.put("void".hashCode(),CVoidType.VOID);
        typeCache.put("int".hashCode(),CNumericTypes.INT);
        typeCache.put("unsigned int".hashCode(),CNumericTypes.UNSIGNED_INT);
        typeCache.put("signed int".hashCode(),CNumericTypes.SHORT_INT);
        typeCache.put("char".hashCode(),CNumericTypes.CHAR);
        typeCache.put("unsigned char".hashCode(),CNumericTypes.UNSIGNED_CHAR);
        typeCache.put("signed char".hashCode(),CNumericTypes.SIGNED_CHAR);
        typeCache.put("short".hashCode(),CNumericTypes.SHORT_INT);
        typeCache.put("unsigned short".hashCode(),CNumericTypes.UNSIGNED_SHORT_INT);
        typeCache.put("long".hashCode(),CNumericTypes.LONG_INT);
        typeCache.put("unsigned long".hashCode(),CNumericTypes.UNSIGNED_LONG_INT);
        typeCache.put("signed long".hashCode(),CNumericTypes.SIGNED_LONG_INT);
        typeCache.put("long long".hashCode(),CNumericTypes.LONG_LONG_INT);
        typeCache.put("unsigned long long".hashCode(),CNumericTypes.UNSIGNED_LONG_LONG_INT);
        typeCache.put("signed long long".hashCode(),CNumericTypes.SIGNED_LONG_LONG_INT);
        typeCache.put("float".hashCode(),CNumericTypes.FLOAT);
        typeCache.put("double".hashCode(),CNumericTypes.DOUBLE);
        typeCache.put("long double".hashCode(),CNumericTypes.LONG_DOUBLE);
        typeCache.put("const bool".hashCode(),CNumericTypes.CONST_BOOL);
        typeCache.put("const int".hashCode(),CNumericTypes.CONST_INT);
        typeCache.put("const unsigned int".hashCode(),CNumericTypes.CONST_UNSIGNED_INT);
        typeCache.put("const signed int".hashCode(),CNumericTypes.CONST_SHORT_INT);
        typeCache.put("const char".hashCode(),CNumericTypes.CONST_CHAR);
        typeCache.put("const unsigned char".hashCode(),CNumericTypes.CONST_UNSIGNED_CHAR);
        typeCache.put("const signed char".hashCode(),CNumericTypes.CONST_SIGNED_CHAR);
        typeCache.put("const short".hashCode(),CNumericTypes.CONST_SHORT_INT);
        typeCache.put("const unsigned short".hashCode(),CNumericTypes.CONST_UNSIGNED_SHORT_INT);
        typeCache.put("const long".hashCode(),CNumericTypes.CONST_LONG_INT);
        typeCache.put("const unsigned long".hashCode(),CNumericTypes.CONST_UNSIGNED_LONG_INT);
        typeCache.put("const signed long".hashCode(),CNumericTypes.CONST_SIGNED_LONG_INT);
        typeCache.put("const long long".hashCode(),CNumericTypes.CONST_LONG_LONG_INT);
        typeCache.put("const unsigned long long".hashCode(),CNumericTypes.CONST_UNSIGNED_LONG_LONG_INT);
        typeCache.put("const signed long long".hashCode(),CNumericTypes.CONST_SIGNED_LONG_LONG_INT);
        typeCache.put("const float".hashCode(),CNumericTypes.CONST_FLOAT);
        typeCache.put("const double".hashCode(),CNumericTypes.CONST_DOUBLE);
        typeCache.put("const long double".hashCode(),CNumericTypes.CONST_LONG_DOUBLE);
    }


    /**
     *@Description generate the struct type from an ast
     *@Param [type]
     *@return org.sosy_lab.cpachecker.cfa.types.c.CType
     **/
    //struct type
    private CType createStructType(ast type) throws result{
        final boolean isConst = false;
        final boolean isVolatile = false;

        //typedef struct: label
        //normal struct: struct label
        String structName = type.pretty_print();
        if(structName.startsWith("const "))//normalize const struct and non-const struct
            structName = structName.substring(6);

        ast struct_type = getStructType(type);
        if(struct_type.is_a(ast_class.getUC_TYPEREF())){

        }else

        if(typeCache.containsKey(structName.hashCode())){
            return new CElaboratedType(
                    false,
                    false,
                    CComplexType.ComplexTypeKind.STRUCT,
                    structName,
                    structName,
                    (CComplexType) typeCache.get(structName.hashCode()));
        }


        CCompositeType cStructType =
                new CCompositeType(isConst, isVolatile, CComplexType.ComplexTypeKind.STRUCT, structName, structName);

        //normalized type
        ast_field_vector items;
        if(struct_type.is_a(ast_class.getUC_STRUCT()))
            items = struct_type.get(ast_ordinal.getUC_FIELDS()).as_ast().children();
        else
            items =struct_type.children(); //

        List<CCompositeType.CCompositeTypeMemberDeclaration> members =
                new ArrayList<>((int)items.size());

        for (int i = 0; i < items.size(); i++) {
            ast_field member = items.get(i);
            String memberName = member.as_ast().pretty_print();

            CType cMemType = getCType(member.as_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());
            CCompositeType.CCompositeTypeMemberDeclaration memDecl =
                    new CCompositeType.CCompositeTypeMemberDeclaration(cMemType, memberName);
            members.add(memDecl);
        }

        cStructType.setMembers(members);

        typeCache.put(structName.hashCode(), cStructType);
        return cStructType;
    }

}
