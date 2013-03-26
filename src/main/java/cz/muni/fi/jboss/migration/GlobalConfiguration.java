package cz.muni.fi.jboss.migration;

import java.io.File;

/**
 * Class for storing global information needed for migration. Like dir of AS7, AS5, and profiles
 *
 * @author Roman Jakubco
 */
public class GlobalConfiguration {
    
    // TODO: With regard of possibility to migrate from other vendors' AS,
    //       split to AS 7 config class, and then 1 class per server (AS 5, WebLogic, ...)

    // AS 7 stuff
    private String dirAS7;
    private String confPathAS7 = "standalone/configuration/standalone.xml";

    // AS 5 stuff
    private String dirAS5;
    private String as5profileName = "default";
    public static final String AS5_PROFILES_DIR = "server" + File.separator; // TODO: Move to AS5-specific class method.

    public File getProfileDir(){
        return new File( dirAS5, GlobalConfiguration.AS5_PROFILES_DIR + as5profileName );
    }

    // Non-server stuff
    private String appPath;

    private boolean skipValidation;
    
    
    
    //<editor-fold defaultstate="collapsed" desc="get/set">
    public String getDirAS7() { return dirAS7; }
    public void setDirAS7(String dirAS7) { this.dirAS7 = dirAS7; }
    public String getConfPathAS7() { return confPathAS7; }
    public void setConfPathAS7(String confPathAS7) { this.confPathAS7 = confPathAS7; }
    public String getStandaloneFilePath() {
        return new File(getDirAS7(), getConfPathAS7()).getPath();  // TODO: Return File and use that.
    }
    //public void setStandaloneFilePath() { standaloneFilePath = getDirAS7() + File.separator + getConfPathAS7(); }

    public String getDirAS5() { return dirAS5; }
    public void setDirAS5(String dirAS5) { this.dirAS5 = dirAS5; }
    public String getAS5ProfileName() { return as5profileName; }
    public void setAS5ProfileName(String profileName) { this.as5profileName = profileName; }

    public String getAppPath() { return appPath; }
    public void setAppPath(String appPath) { this.appPath = appPath; }
    
    public boolean isSkipValidation() { return skipValidation; }
    public void setSkipValidation(boolean skipValidation) { this.skipValidation = skipValidation; }
    //</editor-fold>
    
}// class
