/**
 * @ClassName CFGTypeConverter
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/28/19 5:42 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.ast;
import com.grammatech.cs.ast_field;
import com.grammatech.cs.result;
import com.grammatech.cs.symbol;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CFGTypeConverter {
    private final MachineModel machineModel;
    private final LogManager logger;

    private final Map<Integer, CType> typeCache = new HashMap<>();

    public CFGTypeConverter(final MachineModel pMachineModel, final LogManager pLogger) {
        machineModel = pMachineModel;
        logger = pLogger;
    }


    public CType getCType(symbol param_symbol) throws result {
        return getCType(param_symbol.get_type());
    }

    public CType getCType(ast type) throws result {
        String typeString = type.pretty_print();
        CType cType = getCSimpleType(typeString);
        if(cType!=null)
            return cType;
        else {

            return null;
        }
    }

    private CType getCSimpleType(String typeString){

        switch (typeString){
            case "void":
                return CVoidType.VOID;
            case "int":
                return CNumericTypes.INT;
            case "unsigned int":
                return CNumericTypes.UNSIGNED_INT;
            case "signed int":
                return CNumericTypes.SIGNED_INT;
            case "char":
                return CNumericTypes.CHAR;
            case "unsigned char":
                return CNumericTypes.UNSIGNED_CHAR;
            case "signed char":
                return CNumericTypes.SIGNED_CHAR;
            case "short":
                return CNumericTypes.SHORT_INT;
            case "unsigned short":
                return CNumericTypes.UNSIGNED_SHORT_INT;
            case "long":
                return CNumericTypes.LONG_INT;
            case "unsigned long":
                return CNumericTypes.UNSIGNED_LONG_INT;
            case "signed long":
                return CNumericTypes.SIGNED_LONG_INT;
            case "long long":
                return CNumericTypes.LONG_LONG_INT;
            case "unsigned long long":
                return CNumericTypes.UNSIGNED_LONG_LONG_INT;
            case "signed long long":
                return CNumericTypes.SIGNED_LONG_LONG_INT;
            case "float":
                return CNumericTypes.FLOAT;
            case "double":
                return CNumericTypes.DOUBLE;
            case "long double":
                return CNumericTypes.LONG_DOUBLE;
        }

        return  null;
    }

    private CType createStructType(ast type){
        return null;
    }

}
