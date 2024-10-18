package com.lx.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.lx.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "9021000136647251";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCpAhUDheekoQ4NR0rRBBRLd6og1WVTVxw3meExda4aPM8qhbTf8sMhzi3cavp8W29IUqopOnl5Ywzm8E9f8HLVOdyuirrUT7wSiDhIH1PKwF4RB5mRA96Y4Vku2BDRQj5Z4WZUjE/LLP40+q0Df5I9i0CHdqnKxSKjXStIqS+axl21uhyiJbwfP/oolSwcn3AtxcPpKZU+R5tLxF5POqGYm8HnzY5yitB8g7JjejXhqcBKnLHPURUSSLmpytGRPavvAm28UAMhG0EDBSrjSNK/LDYwoHJFKqaMhLHo78pwvk76EJQKe/oL6mjYcCbpjCbgGqgvnluHei1tkU7FAkDXAgMBAAECggEAC0ZMSJPiRCVKXrBm2VLAV9h/zPjvbYPeKgdl8nq7RpzP5TJOQrJbOiIbBR1N4IXpApYhK+buu55T+gQGB4xu3LKjejFfDVLUZRgZSRZt7JSwscokAHyVmPVPkcRwOsWEi1JyFSnXGOLDgp1GdXlJNDnZ8C+GXDSSfvt0K8C9oEGHrG0B4ypXqg9zOBS0ygvFfYCwdQ3UL225gTzpSQS20MRl44KnJU8S+nm2MycoyBFhSCURD+KHCi1cOlfNJraHoVLcKpBIbt5VDuLu+Eh0CQ43EfYxjse84c0TBd5ewpqRxS40YgKKzilofmm3LIhP9gK/dQ6ijlqMgoTsSXi9oQKBgQDbMtsV4g8TxAV0MeIVEano/+gjkepETGtHViZOF8h7U/mF+yDunoBHbarAXXlWxEaTyt8a23vxIFqoa3OptcckgZ2mU7qHwitmaGpYW32p8FPy5mTVDkLtWWwRF2tel25+K+H0UkH4dZedaZGrWwkvXbORQFmUoW3Mwee6b7qdWwKBgQDFYgp0lp5Wlz1+Qw8WLdMgDHqr8xo8unky/tg8XulgRHIQXYkaF5mgJ6JVR/klbOUV7L+gf+s4R91MDK9ATYu0o9ISRlygMVY2HJibERQ3ebEkIO/BdhFvGL/PuU+vexQRdtIEL72WviDAM3uMwdXZ3Kbx8I0sQcwIxr9ue92XNQKBgQDOuffFe52clpJOM311QWNtS2wzn7nBEBFdBfK6U9PP/DKoGV/Fq9Zvhw4yvfVhz1qAqCQhwD3z+v+FN1GuDLHzzApSg0S4JHep7W/eROsPCokh3Afcp8aW8m7o751jaI3ckKZZNfrhyIVk/9tSMFlJQqXHp1jUxCo5oeZZ/mWRYQKBgBlOEm0UCkSrqxSO4FZFlbWcI/X7ereiaHtjNLG4sU7IN59lfng74lQoAKXSTly/8za2XXLkM3HJtNNVJPqndmu4POxe3O0kphrV27K1o9Pg5Bbvqg0xZ9bIY37sEQ08SDxc8VvcCwBSa7x4XTSUfSgrI7QM1OyBmvjdEhnGA4ipAoGBANmv8HbsjUruAQhtiQWVADmClI/+PbrnjCLGSVnvh256NJwH8u6H7tgTTHP4Wg2+XJVe9Exe4UuemxstAcbgbHvFROjsfaIMGLbyBw3gFRvcbvSjvdzNrQxtH00yqTLQX3zm/2zCJ9qtgUo6pAQwci92TElSfCcmFgj26AiVdE9D";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg2mFek85cECC/CTDTiDNtXZm7L8dNVug6BWkjkoSV974Gv0cfV/jtSGgIETlVqkbqESP6rOSNSX0TJvY2YmMGarC4hqCL/77m/ViquEWnfDyzRrU2iJPFjGahsHK3mERBGEy2+O4+KL966fAq6k4M4lfESCu6iaw+BTq8ItloUjPxXivVKNaEGRU9m41hX+3+WBmuZDksfFsyveVpKH/1NJbcod7Z+t5dnkWrYULyLZDsohE7XuFn0rPCX4PQ0dMftmh7/tVdgae1yNXjkQUpReO6uJXbUdnGZTmlMdiIkf0Ok71npP6daJ+Z+WGuV+UtNsEOkn1t3N9OxYHVbZ58QIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url ="http://906utvo24538.vicp.fun/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url="http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    private String timeout="30m";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
