package br.ifsp.edu.validador_documentos;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@Configuration
public class WebServiceConfig {

    //ome do arquivo: /ws/validador.wsdl
    @Bean(name = "validador")
    public DefaultWsdl11Definition validadorWsdl(XsdSchema validadorSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("ValidadorPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://ifsp.edu.br/validador");
        wsdl.setSchema(validadorSchema);
        return wsdl;
    }

    // Carrega o validador.xsd de src/main/resources
    @Bean
    public XsdSchema validadorSchema() {
        XsdSchema schema = new SimpleXsdSchema(new ClassPathResource("validador.xsd"));
        return schema;
    }
}
