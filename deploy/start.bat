@echo off
chcp 65001 >nul
REM SchemaSync Start Script
java -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml
pause
