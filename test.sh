#!/bin/bash
mvn versions:set -DnewVersion='4.5.324'; mvn deploy
mvn versions:set -DnewVersion='8'; mvn deploy
mvn versions:set -DnewVersion='3.0.2'; mvn deploy
mvn versions:set -DnewVersion='1.0.0'; mvn deploy
./NexusCleaner.groovy -url http://localhost:8081/nexus -ns com/nexuscleaner -age 3 -user admin -pass admin123 -arm -debug 2

#keytool -export -alias the-ca-server -keystore trust.jks -file exported-der.crt
#openssl x509 -out exported-pem.crt -outform pem -in exported-der.crt -inform der
#export CURL_CA_BUNDLE=$HOME/.m2/exported-pem.crt
#export JAVA_OPTS="-Djavax.net.ssl.trustStore=$HOME/.m2/trust.jks -Djavax.net.ssl.trustStorePassword=changeit"
