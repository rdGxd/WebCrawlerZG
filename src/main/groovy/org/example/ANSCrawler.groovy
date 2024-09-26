package org.example

import groovyx.net.http.HttpBuilder

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

        componenteDeComunicaCao(downloadDir)

        coletarDadosHistoricos()

        baixarTabelaErros(downloadDir)
    }

    static void componenteDeComunicaCao(String downloadDir) {
        Document gov = configure {
            request.uri = 'https://www.gov.br/ans/pt-br'
        }.get()

        String acessGov = gov.getElementsContainingOwnText("Espaço do Prestador de Serviços de Saúde").first().attr("href")

        Document prestador = HttpBuilder.configure {
            request.uri = acessGov
        }.get() as Document

        String tiss = prestador.getElementsContainingOwnText("TISS - Padrão para Troca de Informação de Saúde Suplementar").first().attr("href")

        Document acessTISS = configure {
            request.uri = tiss
        }.get()

        String mesAno = acessTISS.getElementsContainingOwnText("Clique aqui para acessar a versão").first().attr("href")

        Document acessMesAno = HttpBuilder.configure {
            request.uri = mesAno
        }.get() as Document

        acessMesAno.select("tr").each { tr ->
            String url = tr.select("a").attr("href")
            String saveAs = downloadDir + "/" + tr.select("a").text()
            if(tr.children().first().text().contains("Componente de Comunicação")) {
                baixarArquivo(url, saveAs)
            }
        }
    }

    static void coletarDadosHistoricos() {
        Document gov = configure {
            request.uri = 'https://www.gov.br/ans/pt-br'
        }.get()

        String acessGov = gov.getElementsContainingOwnText("Espaço do Prestador de Serviços de Saúde").first().attr("href")

        Document prestador = HttpBuilder.configure {
            request.uri = acessGov
        }.get() as Document

        String tiss = prestador.getElementsContainingOwnText("TISS - Padrão para Troca de Informação de Saúde Suplementar").first().attr("href")

        Document acessTISS = configure {
            request.uri = tiss
        }.get()

        String historico = acessTISS.getElementsContainingOwnText("Clique aqui para acessar todas as versões dos Componentes").first().attr("href")

        Document acessHistorico = HttpBuilder.configure {
            request.uri = historico
        }.get() as Document

        acessHistorico.select("table tr").each { row ->
            def columns = row.select("td")
            if (columns.size() > 0) {
                String competencia = columns[0].text()
                String publicacao = columns[1].text()
                String vigencia = columns[2].text()

                String ano = competencia.split("/")[1]
                // Filtrar a partir de jan/2016
                if (Integer.parseInt(ano) >= 2016) {
                    escreverArquivo("Competência: ${competencia} | Publicação: ${publicacao} | Vigência: ${vigencia}\n", "Historico.txt")
                }
            }
        }
    }

    static void baixarTabelaErros(String downloadDir) {
        Document gov = configure {
            request.uri = 'https://www.gov.br/ans/pt-br'
        }.get()

        String acessGov = gov.getElementsContainingOwnText("Espaço do Prestador de Serviços de Saúde").first().attr("href")

        Document prestador = HttpBuilder.configure {
            request.uri = acessGov
        }.get() as Document

        String tiss = prestador.getElementsContainingOwnText("TISS - Padrão para Troca de Informação de Saúde Suplementar").first().attr("href")

        Document acessTISS = configure {
            request.uri = tiss
        }.get()

        String tabelas = acessTISS.getElementsContainingOwnText("Clique aqui para acessar as planilhas").first().attr("href")

        Document acessTabelas = HttpBuilder.configure {
            request.uri = tabelas
        }.get() as Document

        String linkDownload = acessTabelas.getElementsContainingOwnText("Clique aqui para baixar a tabela de erros no envio para a ANS (.xlsx)").first().attr("href")

        baixarArquivo(linkDownload, downloadDir + "/TabelaErros.xlsx")
    }

    static void baixarArquivo(String fileUrl, String saveAs) {
        try {
            def url = new URL(fileUrl)
            def inputStream = url.openStream()
            Files.copy(inputStream, Paths.get(saveAs))
            inputStream.close()
            println "Arquivo baixado com sucesso!"
        } catch (IOException e) {
            e.printStackTrace()
        }
        println("Baixando arquivo de $fileUrl para $saveAs")
    }

    static void escreverArquivo(String Dados, String saveAS) {
        def file = new File("./Downloads/Aquivos_padrao_TISS/" + saveAS)
        file.append(Dados)
    }
}

