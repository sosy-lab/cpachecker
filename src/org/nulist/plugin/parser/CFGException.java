/**
 * @ClassName CFGException
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/3/19 1:41 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.google.common.base.Preconditions;
import com.grammatech.cs.result;

import java.io.Serializable;


public class CFGException extends Exception implements Serializable{
    private static final long serialVersionUID = -4447558053972827144L;

    public CFGException(String message) {
        super(message);
        Preconditions.checkNotNull(message);
    }
}
