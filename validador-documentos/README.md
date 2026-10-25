# GBProtocol SOAP: Validador de Documentos

Validador de CPF e CNPJ implementado como **serviço web SOAP**, com **servidor em Java (Spring Boot)** e **cliente em Python (Zeep)**.

Trabalho da disciplina de Sistemas Distribuídos. Esta é a terceira versão do GBProtocol: a primeira usava [sockets TCP](https://github.com/GabrielPontel/Validador-Documentos-Utilizando-Socket) e a segunda [gRPC](https://github.com/GabrielPontel/Validador-Documentos-Utilizando-GRPC). Aqui as mensagens são XML dentro de envelopes SOAP, descritas por um XML Schema (XSD), e o contrato do serviço (WSDL) é gerado automaticamente pelo Spring.

## Como funciona

```
 CLIENTE (Python + Zeep)                            SERVIDOR (Java + Spring)
 -----------------------                            ------------------------
 1. Lê o WSDL  ------- GET /ws/validador.wsdl --->  publica o WSDL gerado do XSD
 2. Monta o envelope SOAP
 3. Envia ------------ POST /ws (Request) ------->  4. ValidadorEndpoint aplica as regras
 6. Traduz o código <-- envelope SOAP (Response) -  5. Devolve valido e erro
```

O servidor devolve apenas códigos. A tradução para mensagem é feita pelo cliente, que também mostra na tela o XML enviado e o recebido.

## Formato das mensagens

Definido em [`validador.xsd`](servidor/src/main/resources/validador.xsd), no namespace `http://ifsp.edu.br/validador`.

### Requisição

```xml
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
  <soap-env:Body>
    <ns0:ValidarDocumentoRequest xmlns:ns0="http://ifsp.edu.br/validador">
      <ns0:tipo_doc>1</ns0:tipo_doc>
      <ns0:doc>52998224725</ns0:doc>
    </ns0:ValidarDocumentoRequest>
  </soap-env:Body>
</soap-env:Envelope>
```

| Campo      | Tipo   | Descrição               |
|------------|--------|-------------------------|
| `tipo_doc` | int    | 1 = CPF, 2 = CNPJ       |
| `doc`      | string | Número do documento     |

### Resposta

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Header/>
  <SOAP-ENV:Body>
    <ns2:ValidarDocumentoResponse xmlns:ns2="http://ifsp.edu.br/validador">
      <ns2:valido>1</ns2:valido>
      <ns2:erro>0</ns2:erro>
    </ns2:ValidarDocumentoResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

| Campo    | Tipo   | Descrição                                  |
|----------|--------|--------------------------------------------|
| `valido` | int    | 1 se o documento é válido, 0 caso contrário |
| `erro`   | string | 0 quando não houve erro, ou o código       |

### Códigos de resposta

| valido | erro | Significado                                                |
|--------|------|------------------------------------------------------------|
| 1      | 0    | Documento válido                                           |
| 0      | 0    | Formato correto, mas os dígitos verificadores não conferem |
| 0      | 1.2  | Tipo de documento não informado                            |
| 0      | 1.3  | Documento não informado ou vazio                           |
| 0      | 2.1  | Tipo de documento diferente de 1 (CPF) ou 2 (CNPJ)         |
| 0      | 3.1  | Quantidade de dígitos menor que a esperada                 |
| 0      | 3.2  | Quantidade de dígitos maior que a esperada                 |
| 0      | 4.1  | Documento contém caracteres que não são dígitos            |

## Estrutura do projeto

```
.
├── servidor/                                  # Java 21 + Spring Boot (Spring Web Services)
│   ├── pom.xml                                # dependências e plugin que gera as classes do XSD
│   ├── rodar-servidor.bat
│   └── src/main/
│       ├── java/br/ifsp/edu/validador_documentos/
│       │   ├── ValidadorDocumentosApplication.java
│       │   ├── ValidadorEndpoint.java         # regras do GBProtocol
│       │   └── WebServiceConfig.java          # publica o WSDL a partir do XSD
│       └── resources/
│           ├── application.properties         # porta 4851 e caminho /ws
│           └── validador.xsd                  # formato das mensagens
└── cliente/                                   # Python + Zeep
    ├── cliente.py
    ├── requirements.txt
    └── rodar-cliente.bat
```

As classes `ValidarDocumentoRequest` e `ValidarDocumentoResponse` não estão no repositório: o plugin `jaxb2-maven-plugin` gera essas classes a partir do XSD a cada compilação.

## Pré-requisitos

- Java 21 (JDK)
- Python 3.8 ou superior

## Como executar

### 1. Servidor

Dê dois cliques em `servidor/rodar-servidor.bat`, ou no PowerShell:

```powershell
cd servidor
.\mvnw.cmd spring-boot:run
```

Espere aparecer `Tomcat started on port 4851`. O WSDL fica em http://localhost:4851/ws/validador.wsdl.

### 2. Cliente

Em outro terminal, dê dois cliques em `cliente/rodar-cliente.bat`, ou:

```powershell
cd cliente
pip install -r requirements.txt
python cliente.py
```

Exemplo de uso:

```
Tipo (1 = CPF, 2 = CNPJ, 0 = sair): 1
Numero do documento (somente digitos): 52998224725

========== XML ENVIADO ==========
...
========== XML RECEBIDO ==========
...

RESULTADO: Documento valido
```

## Documentos para teste

| Tipo     | Número             | Resultado |
|----------|--------------------|-----------|
| 1 (CPF)  | 52998224725        | válido    |
| 1 (CPF)  | 52998224726        | inválido  |
| 2 (CNPJ) | 11222333000181     | válido    |
| 2 (CNPJ) | 11222333000180     | inválido  |
| 2 (CNPJ) | 11.222.333/0001-81 | erro 4.1  |
| 3        | 123                | erro 2.1  |

## Referências

- [Spring: Producing a SOAP web service](https://spring.io/guides/gs/producing-web-service)
- [Zeep: Python SOAP client](https://docs.python-zeep.org/)
- [W3C: SOAP 1.1](https://www.w3.org/TR/2000/NOTE-SOAP-20000508/)

## Autor

Gabriel Pontel de Mori
