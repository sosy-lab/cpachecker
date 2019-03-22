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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nulist.plugin.parser.CFGAST.*;

public class CFGTypeConverter {
    private final LogManager logger;
    private final String STRUCT_PREF = "struct __STRUCT__";
    private final String UNION_PREF = "__UNION__";
    private final String ENUM_PREF = "__ENUM__";
    private static final CSimpleType ARRAY_LENGTH_TYPE = CNumericTypes.LONG_LONG_INT;
    private final Map<Integer, CType> typeCache = new HashMap<>();

    public CFGTypeConverter(final LogManager pLogger) {
        logger = pLogger;
        basicTypeInitialization();
    }

    private String handleUnnamedType(ast type) throws result{
        String typeString = type.pretty_print();
        String typeName ="";
        if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>") ){
            if(isStructType(type)){
                typeName=STRUCT_PREF;
                if(type.is_a(ast_class.getUC_STRUCT()))
                    typeName += type.get(ast_ordinal.getUC_UID()).as_uint32();
                else {
                    typeName += type.get(ast_ordinal.getNC_UNNORMALIZED()).get(ast_ordinal.getUC_UID()).as_uint32();
                }
                return typeName;
            } else if(isUnionType(type)){
                typeName=UNION_PREF;
                if(type.is_a(ast_class.getUC_UNION()))
                    typeName += type.get(ast_ordinal.getUC_UID()).as_uint32();
                else {
                    typeName += type.get(ast_ordinal.getNC_UNNORMALIZED()).get(ast_ordinal.getUC_UID()).as_uint32();
                }
                return typeName;
            }
            return typeString;
        }else
            return typeString;
    }
    //
    //since codesurfer normalizes bool into unsigned char, which may confuse model checking
    // we need to input unnormalized type
    public CType getCType(ast type){
        try {
            String typeString = handleUnnamedType(type);
            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            if(cType!=null)
                return cType;
            else
                return getCType(type, false);
        }catch (result r){
            return null;
        }
    }

    private CType getCType(ast type, boolean isConst){
        try {
            String typeString = type.pretty_print();

            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            //if(isConst && !typeString.startsWith("const "))
            //    cType = typeCache.getOrDefault(("const "+typeString).hashCode(),null);
            if(cType!=null)
                return cType;
            else if(isTypeRef(type)){
                ast originTypeAST = type.get(ast_ordinal.getBASE_TYPE()).as_ast();
                CType cTypedefType;
                if(typeString.startsWith("const ")){
                    cTypedefType = getCType(originTypeAST, true);
                }else {
                    CType originType = getCType(originTypeAST, isConst);
                    cTypedefType = new CTypedefType(originType.isConst(),
                                                                originType.isVolatile(),
                                                                typeString,
                                                                originType);
                }
                typeCache.put(typeString.hashCode(), cTypedefType);
                return cTypedefType;
            }else if(isStructType(type)){//struct
                return createStructType(type, isConst);
            }else if(isPointerType(type)){
                ast pointedTo = type.get(ast_ordinal.getBASE_POINTED_TO()).as_ast();
                cType = getCType(pointedTo);
                CPointerType cPointerType = new CPointerType(isConst, false, cType);
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
                        isConst, false, cType, arrayLength);
                typeCache.put(typeString.hashCode(),cArrayType);
                return cArrayType;
            }else if(isEnumType(type)){
                ast constantList = type.get(ast_ordinal.getUC_CONSTANT_LIST()).as_ast();
                List<CEnumType.CEnumerator> enumerators = new ArrayList<>();
                for(int i=0;i<constantList.children().size();i++){
                    ast enumer = constantList.children().get(i).as_ast();
                    String name = enumer.pretty_print();
                    long value = enumer.get(ast_ordinal.getBASE_VALUE()).as_ast()
                                    .get(ast_ordinal.getBASE_VALUE()).as_int32();
                    CEnumType.CEnumerator enumerator =
                            new CEnumType.CEnumerator(FileLocation.DUMMY,
                                                      name,
                                                      name,
                                                      value);
                    enumerators.add(enumerator);
                }
                String typeName = typeString;
                if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>")){
                    typeName = handleUnnamedType(type);
                    typeString = "";
                }
                CEnumType cEnumType = new CEnumType(isConst, false,
                        enumerators, typeName, typeString);
                CElaboratedType cElaboratedType= new CElaboratedType(isConst, false,
                                            CComplexType.ComplexTypeKind.ENUM,
                                             typeName,typeString,cEnumType);
                typeCache.put(typeName.hashCode(), cElaboratedType);
                return cElaboratedType;
            }else if(type.is_a(ast_class.getNC_UNION())){
                List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>();
                for(int i=0;i<type.children().size();i++){
                    ast memberAST = type.children().get(i).as_ast();
                    CType memeberType = getCType(memberAST.get(ast_ordinal.getBASE_TYPE()).as_ast());
                    String memberName = memberAST.get(ast_ordinal.getBASE_NAME()).as_str();
                    CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration =
                            new CCompositeType.CCompositeTypeMemberDeclaration(memeberType, memberName);
                    members.add(memberDeclaration);
                }
                String typeName = typeString;
                if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>") ){
                    typeName =handleUnnamedType(type);
                    typeString = "";
                }

                CCompositeType cCompositeType = new CCompositeType(isConst, false,
                        CComplexType.ComplexTypeKind.UNION,
                        members, typeName, typeString);
                CElaboratedType cElaboratedType= new CElaboratedType(isConst, false,
                        CComplexType.ComplexTypeKind.UNION,
                        typeName, typeString,cCompositeType);
                typeCache.put(typeName.hashCode(), cElaboratedType);
                return cElaboratedType;
            }else if(type.is_a(ast_class.getUC_VECTOR_TYPE())){//C++ only
                System.out.println();
            }else if(type.is_a(ast_class.getUC_ROUTINE_TYPE())){//
                //CFunctionTypeWithNames

                CType returnType = getCType(type.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast());
                List<CParameterDeclaration> paramList = new ArrayList<>();
                if(type.has_field(ast_ordinal.getUC_PARAM_TYPES())){
                    ast param_types = type.get(ast_ordinal.getUC_PARAM_TYPES()).as_ast();
                    for(int i=0;i<param_types.children().size();i++){
                        ast param = param_types.children().get(0).as_ast();
                        CType paramType = getCType(param.get(ast_ordinal.getBASE_TYPE()).as_ast());
                        CParameterDeclaration paramDeclaration = new CParameterDeclaration()
                    }
                }
                CFunctionTypeWithNames cFunctionTypeWithNames= new CFunctionTypeWithNames(returnType, paramList, true);
            }else
                throw new RuntimeException("Unsupported type "+ type.toString());

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
    private CType createStructType(ast type, boolean isConst) throws result{

        final boolean isVolatile = false;

        //typedef struct: label
        //normal struct: struct label
        String structName = type.pretty_print();
        String typeName = structName;
        if(structName.endsWith("<UNNAMED>") || structName.endsWith("<unnamed>") ){
            typeName = handleUnnamedType(type);
            structName = "";
        }


        ast struct_type = getStructType(type);

        if(typeCache.containsKey(typeName.hashCode())){
            return new CElaboratedType(
                    false,
                    false,
                    CComplexType.ComplexTypeKind.STRUCT,
                    typeName,
                    structName,
                    (CComplexType) typeCache.get(typeName.hashCode()));
        }


        CCompositeType cStructType =
                new CCompositeType(isConst, isVolatile, CComplexType.ComplexTypeKind.STRUCT, typeName, structName);

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

        CElaboratedType cType = new CElaboratedType(isConst,
                                                    isVolatile,
                                                    CComplexType.ComplexTypeKind.STRUCT,
                                                    typeName,
                                                    structName,
                                                    cStructType);

        typeCache.put(typeName.hashCode(), cType);
        return cType;
    }

}
