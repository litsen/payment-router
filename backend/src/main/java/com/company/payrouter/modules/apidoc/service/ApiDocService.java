package com.company.payrouter.modules.apidoc.service;

import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.apidoc.dto.ApiDocDtos.ApiDocErrorCode;
import com.company.payrouter.modules.apidoc.dto.ApiDocDtos.ApiDocParameter;
import com.company.payrouter.modules.apidoc.dto.ApiDocDtos.ApiDocResponse;
import com.company.payrouter.modules.apidoc.dto.ApiDocDtos.ApiDocSummary;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiDocService {
    private static final List<ApiDocErrorCode> ERROR_CODES = List.of(
            errorCode("SUCCESS", 0, "成功"),
            errorCode(BusinessErrorCode.INVALID_APP_ID, "appId 无效或已停用"),
            errorCode(BusinessErrorCode.INVALID_SIGN, "请求签名错误"),
            errorCode(BusinessErrorCode.REQUEST_EXPIRED, "timestamp 超出 5 分钟时间窗口"),
            errorCode(BusinessErrorCode.DUPLICATE_NONCE, "nonce 重复，疑似重放请求"),
            errorCode(BusinessErrorCode.UNSUPPORTED_PAY_METHOD, "支付方式不支持"),
            errorCode(BusinessErrorCode.PAY_METHOD_DISABLED, "支付方式未开启"),
            errorCode(BusinessErrorCode.ORDER_ALREADY_FAILED, "原订单已失败，请更换商户订单号"),
            errorCode(BusinessErrorCode.ORDER_NOT_FOUND, "订单不存在"),
            errorCode(BusinessErrorCode.ORDER_NOT_REFUNDABLE, "仅成功订单可退款"),
            errorCode(BusinessErrorCode.INVALID_REQUEST_PARAMETER, "请求参数不合法"),
            errorCode(BusinessErrorCode.NO_AVAILABLE_ACCOUNT, "无可用支付参数或商户号"),
            errorCode(BusinessErrorCode.ROUTE_FAILED, "路由处理失败"),
            errorCode(BusinessErrorCode.CHANNEL_ERROR, "上游通道错误"),
            errorCode(BusinessErrorCode.INVALID_NOTIFY, "通知报文不合法"),
            errorCode(BusinessErrorCode.INVALID_NOTIFY_SIGN, "通知验签失败"),
            errorCode(BusinessErrorCode.TOO_MANY_REQUESTS, "请求过于频繁"),
            errorCode(BusinessErrorCode.INTERNAL_ERROR, "系统异常")
    );

    private final Map<String, ApiDocResponse> docs = new LinkedHashMap<>();

    public ApiDocService() {
        register(doc(
                "barcode-pay",
                "条码支付接口",
                "外部商户系统提交付款码、订单号、金额和签名后，系统完成支付方式校验、路由选择、订单创建和上游通道调用。",
                """
                        POST /api/pay/barcode
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "P202605300001",
                          "amount": 1.00,
                          "authCode": "付款码",
                          "subject": "测试商品",
                          "notifyUrl": "https://merchant.example.com/pay/notify",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "appId": "MCH202605300001",
                            "merchantOrderNo": "P202605300001",
                            "amount": 1.00,
                            "payMethod": "BARCODE_PAY",
                            "status": "SUCCESS",
                            "platformOrderNo": "平台订单号",
                            "channelOrderNo": "上游流水号",
                            "message": "Payment success"
                          }
                        }""",
                "<p>同一 appId + merchantOrderNo 只允许创建一笔订单。SUCCESS、PAYING、UNKNOWN 会按当前订单状态返回；FAILED 订单不允许用同一商户订单号再次支付。</p>"
        ));
        register(doc(
                "query-pay",
                "查单接口",
                "外部商户系统按 appId 和 merchantOrderNo 查询订单状态。非终态订单会触发一次上游查单。",
                """
                        POST /api/pay/query
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "P202605300001",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "appId": "MCH202605300001",
                            "merchantOrderNo": "P202605300001",
                            "amount": 1.00,
                            "payMethod": "BARCODE_PAY",
                            "status": "PAYING",
                            "platformOrderNo": "平台订单号",
                            "channelOrderNo": "上游流水号",
                            "message": "Waiting for payment"
                          }
                        }""",
                "<p>订单不存在时返回 ORDER_NOT_FOUND。SUCCESS 和 FAILED 为终态，查询时直接返回当前状态。</p>"
        ));
        register(doc(
                "scan-pay",
                "聚合扫码支付接口",
                "外部商户系统创建 LFWin 聚合扫码支付订单，系统调用 /payapi/trans/kxpay，使用 service=pay.comm.jspay，返回二维码或支付链接参数。",
                """
                        POST /api/pay/scan
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "S202605300001",
                          "amount": 1.00,
                          "subject": "测试商品",
                          "notifyUrl": "https://merchant.example.com/pay/notify",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "payMethod": "SCAN_PAY",
                            "status": "PAYING",
                            "payData": {
                              "qrCode": "二维码内容"
                            }
                          }
                        }""",
                "<p>聚合扫码支付创建成功后通常返回 PAYING，商户侧展示 payData 中的 url、qr_code 或 code_url，后续通过通知或查单确认终态。该接口固定映射 LFWin /payapi/trans/kxpay。</p>"
        ));
        register(doc(
                "qrcode-pay",
                "扫码支付接口",
                "外部商户系统创建 LFWin 指定通道扫码支付订单，系统调用 /payapi/pay/qrcode，service 必填并参与签名。",
                """
                        POST /api/pay/qrcode
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "Q202605300001",
                          "amount": 1.00,
                          "subject": "测试商品",
                          "service": "pay.wxpay.qrcode",
                          "notifyUrl": "https://merchant.example.com/pay/notify",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "payMethod": "QRCODE_PAY",
                            "status": "PAYING",
                            "payData": {
                              "qr_code": "二维码内容",
                              "code_url": "二维码图片地址"
                            }
                          }
                        }""",
                "<p>service 仅支持 pay.alipay.qrcode、pay.wxpay.qrcode、pay.unpay.qrcode。创建成功后展示 payData 中的 qr_code、code_url 或 url，后续通过通知或查单确认终态。</p>"
        ));
        register(doc(
                "h5-pay",
                "H5/链接跳转支付接口",
                "外部商户系统创建 H5/链接跳转支付订单，系统返回跳转支付链接。",
                """
                        POST /api/pay/h5
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "H202605300001",
                          "amount": 1.00,
                          "subject": "测试商品",
                          "notifyUrl": "https://merchant.example.com/pay/notify",
                          "returnUrl": "https://merchant.example.com/pay/success",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "payMethod": "H5_PAY",
                            "status": "PAYING",
                            "payData": {
                              "payUrl": "https://pay.example.com/h5/..."
                            }
                          }
                        }""",
                "<p>returnUrl 传入时参与签名。H5 支付创建成功后由商户前端跳转到 payData 中的支付地址。</p>"
        ));
        register(doc(
                "wechat-jsapi-pay",
                "微信公众号和小程序支付接口",
                "外部商户系统创建微信公众号或小程序支付订单，系统返回前端调起微信支付参数。",
                """
                        POST /api/pay/wechat-jsapi
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "J202605300001",
                          "amount": 1.00,
                          "subject": "测试商品",
                          "notifyUrl": "https://merchant.example.com/pay/notify",
                          "subAppId": "wx-sub-appid",
                          "subOpenId": "wx-openid",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "payMethod": "WECHAT_JSAPI_PAY",
                            "status": "PAYING",
                            "payData": {
                              "appId": "wx-sub-appid",
                              "timeStamp": "1790000000",
                              "nonceStr": "random",
                              "package": "prepay_id=xxx",
                              "signType": "MD5",
                              "paySign": "pay_sign"
                            }
                          }
                        }""",
                "<p>subAppId 和 subOpenId 必填，并参与签名。该接口映射 LFWin /payapi/mini/wxpay。</p>"
        ));
        register(doc(
                "alipay-jsapi-pay",
                "支付宝生活号和小程序支付接口",
                "外部商户系统创建支付宝生活号或小程序支付订单，系统返回上游支付参数。",
                """
                        POST /api/pay/alipay-jsapi
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "A202605300001",
                          "amount": 1.00,
                          "subject": "测试商品",
                          "notifyUrl": "https://merchant.example.com/pay/notify",
                          "subAppId": "alipay-sub-appid",
                          "buyerId": "2088...",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "payMethod": "ALIPAY_JSAPI_PAY",
                            "status": "PAYING",
                            "payData": {
                              "tradeNo": "mock-trade-no"
                            }
                          }
                        }""",
                "<p>buyerId 和 buyerOpenId 二选一必填。subAppId 可选，传入时参与签名。该接口映射 LFWin /payapi/trade/alipay。</p>"
        ));
        register(doc(
                "refund",
                "退款接口",
                "外部商户系统对本系统已成功支付订单发起退款，系统按原订单路由到对应上游商户号。",
                """
                        POST /api/pay/refund
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "P202605300001",
                          "merchantRefundNo": "R202605300001",
                          "refundAmount": 1.00,
                          "reason": "用户申请退款",
                          "notifyUrl": "https://merchant.example.com/refund/notify",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "appId": "MCH202605300001",
                            "merchantOrderNo": "P202605300001",
                            "merchantRefundNo": "R202605300001",
                            "refundAmount": 1.00,
                            "status": "PROCESSING",
                            "data": {
                              "refundStatus": "PROCESSING"
                            },
                            "message": "SUCCESS"
                          }
                        }""",
                "<p>仅允许对 SUCCESS 支付订单发起退款。refundAmount 单位为元，并参与签名。merchantRefundNo、reason、notifyUrl 传入时也参与签名。退款请求会映射 LFWin /payapi/pay/refund_order。</p>"
        ));
        register(doc(
                "refund-query",
                "退款查询接口",
                "外部商户系统按原支付订单号和可选退款单号查询退款处理结果。",
                """
                        POST /api/pay/refund/query
                        Content-Type: application/json

                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "P202605300001",
                          "merchantRefundNo": "R202605300001",
                          "timestamp": 1790000000000,
                          "nonce": "random_string",
                          "sign": "sha256_hex_sign"
                        }""",
                """
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "appId": "MCH202605300001",
                            "merchantOrderNo": "P202605300001",
                            "merchantRefundNo": "R202605300001",
                            "refundAmount": null,
                            "status": "SUCCESS",
                            "data": {
                              "refundStatus": "SUCCESS"
                            },
                            "message": "SUCCESS"
                          }
                        }""",
                "<p>merchantRefundNo 可选；如果传入则参与签名。退款查询会映射 LFWin /payapi/pay/query_refund。返回状态统一为 SUCCESS、FAILED、PROCESSING。</p>"
        ));
        register(doc(
                "sign",
                "签名规则",
                "对外支付、查单、退款和退款查询接口均使用相同的下游商户签名规则。",
                """
                        待签名参数示例：
                        {
                          "appId": "MCH202605300001",
                          "merchantOrderNo": "P202605300001",
                          "amount": 1.00,
                          "timestamp": 1790000000000,
                          "nonce": "random_string"
                        }""",
                """
                        拼接结果：amount=1.00&appId=MCH202605300001&merchantOrderNo=P202605300001&nonce=random_string&timestamp=1790000000000{API_SECRET}

                        签名：SHA256(拼接结果).toLowerHex()""",
                "<ol><li>排除 sign 字段。</li><li>按参数名 ASCII 升序排序。</li><li>按 key=value 使用 & 拼接。</li><li>末尾直接拼接 API Secret，不额外增加参数名。</li><li>使用 SHA256 生成小写十六进制字符串。</li><li>timestamp 默认允许 5 分钟时间窗口，nonce 会做防重放校验。</li></ol>"
        ));
        register(new ApiDocResponse(
                "error-codes",
                "错误码说明",
                "对外支付接口建议统一使用以下业务错误码，便于商户系统排查和重试。",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ERROR_CODES
        ));
    }

    public List<ApiDocSummary> listDocs() {
        return docs.values().stream()
                .map(doc -> new ApiDocSummary(doc.slug(), doc.title(), "/api-docs/" + doc.slug()))
                .toList();
    }

    public ApiDocResponse getDoc(String slug) {
        ApiDocResponse doc = docs.get(slug);
        if (doc == null) {
            throw new BizException("接口文档不存在: " + slug);
        }
        return doc;
    }

    private void register(ApiDocResponse doc) {
        docs.put(doc.slug(), doc);
    }

    private static ApiDocResponse doc(String slug, String title, String description, String request, String response, String rules) {
        RequestAddress address = requestAddress(request);
        return new ApiDocResponse(slug, title, description, address.method(), address.path(), requestParams(slug), responseParams(slug), request, response, rules, null);
    }

    private static RequestAddress requestAddress(String request) {
        if (request == null) {
            return new RequestAddress(null, null);
        }
        for (String rawLine : request.stripLeading().split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("GET ") || line.startsWith("POST ") || line.startsWith("PUT ") || line.startsWith("DELETE ")) {
                String[] parts = line.split("\\s+", 2);
                return new RequestAddress(parts[0], parts.length > 1 ? parts[1] : null);
            }
        }
        return new RequestAddress(null, null);
    }

    private record RequestAddress(String method, String path) {
    }

    private static ApiDocErrorCode errorCode(BusinessErrorCode errorCode, String message) {
        return errorCode(errorCode.name(), errorCode.code(), message);
    }

    private static ApiDocErrorCode errorCode(String name, int code, String message) {
        return new ApiDocErrorCode(code + " " + name, message);
    }

    private static List<ApiDocParameter> requestParams(String slug) {
        return switch (slug) {
            case "barcode-pay" -> withCommonPayParams(param("authCode", "string", "是", "付款码，通常来自微信、支付宝或银行卡付款码。", "", "288888888888888888"));
            case "scan-pay" -> commonPayParams();
            case "qrcode-pay" -> withCommonPayParams(param("service", "string", "是", "LFWin 指定通道扫码 service；参与签名。", "pay.alipay.qrcode, pay.wxpay.qrcode, pay.unpay.qrcode", "pay.wxpay.qrcode"));
            case "h5-pay" -> withCommonPayParams(param("returnUrl", "string", "否", "支付完成后的前端跳转地址；传入时参与签名。", "", "https://merchant.example.com/pay/success"));
            case "wechat-jsapi-pay" -> withCommonPayParams(
                    param("subAppId", "string", "是", "微信公众号或小程序 appId。", "", "wx-sub-appid"),
                    param("subOpenId", "string", "是", "付款用户在对应应用下的 openId。", "", "wx-openid")
            );
            case "alipay-jsapi-pay" -> withCommonPayParams(
                    param("subAppId", "string", "否", "支付宝生活号或小程序 appId；传入时参与签名。", "", "alipay-sub-appid"),
                    param("buyerId", "string", "条件必填", "支付宝买家 userId；buyerId 和 buyerOpenId 二选一。", "", "2088..."),
                    param("buyerOpenId", "string", "条件必填", "支付宝买家 openId；buyerId 和 buyerOpenId 二选一。", "", "074...openId")
            );
            case "query-pay" -> List.of(appIdParam(), merchantOrderNoParam(), timestampParam(), nonceParam(), signParam());
            case "refund" -> List.of(
                    appIdParam(),
                    merchantOrderNoParam(),
                    param("merchantRefundNo", "string", "否", "商户退款单号；不传时系统自动生成并返回；传入时参与签名。", "", "R202605300001"),
                    param("refundAmount", "number", "是", "退款金额，单位元，最小 0.01，不能超过原订单金额。", "", "1.00"),
                    param("reason", "string", "否", "退款原因；传入时参与签名。", "", "用户申请退款"),
                    notifyUrlParam("退款结果通知地址；传入时参与签名。"),
                    timestampParam(),
                    nonceParam(),
                    signParam()
            );
            case "refund-query" -> List.of(
                    appIdParam(),
                    merchantOrderNoParam(),
                    param("merchantRefundNo", "string", "是", "商户退款单号。", "", "R202605300001"),
                    timestampParam(),
                    nonceParam(),
                    signParam()
            );
            case "sign" -> List.of(
                    param("sign", "string", "否", "签名计算时排除 sign 字段。", "", "sha256_hex_sign"),
                    param("appSecret", "string", "是", "商户管理中系统生成的 appSecret，不作为请求字段传输，仅用于本地签名。", "", "YhqL_...")
            );
            default -> null;
        };
    }

    private static List<ApiDocParameter> responseParams(String slug) {
        return switch (slug) {
            case "barcode-pay", "scan-pay", "qrcode-pay", "h5-pay", "wechat-jsapi-pay", "alipay-jsapi-pay", "query-pay" -> payResponseParams();
            case "refund", "refund-query" -> refundResponseParams();
            case "sign" -> List.of(
                    param("canonicalString", "string", "是", "按 ASCII 排序后拼接出的待签名字符串，末尾直接拼接 appSecret。", "", "amount=1.00&appId=...{appSecret}"),
                    param("sign", "string", "是", "SHA256 小写十六进制签名结果。", "", "sha256_hex_sign")
            );
            default -> null;
        };
    }

    private static List<ApiDocParameter> commonPayParams() {
        return List.of(
                appIdParam(),
                merchantOrderNoParam(),
                amountParam(),
                subjectParam(),
                notifyUrlParam("支付结果通知地址；传入时参与签名。"),
                timestampParam(),
                nonceParam(),
                signParam()
        );
    }

    private static List<ApiDocParameter> withCommonPayParams(ApiDocParameter... extraParams) {
        java.util.ArrayList<ApiDocParameter> params = new java.util.ArrayList<>();
        params.add(appIdParam());
        params.add(merchantOrderNoParam());
        params.add(amountParam());
        params.addAll(List.of(extraParams));
        params.add(subjectParam());
        params.add(notifyUrlParam("支付结果通知地址；传入时参与签名。"));
        params.add(timestampParam());
        params.add(nonceParam());
        params.add(signParam());
        return params;
    }

    private static List<ApiDocParameter> payResponseParams() {
        return List.of(
                param("code", "number", "是", "业务响应码，0 表示成功。", "", "0"),
                param("message", "string", "是", "业务响应描述。", "", "success"),
                param("data.appId", "string", "是", "请求中的商户 appId。", "", "MCH202605300001"),
                param("data.merchantOrderNo", "string", "是", "商户订单号。", "", "P202605300001"),
                param("data.amount", "number", "否", "订单金额，单位元。", "", "1.00"),
                param("data.payMethod", "string", "是", "支付方式编码。", "BARCODE_PAY, SCAN_PAY, H5_PAY, WECHAT_JSAPI_PAY, ALIPAY_JSAPI_PAY, PRE_ORDER, QRCODE_PAY", "BARCODE_PAY"),
                param("data.status", "string", "是", "订单状态。", "PAYING, SUCCESS, FAILED, UNKNOWN", "PAYING"),
                param("data.platformOrderNo", "string", "否", "本系统或上游平台订单号。", "", "平台订单号"),
                param("data.channelOrderNo", "string", "否", "上游通道交易流水号。", "", "上游流水号"),
                param("data.payData", "object", "否", "不同支付方式返回的支付载荷，如二维码、支付链接或 JSAPI 调起参数。", "", "{\"payUrl\":\"https://...\"}"),
                param("data.message", "string", "否", "上游或系统处理消息。", "", "Payment success")
        );
    }

    private static List<ApiDocParameter> refundResponseParams() {
        return List.of(
                param("code", "number", "是", "业务响应码，0 表示成功。", "", "0"),
                param("message", "string", "是", "业务响应描述。", "", "success"),
                param("data.appId", "string", "是", "请求中的商户 appId。", "", "MCH202605300001"),
                param("data.merchantOrderNo", "string", "是", "原支付商户订单号。", "", "P202605300001"),
                param("data.merchantRefundNo", "string", "是", "商户退款单号；未传时由系统生成。", "", "R202605300001"),
                param("data.refundAmount", "number", "否", "退款金额，单位元。", "", "1.00"),
                param("data.status", "string", "是", "退款状态。", "PROCESSING, SUCCESS, FAILED", "PROCESSING"),
                param("data.data", "object", "否", "上游退款处理载荷。", "", "{\"refundStatus\":\"PROCESSING\"}"),
                param("data.message", "string", "否", "上游或系统处理消息。", "", "SUCCESS")
        );
    }

    private static ApiDocParameter appIdParam() {
        return param("appId", "string", "是", "商户管理中生成的接口 appId，默认使用商户编码。", "", "MCH202605300001");
    }

    private static ApiDocParameter merchantOrderNoParam() {
        return param("merchantOrderNo", "string", "是", "商户订单号；同一 appId 下必须唯一。", "", "P202605300001");
    }

    private static ApiDocParameter amountParam() {
        return param("amount", "number", "是", "订单金额，单位元，最小 0.01。", "", "1.00");
    }

    private static ApiDocParameter subjectParam() {
        return param("subject", "string", "是", "订单标题或商品描述。", "", "测试商品");
    }

    private static ApiDocParameter notifyUrlParam(String description) {
        return param("notifyUrl", "string", "否", description, "", "https://merchant.example.com/pay/notify");
    }

    private static ApiDocParameter timestampParam() {
        return param("timestamp", "number", "是", "毫秒时间戳。服务端默认校验 5 分钟时间窗口，并参与签名。", "", "1790000000000");
    }

    private static ApiDocParameter nonceParam() {
        return param("nonce", "string", "是", "一次性随机字符串。同一 appId 下 5 分钟内不能重复，并参与签名。", "", "random_string");
    }

    private static ApiDocParameter signParam() {
        return param("sign", "string", "是", "SHA256 小写十六进制签名。签名时排除 sign 字段，其他非空字段按 ASCII 升序拼接后追加 appSecret。", "", "sha256_hex_sign");
    }

    private static ApiDocParameter param(String name, String type, String required, String description, String enums, String example) {
        return new ApiDocParameter(name, type, required, description, enums, example);
    }
}
