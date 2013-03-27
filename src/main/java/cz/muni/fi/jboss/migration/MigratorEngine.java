package cz.muni.fi.jboss.migration;

import cz.muni.fi.jboss.migration.ex.InitMigratorsExceptions;
import cz.muni.fi.jboss.migration.conf.Configuration;
import cz.muni.fi.jboss.migration.conf.GlobalConfiguration;
import cz.muni.fi.jboss.migration.ex.*;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ResAdapterMigrator;
import cz.muni.fi.jboss.migration.migrators.dataSources.DatasourceMigrator;
import cz.muni.fi.jboss.migration.migrators.logging.LoggingMigrator;
import cz.muni.fi.jboss.migration.migrators.security.SecurityMigrator;
import cz.muni.fi.jboss.migration.migrators.server.ServerMigrator;
import cz.muni.fi.jboss.migration.spi.IMigrator;
import cz.muni.fi.jboss.migration.utils.AS7ModuleUtils;
import cz.muni.fi.jboss.migration.utils.RollbackUtils;
import cz.muni.fi.jboss.migration.utils.Utils;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FileUtils;
import org.eclipse.persistence.exceptions.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.w3c.dom.Document;


/**
 *  Controls the core migration processes.
 *
 *  @author Roman Jakubco
 */
public class MigratorEngine {
    
    private static final Logger log = LoggerFactory.getLogger(MigratorEngine.class);
    

    private Configuration config;

    private MigrationContext ctx;

    private List<IMigrator> migrators;
    
    

    public MigratorEngine( Configuration config, MigrationContext context ) throws InitMigratorsExceptions {
        this.config = config;
        this.ctx = context;
        this.init();
    }

    /**
     *  Initializes this Migrator, especially instantiates the IMigrators.
     */
    private void init() throws InitMigratorsExceptions {
        
        // Find IMigrator implementations.
        List<Class<? extends IMigrator>> migratorClasses = findMigratorClasses();

        // Initialize migrator instances. 
        Map<Class<? extends IMigrator>, IMigrator> migratorsMap = 
                createMigrators( migratorClasses, config.getGlobal(), null); // TODO! MultiValueMap of plugin-specific config values.
        
        this.migrators = new ArrayList(migratorsMap.values());
        
        // For each migrator (AKA module, AKA plugin)...
        for( IMigrator mig : this.migrators ){
            
            // Supply some references.
            mig.setGlobalConfig( this.config.getGlobal() );
            
            // Let migrators process module-specific args.
            for( Configuration.ModuleSpecificProperty moduleOption : config.getModuleConfigs() ){
                mig.examineConfigProperty( moduleOption );
            }
        }
        
    }// init()
    
    
    
    /**
     *  Instantiate the plugins.
     */
    private static Map<Class<? extends IMigrator>, IMigrator> createMigrators(
            List<Class<? extends IMigrator>> migratorClasses,
            GlobalConfiguration globalConfig,
            MultiValueMap config
        ) throws InitMigratorsExceptions {
        
        Map<Class<? extends IMigrator>, IMigrator> migs = new HashMap<>();
        List<Exception> exs  = new LinkedList<>();
        
        for( Class<? extends IMigrator> cls : migratorClasses ){
            try {
                //IMigrator mig = cls.newInstance();
                //GlobalConfiguration globalConfig, MultiValueMap config
                Constructor<? extends IMigrator> ctor = cls.getConstructor(GlobalConfiguration.class, MultiValueMap.class);
                IMigrator mig = ctor.newInstance(globalConfig, config);
                migs.put(cls, mig);
            }
            catch( NoSuchMethodException ex ){
                String msg = cls.getName() + " doesn't have constructor ...(GlobalConfiguration globalConfig, MultiValueMap config).";
                log.error( msg );
                exs.add( new MigrationException(msg) );
            }
            catch( InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                log.error("Failed instantiating " + cls.getSimpleName() + ": " + ex.toString());
                log.debug("Stack trace: ", ex);
                exs.add(ex);
            }
        }
        
        if( ! exs.isEmpty() ){
            throw new InitMigratorsExceptions(exs);
        }
        
        return migs;
    }// createMigrators()
    
    
    /**
     *  Finds the implementations of the IMigrator.
     *  TODO: Implement scanning for classes.
     */
    private static List<Class<? extends IMigrator>> findMigratorClasses() {
        
        LinkedList<Class<? extends IMigrator>> migratorClasses = new LinkedList();
        migratorClasses.add( SecurityMigrator.class );
        migratorClasses.add( ServerMigrator.class );
        migratorClasses.add( DatasourceMigrator.class );
        migratorClasses.add( ResAdapterMigrator.class );
        migratorClasses.add( LoggingMigrator.class );
        
        return migratorClasses;
    }
    


