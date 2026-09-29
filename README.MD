# Automação, Observabilidade e Esteira DevSecOps

Projeto desenvolvido para a disciplina de DevSecOps, com o objetivo de aplicar práticas de containerização, CI/CD, monitoramento, observabilidade e centralização de logs em uma aplicação Spring Boot.

## Tecnologias

- Java 25
- Spring Boot
- Maven
- Docker
- Docker Compose
- Prometheus
- Grafana
- Graylog
- MongoDB
- OpenSearch
- GitHub Actions
- GitHub Container Registry (GHCR)

## Arquitetura

A aplicação é composta pelos seguintes serviços:

- **devops-app** — aplicação Spring Boot
- **Prometheus** — coleta de métricas
- **Grafana** — visualização das métricas
- **Graylog** — centralização e consulta dos logs
- **MongoDB** — armazenamento utilizado pelo Graylog
- **OpenSearch** — armazenamento e indexação dos logs

Os serviços são executados por meio do Docker Compose.

## Execução

Para iniciar todo o ambiente:

```bash
docker compose up -d
