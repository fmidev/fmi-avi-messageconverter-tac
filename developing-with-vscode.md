# Developing using vscode

## For vscode, you will need the following extensions:

- java extension pack
- language support for java(TM) by Red Hat
- Maven for Java
- Java Test Runner
- Project Manager for Java
- Debugger for Java

## Set the right JRE (1.8):

To access various settings for using the JDK, bring up the Command Palette `(Ctrl+Shift+P)` and use the command <b>Java: Configure Java Runtime</b>. Follow the instructions (download and install openJDK8).
Stop vscode, then export JAVA_HOME=<to your jdk8u275-b01 folder> and start vscode again. Press `(Ctrl+Shift+P)`, and set the Java Runtime to 1.8 under the `Maven/Gradle Projects` section

## Add the following to projects to your workspace, base folder is called avi-msgconverter:

So in folder `avi-msgconverter`, clone the following projects:

- git@gitlab.com:opengeoweb/avi-msgconverter/fmi-avi-messageconverter.git
- git@gitlab.com:opengeoweb/avi-msgconverter/fmi-avi-messageconverter-tac.git

So your folder structure looks like:

avi-msgconverter

- fmi-avi-messageconverter
- fmi-avi-messageconverter-tac

Start vscode in avi-msgconverter and add both subfolders to your workspace.

Thats it! You can now start developing and run the tests from vscode.
