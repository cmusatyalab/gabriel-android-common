package edu.cmu.cs.gabriel.client.comm;

import android.os.Build;
import android.os.Environment;

import de.mindpipe.android.logging.log4j.LogConfigurator;


public class Log4jHelper {
    private final static LogConfigurator mLogConfigrator = new LogConfigurator();
    private static String fname;
    static {
        fname = configureLog4j();
    }

    public String getFname() {
        return this.fname;
    }
    private static String configureLog4j() {
        String fileName = Environment.getExternalStorageDirectory() + "/Gabriel/" + Build.MODEL +"-openrtist-log4j.log";
        String filePattern = "%d - [%c] - %p : %m%n";
        int maxBackupSize = 10;
        long maxFileSize = 1024 * 1024;

        configure( fileName, filePattern, maxBackupSize, maxFileSize );
        return fileName;
    }

    private static void configure( String fileName, String filePattern, int maxBackupSize, long maxFileSize ) {
        mLogConfigrator.setFileName( fileName );
        mLogConfigrator.setMaxFileSize( maxFileSize );
        mLogConfigrator.setFilePattern(filePattern);
        mLogConfigrator.setMaxBackupSize(maxBackupSize);
        mLogConfigrator.setUseLogCatAppender(true);
        mLogConfigrator.configure();

    }

    public static org.apache.log4j.Logger getLogger( String name ) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( name );
        return logger;
    }
}
