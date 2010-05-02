package com.ettrema.http;

import java.util.Date;

/**
 *
 * @author brad
 */
public interface EventResource extends ICalResource {
    Date getStart();

    Date getEnd();

    String getSummary();
}
