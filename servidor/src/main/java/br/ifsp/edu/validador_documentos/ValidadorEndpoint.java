package br.ifsp.edu.validador_documentos;

import java.util.Map;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import br.edu.ifsp.validador.ValidarDocumentoRequest;   // gerada validador.xsd
import br.edu.ifsp.validador.ValidarDocumentoResponse;  // gerada pelo xjc a partir do validador.xsd


@Endpoint  
public class ValidadorEndpoint {

    private static final String NAMESPACE = "http://ifsp.edu.br/validador";  // o mesmo targetNamespace do XSD
    private static final Map<Integer, Integer> QUANTIDADE_DIGITOS = Map.of(1, 11, 2, 14);  // 1 = CPF, 2 = CNPJ

    // Chamado quando chega um <ValidarDocumentoRequest> nesse namespace
    @PayloadRoot(namespace = NAMESPACE, localPart = "ValidarDocumentoRequest")
    @ResponsePayload
    public ValidarDocumentoResponse validarDocumento(@RequestPayload ValidarDocumentoRequest requisicao) {
        ValidarDocumentoResponse resposta = new ValidarDocumentoResponse();
        int tipo = requisicao.getTipoDoc();
        String documento = requisicao.getDoc() == null ? "" : requisicao.getDoc().trim();
        int ideal = QUANTIDADE_DIGITOS.getOrDefault(tipo, 0);
        boolean valido = false;
        String erro = "0";

        if (tipo == 0) {  // 0 = tipo não informado (valor padrão do int)
            erro = "1.2";
        } else if (documento.isEmpty()) {
            erro = "1.3";
        } else if (!QUANTIDADE_DIGITOS.containsKey(tipo)) {
            erro = "2.1";
        } else if (!documento.matches("[0-9]+")) {
            erro = "4.1";
        } else if (documento.length() < ideal) {
            erro = "3.1";
        } else if (documento.length() > ideal) {
            erro = "3.2";
        } else if (tipo == 1) {
            valido = validarCpf(documento);
        } else {
            valido = validarCnpj(documento);
        }

        resposta.setValido(valido ? 1 : 0);
        resposta.setErro(erro);
        System.out.println("Recebido: tipo_doc=" + tipo + " doc=" + documento + " -> valido=" + (valido ? 1 : 0) + " erro=" + erro);
        return resposta;
    }

    // Confere os dois dígitos verificadores do CPF
    private boolean validarCpf(String cpf) {
        boolean digitosCorretos = true;
        int soma;
        int digitoCalculado;

        if (cpf.chars().distinct().count() == 1) {  // 000..., 111... são inválidos
            digitosCorretos = false;
        } else {
            for (int posicao = 9; posicao <= 10; posicao++) {
                soma = 0;
                for (int i = 0; i < posicao; i++) {
                    soma += (cpf.charAt(i) - '0') * (posicao + 1 - i);
                }
                digitoCalculado = (soma * 10) % 11 % 10;
                if (digitoCalculado != cpf.charAt(posicao) - '0') {
                    digitosCorretos = false;
                }
            }
        }
        return digitosCorretos;
    }

    // Confere os dois dígitos verificadores do CNPJ
    private boolean validarCnpj(String cnpj) {
        int[] pesosPrimeiro = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesosSegundo = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        boolean digitosCorretos = true;
        int[] pesos;
        int soma;
        int resto;
        int digitoCalculado;

        if (cnpj.chars().distinct().count() == 1) {
            digitosCorretos = false;
        } else {
            for (int posicao = 12; posicao <= 13; posicao++) {
                pesos = posicao == 12 ? pesosPrimeiro : pesosSegundo;
                soma = 0;
                for (int i = 0; i < posicao; i++) {
                    soma += (cnpj.charAt(i) - '0') * pesos[i];
                }
                resto = soma % 11;
                digitoCalculado = resto < 2 ? 0 : 11 - resto;
                if (digitoCalculado != cnpj.charAt(posicao) - '0') {
                    digitosCorretos = false;
                }
            }
        }
        return digitosCorretos;
    }
}
