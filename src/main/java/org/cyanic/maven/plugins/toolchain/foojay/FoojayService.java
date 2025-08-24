/*
 * Copyright 2025 pjgomez97
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanic.maven.plugins.toolchain.foojay;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FoojayService {

    private static final String JDK_PATH_PROPERTY = "jdk.home";

    private FoojayService() {}

    public static Path downloadAndExtractJdk(Log log, Proxy proxySettings, String version, String vendor) throws Exception {
        log.info("Downloading JDK " + vendor + " " + version + " from Foojay");

        String[] fileNameAndDownloadUrl = parseFileNameAndDownloadUrl(log, proxySettings, version, vendor);

        if (fileNameAndDownloadUrl == null) {
            return null;
        }

        String jdkFileName = fileNameAndDownloadUrl[0];

        String downloadUrl = fileNameAndDownloadUrl[1];

        Path jdksDir;

        String jdkPath = System.getProperty(JDK_PATH_PROPERTY);

        if (jdkPath == null) {
            Path userHome = Paths.get(System.getProperty("user.home"));

            jdksDir = userHome.resolve(".m2").resolve("jdks");
        } else {
            jdksDir = Paths.get(jdkPath);
        }

        if (!jdksDir.toFile().exists()) {
            jdksDir.toFile().mkdir();
        }

        Path jdkHome = downloadAndExtract(log, downloadUrl, jdkFileName, jdksDir);

        if (jdkHome.resolve("Contents").resolve("Home").toFile().exists()) {
            jdkHome = jdkHome.resolve("Contents").resolve("Home");
        }

        log.info("JDK downloaded: " + jdkHome.toAbsolutePath());

        if (vendor.contains("graalvm")) {
            Path guBin = jdkHome.resolve("bin").resolve("gu");

            ProcessBuilder pb = new ProcessBuilder(guBin.toAbsolutePath().toString(), "install", "native-image", "--ignore");

            pb.environment().put("GRAALVM_HOME", jdkHome.toAbsolutePath().toString());

            pb.start();
        }

        return jdkHome;
    }

    private static CloseableHttpClient buildHttpClient(Proxy proxy) {
        if (proxy != null) {
            HttpClientBuilder builder = HttpClients.custom();

            builder.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));

            if (proxy.getUsername() != null) {
                Credentials credentials = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword().toCharArray());

                AuthScope authScope = new AuthScope(proxy.getHost(), proxy.getPort());

                BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

                credsProvider.setCredentials(authScope, credentials);

                builder.setDefaultCredentialsProvider(credsProvider);
            }

            return builder.build();
        }

        return HttpClients.createDefault();
    }

    private static String[] parseFileNameAndDownloadUrl(Log log, Proxy proxySettings, String version, String vendor) {
        String os = getOsName();

        String archName = getArchName();

        String archiveType = os.equals("windows") ? "zip" : "tar.gz";

        String bitness = archName.equals("x32") ? "32" : "64";

        String libcType = switch (os) {
            case "linux" -> "glibc";
            case "windows" -> "c_std_lib";
            case "macos" -> "libc";
            default -> "";
        };

        String queryUrl = "https://api.foojay.io/disco/v3.0/packages?"
                + "distribution=" + vendor
                + "&version=" + version
                + "&operating_system=" + os
                + "&architecture=" + archName
                + "&bitness=" + bitness
                + "&archive_type=" + archiveType
                + "&libc_type=" + libcType
                + "&latest=overall&package_type=jdk&discovery_scope_id=directly_downloadable&match=any&javafx_bundled=false&directly_downloadable=true&release_status=ga";

        HttpGet request = new HttpGet(queryUrl);

        try (CloseableHttpClient httpClient = buildHttpClient(proxySettings)) {
            CloseableHttpResponse response = httpClient.execute(request);

            if (response.getCode() == 200) {
                Gson gson = new Gson();

                JsonObject jsonObject = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonElement.class).getAsJsonObject();

                JsonObject pkgJson = jsonObject.getAsJsonArray("result").get(0).getAsJsonObject();

                String pkgInfoUri = pkgJson.getAsJsonObject("links").get("pkg_info_uri").getAsString();

                HttpGet pkgInfoGet = new HttpGet(pkgInfoUri);

                CloseableHttpResponse pkgInfoResponse = httpClient.execute(pkgInfoGet);

                if (pkgInfoResponse.getCode() == 200) {
                    JsonObject pkgInfoJson = gson.fromJson(EntityUtils.toString(pkgInfoResponse.getEntity()), JsonElement.class).getAsJsonObject();

                    String downloadUrl = pkgInfoJson.getAsJsonArray("result").get(0).getAsJsonObject().get("direct_download_uri").getAsString();

                    return new String[]{pkgJson.get("filename").getAsString(), downloadUrl};
                }
            }
        } catch (Exception e) {
            log.error("Error to parse response from " + queryUrl, e);
        }

        return null;
    }

    private static String getOsName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return "macos";
        } else if (os.contains("windows")) {
            return "windows";
        } else {
            return "linux";
        }
    }

    private static String getArchName() {
        String arch = System.getProperty("os.arch").toLowerCase();

        if (arch.contains("x86_32") || arch.contains("amd32")) {
            arch = "x32";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            arch = "aarch64";
        } else {
            arch = "x64";
        }

        return arch;
    }

    private static Path downloadAndExtract(Log log, String link, String fileName, Path destDir) throws Exception {
        File destFile = destDir.resolve(fileName).toFile();

        if (!destFile.exists()) {
            log.debug("Downloading " + link);

            FileUtils.copyURLToFile(new URI(link).toURL(), destFile);
        }

        log.debug("Extracting " + fileName);

        String extractDir = getRootNameInArchive(destFile);

        extractArchiveFile(destFile, destDir.toFile());

        destFile.delete();

        return destDir.resolve(extractDir);
    }

    private static String getRootNameInArchive(File archiveFile) throws Exception {
        ArchiveInputStream<?> archiveInputStream;

        if (archiveFile.getName().endsWith("tar.gz") || archiveFile.getName().endsWith("tgz")) {
            archiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(Files.newInputStream(archiveFile.toPath())));
        } else {
            archiveInputStream = new ZipArchiveInputStream((Files.newInputStream(archiveFile.toPath())));
        }

        String name = archiveInputStream.getNextEntry().getName();

        archiveInputStream.close();

        return name;
    }

    private static void extractArchiveFile(File sourceFile, File destDir) {
        String fileName = sourceFile.getName();

        AbstractUnArchiver unArchiver;

        if (fileName.endsWith(".tgz") || fileName.endsWith(".tar.gz")) {
            unArchiver = new TarGZipUnArchiver();
        } else {
            unArchiver = new ZipUnArchiver();
        }

        unArchiver.setSourceFile(sourceFile);

        unArchiver.setDestDirectory(destDir);

        unArchiver.setOverwrite(true);

        unArchiver.extract();
    }
}
