/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2017>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */
package org.egov.bpa.transaction.notice.util;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.egov.bpa.utils.BpaConstants.APPLICATION_MODULE_TYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.egov.bpa.config.reports.properties.BpaApplicationReportProperties;
import org.egov.bpa.transaction.entity.OwnershipTransfer;
import org.egov.bpa.transaction.entity.OwnershipTransferConditions;
import org.egov.bpa.transaction.entity.OwnershipTransferNotice;
import org.egov.bpa.transaction.entity.common.NoticeCommon;
import org.egov.bpa.transaction.entity.enums.ConditionType;
import org.egov.bpa.transaction.repository.OwnershipTransferNoticeRepository;
import org.egov.bpa.transaction.service.OwnershipTransferConditionsService;
import org.egov.bpa.transaction.service.OwnershipTransferService;
import org.egov.bpa.transaction.workflow.BpaWorkFlowService;
import org.egov.bpa.utils.BpaUtils;
import org.egov.infra.admin.master.service.CityService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.reporting.engine.ReportOutput;
import org.egov.infra.reporting.engine.ReportRequest;
import org.egov.infra.reporting.engine.ReportService;
import org.egov.infra.workflow.entity.StateHistory;
import org.egov.pims.commons.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OwnershipTransferNoticeUtil {

    public static final String TWO_NEW_LINE = "\n\n";
    public static final String N_A = "N/A";
    public static final String ONE_NEW_LINE = "\n";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String APPLICATION_REJECTION_REASON = "applctn.reject.reason";
    private static final String APPLICATION_AUTO_REJECTION_REASON = "applctn.auto.reject.reason";
    public static final String SECTION_CLERK = "SECTION CLERK";

    @Autowired
    @Qualifier("fileStoreService")
    protected FileStoreService fileStoreService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private BpaUtils bpaUtils;
    @Autowired
    @Qualifier("parentMessageSource")
    private MessageSource bpaMessageSource;
    @Autowired
    private OwnershipTransferNoticeRepository ownershipNoticeRepository;
    @Autowired
    private OwnershipTransferConditionsService ownershipConditionsService;
    @Autowired
    private CityService cityService;
    @Autowired
    private OwnershipTransferService ownershipService;
    @Autowired
    private BpaNoticeUtil bpaNoticeUtil;
    @Autowired
    private BpaApplicationReportProperties bpaApplicationReportProperties;
    @Autowired
    private BpaWorkFlowService bpaWorkFlowService;

    public OwnershipTransferNotice findByOwnershipAndNoticeType(final OwnershipTransfer ownershipTransfer, final String noticeType) {
        return ownershipNoticeRepository.findByOwnershipAndNoticeType(ownershipTransfer, noticeType);
    }    

    public ReportOutput getReportOutput(OwnershipTransfer ownershipTransfer, String fileName, OwnershipTransferNotice ownershipNotice,
            String rejectionfilename, String rejectionNoticeType, HttpServletRequest request) throws IOException {
        ReportOutput reportOutput = new ReportOutput();
        if (ownershipNotice == null || ownershipNotice.getNoticeCommon().getNoticeFileStore() == null) {
            final Map<String, Object> reportParams = bpaNoticeUtil.buildParametersForReport(ownershipTransfer.getParent());
            reportParams.putAll(getUlbDetails());
            reportParams.put("refusalFormat", bpaApplicationReportProperties.getPermitRefusalFormat());
            reportParams.put("applicationNumber", ownershipTransfer.getApplicationNumber());
            reportParams.put("rejectionReasons", buildRejectionReasons(ownershipTransfer));
            reportParams.put("approverName", getRejector(ownershipTransfer));
            reportParams.put("permitOrderTitle", "OWNERSHIP TRANSFER ORDER");
            reportParams.put("subHeaderTitle", "Building permit ownership transfer,");
            reportParams.put("ownershipNumber", ownershipTransfer.getOwnershipNumber());
        
            ReportRequest reportInput = new ReportRequest(rejectionfilename, ownershipTransfer, reportParams);
            reportOutput = reportService.createReport(reportInput);
            saveOwnershipNotice(ownershipTransfer, fileName, reportOutput, null, rejectionNoticeType);
        } else {
            final FileStoreMapper fmp = ownershipNotice.getNoticeCommon().getNoticeFileStore();
            Path path = fileStoreService.fetchAsPath(fmp.getFileStoreId(), APPLICATION_MODULE_TYPE);
            reportOutput.setReportOutputData(Files.readAllBytes(path));
        }
        return reportOutput;
    }
    
    

    public OwnershipTransferNotice saveOwnershipNotice(OwnershipTransfer ownershipTransfer, String fileName, ReportOutput reportOutput,ReportOutput reportOutputForPermitNote, String noticeType) {
    	OwnershipTransferNotice ownershipNotice = new OwnershipTransferNotice();
        final List<InputStream> pdfs = new ArrayList<>();
    	if (reportOutputForPermitNote != null)
            pdfs.add(new ByteArrayInputStream(reportOutputForPermitNote.getReportOutputData()));
    	ownershipNotice.setOwnershipTransfer(ownershipTransfer);
        NoticeCommon noticeCommon = new NoticeCommon();
        noticeCommon.setNoticeGeneratedDate(new Date());
        noticeCommon.setNoticeType(noticeType);
        noticeCommon.setNoticeFileStore(
                fileStoreService.store(new ByteArrayInputStream(reportOutput.getReportOutputData()), fileName, APPLICATION_PDF,
                        APPLICATION_MODULE_TYPE));
        ownershipNotice.setNoticeCommon(noticeCommon);
        ownershipTransfer.addOwnershipNotice(ownershipNotice);
        ownershipService.saveOwnership(ownershipTransfer);
        return ownershipNotice;
    }

    public Map<String, Object> getUlbDetails() {
        final Map<String, Object> ulbDetailsReportParams = new HashMap<>();
        ulbDetailsReportParams.put("cityName", ApplicationThreadLocals.getCityName());
        ulbDetailsReportParams.put("logoPath", cityService.getCityLogoAsStream());
        ulbDetailsReportParams.put("ulbName", ApplicationThreadLocals.getMunicipalityName());
        ulbDetailsReportParams.put("ulbGrade", cityService.getCityGrade());
        ulbDetailsReportParams.put("cityNameLocal", ApplicationThreadLocals.getCityNameLocal());
        return ulbDetailsReportParams;
    }    


    public String buildRejectionReasons(final OwnershipTransfer ownershipTransfer) {
        StringBuilder rejectReasons = new StringBuilder();
        if (!ownershipTransfer.getRejectionReasons().isEmpty()) {
            List<OwnershipTransferConditions> additionalRenewalConditions = ownershipConditionsService
                    .findAllConditionsByOwnershipAndType(ownershipTransfer, ConditionType.ADDITIONALREJECTIONREASONS);
            int order = buildPredefinedRejectReasons(ownershipTransfer, rejectReasons);
            int additionalOrder = buildAdditionalOwnershipConditionsOrRejectionReason(rejectReasons, additionalRenewalConditions,
                    order);
            StateHistory<Position> stateHistory = bpaUtils.getRejectionComments(ownershipTransfer.getStateHistory());
            if (stateHistory != null && isNotBlank(stateHistory.getComments()))
                rejectReasons.append(additionalOrder + ") "
                        + stateHistory.getComments() + TWO_NEW_LINE);
        } else {
            rejectReasons.append(ownershipTransfer.getState().getComments() != null
                    && ownershipTransfer.getState().getComments().equalsIgnoreCase("Application cancelled by citizen")
                            ? getMessageFromPropertyFile(APPLICATION_REJECTION_REASON)
                            : getMessageFromPropertyFile(APPLICATION_AUTO_REJECTION_REASON));
        }
        return rejectReasons.toString();
    }
    
    public String getMessageFromPropertyFile(String key) {
        return bpaMessageSource.getMessage(key, null, null);
    }

    private int buildPredefinedRejectReasons(final OwnershipTransfer ownershipTransfer,
            StringBuilder ownershipConditions) {
        int order = 1;
        for (OwnershipTransferConditions rejectReason : ownershipTransfer.getRejectionReasons()) {
            if (rejectReason.getNoticeCondition().isRequired()
                    && ConditionType.OWNERSHIPREJECTIONREASONS.equals(rejectReason.getNoticeCondition().getType())) {
            	ownershipConditions
                        .append(order + ") "
                                + rejectReason.getNoticeCondition().getChecklistServicetype().getChecklist().getDescription()
                                + TWO_NEW_LINE);
                order++;
            }
        }
        return order;
    }
    
    private int buildAdditionalOwnershipConditionsOrRejectionReason(StringBuilder ownershipConditions,
            List<OwnershipTransferConditions> additionalConditions, int order) {
        int additionalOrder = order;
        if (!additionalConditions.isEmpty()
                && isNotBlank(additionalConditions.get(0).getNoticeCondition().getAdditionalCondition())) {
            for (OwnershipTransferConditions addnlNoticeCondition : additionalConditions) {
                if (isNotBlank(addnlNoticeCondition.getNoticeCondition().getAdditionalCondition())) {
                	ownershipConditions.append(
                            String.valueOf(additionalOrder) + ") "
                                    + addnlNoticeCondition.getNoticeCondition().getAdditionalCondition()
                                    + TWO_NEW_LINE);
                    additionalOrder++;
                }
            }
        }
        return additionalOrder;
    }
    
    private String getRejector(final OwnershipTransfer ownershipTransfer) {
        return bpaWorkFlowService.getApproverAssignmentByDate(ownershipTransfer.getState().getOwnerPosition(), ownershipTransfer.getState().getLastModifiedDate()).getEmployee().getName();
    }

}
