package com.framework.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SystemInfoUtils
 *
 * A collection of system / network / OS information helper methods.
 *
 * Notes:
 *  - Some values (DNS servers, gateway, service status) have no
 *    standard cross-platform Java API and are obtained by shelling
 *    out to OS-native commands and parsing their output. These are
 *    best-effort and may need tweaking for your exact OS/locale.
 *  - getTotalMemoryMB / getFreeMemoryMB / getCpuUsage use the
 *    com.sun.management.OperatingSystemMXBean extension, which is
 *    available on Oracle/OpenJDK but is technically a JDK-internal
 *    (non-guaranteed-portable) API.
 */
public class SystemInfoUtils {

    // ---------- Host / Network ----------

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static String getIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static String getMacAddress() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) return "UNKNOWN";
            byte[] mac = network.getHardwareAddress();
            if (mac == null) return "UNKNOWN";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static String getDomainName() {
        // Best-effort: canonical hostname often includes the domain suffix.
        try {
            String canonical = InetAddress.getLocalHost().getCanonicalHostName();
            int firstDot = canonical.indexOf('.');
            if (firstDot > -1 && firstDot < canonical.length() - 1) {
                return canonical.substring(firstDot + 1);
            }
            // Windows fallback via environment variable
            String winDomain = System.getenv("USERDNSDOMAIN");
            if (winDomain != null && !winDomain.isEmpty()) return winDomain;
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static List<String> getDnsServers() {
        List<String> servers = new ArrayList<>();
        String os = getOSName().toLowerCase();
        try {
            if (os.contains("win")) {
                String output = runCommand("ipconfig", "/all");
                for (String line : output.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("DNS Servers")) {
                        String[] parts = trimmed.split(":", 2);
                        if (parts.length == 2) servers.add(parts[1].trim());
                    } else if (!servers.isEmpty() && trimmed.matches("[0-9.]+")) {
                        // Continuation lines list additional DNS servers indented below the label
                        servers.add(trimmed);
                    }
                }
            } else {
                // Linux / macOS: read /etc/resolv.conf
                File resolvConf = new File("/etc/resolv.conf");
                if (resolvConf.exists()) {
                    try (BufferedReader reader = new BufferedReader(new java.io.FileReader(resolvConf))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("nameserver")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) servers.add(parts[1]);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // return whatever was collected, possibly empty
        }
        return servers;
    }

    public static String getGateway() {
        String os = getOSName().toLowerCase();
        try {
            String output;
            if (os.contains("win")) {
                output = runCommand("ipconfig", "/all");
                for (String line : output.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("Default Gateway")) {
                        String[] parts = trimmed.split(":", 2);
                        if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                            return parts[1].trim();
                        }
                    }
                }
            } else if (os.contains("mac")) {
                output = runCommand("route", "-n", "get", "default");
                for (String line : output.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("gateway:")) {
                        return trimmed.substring("gateway:".length()).trim();
                    }
                }
            } else {
                // Linux
                output = runCommand("sh", "-c", "ip route | grep default");
                // Example: "default via 192.168.1.1 dev eth0"
                String[] tokens = output.trim().split("\\s+");
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (tokens[i].equals("via")) return tokens[i + 1];
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return "UNKNOWN";
    }

    public static boolean isPortOpen(String host, int port) {
        return isPortOpen(host, port, 2000);
    }

    public static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInternetAvailable() {
        // Try a couple of well-known reliable hosts on a common port.
        String[] hosts = {"8.8.8.8", "1.1.1.1"};
        for (String host : hosts) {
            if (isPortOpen(host, 53, 2000)) return true;
        }
        return false;
    }

    // ---------- OS / User / Java ----------

    public static String getOSName() {
        return System.getProperty("os.name", "UNKNOWN");
    }

    public static String getOSVersion() {
        return System.getProperty("os.version", "UNKNOWN");
    }

    public static String getUserName() {
        return System.getProperty("user.name", "UNKNOWN");
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version", "UNKNOWN");
    }

    public static String getEnvironmentVariable(String name) {
        String value = System.getenv(name);
        return value != null ? value : "";
    }

    // ---------- Memory / CPU / Disk ----------

    @SuppressWarnings({"restriction", "deprecation"})
    public static long getTotalMemoryMB() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            // Deprecated since Java 14 (replaced by getTotalMemorySize()), kept for pre-14 compatibility
            return osBean.getTotalPhysicalMemorySize() / (1024 * 1024);
        } catch (Exception e) {
            return -1;
        }
    }

    @SuppressWarnings({"restriction", "deprecation"})
    public static long getFreeMemoryMB() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            // Deprecated since Java 14 (replaced by getFreeMemorySize()), kept for pre-14 compatibility
            return osBean.getFreePhysicalMemorySize() / (1024 * 1024);
        } catch (Exception e) {
            return -1;
        }
    }

    @SuppressWarnings("restriction")
    public static double getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getSystemCpuLoad(); // 0.0 - 1.0, or -1 if unavailable
            return load < 0 ? -1 : load * 100.0;
        } catch (Exception e) {
            return -1;
        }
    }

    public static double getDiskFreeSpaceGB(String path) {
        try {
            File file = new File(path);
            long freeBytes = file.getFreeSpace();
            return freeBytes / (1024.0 * 1024.0 * 1024.0);
        } catch (Exception e) {
            return -1;
        }
    }

    public static double getDiskFreeSpaceGB() {
        // Default to root of the filesystem the JVM is running on.
        return getDiskFreeSpaceGB(File.listRoots()[0].getAbsolutePath());
    }

    // ---------- Services / Files ----------

    /**
     * Returns a status string for a named service.
     * Windows: uses "sc query <name>" and looks for STATE.
     * Linux: uses "systemctl is-active <name>".
     * macOS: uses "launchctl list <name>".
     */
    public static String getServiceStatus(String serviceName) {
        String os = getOSName().toLowerCase();
        try {
            if (os.contains("win")) {
                String output = runCommand("sc", "query", serviceName);
                for (String line : output.split("\\r?\\n")) {
                    if (line.trim().startsWith("STATE")) {
                        return line.trim();
                    }
                }
                return "UNKNOWN";
            } else if (os.contains("mac")) {
                String output = runCommand("launchctl", "list", serviceName);
                return output.isEmpty() ? "NOT RUNNING" : "RUNNING";
            } else {
                String output = runCommand("systemctl", "is-active", serviceName);
                return output.trim();
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static boolean isFileExist(String path) {
        File f = new File(path);
        return f.exists() && f.isFile();
    }

    public static boolean isFolderExist(String path) {
        File f = new File(path);
        return f.exists() && f.isDirectory();
    }

    // ---------- Helper ----------

    private static String runCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        process.waitFor(5, TimeUnit.SECONDS);
        return output.toString();
    }
}