# Automação, Observabilidade e Esteira DevSecOps

Aplicação Java Spring Boot conteinerizada com Docker Multistage Build, pipeline CI/CD no GitHub Actions, monitoramento com Prometheus e Grafana e centralização de logs com Graylog.

O frontend do projeto apresenta o **Polo Astronômico de Videira** e serve como interface visual para demonstrar requisições reais atendidas pelo Spring Boot. O foco principal, porém, é a automação de build e publicação, a execução integrada dos serviços e a observabilidade da aplicação.

> **Repositório:** [Jopako/DevSecOps](https://github.com/Jopako/DevSecOps)  
> **Imagem publicada no GHCR:** `ghcr.io/jopako/devsecops:main`

---

## Sumário

- [Arquitetura](#arquitetura)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Aplicação e endpoints](#aplicação-e-endpoints)
- [Docker Multistage Build](#docker-multistage-build)
- [Pipeline CI/CD](#pipeline-cicd)
- [Monitoramento com Prometheus](#monitoramento-com-prometheus)
- [Dashboard Grafana](#dashboard-grafana)
- [Logs centralizados com Graylog](#logs-centralizados-com-graylog)
- [Testes rápidos de observabilidade](#testes-rápidos-de-observabilidade)
- [Evidências](#evidências)
- [Persistência e encerramento](#persistência-e-encerramento)
- [Solução de problemas](#solução-de-problemas)

## Arquitetura

A infraestrutura local é definida em `docker-compose.yml` e inclui a aplicação Spring Boot, Prometheus, Grafana e a stack necessária ao Graylog (MongoDB e OpenSearch).

```text
Navegador
   │ HTTP :8081
   ▼
Spring Boot (container :8080)
   ├── /actuator/prometheus ──► Prometheus ──► Grafana
   │                              scrape       dashboard
   └── Logback / GELF ─────────► Graylog ─────► OpenSearch
                                  │
                                  └────────── MongoDB (metadados/configuração)
```

### Endereços locais

| Serviço | Endereço | Finalidade |
|---|---|---|
| Aplicação / Observatório | [http://localhost:8081](http://localhost:8081) | Frontend entregue pelo Spring Boot |
| Health check | [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) | Estado da aplicação |
| Métricas Prometheus | [http://localhost:8081/actuator/prometheus](http://localhost:8081/actuator/prometheus) | Métricas exportadas pelo Actuator/Micrometer |
| Prometheus | [http://localhost:9090](http://localhost:9090) | Consulta das métricas e estado dos targets |
| Prometheus targets | [http://localhost:9090/targets](http://localhost:9090/targets) | Verificação da coleta da aplicação |
| Grafana | [http://localhost:3000](http://localhost:3000) | Dashboards de monitoramento |
| Graylog | [http://localhost:9000](http://localhost:9000) | Pesquisa e análise de logs |
| Entrada GELF | `graylog:12201/UDP` na rede do Compose | Recepção de logs da aplicação |

As portas internas e a comunicação entre containers são configuradas pelo Docker Compose. O endereço `app:8080`, por exemplo, é usado pelo Prometheus dentro da rede Docker; não deve ser substituído por `localhost:8081` na configuração de scrape do container.

## Tecnologias utilizadas

- **Java e Spring Boot:** aplicação web e endpoints HTTP.
- **Spring Boot Actuator + Micrometer Prometheus:** exposição das métricas.
- **Maven Wrapper:** compilação e empacotamento da aplicação.
- **Dockerfile Multistage:** separação entre build e runtime.
- **Docker Compose:** orquestração da stack local.
- **GitHub Actions:** automação de testes, build e publicação da imagem.
- **GitHub Container Registry (GHCR):** armazenamento da imagem da aplicação.
- **Prometheus:** coleta e armazenamento temporal de métricas.
- **Grafana:** visualização das métricas em dashboard.
- **Logback + GELF:** envio de logs estruturados ao Graylog.
- **Graylog + MongoDB + OpenSearch:** gestão, consulta e armazenamento dos logs.
- **HTML, CSS e JavaScript:** interface visual do Observatório Astronômico de Videira.

## Estrutura do projeto

```text
.
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── prometheus/
│   └── prometheus.yml
├── src/
│   └── main/
│       ├── java/com/example/devopsapp/
│       │   ├── controller/
│       │   └── filter/
│       │       └── HttpRequestLoggingFilter.java
│       └── resources/
│           ├── static/
│           │   ├── index.html
│           │   ├── style.css
│           │   └── script.js
│           ├── application.yaml
│           └── logback-spring.xml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

A estrutura acima resume os principais arquivos utilizados no projeto.

## Pré-requisitos

- Docker Engine instalado e em execução.
- Docker Compose v2 (`docker compose`).
- Conexão com a internet na primeira execução, para baixar imagens e dependências.
- Git, caso o projeto seja clonado do repositório.

A execução conteinerizada não exige instalar Java ou Maven diretamente na máquina host, pois a compilação é feita no estágio de build do Dockerfile.

## Como executar

Clone o repositório e entre na pasta do projeto:

```bash
git clone https://github.com/Jopako/DevSecOps.git
cd DevSecOps
```

Suba a stack com o comando único previsto no projeto:

```bash
docker compose up --build
```

O comando constrói a imagem da aplicação usando o Dockerfile Multistage e inicia os serviços descritos no Compose. Na primeira inicialização, os serviços de observabilidade podem precisar de alguns instantes para ficarem prontos.

Para executar em segundo plano:

```bash
docker compose up -d --build
```

Confira os containers e seus estados:

```bash
docker compose ps
```

Acesse os endereços descritos na seção [Arquitetura](#arquitetura).

### Credenciais

Use as credenciais definidas na configuração local do ambiente. Senhas, tokens e outras credenciais não devem ser adicionados ao repositório nem publicados em capturas de tela. Caso sejam utilizadas variáveis de ambiente, mantenha os valores secretos fora do Git e documente apenas os nomes das variáveis necessárias.

### Configuração inicial de observabilidade

O ambiente de desenvolvimento utilizado durante a implementação teve o Data Source/dashboard do Grafana e a entrada GELF do Graylog configurados pelas respectivas interfaces e mantidos nos volumes persistentes. Em uma instalação nova sem esses dados persistidos, confira se as configurações foram provisionadas automaticamente; se ainda não estiverem, configure-as antes de validar a observabilidade. Para que a instalação seja inteiramente reproduzível sem nenhuma intervenção manual, essas configurações devem ser mantidas em arquivos de provisionamento versionados junto ao projeto.

No Graylog, a entrada utilizada pela aplicação é do tipo **GELF UDP**, na porta `12201`, com bind address `0.0.0.0`. No Grafana, a fonte de dados Prometheus deve apontar para `http://prometheus:9090` quando o Grafana estiver rodando no Compose.

## Aplicação e endpoints

| Endpoint | Uso |
|---|---|
| `GET /` | Página do Observatório Astronômico |
| `GET /hello` | Rota simples utilizada para validar a aplicação e gerar requisições de teste |
| `GET /actuator/health` | Verificação de saúde do Spring Boot |
| `GET /actuator/prometheus` | Exposição das métricas para o Prometheus |

A aplicação está configurada para expor os endpoints `health`, `info` e `prometheus` pelo Actuator.

O filtro `HttpRequestLoggingFilter` registra as requisições HTTP atendidas pela aplicação com informações como método, caminho, status e duração. As requisições para `/actuator/*` são excluídas desse log de acesso para evitar excesso de mensagens causado pelas coletas do Prometheus.

A navegação interna do frontend por âncoras (por exemplo, clicar em “Explorar” e rolar até uma seção) ocorre no próprio navegador e não gera uma nova requisição HTTP. Atualizações de página e solicitações de recursos servidos pelo Spring Boot, como `index.html`, `style.css` e `script.js`, geram requisições observáveis.

## Docker Multistage Build

O `Dockerfile` utiliza dois estágios:

1. **Build:** imagem com JDK e Maven Wrapper para resolver dependências, compilar o código e gerar o arquivo JAR.
2. **Runtime:** imagem mais enxuta com JRE para executar o JAR, sem incluir o código-fonte nem as ferramentas de compilação.

Essa separação reduz o conteúdo desnecessário da imagem final e segue o requisito obrigatório de Multistage Build do trabalho.

## Pipeline CI/CD

A pipeline está definida em `.github/workflows/ci-cd.yml` e utiliza GitHub Actions. O fluxo automatiza as etapas definidas no workflow para validar a aplicação, construir a imagem e publicá-la no GitHub Container Registry.

- **Testes e build da aplicação:** execução do Maven Wrapper no pipeline.
- **Build da imagem:** construção usando o `Dockerfile` Multistage.
- **Publicação:** push da imagem para o GHCR conforme as condições configuradas no workflow.
- **Imagem publicada:** `ghcr.io/jopako/devsecops:main`.

O resultado de cada execução pode ser consultado na aba **Actions** do repositório. Consulte o workflow para verificar os eventos de disparo e as condições exatas para publicação da imagem.

## Monitoramento com Prometheus

O Spring Boot Actuator e o Micrometer disponibilizam as métricas no endpoint:

```text
http://localhost:8081/actuator/prometheus
```

O arquivo `prometheus/prometheus.yml` configura o Prometheus para coletar as métricas da aplicação pelo endereço interno `app:8080`, usando `/actuator/prometheus` como caminho de métricas.

Para verificar o funcionamento, acesse [Prometheus Targets](http://localhost:9090/targets) e confirme que o target `spring-app` está com estado **UP**.

## Dashboard Grafana

O Grafana utiliza o Prometheus como fonte de dados. O dashboard do projeto acompanha métricas de recursos da JVM e de tráfego HTTP.

| Painel | Objetivo / métrica típica |
|---|---|
| Heap Memory Usage | Memória heap usada pela JVM, por exemplo `sum(jvm_memory_used_bytes{area="heap"})` |
| CPU Usage | Uso de CPU do processo, por exemplo `process_cpu_usage * 100` |
| Requests per Second (RPS) | Taxa de requisições HTTP, por exemplo `sum(rate(http_server_requests_seconds_count[1m]))` |
| HTTP 2xx | Taxa de respostas de sucesso, filtrando status `2..` |
| HTTP 4xx | Taxa de respostas de erro do cliente, filtrando status `4..` |
| HTTP 5xx | Taxa de respostas de erro do servidor, filtrando status `5..` |
| HTTP Response Time | Tempo médio de resposta calculado a partir da soma e da contagem de durações HTTP |

As taxas usam janelas de tempo; por isso os gráficos podem continuar apresentando valores por algum tempo após uma sequência de requisições. Uma série 5xx em zero pode ser um resultado correto quando não houve respostas dessa classe. Respostas `304` pertencem à categoria 3xx, não à série 2xx.

## Logs centralizados com Graylog

A aplicação utiliza Logback com um appender GELF para encaminhar mensagens ao Graylog pela rede Docker. O Graylog utiliza OpenSearch para armazenar e pesquisar mensagens e MongoDB para metadados e configuração.

O filtro de requisições registra mensagens com formato semelhante a:

```text
http_request method=GET path=/style.css status=200 duration_ms=16 outcome=success
```

Os campos da mensagem ajudam a investigar o método HTTP, o caminho solicitado, o status da resposta, a duração e o resultado. As mensagens são encaminhadas ao Graylog pela entrada GELF UDP em `12201`.

Para validar a coleta, abra o Graylog, escolha um intervalo recente (por exemplo, últimos cinco minutos) e pesquise por `http_request`. Depois, atualize o site no navegador e confira as mensagens referentes a `/`, `/style.css` e `/script.js`.

Uma resposta `404` para `/favicon.ico`, por exemplo, significa que o navegador solicitou um ícone de aba que não está presente. Isso pode ser identificado no log e corrigido adicionando um favicon ao frontend.

## Testes rápidos de observabilidade

Os comandos abaixo geram tráfego real contra a aplicação local e ajudam a verificar as métricas HTTP. Execute-os em um terminal com a stack em execução.

Gerar respostas `200`:

```bash
for i in {1..20}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/hello
done
```

Gerar respostas `404` de forma controlada:

```bash
for i in {1..10}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/rota-inexistente
done
```

Os comandos servem apenas para teste. O tráfego gerado pelo navegador ao abrir ou atualizar a página também deve aparecer nas métricas HTTP e, quando registrado pelo filtro, no Graylog.

Verificar saúde e métricas:

```bash
curl -i http://localhost:8081/actuator/health
curl -s http://localhost:8081/actuator/prometheus | head
```

Ver logs recentes da aplicação:

```bash
docker compose logs --tail=100 app
```

## Evidências

As capturas devem ser adicionadas manualmente ao repositório em `docs/evidencias/`, com os nomes abaixo. Substitua ou atualize as imagens conforme as capturas reais do ambiente. Não inclua senhas, tokens nem outros dados secretos nas imagens.

### Aplicação — Observatório Astronômico

<img width="1437" height="984" alt="site" src="https://github.com/user-attachments/assets/6af3ef60-8691-4e00-b366-d56e76bde5d8" />


### Dashboard Grafana

<img width="1377" height="899" alt="grafana" src="https://github.com/user-attachments/assets/1824923c-d22b-4a35-bf71-cbb9d2392420" />

### Prometheus — targets

<img width="819" height="294" alt="Prometheus" src="https://github.com/user-attachments/assets/676454cd-16e4-4490-9904-034cf30511a0" />

### Graylog — mensagens da aplicação

<img width="1597" height="872" alt="graylog" src="https://github.com/user-attachments/assets/e3ba174c-410a-4e35-96af-b9305c79e6af" />

### GitHub Actions — pipeline CI/CD

<img width="1260" height="437" alt="workflows" src="https://github.com/user-attachments/assets/2a353ba1-5c54-47ad-bf39-c9e63ff0066f" />

### Docker Compose — serviços em execução

<img width="1306" height="270" alt="dockerps" src="https://github.com/user-attachments/assets/2ae713a7-c29b-49e7-bfcb-c5166e363463" />

## Persistência e encerramento

Os serviços usam volumes nomeados no Docker Compose para preservar dados entre reinicializações, conforme a configuração de cada serviço.

Parar os serviços sem apagar os dados persistidos:

```bash
docker compose down
```

> Evite `docker compose down -v` quando quiser preservar os dados do Grafana, Prometheus, Graylog, MongoDB e OpenSearch: a opção `-v` remove os volumes nomeados do projeto.

## Solução de problemas

**A aplicação não abre em `localhost:8081`**

```bash
docker compose ps
docker compose logs --tail=100 app
```

Confira se o serviço `app` está ativo e se a porta host `8081` está mapeada para a porta `8080` do container.

**O target do Prometheus está `DOWN`**

Confira se o scrape usa o endereço interno `app:8080` e se `/actuator/prometheus` está acessível dentro da rede do Compose. No container Prometheus, `localhost` significa o próprio container, não o Spring Boot.

**Não aparecem mensagens no Graylog**

Confirme que o serviço Graylog está saudável, que existe uma entrada **GELF UDP** ativa na porta `12201` e que a aplicação consegue resolver o host `graylog` pela rede Docker. Consulte também `docker compose logs --tail=100 app`.

**O dashboard está sem dados**

Confirme que o target do Prometheus está `UP`, que o Data Source do Grafana aponta para `http://prometheus:9090`, que o dashboard usa as métricas exportadas pela aplicação e que houve tráfego HTTP no período selecionado.

---

## Observação sobre credenciais e configuração reproduzível

O repositório deve conter arquivos de configuração, e não credenciais pessoais. Para que uma instalação nova satisfaça integralmente o requisito de inicialização sem intervenção manual, o Data Source e dashboard do Grafana, bem como a entrada GELF do Graylog, devem estar provisionados por arquivos versionados ou outro mecanismo automatizado compatível com a stack. A configuração que existe apenas em volumes criados manualmente no ambiente de desenvolvimento não é automaticamente reproduzida em uma máquina limpa.
