@echo off
cd %~dp0
java -cp ../SfdcDataCommon/bin;./bin;../SfdcDataCommon/lib/* sfdc.mip.client.app.TeleCallExpTrigger

echo %ERRORLEVEL%

@echo on