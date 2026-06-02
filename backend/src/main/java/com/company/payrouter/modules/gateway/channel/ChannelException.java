package com.company.payrouter.modules.gateway.channel;

import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;

public class ChannelException extends BizException {
    private final String rawResponse;

    public ChannelException(String message, String rawResponse) {
        super(BusinessErrorCode.CHANNEL_ERROR, message);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
