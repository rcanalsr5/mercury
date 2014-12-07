package it.skarafaz.mercury.manager;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import it.skarafaz.mercury.MercuryApplication;
import it.skarafaz.mercury.R;
import it.skarafaz.mercury.data.InitTaskResult;
import it.skarafaz.mercury.data.Server;

public class ConfigManager {
    private static ConfigManager instance;
    private List<Server> servers;

    private ConfigManager() {
        servers = new ArrayList<Server>();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public List<Server> getServers() {
        return servers;
    }

    public InitTaskResult init() {
        servers.clear();
        InitTaskResult result = InitTaskResult.SUCCESS;
        if (isExternalStorageReadable()) {
            File configDir = getConfigDir();
            if (configDir.exists() && configDir.isDirectory()) {
                List<String> files = getConfigFiles(configDir);
                try {
                    for (String file : files) {
                        servers.add(MercuryApplication.getObjectMapper().readValue(file, Server.class));
                    }
                } catch (IOException e) {
                    Log.e(ConfigManager.class.getSimpleName(), e.getMessage());
                }
            } else {
                if (!createDir(configDir)) {
                    result = InitTaskResult.CANNOT_CREATE_CONFIG_DIR;
                }
            }
        } else {
            result = InitTaskResult.CANNOT_READ_EXT_STORAGE;
        }
        return result;
    }

    private File getConfigDir() {
        String appName = MercuryApplication.getContext().getResources().getString(R.string.app_name);
        return new File(Environment.getExternalStorageDirectory(), appName);
    }

    private List<String> getConfigFiles(File configDir) {
        List<String> jsonFiles = new ArrayList<String>();
        File[] files = configDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.toLowerCase().endsWith(".json");
            }
        });
        for (File file : files) {
            try {
                InputStream is = FileUtils.openInputStream(file);
                jsonFiles.add(IOUtils.toString(is, "UTF-8"));
                is.close();
            } catch (IOException e) {
                Log.e(ConfigManager.class.getSimpleName(), e.getMessage());
            }
        }
        return jsonFiles;
    }

    private boolean createDir(File configDir) {
        return isExternalStorageWritable() && configDir.mkdirs();
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}