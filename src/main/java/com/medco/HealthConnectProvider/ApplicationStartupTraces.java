package com.medco.HealthConnectProvider;

import com.medco.HealthConnectProvider.shared.error.Assert;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ApplicationStartupTraces {

    private static final String SEPARATOR = "-".repeat(58);
    private static final String BREAK = "\n";

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupTraces.class);

    private ApplicationStartupTraces() {
    }

    static String of(Environment environment) {
        Assert.notNull("environment", environment);

        return new ApplicationStartupTracesBuilder()
                .append(BREAK)
                .appendSeparator()
                .append(applicationRunningTrace(environment))
                .append(localUrl(environment))
                .append(externalUrl(environment))
                .append(profilesTrace(environment))
                .appendSeparator()
                .append(configServer(environment))
                .build();
    }

    private static String applicationRunningTrace(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name");

        if (StringUtils.isBlank(applicationName)) {
            return "Application is running!";
        }

        return "Application '" + applicationName + "' is running!";
    }

    private static String localUrl(Environment environment) {
        return url("Local", "localhost", environment);
    }

    private static String externalUrl(Environment environment) {
        return url("External", hostAddress(), environment);
    }

    private static String url(String type, String host, Environment environment) {
        if (notWebEnvironment(environment)) {
            return null;
        }

        return type +
                ": \t" +
                protocol(environment) +
                "://" +
                host +
                ":" +
                port(environment) +
                contextPath(environment);
    }

    private static boolean notWebEnvironment(Environment environment) {
        return StringUtils.isBlank(environment.getProperty("server.port"));
    }

    private static String protocol(Environment environment) {
        if (noKeyStore(environment)) {
            return "http";
        }

        return "https";

    }

    private static boolean noKeyStore(Environment environment) {
        return StringUtils.isBlank(environment.getProperty("server.ssl.key-store"));
    }

    private static String port(Environment environment) {
        return environment.getProperty("server.port");
    }

    private static String profilesTrace(Environment environment) {
        String[] profiles = environment.getActiveProfiles();

        if (ArrayUtils.isEmpty(profiles)) {
            return null;
        }

        return "Profile(s): \t" + Stream.of(profiles).collect(Collectors.joining(", "));
    }

    private static String hostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }

        return "localhost";
    }

    private static String contextPath(Environment environment) {
        String contextPath = environment.getProperty("server.servlet.context-path");

        if (StringUtils.isBlank(contextPath)) {
            return "/";
        }

        return contextPath;
    }

    private static String configServer(Environment environment) {
        String configServer = environment.getProperty("configserver.status");

        if (StringUtils.isBlank(configServer)) {
            return null;
        }

        return "Config Server: " + configServer + BREAK + SEPARATOR + BREAK;
    }

    private static class ApplicationStartupTracesBuilder {

        private static final String SPACER = "  ";

        private final StringBuilder trace = new StringBuilder();

        public ApplicationStartupTracesBuilder appendSeparator() {
            trace.append(SEPARATOR).append(BREAK);

            return this;
        }

        public ApplicationStartupTracesBuilder append(String line) {
            if (line == null) {
                return this;
            }

            trace.append(SPACER).append(line).append(BREAK);

            return this;
        }

        public String build() {
            return trace.toString();
        }
    }
}
