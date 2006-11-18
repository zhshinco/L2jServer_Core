@echo off
:start
echo Starting L2J Game Server.
echo.
java -Xmx512m -cp bsf.jar;commons-logging-1.1.jar;javolution.jar;c3p0-0.9.1-pre10.jar;mysql-connector-java-5.0.4-bin.jar;l2jserver.jar;jython.jar net.sf.l2j.gameserver.GameServer
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormaly
echo.
:end
echo.
echo server terminated
echo.
pause
