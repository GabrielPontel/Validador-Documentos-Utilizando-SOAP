from lxml import etree
from requests.exceptions import ConnectionError as ErroConexao
from zeep import Client
from zeep.exceptions import Fault
from zeep.plugins import HistoryPlugin

URL_WSDL = "http://localhost:4851/ws/validador.wsdl"


def mostrar_xml(titulo: str, envelope) -> None:
    texto = etree.tostring(envelope, pretty_print=True, encoding="unicode")
    print(f"\n========== {titulo} ==========")
    print(texto.strip())


def mensagem_caracteres(doc: str) -> str:
    invalidos = []
    mensagem = "";
    for caractere in doc:
        if not caractere.isdigit() and caractere not in invalidos:
            invalidos.append(caractere)
    if len(invalidos)==1:
        mensagem = f"O campo doc so aceita digitos e o caractere {invalidos[0]} nao e permitido"
    else:
        mensagem=(f"O campo doc so aceita digitos e os caracteres {', '.join(invalidos[:-1])} e {invalidos[-1]} nao sao permitidos")
    return mensagem


def traduzir_resposta(valido: int, erro: str, tipo: int, doc: str) -> str:
    quantidade_digitos = {1: 11,2:14}
    nomes_tipos = {1: "CPF", 2:"CNPJ"}
    nome = nomes_tipos.get(tipo,"")
    ideal =quantidade_digitos.get(tipo, 0)
    mensagem = ""

    if valido== 1:
        mensagem = "Documento valido"
    elif erro =="0":
        mensagem= "Documento invalido"
    elif erro =="1.1":
        mensagem= "Estrutura da requisicao invalida"
    elif erro =="1.2":
        mensagem= "O campo tipo_doc nao foi informado"
    elif erro =="1.3":
        mensagem="O campo doc nao foi informado ou esta vazio"
    elif erro == "2.1":
        mensagem=f"O tipo {tipo} nao e processado pelo GBProtocol (use 1 = CPF ou 2 = CNPJ)"
    elif erro =="3.1":
        mensagem =(f"A quantidade ideal para {nome} e de {ideal} digitos e seu documento possui {len(doc)}, portanto faltam {ideal - len(doc)} digitos")
    elif erro == "3.2":
        mensagem= (f"A quantidade ideal para {nome} e de {ideal} digitos e seu documento possui {len(doc)}, portanto existem {len(doc) - ideal} digitos a mais")
    elif erro =="4.1":
        mensagem = mensagem_caracteres(doc)
    else:
        mensagem= "Resposta desconhecida do servidor"
    return mensagem


def validar(cliente: Client, historico: HistoryPlugin, tipo: int, doc: str) -> str:
    # Chama a operação remota e devolve a mensagem para o usuário
    resposta = None
    mensagem = ""

    try:
        # O Zeep monta o envelope SOAP a partir do WSDL; aqui é só chamar o método
        resposta = cliente.service.ValidarDocumento(tipo_doc=tipo, doc=doc)
        mostrar_xml("XML ENVIADO", historico.last_sent["envelope"])
        mostrar_xml("XML RECEBIDO", historico.last_received["envelope"])
        mensagem = traduzir_resposta(resposta.valido, resposta.erro, tipo, doc)
    except Fault as falha:
        mensagem = f"O servidor devolveu um SOAP Fault: {falha.message}"
    except ErroConexao:
        mensagem = "Nao foi possivel falar com o servidor"
    return mensagem


def main() -> None:
    historico = HistoryPlugin()  # guarda o último XML enviado e recebido
    cliente = None
    executando = True
    entrada = ""
    tipo = 0
    doc = ""

    try:
        cliente = Client(URL_WSDL, plugins=[historico])  # lê o WSDL e cria os métodos
    except ErroConexao:
        executando = False
        print(f"Nao foi possivel carregar o WSDL em {URL_WSDL}. O servidor esta rodando?")

    if executando:
        print("*************** GBProtocol SOAP (CPF / CNPJ) ***************")
    while executando:
        entrada = input("\nTipo (1 = CPF, 2 = CNPJ, 0 = sair): ").strip()
        if entrada == "0":
            executando = False
        elif not entrada.isdigit():
            print("RESULTADO: O tipo deve ser um numero (1 = CPF, 2 = CNPJ)")
        else:
            tipo = int(entrada)
            doc = input("Numero do documento (somente digitos): ").strip()
            print(f"\nRESULTADO:{validar(cliente, historico, tipo, doc)}")
    print("Cliente encerrado.")


if __name__ == "__main__":
    main()
