/**
 * @ClassName ClassTool
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/1/19 1:09 PM
 * @Version 1.0
 **/
package org.nulist.plugin.util;

public class ClassTool {

    public static long getUnsignedInt(int x){
        return x & 0x00000000ffffffffL;
    }


}
