package com.ta3lim.backend;

import com.ta3lim.backend.repository.jpa.NoteRepository;
import com.ta3lim.backend.service.NoteService;
import com.ta3lim.backend.utils.TimestampUtil;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class ScheduledTasks {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private NoteService noteService;

    private static String dbUser;
    private static String dbPassword;
    private static String extStorageUser;
    private static String extStoragePassword;

    @Value("${app.tasks.runDbDump.enabled}")
    private String runDbDumpEnabled;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${app.tasks.runPushDbDumpToCloud.enabled}")
    private String runPushDbDumpToCloudEnabled;

    @Value("${app.tasks.runPushUploadsToCloud.enabled}")
    private String runPushUploadsToCloudEnabled;

    @Value("${app.tasks.runDbDump.dbName}")
    private String dbName;

    @Value("${app.tasks.runDbDump.dbPort}")
    private String dbPort;

    @Value("${ELASTIC_URL}")
    private String elasticUrl;

    @Value("${spring.elasticsearch.username}")
    private String elasticUser;

    @Value("${spring.elasticsearch.password}")
    private String elasticPassword;

    @Value("${spring.datasource.username}")
    public void setDbUser(String dbUser) { ScheduledTasks.dbUser = dbUser;}

    @Value("${spring.datasource.password}")
    public void setDbPassword(String dbPassword) { ScheduledTasks.dbPassword = dbPassword;}

    @Value("${app.external-storage-user}")
    public void setExtStorageUser(String extStorageUser) { ScheduledTasks.extStorageUser = extStorageUser;}

    @Value("${app.external-storage-password}")
    public void setExtStoragePassword(String extStoragePassword) { ScheduledTasks.extStoragePassword = extStoragePassword;}

    // Initialize the timestamp file at first application startup
    @EventListener(ContextRefreshedEvent.class)
    public void initialize() throws URISyntaxException {
        TimestampUtil.initializeTimestampFile();
        imageCleaner.cleanUnusedImages();
        if (applicationName.equals("demo")) {
            deleteIndex();
            noteService.reindexAllNotes();
        }
    }

    public void deleteIndex() throws URISyntaxException {
        URI uri = new URI(elasticUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        final HttpHost targetHost = new HttpHost(scheme, host, port);
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        AuthScope authScope = new AuthScope(targetHost);
        credentialsProvider.setCredentials(authScope, new UsernamePasswordCredentials(elasticUser, elasticPassword.toCharArray()));
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {
            HttpDelete deleteRequest = new HttpDelete(targetHost + "/" + applicationName);
            try (CloseableHttpResponse response = httpClient.execute(deleteRequest)) {
                System.out.println("Elasticsearch index '" +applicationName+"' DELETE request response: " + response.getCode());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Scheduled(fixedRate = 600000, initialDelay = 600000) // every 10 min
    public void runDbDump() {
        if (runDbDumpEnabled.equals("true") && !applicationName.equals("demo")) {
            try {
                String command="cd \"C:\\Program Files\\MySQL\\MySQL Server 9.0\\bin\" && mysqldump -u "+dbUser+" -p"+dbPassword+" -P "+dbPort+" --no-tablespaces "+dbName+" > c:\\dumps\\pkms.sql";
                int exitCode = commandRunner(command);
                System.out.println("runDbDump exited with code: " + exitCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(fixedRate = 600000, initialDelay = 60000) // every 10 min, 1 minute after runDbDump, if changes to push
    public void runPushDbDumpToCloud() {
        LocalDateTime lastRunPushDbDumpToCloudTime = TimestampUtil.readTimestamp();
        LocalDateTime latestNoteUpdateDateTime = getLatestNoteUpdateDateTime();
        if (runPushDbDumpToCloudEnabled.equals("true") && !applicationName.equals("demo")) {
            try {
                if (lastRunPushDbDumpToCloudTime != null && !latestNoteUpdateDateTime.isAfter(lastRunPushDbDumpToCloudTime)) {
                    return;
                }
                String command = "powershell.exe $dateTime = Get-Date -Format \"yyyy-MM-dd-HH-mm\"; megatools put -u "+extStorageUser+" -p "+extStoragePassword+" --path /Root/dbdump/pkms-$dateTime.sql C:\\dumps\\pkms.sql";
                int exitCode = commandRunner(command);
                System.out.println("runPushDbDumpToCloud exited with code: " + exitCode);
                if (exitCode == 0) {
                    TimestampUtil.writeTimestamp(LocalDateTime.now());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(fixedRate = 43200000, initialDelay = 3720000) // every 12h or 1h02m after startup
    public void runPushUploadsToCloud() {
        if (runPushUploadsToCloudEnabled.equals("true") && !applicationName.equals("demo")) {
            try {
                String command = "megatools copy -u "+extStorageUser+" -p "+extStoragePassword+" -r /Root/uploads -l C:\\uploads";
                int exitCode = commandRunner(command);
                System.out.println("runPushUploadsToCloud exited with code: " + exitCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Integer commandRunner(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        return process.waitFor();
    }

    public LocalDateTime getLatestNoteUpdateDateTime() {
        return noteRepository.findLatestUpdateDateTime();
    }

    @Autowired
    private ImageCleaner imageCleaner;

    @Component
    public static class ImageCleaner {

        @Autowired
        private URLExtractor urlExtractor;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        public void cleanUnusedImages() {
            List<String> imagePaths = urlExtractor.extractImagePaths();
            if (imagePaths.isEmpty()) { return; }
            // Prepare placeholders for SQL query
            String placeholders = imagePaths.stream()
                    .map(path -> "'" + path + "'").collect(Collectors.joining(", "));
            // Retrieve unused image paths from the database
            String selectSql = "SELECT image_path FROM image WHERE image_path NOT IN (" + placeholders + ")";
            List<String> unusedImagePaths = jdbcTemplate.queryForList(selectSql, String.class);
            // Delete files from the filesystem
            for (String filePath : unusedImagePaths) {
                File file = new File("C:\\uploads\\" + filePath);
                if (file.exists() && file.delete()) {
                    System.out.println("Cleaned orphan file: " + filePath);
                }
            }
            String deleteSql = "DELETE FROM image WHERE image_path NOT IN (" + placeholders + ")";
            jdbcTemplate.execute(deleteSql);
        }
    }

    @Component
    public static class URLExtractor {

        @Autowired
        private NoteRepository noteRepository;

        @Autowired
        private NoteService noteService;

        private static final String URL_PATTERN = "http://localhost:8080/images/([\\w\\-. \\[\\]()]+)";

        public List<String> extractImagePaths() {
            List<String> contents = noteRepository.findAllContents();
            Pattern pattern = Pattern.compile(URL_PATTERN);

            return contents.stream()
                    .flatMap(content -> {
                        Matcher matcher = pattern.matcher(content);
                        return matcher.results().map(m -> m.group(1));
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }
    }
}
