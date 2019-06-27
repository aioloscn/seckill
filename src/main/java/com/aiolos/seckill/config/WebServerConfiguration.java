package com.aiolos.seckill.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * 当spring容器中没有TomcatEmbeddedServletContainerFactory这个bean时，会加载这个bean
 * @author Aiolos
 * @date 2019-06-27 19:55
 */
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {

        // 使用对应工厂类提供的接口定制化我们的tomcat connector
        ((TomcatServletWebServerFactory) factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

                // 定制化keepalive timeout，设置30s内没有请求则服务端自动断开keepalive连接
                protocol.setKeepAliveTimeout(30000);
                // 当客户端发送超过10000个请求则自动断开连接
                protocol.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
