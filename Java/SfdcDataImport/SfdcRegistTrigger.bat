@echo off
cd %~dp0
java -cp ../SfdcDataCommon/bin;./bin;../SfdcDataCommon/lib/* sfdc.register.client.app.SfdcRegistTrigger

echo %ERRORLEVEL%

@echo on