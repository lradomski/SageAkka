#!/usr/bin/env bash
$JAVA_HOME/bin/jjs -scripting -Dfile.encoding=UTF-8 -classpath "/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/lib/tools.jar:/Users/luke/git/SageAkka/target/classes:/Users/luke/.m2/repository/com/typesafe/akka/akka-actor_2.11/2.4.7/akka-actor_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar:/Users/luke/.m2/repository/com/typesafe/config/1.3.0/config-1.3.0.jar:/Users/luke/.m2/repository/org/scala-lang/modules/scala-java8-compat_2.11/0.7.0/scala-java8-compat_2.11-0.7.0.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-agent_2.11/2.4.7/akka-agent_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/scala-stm/scala-stm_2.11/0.7/scala-stm_2.11-0.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-camel_2.11/2.4.7/akka-camel_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/apache/camel/camel-core/2.13.4/camel-core-2.13.4.jar:/Users/luke/.m2/repository/com/sun/xml/bind/jaxb-impl/2.2.6/jaxb-impl-2.2.6.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-cluster_2.11/2.4.7/akka-cluster_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-cluster-metrics_2.11/2.4.7/akka-cluster-metrics_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-cluster-sharding_2.11/2.4.7/akka-cluster-sharding_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-cluster-tools_2.11/2.4.7/akka-cluster-tools_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-contrib_2.11/2.4.7/akka-contrib_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-http-core_2.11/2.4.7/akka-http-core_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-parsing_2.11/2.4.7/akka-parsing_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-http-testkit_2.11/2.4.7/akka-http-testkit_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-multi-node-testkit_2.11/2.4.7/akka-multi-node-testkit_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-osgi_2.11/2.4.7/akka-osgi_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/osgi/org.osgi.core/4.3.1/org.osgi.core-4.3.1.jar:/Users/luke/.m2/repository/org/osgi/org.osgi.compendium/4.3.1/org.osgi.compendium-4.3.1.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-persistence_2.11/2.4.7/akka-persistence_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-protobuf_2.11/2.4.7/akka-protobuf_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-persistence-tck_2.11/2.4.7/akka-persistence-tck_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/scalatest/scalatest_2.11/2.2.4/scalatest_2.11-2.2.4.jar:/Users/luke/.m2/repository/org/scala-lang/scala-reflect/2.11.2/scala-reflect-2.11.2.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-remote_2.11/2.4.7/akka-remote_2.11-2.4.7.jar:/Users/luke/.m2/repository/io/netty/netty/3.10.3.Final/netty-3.10.3.Final.jar:/Users/luke/.m2/repository/org/uncommons/maths/uncommons-maths/1.2.2a/uncommons-maths-1.2.2a.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-slf4j_2.11/2.4.7/akka-slf4j_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/slf4j/slf4j-api/1.7.16/slf4j-api-1.7.16.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-stream_2.11/2.4.7/akka-stream_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/ssl-config-akka_2.11/0.2.1/ssl-config-akka_2.11-0.2.1.jar:/Users/luke/.m2/repository/com/typesafe/ssl-config-core_2.11/0.2.1/ssl-config-core_2.11-0.2.1.jar:/Users/luke/.m2/repository/org/scala-lang/modules/scala-parser-combinators_2.11/1.0.4/scala-parser-combinators_2.11-1.0.4.jar:/Users/luke/.m2/repository/org/reactivestreams/reactive-streams/1.0.0/reactive-streams-1.0.0.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-stream-testkit_2.11/2.4.7/akka-stream-testkit_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-testkit_2.11/2.4.7/akka-testkit_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-distributed-data-experimental_2.11/2.4.7/akka-distributed-data-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-typed-experimental_2.11/2.4.7/akka-typed-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-http-experimental_2.11/2.4.7/akka-http-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-http-jackson-experimental_2.11/2.4.7/akka-http-jackson-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.7.4/jackson-databind-2.7.4.jar:/Users/luke/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.7.4/jackson-core-2.7.4.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-http-spray-json-experimental_2.11/2.4.7/akka-http-spray-json-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/io/spray/spray-json_2.11/1.3.2/spray-json_2.11-1.3.2.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-http-xml-experimental_2.11/2.4.7/akka-http-xml-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar:/Users/luke/.m2/repository/com/typesafe/akka/akka-persistence-query-experimental_2.11/2.4.7/akka-persistence-query-experimental_2.11-2.4.7.jar:/Users/luke/.m2/repository/com/google/guava/guava/19.0/guava-19.0.jar:/Users/luke/.m2/repository/com/tr/analytics/sage/engine-common/0-SNAPSHOT/engine-common-0-SNAPSHOT.jar:/Users/luke/.m2/repository/org/apache/logging/log4j/log4j-api/2.5/log4j-api-2.5.jar:/Users/luke/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.8.0.rc1/jackson-annotations-2.8.0.rc1.jar:/Users/luke/.m2/repository/io/netty/netty-all/4.0.36.Final/netty-all-4.0.36.Final.jar:/Users/luke/.m2/repository/org/reflections/reflections/0.9.10/reflections-0.9.10.jar:/Users/luke/.m2/repository/org/javassist/javassist/3.19.0-GA/javassist-3.19.0-GA.jar:/Users/luke/.m2/repository/com/google/code/findbugs/annotations/2.0.1/annotations-2.0.1.jar:/Users/luke/.m2/repository/org/apache/logging/log4j/log4j-core/2.5/log4j-core-2.5.jar:/Users/luke/.m2/repository/com/tr/analytics/sage/engine-core/0-SNAPSHOT/engine-core-0-SNAPSHOT.jar:/Users/luke/.m2/repository/org/aeonbits/owner/owner/1.0.9/owner-1.0.9.jar:/Users/luke/.m2/repository/org/apache/commons/commons-collections4/4.0/commons-collections4-4.0.jar:/Users/luke/.m2/repository/commons-configuration/commons-configuration/1.10/commons-configuration-1.10.jar:/Users/luke/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:/Users/luke/.m2/repository/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar:/Users/luke/.m2/repository/commons-io/commons-io/2.4/commons-io-2.4.jar:/Users/luke/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.5.4/jackson-dataformat-yaml-2.5.4.jar:/Users/luke/.m2/repository/org/yaml/snakeyaml/1.12/snakeyaml-1.12.jar:/Users/luke/.m2/repository/io/searchbox/jest/2.0.1/jest-2.0.1.jar:/Users/luke/.m2/repository/io/searchbox/jest-common/2.0.1/jest-common-2.0.1.jar:/Users/luke/.m2/repository/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar:/Users/luke/.m2/repository/org/apache/commons/commons-lang3/3.4/commons-lang3-3.4.jar:/Users/luke/.m2/repository/org/apache/httpcomponents/httpcore-nio/4.4.4/httpcore-nio-4.4.4.jar:/Users/luke/.m2/repository/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar:/Users/luke/.m2/repository/org/apache/httpcomponents/httpclient/4.5.2/httpclient-4.5.2.jar:/Users/luke/.m2/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:/Users/luke/.m2/repository/org/apache/httpcomponents/httpasyncclient/4.1.1/httpasyncclient-4.1.1.jar:/Users/luke/.m2/repository/mysql/mysql-connector-java/5.1.6/mysql-connector-java-5.1.6.jar:/Users/luke/.m2/repository/org/apache/geode/geode-core/1.0.0-incubating.M2/geode-core-1.0.0-incubating.M2.jar:/Users/luke/.m2/repository/org/apache/geode/geode-joptsimple/1.0.0-incubating.M2/geode-joptsimple-1.0.0-incubating.M2.jar:/Users/luke/.m2/repository/javax/resource/javax.resource-api/1.7/javax.resource-api-1.7.jar:/Users/luke/.m2/repository/javax/transaction/javax.transaction-api/1.2/javax.transaction-api-1.2.jar:/Users/luke/.m2/repository/org/apache/geode/geode-json/1.0.0-incubating.M2/geode-json-1.0.0-incubating.M2.jar:/Users/luke/.m2/repository/it/unimi/dsi/fastutil/7.0.2/fastutil-7.0.2.jar:/Users/luke/.m2/repository/org/apache/logging/log4j/log4j-jul/2.5/log4j-jul-2.5.jar:/Users/luke/.m2/repository/com/github/stephenc/findbugs/findbugs-annotations/1.3.9-1/findbugs-annotations-1.3.9-1.jar:/Users/luke/.m2/repository/org/apache/logging/log4j/log4j-jcl/2.5/log4j-jcl-2.5.jar:/Users/luke/.m2/repository/org/fusesource/jansi/jansi/1.8/jansi-1.8.jar:/Users/luke/.m2/repository/antlr/antlr/2.7.7/antlr-2.7.7.jar:/Users/luke/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.5/log4j-slf4j-impl-2.5.jar:/Users/luke/.m2/repository/org/jgroups/jgroups/3.6.8.Final/jgroups-3.6.8.Final.jar:/Users/luke/.m2/repository/net/java/dev/jna/jna/4.0.0/jna-4.0.0.jar:/Users/luke/.m2/repository/org/apache/geode/geode-common/1.0.0-incubating.M2/geode-common-1.0.0-incubating.M2.jar:/Users/luke/.m2/repository/io/netty/netty-buffer/4.1.0.CR7/netty-buffer-4.1.0.CR7.jar:/Users/luke/.m2/repository/io/netty/netty-common/4.1.0.CR7/netty-common-4.1.0.CR7.jar:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar"
