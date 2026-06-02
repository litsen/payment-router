package com.company.payrouter.modules.gateway.channel;

import com.company.payrouter.modules.gateway.channel.ChannelDtos.BarcodeChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.ChannelContext;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.ChannelResponse;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.AlipayJsapiChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.DecodeBarChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.H5ChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.PreOrderChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.QrcodeChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.QueryChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.RefundChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.RefundQueryChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.ScanChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.WechatJsapiChannelRequest;

public interface PaymentChannelAdapter {
    ChannelResponse barcodePay(BarcodeChannelRequest request, ChannelContext context);

    ChannelResponse preOrder(PreOrderChannelRequest request, ChannelContext context);

    ChannelResponse decodeBar(DecodeBarChannelRequest request, ChannelContext context);

    ChannelResponse scanPay(ScanChannelRequest request, ChannelContext context);

    ChannelResponse h5Pay(H5ChannelRequest request, ChannelContext context);

    ChannelResponse qrcodePay(QrcodeChannelRequest request, ChannelContext context);

    ChannelResponse wechatJsapiPay(WechatJsapiChannelRequest request, ChannelContext context);

    ChannelResponse alipayJsapiPay(AlipayJsapiChannelRequest request, ChannelContext context);

    ChannelResponse queryPay(QueryChannelRequest request, ChannelContext context);

    ChannelResponse refund(RefundChannelRequest request, ChannelContext context);

    ChannelResponse queryRefund(RefundQueryChannelRequest request, ChannelContext context);
}
