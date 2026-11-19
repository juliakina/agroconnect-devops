#!/usr/bin/env bash
set -euo pipefail

DbHost="${1:?DbHost ausente}"
DbPort="${2:?DbPort ausente}"
DbSid="${3:?DbSid ausente}"
DbUser="${4:?DbUser ausente}"
DbPass="${5:?DbPass ausente}"

if [ ! -x "$HOME/sqlcl/bin/sql" ]; then
  echo "Baixando SQLcl..."
  curl -sSL -o "$HOME/sqlcl.zip" "https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip"
  echo "Extraindo SQLcl..."
  unzip -q "$HOME/sqlcl.zip" -d "$HOME"
fi
SQLBIN="$HOME/sqlcl/bin/sql"

CONEXAO="${DbUser}/\"${DbPass}\"@${DbHost}:${DbPort}/${DbSid}"

echo "Verificando objetos do AgroConnect no Banco..."
cat > verifica_objetos.sql <<'SQL'
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 VERIFY OFF
SELECT COUNT(*) FROM user_tables WHERE table_name = 'PROPERTIES';
EXIT
SQL

if ! RESULTADO="$("$SQLBIN" -S -L "$CONEXAO" @verifica_objetos.sql)"; then
  echo "[ERRO] Nao foi possivel conectar no Oracle. Confira ORACLE_USER e ORACLE_PASS."
  exit 1
fi
QTD_TABELAS="$(echo "$RESULTADO" | tr -d '[:space:]')"

if [ "$QTD_TABELAS" = "0" ]; then
  echo "Criando objetos do AgroConnect com o scripts/script-bd.sql..."
  cat > cria_objetos.sql <<'SQL'
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF
@scripts/script-bd.sql
EXIT
SQL
  "$SQLBIN" -S -L "$CONEXAO" @cria_objetos.sql
  echo "[OK] Objetos do BD criados"
elif [[ "$QTD_TABELAS" =~ ^[0-9]+$ ]]; then
  echo "[OK] Objetos do BD ja existem - mantendo os dados atuais"
else
  echo "[ERRO] Retorno inesperado do Banco: $RESULTADO"
  exit 1
fi

rg="rg-agroconnect"
location=${LOCATION}
plan="plan-agroconnect"
app=${NOME_WEBAPP}
runtime="JAVA:17-java17"
sku="B1"

echo "Criando Grupo de Recursos..."
az group create --name "$rg" --location "$location" 1>/dev/null
echo "Criando Plano de Servico..."
az appservice plan create --name "$plan" --resource-group "$rg" --location "$location" --sku "$sku" --is-linux 1>/dev/null
echo "Criando Servico de Aplicativo..."
az webapp create --resource-group "$rg" --plan "$plan" --runtime "$runtime" --name "$app" 1>/dev/null

echo "Habilitando Always On..."
az webapp config set --resource-group "$rg" --name "$app" --always-on true 1>/dev/null

app_logging="$(az webapp log show -g "$rg" -n "$app" --query 'applicationLogs.fileSystem.level' -o tsv 2>/dev/null || true)"
ws_logging="$(az webapp log show -g "$rg" -n "$app" --query 'httpLogs.fileSystem.enabled' -o tsv 2>/dev/null || true)"

if [ "$app_logging" != "Information" ] || [ "$ws_logging" != "true" ]; then
  echo "Habilitando Logs do Servico de Aplicativo..."
  az webapp log config \
    --resource-group "$rg" \
    --name "$app" \
    --application-logging filesystem \
    --web-server-logging filesystem \
    --level information 1>/dev/null
else
  echo "Logs ja configurados"
fi

echo "[OK] Objetos da Azure prontos"
