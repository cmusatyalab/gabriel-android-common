package edu.cmu.cs.gabriel.client.comm;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.*;

import com.jcraft.jsch.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Vector;

public class ScpHelper {
    private String hostname = "128.2.209.144";
    private int portSSH = 22;
    private String username = "anonymous";
    private String passwd = "anonymous";
    private String upDestination = "scp/";
    private String downDestination = "scp/";
    private volatile Session session;
    private volatile ChannelSftp channel;
    private String fname;
    private String basename;
    private byte[] fileBytes;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public ScpHelper(String fname) { // Constructor
        this.fname = fname;
        this.basename = new File(fname).getName();
        try {
            fileBytes = Files.readAllBytes(Paths.get(fname));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public ScpHelper() { // Constructor

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void config(String fname) {
        this.fname = fname;
        this.basename = new File(fname).getName();
        try {
            fileBytes = Files.readAllBytes(Paths.get(fname));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendLog() {
        try {
            this.connect(this.hostname, this.portSSH, this.username, this.passwd);
        }
        catch (JSchException e) {
            System.err.println("Failed to connect the server!");
            e.printStackTrace();
            return;
        }
        try {
            this.uploadFile(this.upDestination, this.fname, this.basename, this.fileBytes,true);
        } catch (SftpException e) {
            e.printStackTrace();
        }
        this.disconnect();
    }
    public String getFname() { return this.fname;  }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setPort(int port) { this.portSSH = port; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswd(String passwd) { this.passwd = passwd; }
    public void setUpDestination(String upDestination) { this.upDestination = upDestination; }
    public void setDownDestination(String downDestination) { this.downDestination = downDestination; }

    public synchronized void connect(String host, int port, String username, String password) throws JSchException
    {
        JSch jsch = new JSch();

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig(config);
        session.setInputStream(System.in);
        session.connect();

        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }
    public synchronized void uploadFile(String destDir, String fullfileName, String basefileName, byte[] fileBytes, boolean overwrite) throws SftpException
    {
        if(session == null || channel == null)
        {
            System.err.println("No open session!");
            return;
        }
        File tfile = new File(fullfileName);
        if (!tfile.exists()) {
            System.err.println("File does not exist: " + fullfileName);
            return;
        }
        // a workaround to check if the directory exists. Otherwise, create it
        Vector lsdir = channel.ls(destDir);
        channel.cd(destDir);
        channel.put(new ByteArrayInputStream(fileBytes), basefileName, overwrite ? ChannelSftp.OVERWRITE : ChannelSftp.RESUME);
    }


    public synchronized void disconnect()
    {
        if(session == null || channel == null)
        {
            System.err.println("No open session!");
            return;
        }
        channel.exit();
        channel.disconnect();
        session.disconnect();
        channel = null;
        session = null;
    }
}
