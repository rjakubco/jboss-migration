package cz.muni.fi.jboss.migration;

import cz.muni.fi.jboss.migration.actions.IMigrationAction;
import cz.muni.fi.jboss.migration.conf.Configuration;
import cz.muni.fi.jboss.migration.conf.GlobalConfiguration;
import cz.muni.fi.jboss.migration.ex.*;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ResAdapterMigrator;
import cz.muni.fi.jboss.migration.migrators.dataSources.DatasourceMigrator;
import cz.muni.fi.jboss.migration.migrators.logging.LoggingMigrator;
import cz.muni.fi.jboss.migration.migrators.security.SecurityMigrator;
import cz.muni.fi.jboss.migration.migrators.server.ServerMigrator;
import cz.muni.fi.jboss.migration.spi.IMigrator;
import cz.muni.fi.jboss.migration.utils.RollbackUtils;
import cz.muni.fi.jboss.migration.utils.Utils;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FileUtils;
import org.eclipse.persistence.exceptions.JAXBException;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 *  Controls the core migration processes.
 * 
 *  TODO: Perhaps leave init() and doMigration() in here 
 *        and separate the other methods to a MigrationService{} ?
 *
 *  @author Roman Jakubco
 */
public class MigratorEngine {
    
    private static final Logger log = LoggerFactory.getLogger(MigratorEngine.class);
    

    private Configuration config;

    private MigrationContext ctx;

    private List<IMigrator> migrators;
    
    

    public MigratorEngine( Configuration config ) throws InitMigratorsExceptions {
        this.config = config;
        this.init();
        this.resetContext();
    }
    
    private void resetContext() {
        this.ctx = new MigrationContext();
    }

