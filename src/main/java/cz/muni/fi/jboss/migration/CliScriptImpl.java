package cz.muni.fi.jboss.migration;

import cz.muni.fi.jboss.migration.ex.CliScriptException;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ConfigProperty;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ConnectionDefinition;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ResourceAdapter;
import cz.muni.fi.jboss.migration.migrators.dataSources.*;
import cz.muni.fi.jboss.migration.migrators.logging.*;
import cz.muni.fi.jboss.migration.migrators.security.LoginModuleAS7;
import cz.muni.fi.jboss.migration.migrators.security.ModuleOptionAS7;
import cz.muni.fi.jboss.migration.migrators.security.SecurityDomain;
import cz.muni.fi.jboss.migration.migrators.server.ConnectorAS7;
import cz.muni.fi.jboss.migration.migrators.server.SocketBinding;
import cz.muni.fi.jboss.migration.migrators.server.VirtualServer;
import cz.muni.fi.jboss.migration.old.CliScript;


/**
 * Class for creating CLI scripts of migrated configuration of AS5. Implementation of interface CliScript
 *
 * @author  Roman Jakubco
 * Date: 10/7/12
 * Time: 2:29 PM
 */

public class CliScriptImpl implements CliScript {

    private String checkingMethod(String script, String name, String setter){
        if(setter != null){
            if (!setter.isEmpty()) {
                script = script.concat(name + "=" + setter);
            }
        }
        return script;
    }


    @Override
    public String createDatasourceScript(DatasourceAS7 datasourceAS7) throws CliScriptException {
        if((datasourceAS7.getPoolName() == null) || (datasourceAS7.getPoolName().isEmpty())){
              throw new CliScriptException("Error: pool-name of datasource cannot be null or empty",
                      new NullPointerException());
        }

        if((datasourceAS7.getJndiName() == null) || (datasourceAS7.getJndiName().isEmpty())){
            throw new CliScriptException("Error: jndi-name of datasource cannot be null or empty",
                    new NullPointerException());
        }

        if((datasourceAS7.getConnectionUrl() == null) || (datasourceAS7.getConnectionUrl().isEmpty())){
            throw new CliScriptException("Error: connection-url in datasource cannot be null or empty",
                    new NullPointerException());
        }

        if((datasourceAS7.getDriver() == null) || (datasourceAS7.getDriver().isEmpty())){
            throw new CliScriptException("Error: driver-name in datasource cannot be null or empty",
                    new NullPointerException());
        }

        String script = "/subsystem=datasources/data-source=";
        script = script.concat(datasourceAS7.getPoolName()+":add(");
        script = checkingMethod(script, "jndi-name", datasourceAS7.getJndiName());
        script = checkingMethod(script, ", enabled", datasourceAS7.getEnabled());
        script = checkingMethod(script, ", use-java-context", datasourceAS7.getUseJavaContext());
        script = checkingMethod(script, ", driver-name", datasourceAS7.getDriver());
        script = checkingMethod(script, ", connection-url", datasourceAS7.getConnectionUrl());
        script = checkingMethod(script, ", url-delimeter", datasourceAS7.getUrlDelimeter());
        script = checkingMethod(script, ", url-selector-strategy-class-name", datasourceAS7.getUrlSelector());
        script = checkingMethod(script, ", transaction-isolation", datasourceAS7.getTransIsolation());
        script = checkingMethod(script, ", new-connection-sql", datasourceAS7.getNewConnectionSql());
        script = checkingMethod(script, ", prefill", datasourceAS7.getPrefill());
        script = checkingMethod(script, ", min-pool-size", datasourceAS7.getMinPoolSize());
        script = checkingMethod(script, ", max-pool-size", datasourceAS7.getMaxPoolSize());
        script = checkingMethod(script, ", password", datasourceAS7.getPassword());
        script = checkingMethod(script, ", user-name", datasourceAS7.getUserName());
        script = checkingMethod(script, ", security-domain", datasourceAS7.getSecurityDomain());
        script = checkingMethod(script, ", check-valid-connection-sql", datasourceAS7.getCheckValidConSql());
        script = checkingMethod(script, ", validate-on-match", datasourceAS7.getValidateOnMatch());
        script = checkingMethod(script, ", background-validation", datasourceAS7.getBackgroundValid());
        script = checkingMethod(script, ", background-validation-minutes", datasourceAS7.getBackgroundValidMin());
        script = checkingMethod(script, ", use-fast-fail", datasourceAS7.getUseFastFail());
        script = checkingMethod(script, ", exception-sorter-class-name", datasourceAS7.getExceptionSorter());
        script = checkingMethod(script, ", valid-connection-checker-class-name", datasourceAS7.getValidateOnMatch());
        script = checkingMethod(script, ", stale-connection-checker-class-name", datasourceAS7.getStaleConChecker());
        script = checkingMethod(script, ", blocking-timeout-millis", datasourceAS7.getBlockingTimeoutMillis());
        script = checkingMethod(script, ", idle-timeout-minutes", datasourceAS7.getIdleTimeoutMin());
        script = checkingMethod(script, ", set-tx-query-timeout", datasourceAS7.getSetTxQueryTimeout());
        script = checkingMethod(script, ", query-timeout", datasourceAS7.getQueryTimeout());
        script = checkingMethod(script, ", allocation-retry", datasourceAS7.getAllocationRetry());
        script = checkingMethod(script, ", allocation-retry-wait-millis", datasourceAS7.getAllocRetryWaitMillis());
        script = checkingMethod(script, ", use-try-lock", datasourceAS7.getUseTryLock());
        script = checkingMethod(script, ", prepared-statement-cache-size", datasourceAS7.getPreStatementCacheSize());
        script = checkingMethod(script, ", track-statements", datasourceAS7.getTrackStatements());
        script = checkingMethod(script, ", share-prepared-statements", datasourceAS7.getSharePreStatements());
        script = script.concat(")\n");
        script = script.concat("data-source enable --name=" + datasourceAS7.getPoolName());

        return script;
    }

