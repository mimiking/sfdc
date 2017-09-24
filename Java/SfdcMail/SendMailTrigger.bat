@echo off
cd %~dp0
java -cp ./bin;./lib/* salesforce.mail.trigger.SfdcMailTrigger

echo %ERRORLEVEL%

@echo on