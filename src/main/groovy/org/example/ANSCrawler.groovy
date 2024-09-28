package org.example


import java.nio.file.Files
import java.nio.file.Paths

@Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')
@Grab('org.jsoup:jsoup:1.14.3')

import static groovyx.net.http.HttpBuilder.configure
import org.jsoup.nodes.Document

class ANSCrawler {
    static void main(String[] args) {
        String downloadDir = "./Downloads/Aquivos_padrao_TISS"
        new File(downloadDir).mkdirs()

        Document Inicial = configure {
            request.uri = 'https://www.gov.br/ans/pt-br'
        }.get()
        String acessGov = Inicial.getElementsContainingOwnText("Espaço do Prestador de Serviços de Saúde").first().attr("href")
        Document gov = configure {
            request.uri = acessGov
        }.get()

        componenteDeComunicaCao(downloadDir, gov)

        coletarDadosHistoricos(downloadDir, gov)

        baixarTabelaErros(downloadDir, gov)
    }

    static void componenteDeComunicaCao(String downloadDir, Document gov) {

        String tiss = gov.getElementsContainingOwnText("TISS - Padrão para Troca de Informação de Saúde Suplementar").first().attr("href")

        Document acessTISS = configure {
            request.uri = tiss
        }.get()

        String mesAno = acessTISS.getElementsContainingOwnText("Clique aqui para acessar a versão").first().attr("href")

        Document acessMesAno = configure {
            request.uri = mesAno
        }.get()

        acessMesAno.select("tr").each { tr ->
            String url = tr.select("a").attr("href")
            if(tr.children().first().text().contains("Componente de Comunicação")) {
                baixarArquivo(url.trim(), downloadDir + "/ComponenteDeComunicacao.zip")
            }
        }
    }

    static void coletarDadosHistoricos(String downloadDir, Document gov) {

        String tiss = gov.getElementsContainingOwnText("TISS - Padrão para Troca de Informação de Saúde Suplementar").first().attr("href")

        Document acessTISS = configure {
            request.uri = tiss
        }.get()

        String historico = acessTISS.getElementsContainingOwnText("Clique aqui para acessar todas as versões dos Componentes").first().attr("href")

        Document acessHistorico = configure {
            request.uri = historico
        }.get()

        acessHistorico.select("table tr").each { row ->
            def columns = row.select("td")
            if (columns.size() > 0) {
                String competencia = columns[0].text()
                String publicacao = columns[1].text()
                String vigencia = columns[2].text()

                String ano = competencia.split("/")[1]
                // Filtrar a partir de jan/2016
                if (Integer.parseInt(ano) >= 2016) {
                    escreverArquivo("Competência: ${competencia} | Publicação: ${publicacao} | Vigência: ${vigencia}\n", downloadDir+"/Historico.txt")
                }
            }
        }
    }

    static void baixarTabelaErros(String downloadDir, Document gov) {

        String tiss = gov.getElementsContainingOwnText("TISS - Padrão para Troca de Informação de Saúde Suplementar").first().attr("href")

        Document acessTISS = configure {
            request.uri = tiss
        }.get()

        String tabelas = acessTISS.getElementsContainingOwnText("Clique aqui para acessar as planilhas").first().attr("href")

        Document acessTabelas = configure {
            request.uri = tabelas
        }.get()

        String linkDownload = acessTabelas.getElementsContainingOwnText("Clique aqui para baixar a tabela de erros no envio para a ANS (.xlsx)").first().attr("href")

        baixarArquivo(linkDownload, downloadDir + "/TabelaErros.xlsx")
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

    static void escreverArquivo(String Dados, String saveAS) {
        def file = new File(saveAS)
        file.append(Dados)
    }
}