    @Override
    public String createXaDatasourceScript(XaDatasourceAS7 xaDatasourceAS7) throws  CliScriptException{
        if((xaDatasourceAS7.getPoolName() == null) || (xaDatasourceAS7.getPoolName().isEmpty())){
            throw new CliScriptException("Error: pool-name of xa-datasource cannot be null or empty",
                    new NullPointerException());
        }

        if((xaDatasourceAS7.getJndiName() == null) || (xaDatasourceAS7.getJndiName().isEmpty())){
            throw new CliScriptException("Error: jndi-name of xa-datasource cannot be null or empty",
                    new NullPointerException());
        }

        if((xaDatasourceAS7.getDriver() == null) || (xaDatasourceAS7.getDriver().isEmpty())){
            throw new CliScriptException("Error: driver-name in xa-datasource cannot be null",
                    new NullPointerException());
        }

        String script = "/subsystem=datasources/xa-data-source=";
        script = script.concat(xaDatasourceAS7.getPoolName()+":add(");
        script = checkingMethod(script, "jndi-name", xaDatasourceAS7.getJndiName());
        script = checkingMethod(script, ", enabled", xaDatasourceAS7.getEnabled());
        script = checkingMethod(script, ", use-java-context", xaDatasourceAS7.getUseJavaContext());
        script = checkingMethod(script, ", driver-name", xaDatasourceAS7.getDriver());
        script = checkingMethod(script, ", url-delimeter", xaDatasourceAS7.getUrlDelimeter());
        script = checkingMethod(script, ", url-selector-strategy-class-name", xaDatasourceAS7.getUrlSelector());
        script = checkingMethod(script, ", transaction-isolation", xaDatasourceAS7.getTransIsolation());
        script = checkingMethod(script, ", new-connection-sql", xaDatasourceAS7.getNewConnectionSql());
        script = checkingMethod(script, ", prefill", xaDatasourceAS7.getPrefill());
        script = checkingMethod(script, ", min-pool-size", xaDatasourceAS7.getMinPoolSize());
        script = checkingMethod(script, ", max-pool-size", xaDatasourceAS7.getMaxPoolSize());
        script = checkingMethod(script, ", is-same-rm-override", xaDatasourceAS7.getSameRmOverride());
        script = checkingMethod(script, ", interleaving", xaDatasourceAS7.getInterleaving());
        script = checkingMethod(script, ", no-tx-separate-pools", xaDatasourceAS7.getNoTxSeparatePools());
        script = checkingMethod(script, ", password", xaDatasourceAS7.getPassword());
        script = checkingMethod(script, ", user-name", xaDatasourceAS7.getUserName());
        script = checkingMethod(script, ", security-domain", xaDatasourceAS7.getSecurityDomain());
        script = checkingMethod(script, ", check-valid-connection-sql", xaDatasourceAS7.getCheckValidConSql());
        script = checkingMethod(script, ", validate-on-match", xaDatasourceAS7.getValidateOnMatch());
        script = checkingMethod(script, ", background-validation", xaDatasourceAS7.getBackgroundValid());
        script = checkingMethod(script, ", background-validation-minutes", xaDatasourceAS7.getBackgroundValidMin());
        script = checkingMethod(script, ", use-fast-fail", xaDatasourceAS7.getUseFastFail());
        script = checkingMethod(script, ", exception-sorter-class-name", xaDatasourceAS7.getExceptionSorter());
        script = checkingMethod(script, ", valid-connection-checker-class-name", xaDatasourceAS7.getValidateOnMatch());
        script = checkingMethod(script, ", stale-connection-checker-class-name", xaDatasourceAS7.getStaleConChecker());
        script = checkingMethod(script, ", blocking-timeout-millis", xaDatasourceAS7.getBlockingTimeoutMillis());
        script = checkingMethod(script, ", idle-timeout-minutes", xaDatasourceAS7.getIdleTimeoutMinutes());
        script = checkingMethod(script, ", set-tx-query-timeout", xaDatasourceAS7.getSetTxQueryTimeout());
        script = checkingMethod(script, ", query-timeout", xaDatasourceAS7.getQueryTimeout());
        script = checkingMethod(script, ", allocation-retry", xaDatasourceAS7.getAllocationRetry());
        script = checkingMethod(script, ", allocation-retry-wait-millis", xaDatasourceAS7.getAllocRetryWaitMillis());
        script = checkingMethod(script, ", use-try-lock", xaDatasourceAS7.getUseTryLock());
        script = checkingMethod(script, ", xa-resource-timeout", xaDatasourceAS7.getXaResourceTimeout());
        script = checkingMethod(script, ", prepared-statement-cache-size", xaDatasourceAS7.getPreStatementCacheSize());
        script = checkingMethod(script, ", track-statements", xaDatasourceAS7.getTrackStatements());
        script = checkingMethod(script, ", share-prepared-statements", xaDatasourceAS7.getSharePreStatements());
        script = script.concat(")\n");

       if(xaDatasourceAS7.getXaDatasourceProps() != null){
           for(XaDatasourceProperty xaDatasourceProperty : xaDatasourceAS7.getXaDatasourceProps()){
               script = script.concat("/subsystem=datasources/xa-data-source=" + xaDatasourceAS7.getPoolName());
               script = script.concat("/xa-datasource-properties=" + xaDatasourceProperty.getXaDatasourcePropName());
               script = script.concat(":add(value=" + xaDatasourceProperty.getXaDatasourceProp() + ")\n");

           }
       }
        script = script.concat("xa-data-source enable --name=" + xaDatasourceAS7.getPoolName());
        return script;
    }

