package cz.muni.fi.jboss.migration;

import cz.muni.fi.jboss.migration.spi.IMigrator;
import java.util.List;
import org.apache.commons.collections.map.MultiValueMap;

/**
 *  @author Roman Jakubco
 */
public class Configuration {

    private GlobalConfiguration options;

    private List<ModuleSpecificProperty> moduleOtions;

    public GlobalConfiguration getGlobal() {
        return options;
    }

    public void setOptions(GlobalConfiguration options) {
        this.options = options;
    }

    public List<ModuleSpecificProperty> getModuleOtions() {
        return moduleOtions;
    }

    public void setModuleOtions(List<ModuleSpecificProperty> moduleOtions) {
        this.moduleOtions = moduleOtions;
    }


    private MultiValueMap getForMigrator(Class<? extends IMigrator> migrator) {
        //return moduleOtions.get(migrator);
        throw new UnsupportedOperationException();
    }
    
    
    /**
     *  Triplet for module specific property, e.g --conf.logging.merge=true .
     */
    public static class ModuleSpecificProperty{
        
        private String moduleId;
        private String propName;
        private String value;

        public ModuleSpecificProperty(String moduleId, String propName, String value) {
            this.moduleId = moduleId;
            this.propName = propName;
            this.value = value;
        }

        public String getModuleId() { return moduleId; }
        public void setModuleId(String moduleId) { this.moduleId = moduleId; }
        public String getPropName() { return propName; }
        public void setPropName(String propName) { this.propName = propName; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }        
    }
    
}// class