    /**
     *  Initializes this Migrator, especially instantiates the IMigrators.
     */
    private void init() throws InitMigratorsExceptions {
        
        // Find IMigrator implementations.
        List<Class<? extends IMigrator>> migratorClasses = findMigratorClasses();

        // Initialize migrator instances. 
        Map<Class<? extends IMigrator>, IMigrator> migratorsMap = 
                createMigrators( migratorClasses, config.getGlobal(), null);
        
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
     *  Performs the migration.
     * 
     * TODO:  MIGR-31
     *      1) Parse AS 7 config into context.
            2) Let the migrators gather the data into the context.
            3) Let them prepare the actions.
                  An action should include what caused it to be created. IMigrationAction.getOriginMessage()
            ==== From now on, don't use the scanned data, only actions. ===
            So instead of getDOMElements(), getCLICommand and apply()
            will be List<IMigrationAction> prepareActions().
            4) preValidate
            5) backup
            6) perform
            7) postValidate
            8] rollback
     */
    public void doMigration() throws MigrationException {
        
        log.info("Commencing migration.");
        
        this.resetContext();
        
        // Parse AS 7 config. MIGR-31 OK
        File as7configFile = new File(config.getGlobal().getAS7Config().getConfigFilePath());
        try {
            DocumentBuilder db = Utils.createXmlDocumentBuilder();
            Document doc = db.parse(as7configFile);
            ctx.setAS7ConfigXmlDoc(doc);
            
            // TODO: Do backup at file level, instead of parsing and writing back.
            //       And rework it in general. MIGR-23.
            doc = db.parse(as7configFile);
            ctx.setAs7ConfigXmlDocOriginal(doc);
        } 
        catch ( SAXException | IOException ex ) {
            throw new MigrationException("Failed loading AS 7 config from " + as7configFile, ex );
        }
        
        
        

        
        // MIGR-31 - The new way.
        String message = null;
        try {
            // Load the source server config.
            message = "Failed loading AS 5 config from " + as7configFile;
            this.loadAS5Data();
            
            // Ask all the migrators to create the actions to be performed.
            message = "Failed preparing the migration actions.";
            this.prepareActions();
            message = "Actions validation failed.";
            this.preValidateActions();
            message = "Failed creating backups for the migration actions.";
            this.backupActions();
            message = "Failed performing the migration actions.";
            this.performActions();

            // Specific problem on my PC. Need to connect two times => first time
            try{
                executeRequest(ctx.getBatch().toRequest());
            } catch (MigrationException e){

            }
            // Testing=> checking created CLI scripts
            int i = 1;
            for(BatchedCommand command : ctx.getBatch().getCommands()){
                System.out.println(i++ + ": " + command.getCommand());
            }


            // Execution
            log.info("CLI Batch:");
            message = "Execution of batch failed.";
            executeRequest(ctx.getBatch().toRequest());


            message = "Verification of migration actions results failed.";
            this.postValidateActions();
        }
        catch( MigrationException ex ) {
            this.rollbackActionsWhichWerePerformed();
            throw new MigrationException( message, ex );
        }
        finally {
            this.cleanBackupsIfAny();
        }

    }// migrate()


    /**
     * Temp method for testing
     */
    public static void executeRequest(ModelNode request) throws MigrationException {
        ModelControllerClient client = null;
        try {
            client = ModelControllerClient.Factory.create("localhost", 9999);
            final ModelNode response = client.execute(new OperationBuilder(request).build());
            reportFailure(response);
        } catch (IOException e) {
            throw new MigrationException("Execution of the batch failed: " + e.getMessage(), e);
        } finally {
            safeClose(client);
        }
    }
    
    /**
     * Temp method for testing
     */
    public static void safeClose(final Closeable closeable) throws MigrationException {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            throw new MigrationException("SAFECLOSE FAILED:" + e.getMessage(), e);
        }
    }

    /**
     * Temp method for testing
     */
    private static void reportFailure(final ModelNode node) throws MigrationException {
        if (!node.get(ClientConstants.OUTCOME).asString().equals(ClientConstants.SUCCESS)) {
            final String msg;
            if (node.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                if (node.hasDefined(ClientConstants.OP)) {
                    msg = String.format("Operation '%s' at address '%s' failed: %s", node.get(ClientConstants.OP), node.get(ClientConstants.OP_ADDR), node.get(ClientConstants.FAILURE_DESCRIPTION));
                } else {
                    msg = String.format("Operation failed: %s", node.get(ClientConstants.FAILURE_DESCRIPTION));
                }
            } else {
                msg = String.format("Operation failed: %s", node);
            }
            throw new MigrationException(msg);
        }
    }
    
    /**
     *  Ask all the migrators to create the actions to be performed; stores them in the context.
     */
    private void prepareActions() throws MigrationException {
        log.debug("prepareActions()");
        try {
            for (IMigrator mig : this.migrators) {
                log.debug("    Preparing actions with " + mig.getClass().getSimpleName());
                mig.createActions(this.ctx);
            }
        } catch (JAXBException e) {
            throw new LoadMigrationException(e);
        }
        // TODO: Additional logic to filter out duplicated file copying etc.
    }
    
    
    /**
     *  Actions with the actions.
     */
    private void preValidateActions() throws MigrationException {
        List<IMigrationAction> actions = ctx.getActions();
        for( IMigrationAction action : actions ) {
            action.preValidate();
        }
    }
    private void backupActions() throws MigrationException {
        List<IMigrationAction> actions = ctx.getActions();
        for( IMigrationAction action : actions ) {
            action.backup();
        }
    }
    private void performActions() throws MigrationException {
        List<IMigrationAction> actions = ctx.getActions();
        for( IMigrationAction action : actions ) {
            action.setMigrationContext(ctx);
            action.perform();
        }
    }
    private void postValidateActions() throws MigrationException {
        List<IMigrationAction> actions = ctx.getActions();
        for( IMigrationAction action : actions ) {
            action.postValidate();
        }
    }
    private void cleanBackupsIfAny() throws MigrationException {
        List<IMigrationAction> actions = ctx.getActions();
        for( IMigrationAction action : actions ) {
            //if( action.isAfterBackup())  // Checked in cleanBackup() itself.
            action.cleanBackup();
        }
    }
    private void rollbackActionsWhichWerePerformed() throws MigrationException {
        List<IMigrationAction> actions = ctx.getActions();
        for( IMigrationAction action : actions ) {
            //if( action.isAfterPerform()) // Checked in rollback() itself.
            action.rollback();
        }
    }
    
    
    /**
     * Calls all migrators' callback for loading configuration data from the source server.
     *
     * @throws LoadMigrationException
     */
    public void loadAS5Data() throws LoadMigrationException {
        log.debug("loadAS5Data()");
        try {
            for (IMigrator mig : this.migrators) {
                log.debug("    Scanning with " + mig.getClass().getSimpleName());
                mig.loadAS5Data(this.ctx);
            }
        } catch (JAXBException e) {
            throw new LoadMigrationException(e);
        }
    }
    
    
    /**
     * Calls all migrators' callback for applying migrated configuration.
     * 
     * @throws ApplyMigrationException if inserting of generated nodes fails.
     */
    private void apply() throws ApplyMigrationException {
        log.debug("apply()");
        // Call the callbacks.
        for (IMigrator mig : this.migrators) {
            log.debug("    Applying with " + mig.getClass().getSimpleName());
            mig.apply(this.ctx);
        }
        // Put the resulting DOM to AS 7 config file.
        // TODO: This could alternatively send CLI commands over Management API. MIGR-28.
        try {
            // TODO: Isn't Transformer for XSLT? Use some normal XML output.
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            File targetConfigFile = new File(this.config.getGlobal().getAS7Config().getConfigFilePath());
            StreamResult result = new StreamResult(targetConfigFile);
            DOMSource source = new DOMSource(this.ctx.getAS7ConfigXmlDoc());
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            throw new ApplyMigrationException(ex);
        }
    }


}// class