    @Override
    public String createDriverScript(Driver driver) throws CliScriptException {
        if((driver.getDriverModule() == null) || (driver.getDriverModule().isEmpty())){
            throw new CliScriptException("Error: Driver module in driver cannot be null or empty",
                        new NullPointerException());
        }

        if((driver.getDriverName() == null) || (driver.getDriverName().isEmpty())){
            throw new CliScriptException("Error: Driver name cannot be null or empty",
                        new NullPointerException());
        }

        String script = "/subsystem=data-sources/jdbc-driver=";
        script = script.concat(driver.getDriverName() + ":add(");
        script = script.concat("driver-module-name=" + driver.getDriverModule());
        script = checkingMethod(script, ", driver-class-name", driver.getDriverClass());
        script = checkingMethod(script, ", driver-xa-datasource-class-name", driver.getXaDatasourceClass());
        script = checkingMethod(script, ", driver-major-version", driver.getMajorVersion());
        script = checkingMethod(script, ", driver-minor-version", driver.getMinorVersion());
        script = script.concat(")");

        return script;
    }

    @Override
    public String createResAdapterScript(ResourceAdapter resourceAdapter) throws CliScriptException{
        if((resourceAdapter.getJndiName() == null) || (resourceAdapter.getJndiName().isEmpty())){
             throw new CliScriptException("Error: name of the resource-adapter cannot be null or empty",
                     new NullPointerException());
        }

        if((resourceAdapter.getArchive() == null) || (resourceAdapter.getArchive().isEmpty())){
             throw new CliScriptException("Error: archive in the resource-adapter cannot be null or empty",
                     new NullPointerException());
        }

        String script = "/subsystem=resource-adapters/resource-adapter=";
        script = script.concat(resourceAdapter.getJndiName() + ":add(");
        script = script.concat("archive=" + resourceAdapter.getArchive());
        script = checkingMethod(script, ", transaction-support", resourceAdapter.getTransactionSupport());
        script = script.concat(")\n");

        if(resourceAdapter.getConnectionDefinitions() != null){
            for(ConnectionDefinition connectionDefinition : resourceAdapter.getConnectionDefinitions()){
                if((connectionDefinition.getClassName() == null) || (connectionDefinition.getClassName().isEmpty())){
                     throw new CliScriptException("Error: class-name in the connection definition cannot be null or empty",
                             new NullPointerException());
                }

                script =  script.concat("/subsystem=resource-adapters/resource-adapter=" + resourceAdapter.getJndiName());
                script = script.concat("/connection-definitions=" + connectionDefinition.getPoolName() + ":add(");
                script = checkingMethod(script, " jndi-name", connectionDefinition.getJndiName());
                script = checkingMethod(script, ", enabled", connectionDefinition.getEnabled());
                script = checkingMethod(script, ", use-java-context", connectionDefinition.getUseJavaCont());
                script = checkingMethod(script, ", class-name", connectionDefinition.getClassName());
                script = checkingMethod(script, ", use-ccm", connectionDefinition.getUseCcm());
                script = checkingMethod(script, ", prefill", connectionDefinition.getPrefill());
                script = checkingMethod(script, ", use-strict-min", connectionDefinition.getUseStrictMin());
                script = checkingMethod(script, ", flush-strategy", connectionDefinition.getFlushStrategy());
                script = checkingMethod(script, ", min-pool-size", connectionDefinition.getMinPoolSize());
                script = checkingMethod(script, ", max-pool-size", connectionDefinition.getMaxPoolSize());

                if(connectionDefinition.getSecurityDomain() != null){
                    script = checkingMethod(script, ", security-domain", connectionDefinition.getSecurityDomain());
                }

                if(connectionDefinition.getSecDomainAndApp() != null){
                    script = checkingMethod(script, ", security-domain-and-application",
                            connectionDefinition.getSecDomainAndApp());
                }

                if(connectionDefinition.getAppManagedSec() != null){
                    script = checkingMethod(script, ", application-managed-security",
                            connectionDefinition.getAppManagedSec());
                }

                script = checkingMethod(script, ", background-validation", connectionDefinition.getBackgroundValidation());
                script = checkingMethod(script, ", background-validation-millis", connectionDefinition.getBackgroundValiMillis());
                script = checkingMethod(script, ", blocking-timeout-millis", connectionDefinition.getBackgroundValiMillis());
                script = checkingMethod(script, ", idle-timeout-minutes", connectionDefinition.getIdleTimeoutMinutes());
                script = checkingMethod(script, ", allocation-retry", connectionDefinition.getAllocationRetry());
                script = checkingMethod(script, ", allocation-retry-wait-millis", connectionDefinition.getAllocRetryWaitMillis());
                script = checkingMethod(script, ", xa-resource-timeout", connectionDefinition.getXaResourceTimeout());
                script = script.concat(")\n");

                if(connectionDefinition.getConfigProperties() != null){
                    for(ConfigProperty configProperty : connectionDefinition.getConfigProperties()){
                        script = script.concat("/subsystem=resource-adapters/resource-adapter=" + resourceAdapter.getJndiName());
                        script = script.concat("/connection-definitions=" + connectionDefinition.getPoolName());
                        script = script.concat("/config-properties=" + configProperty.getConfigPropertyName() + ":add(");
                        script = script.concat("value=" + configProperty.getConfigProperty() + ")\n");
                    }
                }
            }
        }
       return script;
    }

