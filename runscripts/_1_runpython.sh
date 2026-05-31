#!/usr/bin/env bash
#@REM ************************************************************************************
#@REM Description: run Python HTTP server to serve compiled classes (RMI codebase)
#@REM Author: Rui S. Moreira (adapted by João Santos)
#@REM ************************************************************************************
source ./setenv.sh server

#@REM Compila o projeto e copia as dependências antes de servir
cd ${JAVAPROJ}
mvn compile dependency:copy-dependencies -DoutputDirectory=target/dependency -q
echo "Classes compiladas e dependências copiadas."

#@REM Serve as classes via HTTP (necessário para RMI codebase)
cd ${ABSPATH2CLASSES}
echo "A servir classes em http://${SERVER_CODEBASE_HOST}:${SERVER_CODEBASE_PORT}/"
#@REM Python 3:
python3 -m http.server ${SERVER_CODEBASE_PORT}
#@REM Python 2.7:
#python -m SimpleHTTPServer ${SERVER_CODEBASE_PORT}

cd ${JAVAPROJ}/${JAVASCRIPTSPATH}
