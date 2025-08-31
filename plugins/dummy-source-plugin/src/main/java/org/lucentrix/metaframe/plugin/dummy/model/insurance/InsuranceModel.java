package org.lucentrix.metaframe.plugin.dummy.model.insurance;

import org.lucentrix.metaframe.plugin.dummy.GeneratorModel;

public record InsuranceModel(int claimMaxCount, int clientMaxCount, int policyMaxCount, int securityMaxCount,
                             int userCount, int groupCount)
        implements GeneratorModel {

}