    @Override
    public String createLoggerScript(Logger logger) throws CliScriptException{
        if((logger.getLoggerLevelName() == null) || (logger.getLoggerLevelName().isEmpty())){
            throw new CliScriptException("Error:name of the logger cannot be null of empty", new NullPointerException());
        }

        String script = "/subsystem=logging/logger=" + logger.getLoggerCategory() + ":add(";
        script = checkingMethod(script, "level", logger.getLoggerLevelName());
        script = checkingMethod(script, ", use-parent-handlers", logger.getUseParentHandlers());

        if(logger.getHandlers() != null){
            String handlers = "";
            for(String handler : logger.getHandlers()){
                  handlers = handlers.concat(",\"" + handler + "\"");
            }
            if(!handlers.isEmpty()){
                handlers = handlers.replaceFirst("\\,","");
                script = script.concat(", handlers=[" + handlers +"])");
            } else{
                script = script.concat(")");
            }
        } else {
           script = script.concat(")");
        }
        return script;

    }

    @Override
    public String createHandlersScript(LoggingAS7 loggingAS7) throws CliScriptException{
        String handlers = "";

        for(ConsoleHandler consoleHandler : loggingAS7.getConsoleHandlers()){
            handlers = handlers.concat(createConsoleHandlerScript(consoleHandler)+ "\n");
        }

        for(PerRotFileHandler periodic : loggingAS7.getPerRotFileHandlers()){
            handlers = handlers.concat(createPerHandlerScript(periodic) + "\n");
        }

        for(SizeRotFileHandler size : loggingAS7.getSizeRotFileHandlers()){
            handlers = handlers.concat(createSizeHandlerScript(size)+ "\n");
        }

        for(AsyncHandler asyncHandler : loggingAS7.getAsyncHandlers()){
            handlers = handlers.concat(createAsyncHandlerScript(asyncHandler) +"\n");
        }

        for(CustomHandler customHandler : loggingAS7.getCustomHandlers()){
            handlers = handlers.concat(createCustomHandlerScript(customHandler) + "\n");
        }

        return handlers;
    }
    @Override
    public String createPerHandlerScript(PerRotFileHandler periodic) throws CliScriptException{
        if((periodic.getName() ==  null) || (periodic.getName().isEmpty())){
            throw new CliScriptException("Error: name of the periodic rotating handler cannot be null or empty",
                    new NullPointerException());
        }

        if((periodic.getSuffix() == null) || (periodic.getSuffix().isEmpty())){
            throw new CliScriptException("Error: suffix in periodic rotating handler cannot be null or empty",
                    new NullPointerException());
        }

        if((periodic.getFileRelativeTo() == null) || (periodic.getFileRelativeTo().isEmpty())){
            throw new CliScriptException("Error: relative-to in <file> in periodic rotating handler"
                    +"cannot be null or empty",
                    new NullPointerException());
        }

        if((periodic.getPath() == null) || (periodic.getPath().isEmpty())){
            throw new CliScriptException("Error:  path in <file> in periodic rotating handler cannot"
                    +" be null or empty", new NullPointerException());
        }

        String script = "/subsystem=logging/periodic-rotating-file-handler=";
        script = script.concat(periodic.getName() + ":add(");
        script = script.concat("file={\"relative-to\"=>\"" + periodic.getFileRelativeTo()+"\"");
        script = script.concat(", \"path\"=>\"" + periodic.getPath() + "\"}");
        script = script.concat(", suffix=" + periodic.getSuffix());
        script = checkingMethod(script, ", level", periodic.getLevel());
        script = checkingMethod(script, ", formatter", periodic.getFormatter());
        script = checkingMethod(script, ", autoflush", periodic.getAutoflush());
        script = checkingMethod(script, ", append", periodic.getAppend());
        script = script.concat(")");

        return script;
    }
    @Override
    public String createSizeHandlerScript(SizeRotFileHandler sizeHandler) throws CliScriptException{
        if((sizeHandler.getName() == null) || (sizeHandler.getName().isEmpty())){
            throw new CliScriptException("Error: name of the size rotating handler cannot be null or empty",
                    new NullPointerException());
        }

        if((sizeHandler.getRelativeTo() == null) || (sizeHandler.getPath().isEmpty())){
            throw new CliScriptException("Error: relative-to in <file> in size rotating handler cannot be null or empty",
                    new NullPointerException());
        }

        if((sizeHandler.getPath() ==  null) || (sizeHandler.getPath().isEmpty())){
            throw new CliScriptException("Error: path in <file> in size rotating handler cannot be null or empty",
                    new NullPointerException());
        }

        String script = "/subsystem=logging/size-rotating-file-handler=";
        script = script.concat(sizeHandler.getName() + ":add(");
        script = script.concat("file={\"" + sizeHandler.getRelativeTo() + "\"=>\"" + sizeHandler.getPath() + "\"}");
        script = checkingMethod(script, "level", sizeHandler.getLevel());
        script = checkingMethod(script, ", filter", sizeHandler.getFilter());
        script = checkingMethod(script, ", formatter", sizeHandler.getFormatter());
        script = checkingMethod(script, ", autoflush", sizeHandler.getAutoflush());
        script = checkingMethod(script, ", append", sizeHandler.getAppend());
        script = checkingMethod(script, ", rotate-size", sizeHandler.getRotateSize());
        script = checkingMethod(script, ", max-backup-index", sizeHandler.getMaxBackupIndex());
        script = script.concat(")");

        return script;

    }

