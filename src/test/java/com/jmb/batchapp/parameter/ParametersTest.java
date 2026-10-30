package com.jmb.batchapp.parameter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParametersTest {

    @Test
    void loadsOnlyKnownParametersForTheJob() {
        Parameters p = new Parameters();
        String[] args = {"jobName=salesSummaryJob", "sellerId=Joe,Beth", "date=20201111000000", "product=Beer", "unknown=x"};
        p.loadAllParams(args, CommonJobParameter.values());
        p.loadAllParams(args, JobsParameters.getParamsForJob("salesSummaryJob").get().getParameters());

        assertEquals("salesSummaryJob", p.getParamValue(CommonJobParameter.JOB_NAME));
        assertEquals("Joe,Beth", p.getParamValue(SalesSummaryJobParams.SELLER_ID));
        assertEquals("Beer", p.getParamValue(SalesSummaryJobParams.PRODUCT));
        assertNull(p.getParamValue(CommonJobParameter.CLIENT_ID));
    }

    @Test
    void ingesterJobAcceptsOptionalWriteMode() {
        assertEquals(3, JobsParameters.getParamsForJob("ingesterJob").get().getParameters().length);
    }
}
