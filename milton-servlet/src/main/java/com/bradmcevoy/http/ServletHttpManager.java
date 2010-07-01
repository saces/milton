package com.bradmcevoy.http;

import com.bradmcevoy.http.webdav.WebDavResponseHandler;

import freenet.log.Logger;

public class ServletHttpManager extends HttpManager implements Initable {
    
    public ServletHttpManager(ResourceFactory resourceFactory, WebDavResponseHandler responseHandler, AuthenticationService authenticationService) {
        super(resourceFactory, responseHandler, authenticationService );
    }

    public ServletHttpManager(ResourceFactory resourceFactory, AuthenticationService authenticationService) {
        super(resourceFactory, authenticationService );
    }


    public ServletHttpManager(ResourceFactory resourceFactory) {
        super(resourceFactory);
    }
    
    public void init(ApplicationConfig config,HttpManager manager) {
        Logger.debug(this, "init");
        if( resourceFactory != null ) { 
            if( resourceFactory instanceof Initable ) {
                Initable i = (Initable)resourceFactory;
                i.init(config,manager);
            }
            for( String paramName : config.getInitParameterNames() ) {
                if( paramName.startsWith("filter_") ) {
                    String filterClass = config.getInitParameter(paramName);
                    Logger.debug(this, "init filter: " + filterClass);
                    String[] arr = paramName.split("_");
                    String ordinal = arr[arr.length-1];
                    int pos = Integer.parseInt(ordinal);
                    initFilter(config, filterClass, pos);
                }
            }
        }
    }

    private void initFilter(final ApplicationConfig config, final String filterClass, final int pos) {
        try {
            Class c = Class.forName(filterClass);
            Filter filter = (Filter) c.newInstance();
            if( filter instanceof Initable ) {
                ((Initable)filter).init(config,this);
            }
            this.addFilter(pos,filter);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(filterClass,ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(filterClass,ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(filterClass,ex);
        }
    }
    
    public void destroy(HttpManager manager) {
        Logger.debug(this, "destroy");
        if( resourceFactory != null ) {
            if( resourceFactory instanceof Initable ) {
                Initable i = (Initable)resourceFactory;
                i.destroy(manager);
            }
        }
    }
}
