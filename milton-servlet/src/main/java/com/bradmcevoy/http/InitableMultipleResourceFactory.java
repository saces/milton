package com.bradmcevoy.http;

import java.util.List;

import freenet.log.Logger;

/**
 *
 * @author brad
 */
public class InitableMultipleResourceFactory extends MultipleResourceFactory {

    public InitableMultipleResourceFactory() {
        super();
    }

    public InitableMultipleResourceFactory( List<ResourceFactory> factories ) {
        super( factories );
    }

    public void init(ApplicationConfig config, HttpManager manager) {
        String sFactories = config.getInitParameter("resource.factory.multiple");
        init(sFactories, config, manager);
    }


    protected void init(String sFactories,ApplicationConfig config, HttpManager manager) {
        Logger.debug(this, "init: " + sFactories );
        String[] arr = sFactories.split(",");
        for(String s : arr ) {
            createFactory(s,config,manager);
        }
    }

    private void createFactory(String s,ApplicationConfig config, HttpManager manager) {
        Logger.debug(this, "createFactory: " + s);
        Class c;
        try {
            c = Class.forName(s);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(s,ex);
        }
        Object o;
        try {
            o = c.newInstance();
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(s,ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(s,ex);
        }
        ResourceFactory rf = (ResourceFactory) o;
        if( rf instanceof Initable ) {
            Initable i = (Initable)rf;
            i.init(config,manager);
        }
        factories.add(rf);
    }
    

    public void destroy(HttpManager manager) {
        if( factories == null ) {
            Logger.warning(this, "factories is null");
            return ;
        }
        for( ResourceFactory f : factories ) {
            if( f instanceof Initable ) {
                ((Initable)f).destroy(manager);
            }
        }
    }
}