    @Override
    public String createAsyncHandlerScript(AsyncHandler asyncHandler) throws  CliScriptException{
        if((asyncHandler.getQueueLength() == null) || (asyncHandler.getQueueLength().isEmpty())){
            throw new CliScriptException("Error: queue-length in async handler cannot be null or empty",
                    new NullPointerException());
        }

        if((asyncHandler.getName() == null) || (asyncHandler.getName().isEmpty())){
            throw new CliScriptException("Error: name of the async handler cannot be null or empty",
                    new NullPointerException());
        }

        String script = "/subsystem=logging/async-handler=";
        script = script.concat(asyncHandler.getName() + ":add(");
        script = script.concat("queue-length=" + asyncHandler.getQueueLength());
        script = checkingMethod(script, ", level", asyncHandler.getLevel());
        script = checkingMethod(script, ", filter", asyncHandler.getFilter());
        script = checkingMethod(script, ", formatter", asyncHandler.getFormatter());
        script = checkingMethod(script, ", overflow-action", asyncHandler.getOverflowAction());

        if(asyncHandler.getSubhandlers() != null){
            String handlers = "";
            for(String subhandler  : asyncHandler.getSubhandlers()){
                handlers=", \"" + subhandler + "\"";
            }
            handlers = handlers.replaceFirst("\\, ", "");
            if(!handlers.isEmpty()){
                script = script.concat(", subhandlers=[" + handlers +"]");
            }
        }

        script = script.concat(")");

        return script;

    }
     @Override
    public String createConsoleHandlerScript(ConsoleHandler consoleHandler) throws CliScriptException{
        if((consoleHandler.getName() == null) || (consoleHandler.getName().isEmpty())){
            throw new CliScriptException("Error: name of the console handler cannot be null or empty",
                    new NullPointerException());
        }

        String script = "/subsystem=logging/console-handler=";
        script = script.concat(consoleHandler.getName() + ":add(");
        script = checkingMethod(script, "level", consoleHandler.getLevel());
        script = checkingMethod(script, ", filter", consoleHandler.getFilter());
        script = checkingMethod(script, ", formatter", consoleHandler.getFormatter());
        script = checkingMethod(script, ", autoflush", consoleHandler.getAutoflush());
        script = checkingMethod(script, ", target", consoleHandler.getTarget());
        script = script.concat(")");

        return script;
    }


