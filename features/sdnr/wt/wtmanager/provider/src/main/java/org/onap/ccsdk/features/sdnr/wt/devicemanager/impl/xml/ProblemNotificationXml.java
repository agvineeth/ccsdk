/*
* Copyright (c) 2016 Wipro Ltd. and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package org.onap.ccsdk.features.sdnr.wt.devicemanager.impl.xml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.internalTypes.InternalDateAndTime;
import org.onap.ccsdk.features.sdnr.wt.devicemanager.base.internalTypes.InternalSeverity;

@XmlRootElement(name = "ProblemNotification")
public class ProblemNotificationXml extends MwtNotificationBase implements GetEventType {

    private static String EVENTTYPE = "ProblemNotification";
    private static final Pattern pattern = Pattern.compile(".*\\[layerProtocol=(.*)\\]");
    /**
     * The leading indication for notification or events that are not in the
     * currentProblem data of the ONF Coremodel
     */
    private static final String NOCURRENTPROBLEMINDICATION = "#";

    @XmlElement(name = "problem")
    private String problem;

    @XmlElement(name = "severity")
    private InternalSeverity severity;

    public ProblemNotificationXml() {

    }

    /**
     * Generic Problem. All the parameters are of type Strings according to YANG
     * specification.
     *
     * @param nodeName                Name of mountpoint
     * @param uuId                    Name of Interface Pac
     * @param problemNameString       Name of the problem
     * @param problemSeverityString   Severitycode of the problem
     * @param counterString           Counter from device
     * @param internaltimeStampString Timestamp according to internal format.
     */
    public ProblemNotificationXml(String nodeName, String uuId, String problemNameString,
            InternalSeverity problemSeverityString, String counterString, InternalDateAndTime internaltimeStampString) {
        super(nodeName, counterString, internaltimeStampString, uuId);
        this.problem = problemNameString;
        this.severity = problemSeverityString;
    }

    public String getProblem() {
        return problem;
    }

    public InternalSeverity getSeverity() {
        return severity;
    }

    public boolean isNotManagedAsCurrentProblem() {
        return problem.startsWith(NOCURRENTPROBLEMINDICATION);
    }

    /**
     * Create a specific ES id for the current log.
     *
     * @return a string with the generated ES Id
     */
    @JsonIgnore
    public String genSpecificEsId() {

        String uuId;

        Matcher matcher = pattern.matcher(getObjectId());
        if (matcher.matches() && matcher.groupCount() == 1) {
            uuId = matcher.group(1);
        } else {
            uuId = getObjectId();
        }

        StringBuffer strBuf = new StringBuffer();
        strBuf.append(getNodeName());
        strBuf.append("/");
        strBuf.append(uuId);
        strBuf.append("/");
        strBuf.append(getProblem());
        return strBuf.toString();
    }

    @Override
    public String toString() {
        return "ProblemNotificationXml [problem=" + problem + ", severity=" + severity + ", toString()="
                + super.toString() + "]";
    }

    @Override
    public String getEventType() {
        return EVENTTYPE;
    }

    @JsonIgnore
    public String getTimeStampOffset(int off) {
        Instant instant = Instant.parse(this.getTimeStamp());
        return instant.truncatedTo(ChronoUnit.SECONDS).plusSeconds(off * 60).toString();
    }

}
