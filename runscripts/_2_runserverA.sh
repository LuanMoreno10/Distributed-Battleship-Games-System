#!/usr/bin/env bash
#@REM ************************************************************************************
#@REM Description: run BattleshipGames Server — Nó A (porta 1099, R5)
#@REM Author: Rui S. Moreira (adapted by João Santos)
#@REM ************************************************************************************
source ./setenv.sh server

cd ${ABSPATH2CLASSES}

echo "[R5] A iniciar Nó A na porta ${REGISTRY_PORT} com peer em ${REGISTRY_PORT_B}..."

${JDK}/bin/java -cp "${CLASSPATH}" \
     -Djava.rmi.server.codebase=${SERVER_CODEBASE} \
     -Djava.rmi.server.hostname=${SERVER_RMI_HOST} \
     ${SERVER_CLASS} --port ${REGISTRY_PORT} --peer ${REGISTRY_HOST}:${REGISTRY_PORT_B}

cd ${JAVAPROJ}/${JAVASCRIPTSPATH}
