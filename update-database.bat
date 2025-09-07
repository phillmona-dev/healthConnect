@echo off
echo Updating database with missing tables...
mvn liquibase:update
echo Done!
pause
