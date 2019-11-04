#!/bin/sh

# Git revision: GIT_REVISION_PLACEHOLDER

MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
  java="$JAVA_HOME/bin/java"
fi

tools_jar_path="$(realpath "$(dirname "$(realpath "$(which "$java")")")/../../lib/tools.jar")"
if test -f "$tools_jar_path"; then
  exec "$java" $java_args -classpath "$tools_jar_path:$MYSELF" steprecorder.core "$@"
else
  echo "The jdi-steprecorder requires JDK to be installed, not just JRE"
fi
exit 1
