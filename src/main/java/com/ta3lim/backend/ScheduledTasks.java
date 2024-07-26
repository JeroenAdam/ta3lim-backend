package com.ta3lim.backend;

import com.ta3lim.backend.config.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class ScheduledTasks {

    private static String dbUser;
    private static String dbPassword;
    private static String extStorageUser;
    private static String extStoragePassword;

    @Value("${app.tasks.runDbDump.enabled}")
    private String runDbDumpEnabled;

    @Value("${app.tasks.runPushDbDumpToCloud.enabled}")
    private String runPushDbDumpToCloudEnabled;

    @Value("${app.tasks.runPushUploadsToCloud.enabled}")
    private String runPushUploadsToCloudEnabled;

    @Value("${app.tasks.runDbDump.dbName}")
    private String dbName;

    @Value("${app.tasks.runDbDump.dbPort}")
    private String dbPort;

    @Value("${spring.datasource.username}")
    public void setDbUser(String dbUser) { ScheduledTasks.dbUser = dbUser;}

    @Value("${spring.datasource.password}")
    public void setDbPassword(String dbPassword) { ScheduledTasks.dbPassword = dbPassword;}

    @Value("${app.external-storage-user}")
    public void setExtStorageUser(String extStorageUser) { ScheduledTasks.extStorageUser = extStorageUser;}

    @Value("${app.external-storage-password}")
    public void setExtStoragePassword(String extStoragePassword) { ScheduledTasks.extStoragePassword = extStoragePassword;}

    @Scheduled(fixedRate = 3600000, initialDelay = 3600000) // every 1h
    public void runDbDump() {
        if (runDbDumpEnabled.equals("true")) {
            try {
                String command="cd \"C:\\Program Files\\MySQL\\MySQL Server 9.0\\bin\" && mysqldump -u "+dbUser+" -p"+dbPassword+" -P "+dbPort+" --no-tablespaces "+dbName+" > c:\\dumps\\pkms.sql";
                int exitCode = commandRunner(command);
                System.out.println("runDbDump exited with code: " + exitCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 3660000) // every 1 hour, 1 minute after runDbDump
    public void runPushDbDumpToCloud() {
        if (runPushDbDumpToCloudEnabled.equals("true")) {
            try {
                String command = "powershell.exe $dateTime = Get-Date -Format \"yyyy-MM-dd-HH-mm\"; megatools put -u "+extStorageUser+" -p "+extStoragePassword+" --path /Root/dbdump/pkms-$dateTime.sql C:\\dumps\\pkms.sql";
                int exitCode = commandRunner(command);
                System.out.println("runPushDbDumpToCloud exited with code: " + exitCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Scheduled(fixedRate = 43200000, initialDelay = 3720000) // every 12h or 1h02m after startup
    public void runPushUploadsToCloud() {
        if (runPushUploadsToCloudEnabled.equals("true") ) {
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
}