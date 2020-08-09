#!/usr/bin/bash
docker build -t fuji-crawler .
docker run -m512M --cpus 2 -it -p 8080:8080 -e VIRTUAL_HOST=fuji.villevalois.fr -e LETSENCRYPT_HOST=fuji.villevalois.fr --rm fuji-crawler