    /**
     * Method which calls method for loading configuration data from AS5 on all migrators.
     *
     * @throws LoadMigrationException
     */
    public void loadAS5Data() throws LoadMigrationException {
        try {
            for (IMigrator mig : this.migrators) {
                mig.loadAS5Data(this.ctx);
            }
        } catch (JAXBException e) {
            throw new LoadMigrationException(e);
        }
    }

    /**
     * Method which calls method for applying migrated configuration on all migrators.
     *
     * @throws ApplyMigrationException if inserting of generated nodes fails.
     */
    public void apply() throws ApplyMigrationException {
        for (IMigrator mig : this.migrators) {
            mig.apply(this.ctx);
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult result = new StreamResult(new File(this.config.getGlobal().getAS7Config().getConfigFilePath()));
            DOMSource source = new DOMSource(this.ctx.getAS7ConfigXmlDoc());
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            throw new ApplyMigrationException(ex);
        }
    }

    
    /**
     * Calls all migrators' callback for generating Dom Nodes.
     *
     * @return List containing all generated Nodes
     * @throws MigrationException if migrating of file or generating of nodes fails.
     */
    public List<Node> getDOMElements() throws MigrationException {
        List<Node> elements = new LinkedList();
        for (IMigrator mig : this.migrators) {
            elements.addAll(mig.generateDomElements(this.ctx));
        }
        return elements;
    }

    
    /**
     * Calls all migrators' callback for generating CLI scripts.
     *
     * @return List containing generated scripts from all migrated subsystems
     * @throws CliScriptException if creation of scripts fail
     */
    public List<String> getCLIScripts() throws CliScriptException {
        List<String> scripts = new LinkedList();
        for (IMigrator mig : this.migrators) {
            scripts.addAll(mig.generateCliScripts(this.ctx));
        }

        return scripts;
    }

    
    /**
     * Copies all necessary files for migration from AS5 to their place in the AS7 home folder.
     *
     * @throws CopyException if copying of files fails.
     */
    public void copyItems() throws CopyException {
        
        String targetPath = this.config.getGlobal().getAS7Config().getDir();
        File as5ProfileDir = this.config.getGlobal().getAS5Config().getProfileDir();
        File as5commonLibDir = Utils.createPath(this.config.getGlobal().getAS5Config().getDir(), "common", "lib");

        for (RollbackData rollData : this.ctx.getRollbackData()) {
            
            if (rollData.getName() == null || rollData.getName().isEmpty()) {
                throw new IllegalStateException("Rollback data name is not set.");
            }

            List<File> list = Utils.searchForFile(rollData, as5ProfileDir);

            switch (rollData.getType()) {
                case DRIVER: case LOGMODULE:{
                    // For now only expecting one jar for driver. Pick the first one.
                    if (list.isEmpty()) {
                        List<File> altList = Utils.searchForFile(rollData, as5commonLibDir);
                        RollbackUtils.setRollbackData(rollData, altList, targetPath);
                    } else {
                        RollbackUtils.setRollbackData(rollData, list, targetPath);
                    }
                }
                break;
                case LOG:
                    RollbackUtils.setRollbackData(rollData, list, targetPath);
                    break;
                case SECURITY:
                    RollbackUtils.setRollbackData(rollData, list, targetPath);
                    break;
                case RESOURCE:
                    RollbackUtils.setRollbackData(rollData, list, targetPath);
                    break;
            }
        }// for each rollData

        try {
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

            for( RollbackData cp : this.ctx.getRollbackData() ) {
                
                RollbackData.Type type = cp.getType();
                if( type.equals(RollbackData.Type.DRIVER) || type.equals(RollbackData.Type.LOGMODULE) ) {
                    File directories = new File(cp.getTargetPath());
                    FileUtils.forceMkdir(directories);
                    File moduleXml = new File(directories.getAbsolutePath(), "module.xml");

                    if( ! moduleXml.createNewFile() )
                        throw new CopyException("File already exists: " + moduleXml.getPath());
                    
                    Document doc = RollbackData.Type.DRIVER.equals(type)
                            ? AS7ModuleUtils.createDriverModuleXML(cp)
                            : AS7ModuleUtils.createLogModuleXML(cp);
                    
                    transformer.transform( new DOMSource(doc), new StreamResult(moduleXml));
                }

                FileUtils.copyFileToDirectory(new File(cp.getHomePath()), new File(cp.getTargetPath()));
            }
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new CopyException(e);
        }
    }// copyItems()


}// class