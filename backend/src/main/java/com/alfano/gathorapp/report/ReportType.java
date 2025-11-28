package com.alfano.gathorapp.report;

/**
 * Types of reports that users can submit.
 */
public enum ReportType {
    /**
     * Report for misleading advertising or false event/outing information.
     */
    MISLEADING_AD,

    /**
     * Report for user pretending to be someone else or fake identity.
     */
    FAKE_IDENTITY,

    /**
     * Report for inappropriate behavior or conduct.
     */
    INAPPROPRIATE_BEHAVIOR,

    /**
     * Other type of report with custom reason.
     */
    OTHER
}
