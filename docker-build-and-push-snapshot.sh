#!/bin/bash

./mvnw clean package -DskipTests=true dockerfile:build -Ddocker.org=harbor-13-78-118-111.sslip.io/categolj
docker push harbor-13-78-118-111.sslip.io/categolj/blog-gateway:0.0.1-SNAPSHOT