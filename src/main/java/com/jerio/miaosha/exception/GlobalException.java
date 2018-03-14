package com.jerio.miaosha.exception;

import com.jerio.miaosha.result.CodeMsg;

/**
 * Created by Jerio on 2018/3/14.
 */
public class GlobalException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    private CodeMsg cm;

    public GlobalException(CodeMsg cm) {
        super(cm.toString());
        this.cm = cm;
    }

    public CodeMsg getCm() {
        return cm;
    }
}
