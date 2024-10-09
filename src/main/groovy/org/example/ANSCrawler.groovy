package org.example

import java.nio.file.Files
import java.nio.file.Paths
import static groovyx.net.http.HttpBuilder.configure
import org.jsoup.nodes.Document

@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
@Grab('org.jsoup:jsoup:1.14.3')

class ANSCrawler {
    static void main(String[] args) {
        String downloadDir = "./Downloads/Arquivos_padrao_TISS"
        new File(downloadDir).mkdirs()

        Document inicial = fetchDocument('https://www.gov.br/ans/pt-br')
        String acessGov = getFirstHref(inicial, "Espaço do Prestador de Serviços de Saúde")
        Document gov = fetchDocument(acessGov)

        componenteDeComunicaCao(downloadDir, gov)
        coletarDadosHistoricos(downloadDir, gov)
        baixarTabelaErros(downloadDir, gov)
    }

    static void componenteDeComunicaCao(String downloadDir, Document gov) {
        String tiss = getFirstHref(gov, "TISS - Padrão para Troca de Informação de Saúde Suplementar")
        Document acessTISS = fetchDocument(tiss)
        String mesAno = getFirstHref(acessTISS, "Clique aqui para acessar a versão")
        Document acessMesAno = fetchDocument(mesAno)

        acessMesAno.select("tr").each { tr ->
            String url = tr.select("a").attr("href")
            if (tr.children().first().text().contains("Componente de Comunicação")) {
                baixarArquivo(url.trim(), "$downloadDir/ComponenteDeComunicacao.zip")
            }
        }
    }

    static void coletarDadosHistoricos(String downloadDir, Document gov) {
        String tiss = getFirstHref(gov, "TISS - Padrão para Troca de Informação de Saúde Suplementar")
        Document acessTISS = fetchDocument(tiss)
        String historico = getFirstHref(acessTISS, "Clique aqui para acessar todas as versões dos Componentes")
        Document acessHistorico = fetchDocument(historico)

        acessHistorico.select("table tr").each { row ->
            def columns = row.select("td")
            if (columns.size() > 0) {
                String competencia = columns[0].text()
                String publicacao = columns[1].text()
                String vigencia = columns[2].text()
                String ano = competencia.split("/")[1]

                if (Integer.parseInt(ano) >= 2016) {
                    escreverArquivo("Competência: $competencia | Publicação: $publicacao | Vigência: $vigencia\n", "$downloadDir/Historico.txt")
                }
            }
        }
    }

    static void baixarTabelaErros(String downloadDir, Document gov) {
        String tiss = getFirstHref(gov, "TISS - Padrão para Troca de Informação de Saúde Suplementar")
        Document acessTISS = fetchDocument(tiss)
        String tabelas = getFirstHref(acessTISS, "Clique aqui para acessar as planilhas")
        Document acessTabelas = fetchDocument(tabelas)
        String linkDownload = getFirstHref(acessTabelas, "Clique aqui para baixar a tabela de erros no envio para a ANS (.xlsx)")

        baixarArquivo(linkDownload, "$downloadDir/TabelaErros.xlsx")
    }

    static Document fetchDocument(String uri) {
        return configure { request.uri = uri }.get()
    }

    static String getFirstHref(Document doc, String text) {
        return doc.getElementsContainingOwnText(text).first().attr("href")
    }

    static void baixarArquivo(String fileUrl, String saveAs) {
        try {
            def url = new URL(fileUrl)
            def inputStream = url.openStream()
            Files.copy(inputStream, Paths.get(saveAs))
            inputStream.close()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    static void escreverArquivo(String dados, String saveAs) {
        new File(saveAs).append(dados)
    }
}