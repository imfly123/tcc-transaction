package org.mengyun.tcctransaction.api;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by changmingxie on 10/26/15.
 */
public class TransactionXid implements  Serializable {

    private static final long serialVersionUID = -6817267250789142043L;

    private String globalTransactionId;

    private String branchQualifier;

    public TransactionXid() {
        globalTransactionId = UUID.randomUUID().toString();
        branchQualifier = UUID.randomUUID().toString();
    }

    public TransactionXid(String globalTransactionId) {
        this.globalTransactionId = globalTransactionId;
        branchQualifier = UUID.randomUUID().toString();
    }

    public String getGlobalTransactionId() {
        return globalTransactionId;
    }

    public String getBranchQualifier() {
        return branchQualifier;
    }

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(globalTransactionId.toString());
        stringBuilder.append(":").append(branchQualifier.toString());

        return stringBuilder.toString();
    }

}