    @Override
    public String createCustomHandlerScript (CustomHandler customHandler) throws  CliScriptException{
        if((customHandler.getName() == null) || (customHandler.getName().isEmpty())){
            throw new CliScriptException("Error: name of the custom handler cannot be null or empty",
                    new NullPointerException());
        }

        if((customHandler.getModule() == null) || (customHandler.getModule().isEmpty())){
            throw new CliScriptException("Error: module in the custom handler cannot be null or empty",
                    new NullPointerException());
        }

        if((customHandler.getClassValue() == null) || (customHandler.getClassValue().isEmpty())){
            throw new CliScriptException("Error: class in the custom handler cannot be null or empty",
                    new NullPointerException());
        }

        String script = "/subsystem=logging/custom-handler=";
        script = script.concat(customHandler.getName() + ":add(");
        script = checkingMethod(script, "level", customHandler.getLevel());
        script = checkingMethod(script, ", filter", customHandler.getFilter());
        script = checkingMethod(script, ", formatter", customHandler.getFormatter());
        script = checkingMethod(script, ", class", customHandler.getClassValue());
        script = checkingMethod(script, ", module", customHandler.getModule());

        if(customHandler.getProperties() != null){
            String properties = "";
            for(Property property : customHandler.getProperties()){
                properties = properties.concat(", \"" + property.getName() + "\"=>");
                properties = properties.concat("\"" + property.getValue() + "\"");
            }

            if(!properties.isEmpty()){
                properties = properties.replaceFirst("\\, ", "");
                script = script.concat(", properties={" + properties + "}");
            }
        }

        script = script.concat(")");

        return script;
    }

