package org.lucentrix.metaframe.plugin.dummy.model.insurance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.LxEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
public class InsuranceGenerator {

    ClaimGenerator claimGenerator;
    ClientGenerator clientGenerator;
    PolicyGenerator policyGenerator;
    SecurityGenerator securityGenerator;

    public InsuranceGenerator(Random random, int securityMaxCount, int clientMaxCount, int claimMaxCount, int policyMaxCount,
                              int userCount, int groupCount) {
        InsuranceModel model = new InsuranceModel(claimMaxCount, clientMaxCount, policyMaxCount, securityMaxCount, userCount, groupCount);
        securityGenerator = new SecurityGenerator(random, securityMaxCount, model);
        clientGenerator = new ClientGenerator(random, claimMaxCount, model);
        policyGenerator = new PolicyGenerator(random, policyMaxCount, model);
        claimGenerator = new ClaimGenerator(random, clientMaxCount, model);
    }

    public int getSecurityCount() {
        return securityGenerator.getCount();
    }

    public int getClientCount() {
        return clientGenerator.getCount();
    }

    public int getClaimCount() {
        return claimGenerator.getCount();
    }

    public int getPolicyCount() {
        return policyGenerator.getCount();
    }

    public boolean hasNext() {
        return securityGenerator.hasNext() || clientGenerator.hasNext() || policyGenerator.hasNext() || claimGenerator.hasNext();
    }

    public List<LxEvent> next(int limit) {
        List<LxEvent> documents = new ArrayList<>();

        while (documents.size() < limit && securityGenerator.hasNext()) {
            documents.add(securityGenerator.generate());
        }
        while (documents.size() < limit && clientGenerator.hasNext()) {
            documents.add(clientGenerator.generate());
        }
        while (documents.size() < limit && policyGenerator.hasNext()) {
            documents.add(policyGenerator.generate());
        }
        while (documents.size() < limit && claimGenerator.hasNext()) {
            documents.add(claimGenerator.generate());
        }

        return documents;
    }
}
