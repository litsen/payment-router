package com.company.payrouter.modules.gateway.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.AlipayJsapiPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.BarcodePayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.DecodeBarRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.DecodeBarResponse;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.H5PayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayResponse;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PreOrderRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.QrcodePayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.QueryPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundQueryRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundResponse;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.ScanPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.WechatJsapiPayRequest;
import com.company.payrouter.modules.order.service.PaymentGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class BarcodePaymentController {
    private final PaymentGatewayService paymentGatewayService;

    public BarcodePaymentController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/barcode")
    public ApiResult<PayResponse> barcodePay(@Valid @RequestBody BarcodePayRequest request) {
        return ApiResult.success(paymentGatewayService.barcodePay(request));
    }

    @PostMapping("/pre-order")
    public ApiResult<PayResponse> preOrder(@Valid @RequestBody PreOrderRequest request) {
        return ApiResult.success(paymentGatewayService.preOrder(request));
    }

    @PostMapping("/decode-bar")
    public ApiResult<DecodeBarResponse> decodeBar(@Valid @RequestBody DecodeBarRequest request) {
        return ApiResult.success(paymentGatewayService.decodeBar(request));
    }

    @PostMapping("/scan")
    public ApiResult<PayResponse> scanPay(@Valid @RequestBody ScanPayRequest request) {
        return ApiResult.success(paymentGatewayService.scanPay(request));
    }

    @PostMapping("/h5")
    public ApiResult<PayResponse> h5Pay(@Valid @RequestBody H5PayRequest request) {
        return ApiResult.success(paymentGatewayService.h5Pay(request));
    }

    @PostMapping("/qrcode")
    public ApiResult<PayResponse> qrcodePay(@Valid @RequestBody QrcodePayRequest request) {
        return ApiResult.success(paymentGatewayService.qrcodePay(request));
    }

    @PostMapping("/wechat-jsapi")
    public ApiResult<PayResponse> wechatJsapiPay(@Valid @RequestBody WechatJsapiPayRequest request) {
        return ApiResult.success(paymentGatewayService.wechatJsapiPay(request));
    }

    @PostMapping("/alipay-jsapi")
    public ApiResult<PayResponse> alipayJsapiPay(@Valid @RequestBody AlipayJsapiPayRequest request) {
        return ApiResult.success(paymentGatewayService.alipayJsapiPay(request));
    }

    @PostMapping("/refund")
    public ApiResult<RefundResponse> refund(@Valid @RequestBody RefundRequest request) {
        return ApiResult.success(paymentGatewayService.refund(request));
    }

    @PostMapping("/refund/query")
    public ApiResult<RefundResponse> queryRefund(@Valid @RequestBody RefundQueryRequest request) {
        return ApiResult.success(paymentGatewayService.queryRefund(request));
    }

    @PostMapping("/query")
    public ApiResult<PayResponse> queryPay(@Valid @RequestBody QueryPayRequest request) {
        return ApiResult.success(paymentGatewayService.queryPay(request));
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(@RequestParam Map<String, String> params) {
        return paymentGatewayService.handleNotify(params);
    }
}
