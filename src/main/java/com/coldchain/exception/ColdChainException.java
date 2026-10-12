package com.coldchain.exception;
/** Custom unchecked exception used across all layers. */
public class ColdChainException extends RuntimeException {
    public ColdChainException(String msg) { super(msg); }
    public ColdChainException(String msg, Throwable cause) { super(msg, cause); }
}