    @Override
    public String createSecurityDomainScript(SecurityDomain securityDomain) throws CliScriptException{
        if((securityDomain.getSecurityDomainName() == null) || (securityDomain.getSecurityDomainName().isEmpty())){
            throw new CliScriptException("Error: name of the security domain cannot be null or empty",
                    new NullPointerException());
        }

        String script = "/subsystem=security/security-domain=";
        script = script.concat(securityDomain.getSecurityDomainName() + ":add(");
        script = checkingMethod(script, "cache-type", securityDomain.getCacheType() + ")\n");

        if(securityDomain.getLoginModules() != null){
            for(LoginModuleAS7 loginModuleAS7 : securityDomain.getLoginModules()){
               script = script.concat("/subsystem=security/security-domain=" + securityDomain.getSecurityDomainName());
                script = script.concat("/authentication=classic:add(login-modules=[{");
                script = checkingMethod(script, "\"code\"", ">\"" + loginModuleAS7.getLoginModuleCode() + "\"");
                script = checkingMethod(script, ", \"flag\"", ">\"" + loginModuleAS7.getLoginModuleFlag() + "\"");

                if((loginModuleAS7.getModuleOptions() != null) || !loginModuleAS7.getModuleOptions().isEmpty()){
                    String modules= "";
                    for(ModuleOptionAS7 moduleOptionAS7 : loginModuleAS7.getModuleOptions()){
                        modules = modules.concat(", (\"" + moduleOptionAS7.getModuleOptionName() + "\"=>");
                        modules = modules.concat("\"" + moduleOptionAS7.getModuleOptionValue() + "\")");
                    }

                    modules = modules.replaceFirst("\\,", "");
                    modules = modules.replaceFirst(" ", "");

                    if(!modules.isEmpty()){
                        script = script.concat(", \"module-option\"=>[" + modules + "]");
                    }
                }
            }
        }

        script = script.concat("}])");

        return script;
    }

