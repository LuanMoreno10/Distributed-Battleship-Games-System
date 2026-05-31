#!/usr/bin/env bash
#@REM ************************************************************************************
#@REM Description: run BattleshipGames Server — Nó B (porta 1100, R5)
#@REM Author: Rui S. Moreira (adapted by João Santos)
#@REM ************************************************************************************
source ./setenv.sh server

cd ${ABSPATH2CLASSES}

echo "[R5] A iniciar Nó B na porta ${REGISTRY_PORT_B} com peer em ${REGISTRY_PORT}..."

${JDK}/bin/java -cp "${CLASSPATH}" \
     -Djava.rmi.server.codebase=${SERVER_CODEBASE} \
     -Djava.rmi.server.hostname=${SERVER_RMI_HOST} \
     ${SERVER_CLASS} --port ${REGISTRY_PORT_B} --peer ${REGISTRY_HOST}:${REGISTRY_PORT}

cd ${JAVAPROJ}/${JAVASCRIPTSPATH}
