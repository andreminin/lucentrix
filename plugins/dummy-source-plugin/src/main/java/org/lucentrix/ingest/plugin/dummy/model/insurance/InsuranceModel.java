package org.lucentrix.ingest.plugin.dummy.model.insurance;

import org.lucentrix.ingest.plugin.dummy.GeneratorModel;

public record InsuranceModel(int claimMaxCount, int clientMaxCount, int policyMaxCount, int securityMaxCount,
                             int userCount, int groupCount)
        implements GeneratorModel {

}