    @Override
    public String createConnectorScript(ConnectorAS7 connectorAS7) throws CliScriptException{
        if((connectorAS7.getScheme() == null) || (connectorAS7.getScheme().isEmpty())){
           throw new CliScriptException("Error: scheme in connector cannot be null or empty", new NullPointerException()) ;
        }

        if((connectorAS7.getSocketBinding() == null) || (connectorAS7.getSocketBinding().isEmpty())){
            throw new CliScriptException("Error: socket-binding in connector cannot be null or empty", new NullPointerException()) ;
        }

        if((connectorAS7.getConnectorName() == null) || (connectorAS7.getConnectorName().isEmpty())){
            throw new CliScriptException("Error: connector name cannot be null or empty", new NullPointerException()) ;
        }

        if((connectorAS7.getProtocol() == null) || (connectorAS7.getConnectorName().isEmpty())){
            throw new CliScriptException("Error: protocol in connector cannot be null or empty", new NullPointerException());
        }

        String script = "/subsystem=web/connector=";
        script = script.concat(connectorAS7.getConnectorName() + ":add(");
        script = checkingMethod(script, "socket-binding", connectorAS7.getSocketBinding());
        script = checkingMethod(script, ",enable-lookups", connectorAS7.getEnableLookups());
        script = checkingMethod(script, ", max-post-size", connectorAS7.getMaxPostSize());
        script = checkingMethod(script, ", max-save-post-size", connectorAS7.getMaxSavePostSize());
        script = checkingMethod(script, ", max-connections", connectorAS7.getMaxConnections());
        script = checkingMethod(script, ", protocol", connectorAS7.getProtocol());
        script = checkingMethod(script, ", proxy-name", connectorAS7.getProxyName());
        script = checkingMethod(script, ", proxy-port", connectorAS7.getProxyPort());
        script = checkingMethod(script, ", redirect-port", connectorAS7.getRedirectPort());
        script = checkingMethod(script, ", scheme", connectorAS7.getScheme());
        script = checkingMethod(script, ", secure", connectorAS7.getSecure());
        script = checkingMethod(script, ", enabled", connectorAS7.getEnabled());
        script = script.concat(")");

        if(connectorAS7.getScheme().equals("https"))  {
            script = script.concat("\n/subsystem=web/connector=" + connectorAS7.getConnectorName()
                      + "/ssl=configuration:add(");
            script = checkingMethod(script, "name", connectorAS7.getSslName());
            script = checkingMethod(script, ", verify-client", connectorAS7.getVerifyClient());
            script = checkingMethod(script, ", verify-depth", connectorAS7.getVerifyDepth());
            script = checkingMethod(script, ", certificate-key-file", connectorAS7.getCertifKeyFile());
            script = checkingMethod(script, ", password", connectorAS7.getPassword());
            script = checkingMethod(script, ", protocol", connectorAS7.getProtocol());
            script = checkingMethod(script, ", ciphers", connectorAS7.getCiphers());
            script = checkingMethod(script, ", key-alias", connectorAS7.getKeyAlias());
            script = checkingMethod(script, ", ca-certificate-file", connectorAS7.getCaCertifFile());
            script = checkingMethod(script, ", session-cache-size", connectorAS7.getSessionCacheSize());
            script = checkingMethod(script, ", session-timeout", connectorAS7.getSessionTimeout());
            script = script.concat(")");
        }

      return script;


    }

    @Override
    public String createVirtualServerScript(VirtualServer virtualServer) {
        String script = "/subsystem=web/virtual-server=";
        script = script.concat(virtualServer.getVirtualServerName() + ":add(");
        script = checkingMethod(script, "enable-welcome-root", virtualServer.getEnableWelcomeRoot());
        script = checkingMethod(script, "default-web-module", virtualServer.getDefaultWebModule());

        if(virtualServer.getAliasName() != null){
            String aliases = "";
            for(String alias : virtualServer.getAliasName()){
                aliases = aliases.concat(", \"" + alias+"\"");
            }

            aliases = aliases.replaceFirst("\\, ", "");

            if(!aliases.isEmpty()){
                script = script.concat(", alias=[" + aliases + "]");
            }
        }

        script = script.concat(")");

        return script;
    }

    @Override
    public String createSocketBinding(SocketBinding socketBinding) throws CliScriptException{
        if((socketBinding.getSocketPort() == null) || (socketBinding.getSocketPort().isEmpty())){
            throw new CliScriptException("Error: port in socket binding cannot be null or empty", new NullPointerException());
        }

        if((socketBinding.getSocketName() == null) || (socketBinding.getSocketName().isEmpty())){
            throw new CliScriptException("Error: name of socket binding cannot be null or empty", new NullPointerException());
        }

        String script = "/socket-binding-group=standard-sockets/socket-binding=";
        script = script.concat(socketBinding.getSocketName() + ":add(");
        script = script.concat("port=" + socketBinding.getSocketPort());
        script = checkingMethod(script, ", interface", socketBinding.getSocketInterface());
        script = script.concat(")");

        return script;
    }
}
