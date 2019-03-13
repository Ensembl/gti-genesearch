package org.elasticsearch.bootstrap;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

public class JarHell {
    private JarHell() {}
    public static void checkJarHell() throws Exception {}
    public static void checkJarHell(Consumer consumer) throws Exception {}
    public static void checkJarHell(URL urls[]) throws Exception {}
    public static void checkVersionFormat(String targetVersion) {}
    public static void checkJavaVersion(String resource, String targetVersion) {}
    public static Set parseClassPath() {return Collections.emptySet();}
}