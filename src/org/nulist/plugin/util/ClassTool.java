/**
 * @ClassName ClassTool
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/1/19 1:09 PM
 * @Version 1.0
 **/
package org.nulist.plugin.util;

public class ClassTool {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static long getUnsignedInt(int x){
        return x & 0x00000000ffffffffL;
    }

    public static void printf(String content){
        System.out.println(content);
    }

    public static void printWARNING(String content){
        System.out.println(ANSI_RED+content+ANSI_RESET);
    }

    public static void printINFO(String content){
        System.out.println(ANSI_YELLOW+content+ANSI_RESET);
    }

}
