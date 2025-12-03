### Build image

```bash
docker build -t retrivium .
```

### Create docker network

```bash
docker network create dai-retrivium
```

Check that it has been created

```bash
docker network ls
```


### Run server

```bash
docker run --rm -it --network dai-retrivium --name retrivium-server retrivium server --port 6433 -D documents
```

### Run client

```bash
docker run --rm -it --network dai-retrivium  retrivium client --port 6433 --host retrivium-server
```


