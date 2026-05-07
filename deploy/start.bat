@echo off`nchcp 65001 >nul`nREM SchemaSync Start Script`njava -jar -Xms256m -Xmx512m schemasync.jar --spring.config.location=application.yml`npause
