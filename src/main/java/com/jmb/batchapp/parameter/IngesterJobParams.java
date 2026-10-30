package com.jmb.batchapp.parameter;

public enum IngesterJobParams implements Parameter<IngesterJobParams> {

    READ_FORMAT("readFormat"),
    FILE_NAME("fileName"),
    /** Optional: "overwrite" clears SALES before loading so a re-run does not double count. Default: append. */
    WRITE_MODE("mode");

    //Others to be added...

    private String paramName;

    IngesterJobParams(String paramName) {
        this.paramName = paramName;
    }

    @Override
    public String getParamName() {
        return paramName;
    }
}
