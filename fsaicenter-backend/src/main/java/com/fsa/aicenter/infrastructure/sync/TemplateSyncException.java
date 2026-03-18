package com.fsa.aicenter.infrastructure.sync;

/**
 * 模板同步异常
 *
 * @author FSA AI Center
 */
public class TemplateSyncException extends RuntimeException {

    private final String sourceName;

    public TemplateSyncException(String sourceName, String message) {
        super(message);
        this.sourceName = sourceName;
    }

    public TemplateSyncException(String sourceName, String message, Throwable cause) {
        super(message, cause);
        this.sourceName = sourceName;
    }

    public String getSourceName() {
        return sourceName;
    }
}
