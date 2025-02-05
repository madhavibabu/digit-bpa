/*
 * eGov suite of products aim to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) <2016>  eGovernments Foundation
 *
 *     The updated version of eGov suite of products as by eGovernments Foundation
 *     is available at http://www.egovernments.org
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see http://www.gnu.org/licenses/ or
 *     http://www.gnu.org/licenses/gpl.html .
 *
 *     In addition to the terms of the GPL license to be adhered to in using this
 *     program, the following additional terms are to be complied with:
 *
 *         1) All versions of this program, verbatim or modified must carry this
 *            Legal Notice.
 *
 *         2) Any misrepresentation of the origin of the material is prohibited. It
 *            is required that all modified versions of this material be marked in
 *            reasonable ways as different from the original version.
 *
 *         3) This license does not grant any rights to any user of the program
 *            with regards to rights under trademark law for use of the trade names
 *            or trademarks of eGovernments Foundation.
 *
 *   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */package org.egov.bpa.transaction.service.collection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.egov.bpa.master.entity.BpaFeeCommon;
import org.egov.bpa.master.entity.BpaFeeDetail;
import org.egov.bpa.master.entity.BpaFeeMapping;
import org.egov.bpa.master.entity.enums.FeeApplicationType;
import org.egov.bpa.master.entity.enums.FeeSubType;
import org.egov.bpa.transaction.entity.ApplicationFee;
import org.egov.bpa.transaction.entity.ApplicationFeeDetail;
import org.egov.bpa.transaction.entity.BpaApplication;
import org.egov.bpa.utils.BpaConstants;
import org.egov.bpa.utils.BpaUtils;
import org.egov.commons.Installment;
import org.egov.commons.dao.InstallmentDao;
import org.egov.demand.dao.DemandGenericDao;
import org.egov.demand.dao.EgDemandReasonDao;
import org.egov.demand.dao.EgDemandReasonMasterDao;
import org.egov.demand.model.EgDemand;
import org.egov.demand.model.EgDemandDetails;
import org.egov.demand.model.EgDemandReason;
import org.egov.demand.model.EgDemandReasonMaster;
import org.egov.demand.model.EgReasonCategory;
import org.egov.infra.admin.master.entity.Module;
import org.egov.infra.admin.master.service.ModuleService;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BpaDemandService {
    public static final String CATEGORY_FEE = "Fee";
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private DemandGenericDao demandGenericDao;
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EgDemandReasonMasterDao egDemandReasonMasterDao;

    @Autowired
    private EgDemandReasonDao egDemandReasonDao;

    @Autowired
    private InstallmentDao installmentDao;

    @Autowired
    private BpaUtils bpaUtills;

    public Session getCurrentSession() {
        return entityManager.unwrap(Session.class);
    }

    @Transactional
    public EgDemand createDemandUsingDemandReasonCodes(EgDemand demand, Map<String, BigDecimal> demandReasonCodes) {

        final Installment installment = getCurrentInstallment(BpaConstants.EGMODULE_NAME, BpaConstants.YEARLY, new Date());
        EgDemand dmd = demand == null ? buildDemandObject(new HashSet<EgDemandDetails>(), BigDecimal.ZERO,
                installment)
                : demand;
        Set<EgDemandDetails> demandDetailsSet = dmd.getEgDemandDetails();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> reasonCode : demandReasonCodes.entrySet()) {
            demandDetailsSet.add(createDemandDetail(reasonCode.getKey(), reasonCode.getValue()));
            totalAmount = totalAmount.add(reasonCode.getValue());
        }
        dmd.getEgDemandDetails().addAll(demandDetailsSet);
        dmd.setBaseDemand(totalAmount);
        return dmd;
    }

    @Transactional
    public EgDemand generateDemandUsingSanctionFeeList(ApplicationFee applicationFee, EgDemand demand) {

        final Installment installment = getCurrentInstallment(BpaConstants.EGMODULE_NAME, BpaConstants.YEARLY, new Date());

        List<Long> delDmdDetailList = new ArrayList<>();

        EgDemand dmd = demand != null ? demand
                : buildDemandObject(new HashSet<EgDemandDetails>(), BigDecimal.ZERO,
                        installment);

        Set<EgDemandDetails> demandDetailsSet = demand.getEgDemandDetails();
        HashMap<String, BigDecimal> feecodeamountmap = new HashMap<>();
        HashMap<String, Long> feecodedemanddetailsIdmap = new HashMap<>();

        for (EgDemandDetails demandDetails : demandDetailsSet) {
            feecodeamountmap.put(demandDetails.getEgDemandReason().getEgDemandReasonMaster().getCode(),
                    demandDetails.getAmount());
            feecodedemanddetailsIdmap.put(demandDetails.getEgDemandReason().getEgDemandReasonMaster().getCode(),
                    demandDetails.getId());
        }

        BigDecimal totaldmdAmt = BigDecimal.ZERO;

        for (ApplicationFeeDetail applicationFeeDtl : applicationFee.getApplicationFeeDetail()) {

            if (applicationFeeDtl.getAmount() != null && !applicationFeeDtl.getAmount().equals(BigDecimal.ZERO)) {

                // DemandDetailid null mean, its a new fee details entered from UI.
                if (feecodedemanddetailsIdmap.get(applicationFeeDtl.getBpaFeeMapping().getBpaFeeCommon().getCode()) == null) {
                    EgDemandDetails dmdDet = createDemandDetail(applicationFeeDtl.getBpaFeeMapping().getBpaFeeCommon().getCode(),
                            applicationFeeDtl.getAmount());
                    if (dmdDet != null) {
                        dmdDet.setEgDemand(dmd);
                        dmd.getEgDemandDetails().add(dmdDet);
                    }
                } else {

                    for (EgDemandDetails dmdDtl : dmd.getEgDemandDetails()) {
                        if (dmdDtl.getId() != null && dmdDtl.getId()
                                .equals(feecodedemanddetailsIdmap
                                        .get(applicationFeeDtl.getBpaFeeMapping().getBpaFeeCommon().getCode()))) {
                            dmdDtl.setAmount(applicationFeeDtl.getAmount());
                            break;
                        }
                    }
                }

            } else {
                if (null != feecodedemanddetailsIdmap.get(applicationFeeDtl.getBpaFeeMapping().getBpaFeeCommon().getCode())) {
                    delDmdDetailList
                            .add(feecodedemanddetailsIdmap.get(applicationFeeDtl.getBpaFeeMapping().getBpaFeeCommon().getCode())); // Delete
                                                                                                                                   // from
                    // the existing
                    // list.
                }
            }

        }

        // Delete from the existing list.
        for (Long dmdDtlId : delDmdDetailList) {
            for (EgDemandDetails dmdDtl : dmd.getEgDemandDetails()) {
                if (dmdDtl.getId() != null && dmdDtlId != null && dmdDtl.getId().equals(dmdDtlId)) {
                    dmd.getEgDemandDetails().remove(dmdDtl);
                    break;
                }
            }
        }

        for (EgDemandDetails det : dmd.getEgDemandDetails()) {
            totaldmdAmt = totaldmdAmt.add(det.getAmount());
        }

        dmd.setBaseDemand(totaldmdAmt);

        /*
         * if (demand == null) { applicationFee.getApplication().setDemand(dmd); }
         */
        return dmd;
    }

    protected EgDemand buildDemandObject(Set<EgDemandDetails> demandDetailSet,
            BigDecimal totaldmdAmt, Installment installment) {
        EgDemand dmd = new EgDemand();
        dmd.setEgDemandDetails(demandDetailSet);
        dmd.setAmtCollected(BigDecimal.ZERO);
        dmd.setAmtRebate(BigDecimal.ZERO);
        dmd.setBaseDemand(totaldmdAmt);
        dmd.setIsHistory("N");
        dmd.setModifiedDate(new Date());
        dmd.setCreateDate(new Date());
        dmd.setEgInstallmentMaster(installment);

        return dmd;
    }

    @Transactional
    public EgDemandDetails createDemandDetail(String demandReasonCode, BigDecimal amount) {
        EgDemandDetails dmdDet = new EgDemandDetails();
        dmdDet.setAmount(amount);
        dmdDet.setAmtCollected(BigDecimal.ZERO);
        dmdDet.setAmtRebate(BigDecimal.ZERO);
        dmdDet.setEgDemandReason(getEgDemandReason(demandReasonCode));
        dmdDet.setCreateDate(new Date());
        dmdDet.setModifiedDate(new Date());
        return dmdDet;
    }

    @Transactional
    public EgDemandReason getEgDemandReason(String demandResaonCode) {
        EgDemandReason egDemandReason;
        EgDemandReasonMaster egDemandReasonMaster;
        Module module = moduleService.getModuleByName(BpaConstants.EGMODULE_NAME);
        egDemandReasonMaster = demandGenericDao.getDemandReasonMasterByCode(demandResaonCode, module);
        egDemandReason = demandGenericDao
                .getDmdReasonByDmdReasonMsterInstallAndMod(egDemandReasonMaster,
                        getCurrentInstallment(BpaConstants.EGMODULE_NAME, BpaConstants.YEARLY, new Date()),
                        module);
        return egDemandReason;
    }

    @Transactional
    public EgDemandReasonMaster createEgDemandReasonMaster(BpaFeeCommon bpaFeeCommon) {
        EgDemandReasonMaster egDmdRsnMstr = null;
        Module module = moduleService.getModuleByName(BpaConstants.EGMODULE_NAME);
        egDmdRsnMstr = (EgDemandReasonMaster) demandGenericDao.getDemandReasonMasterByCode(bpaFeeCommon.getCode(), module);
        if (egDmdRsnMstr == null) {
            egDmdRsnMstr = new EgDemandReasonMaster();
            EgReasonCategory egRsnCategory = demandGenericDao.getReasonCategoryByCode(CATEGORY_FEE);
            egDmdRsnMstr.setReasonMaster(bpaFeeCommon.getName());
            egDmdRsnMstr.setCode(bpaFeeCommon.getCode());
            egDmdRsnMstr.setEgModule(module);
            egDmdRsnMstr.setOrderId(Long.valueOf("1"));
            egDmdRsnMstr.setEgReasonCategory(egRsnCategory);
            egDmdRsnMstr.setIsDebit("N");
            egDmdRsnMstr.setIsDemand(Boolean.TRUE);
            egDmdRsnMstr.setCreatedDate(new Date());
            egDmdRsnMstr.setModifiedDate(new Date());
            egDemandReasonMasterDao.create(egDmdRsnMstr);

            createEgDemandReason(bpaFeeCommon, egDmdRsnMstr);
        }
        return egDmdRsnMstr;
    }

    @Transactional
    public EgDemandReason createEgDemandReason(BpaFeeCommon bpaFeeCommon, EgDemandReasonMaster egDemandReasonMaster) {
        EgDemandReason egDmdRsn = new EgDemandReason();
        egDmdRsn.setEgDemandReasonMaster(egDemandReasonMaster);
        egDmdRsn.setEgInstallmentMaster(
                getCurrentInstallment(BpaConstants.EGMODULE_NAME, BpaConstants.YEARLY, new Date()));
        egDmdRsn.setGlcodeId(bpaFeeCommon.getGlcode());
        egDmdRsn.setCreateDate(new Date());
        egDmdRsn.setModifiedDate(new Date());
        egDemandReasonDao.create(egDmdRsn);
        return egDmdRsn;
    }

    public Installment getCurrentInstallment(final String moduleName, final String installmentType, final Date date) {
        if (null == installmentType)
            return installmentDao.getInsatllmentByModuleForGivenDate(moduleService.getModuleByName(moduleName),
                    date);
        else
            return installmentDao.getInsatllmentByModuleForGivenDateAndInstallmentType(
                    moduleService.getModuleByName(moduleName), date, installmentType);
    }

    /**
     *
     * @param bpaApplication
     * @return
     */
    public Boolean checkAnyTaxIsPendingToCollect(final BpaApplication bpaApplication) {
        Boolean pendingTaxCollection = false;

        if (bpaApplication != null && bpaApplication.getDemand() != null)
            for (final EgDemandDetails demandDtl : bpaApplication.getDemand().getEgDemandDetails())
                if (demandDtl.getAmount().subtract(demandDtl.getAmtCollected()).compareTo(BigDecimal.ZERO) > 0) {
                    pendingTaxCollection = true;
                    break;
                }
        return pendingTaxCollection;
    }

    public Boolean checkIsReconciliationInProgressInOnline(final String applicationNumber) {
        return bpaUtills.checkIsReconciliationInProgress(applicationNumber);
    }

    public EgDemandReason getDemandReasonByCodeAndInstallment(final String demandReason,
            final Installment installment) {
        final Query demandQuery = getCurrentSession().getNamedQuery("DEMANDREASONBY_CODE_AND_INSTALLMENTID");
        demandQuery.setString(0, demandReason);
        demandQuery.setInteger(1, installment.getId());
        return (EgDemandReason) demandQuery.uniqueResult();
    }

    public List<Object> getDmdCollAmtInstallmentWise(final EgDemand egDemand) {
        final StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder
                .append("select dmdRes.id,dmdRes.id_installment, sum(dmdDet.amount) as amount, sum(dmdDet.amt_collected) as amt_collected, "
                        + "sum(dmdDet.amt_rebate) as amt_rebate, inst.start_date from eg_demand_details dmdDet,eg_demand_reason dmdRes, "
                        + "eg_installment_master inst,eg_demand_reason_master dmdresmas where dmdDet.id_demand_reason=dmdRes.id "
                        + "and dmdDet.id_demand =:dmdId and dmdRes.id_installment = inst.id and dmdresmas.id = dmdres.id_demand_reason_master "
                        + "group by dmdRes.id,dmdRes.id_installment, inst.start_date order by inst.start_date ");
        return getCurrentSession().createSQLQuery(queryStringBuilder.toString()).setLong("dmdId", egDemand.getId())
                .list();
    }

    public List<Object> getDmdCollAmtInstallmentWiseForRegFee(final EgDemand egDemand) {
        final StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder
                .append("select dmdRes.id,dmdRes.id_installment, sum(dmdDet.amount) as amount, sum(dmdDet.amt_collected) as amt_collected, "
                        + "sum(dmdDet.amt_rebate) as amt_rebate, inst.start_date from state.eg_demand_details dmdDet,state.eg_demand_reason dmdRes, "
                        + "state.eg_installment_master inst,state.eg_demand_reason_master dmdresmas where dmdDet.id_demand_reason=dmdRes.id "
                        + "and dmdDet.id_demand =:dmdId and dmdRes.id_installment = inst.id and dmdresmas.id = dmdres.id_demand_reason_master "
                        + "group by dmdRes.id,dmdRes.id_installment, inst.start_date order by inst.start_date ");
        return getCurrentSession().createSQLQuery(queryStringBuilder.toString()).setLong("dmdId", egDemand.getId())
                .list();
    }

    public Criteria createCriteriaforFeeAmount(List<Long> serviceTypeList, final String feeType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeDetail.class, "bpafeeDtl")
                .createAlias("bpafeeDtl.bpafee", "bpaFeeObj").createAlias("bpaFeeObj.serviceType", "servicetypeObj");
        feeCrit.add(Restrictions.in("servicetypeObj.id", serviceTypeList));
        feeCrit.add(Restrictions.eq("bpaFeeObj.isActive", Boolean.TRUE));
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.feeType", feeType));

        feeCrit.add(Restrictions.le("startDate", new Date()))
                .add(Restrictions.or(Restrictions.isNull("endDate"), Restrictions.ge("endDate", new Date())));
        return feeCrit;
    }

    public Criteria createCriteriaforApplicationFeeAmount(List<Long> serviceTypeList, final String feeType,
            final FeeSubType feeSubType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeMapping.class, "bpaFeeMap")
                .createAlias("bpaFeeMap.bpaFeeCommon", "bpaFeeObj").createAlias("bpaFeeMap.serviceType", "servicetypeObj");
        feeCrit.add(Restrictions.in("servicetypeObj.id", serviceTypeList));
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.name", feeType));

        feeCrit.add(Restrictions.eq("bpaFeeMap.feeSubType", feeSubType));
        feeCrit.add(Restrictions.eq("bpaFeeMap.applicationType", FeeApplicationType.PERMIT_APPLICATION));
        feeCrit.add(Restrictions.isNull("bpaFeeMap.applicationSubType"));

        return feeCrit;
    }

    public Criteria createCriteriaforApplicationFee(Long serviceType, final String feeType, final FeeSubType feeSubType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeMapping.class, "bpaFeeMap")
                .createAlias("bpaFeeMap.bpaFeeCommon", "bpaFeeObj").createAlias("bpaFeeMap.serviceType", "servicetypeObj");
        feeCrit.add(Restrictions.eq("servicetypeObj.id", serviceType));
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.name", feeType));

        feeCrit.add(Restrictions.eq("bpaFeeMap.feeSubType", feeSubType));
        feeCrit.add(Restrictions.eq("bpaFeeMap.applicationType", FeeApplicationType.PERMIT_APPLICATION));

        return feeCrit;
    }

    public Criteria createCriteriaforRegistrationFeeAmount(final String feeType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeDetail.class, "bpafeeDtl")
                .createAlias("bpafeeDtl.bpafee", "bpaFeeObj").createAlias("bpaFeeObj.serviceType", "servicetypeObj");
        feeCrit.add(Restrictions.eq("bpaFeeObj.isActive", Boolean.TRUE));
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.feeType", feeType));

        feeCrit.add(Restrictions.le("startDate", new Date()))
                .add(Restrictions.or(Restrictions.isNull("endDate"), Restrictions.ge("endDate", new Date())));
        return feeCrit;
    }

    public Criteria createCriteriaforRegistrationFee(final String feeType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeMapping.class, "bpaFeeMap")
                .createAlias("bpaFeeMap.bpaFeeCommon", "bpaFeeObj").createAlias("bpaFeeMap.serviceType", "servicetypeObj");
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.name", feeType));

        return feeCrit;
    }

    public Criteria createCriteriaforOCApplicationFeeAmount(List<Long> serviceTypeList, final String feeType,
            final FeeSubType feeSubType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeMapping.class, "bpaFeeMap")
                .createAlias("bpaFeeMap.bpaFeeCommon", "bpaFeeObj").createAlias("bpaFeeMap.serviceType", "servicetypeObj");
        feeCrit.add(Restrictions.in("servicetypeObj.id", serviceTypeList));
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.name", feeType));

        feeCrit.add(Restrictions.eq("bpaFeeMap.feeSubType", feeSubType));
        feeCrit.add(Restrictions.eq("bpaFeeMap.applicationType", FeeApplicationType.OCCUPANCY_CERTIFICATE));

        return feeCrit;
    }

    public Criteria createCriteriaforOCApplicationFee(Long serviceType, final String feeType, final FeeSubType feeSubType) {

        final Criteria feeCrit = getCurrentSession().createCriteria(BpaFeeMapping.class, "bpaFeeMap")
                .createAlias("bpaFeeMap.bpaFeeCommon", "bpaFeeObj").createAlias("bpaFeeMap.serviceType", "servicetypeObj");
        feeCrit.add(Restrictions.eq("servicetypeObj.id", serviceType));
        if (feeType != null)
            feeCrit.add(Restrictions.ilike("bpaFeeObj.name", feeType));

        feeCrit.add(Restrictions.eq("bpaFeeMap.feeSubType", feeSubType));
        feeCrit.add(Restrictions.eq("bpaFeeMap.applicationType", FeeApplicationType.OCCUPANCY_CERTIFICATE));

        return feeCrit;
    }

}
