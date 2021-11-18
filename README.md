# Cloud File Manager

## DB Setup

```shell
docker pull postgres
docker run -d \
--name dev-postgres \
-e POSTGRES_PASSWORD=Pass2020! \
-v ${HOME}/Desktop/postgres-data:/var/lib/postgresql/data \
-p 5432:5432 \
postgres
```