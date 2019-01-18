package org.mengyun.tcctransaction.repository;


import org.mengyun.tcctransaction.Transaction;
import org.mengyun.tcctransaction.api.TransactionStatus;
import org.mengyun.tcctransaction.api.TransactionXid;

import org.mengyun.tcctransaction.serializer.KryoPoolSerializer;
import org.mengyun.tcctransaction.serializer.ObjectSerializer;
import org.mengyun.tcctransaction.utils.CollectionUtils;
import org.mengyun.tcctransaction.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by changmingxie on 10/30/15.
 */
public class JdbcTransactionRepository extends CachableTransactionRepository {

    static final Logger logger = LoggerFactory.getLogger(CachableTransactionRepository.class.getSimpleName());

    private String domain;

    private String tbSuffix;

    private DataSource dataSource;

    private ObjectSerializer serializer = new KryoPoolSerializer();

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTbSuffix() {
        return tbSuffix;
    }

    public void setTbSuffix(String tbSuffix) {
        this.tbSuffix = tbSuffix;
    }

    public void setSerializer(ObjectSerializer serializer) {
        this.serializer = serializer;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    protected int doCreate(Transaction transaction) {

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("INSERT INTO " + getTableName() +
                    "(GLOBAL_TX_ID,BRANCH_QUALIFIER,TRANSACTION_TYPE,CONTENT,STATUS,RETRIED_COUNT,CREATE_TIME,LAST_UPDATE_TIME,VERSION");
            builder.append(StringUtils.isNotEmpty(domain) ? ",DOMAIN ) VALUES (?,?,?,?,?,?,?,?,?,?)" : ") VALUES (?,?,?,?,?,?,?,?,?)");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setString(1, transaction.getXid().getGlobalTransactionId());
            stmt.setString(2, transaction.getXid().getBranchQualifier());
            stmt.setInt(3, transaction.getTransactionType().getId());
            stmt.setBytes(4, serializer.serialize(transaction));
            stmt.setInt(5, transaction.getStatus().getId());
            stmt.setInt(6, transaction.getRetriedCount());
            stmt.setTimestamp(7, new java.sql.Timestamp(transaction.getCreateTime().getTime()));
            stmt.setTimestamp(8, new java.sql.Timestamp(transaction.getLastUpdateTime().getTime()));
            stmt.setLong(9, transaction.getVersion());

            if (StringUtils.isNotEmpty(domain)) {
                stmt.setString(10, domain);
            }

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TransactionIOException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }

    protected int doUpdate(Transaction transaction) {
        Connection connection = null;
        PreparedStatement stmt = null;

        java.util.Date lastUpdateTime = transaction.getLastUpdateTime();
        long currentVersion = transaction.getVersion();

        transaction.updateTime();
        transaction.updateVersion();

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE " + getTableName() + " SET " +
                    "CONTENT = ?,STATUS = ?,LAST_UPDATE_TIME = ?, RETRIED_COUNT = ?,VERSION = VERSION+1 ,IS_SEND_EMAIL = ? WHERE GLOBAL_TX_ID = ? AND BRANCH_QUALIFIER = ? AND VERSION = ? ");

            builder.append(StringUtils.isNotEmpty(domain) ? " AND DOMAIN = ?" : "");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setBytes(1, serializer.serialize(transaction));
            stmt.setInt(2, transaction.getStatus().getId());
            stmt.setTimestamp(3, new Timestamp(transaction.getLastUpdateTime().getTime()));

            stmt.setInt(4, transaction.getRetriedCount());
            stmt.setInt(5, transaction.getIsSendEmail());
            stmt.setString(6, transaction.getXid().getGlobalTransactionId());
            stmt.setString(7, transaction.getXid().getBranchQualifier());
            stmt.setLong(8, currentVersion);

            if (StringUtils.isNotEmpty(domain)) {
                stmt.setString(9, domain);
            }

            int result = stmt.executeUpdate();

            return result;

        } catch (Throwable e) {
            transaction.setLastUpdateTime(lastUpdateTime);
            transaction.setVersion(currentVersion);
            throw new TransactionIOException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }

    /*protected int doDelete(Transaction transaction) {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("DELETE FROM " + getTableName() +
                    " WHERE GLOBAL_TX_ID = ? AND BRANCH_QUALIFIER = ?");

            builder.append(StringUtils.isNotEmpty(domain) ? " AND DOMAIN = ?" : "");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setBytes(1, transaction.getXid().getGlobalTransactionId());
            stmt.setBytes(2, transaction.getXid().getBranchQualifier());

            if (StringUtils.isNotEmpty(domain)) {
                stmt.setString(3, domain);
            }

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new TransactionIOException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }*/

    protected int doDelete(Transaction transaction) {
        return doLogicDelete(transaction);
    }

    /**
     * 逻辑删除
     * @param transaction
     * @return
     */
    protected int doLogicDelete(Transaction transaction) {
        Connection connection = null;
        PreparedStatement stmt = null;

        java.util.Date lastUpdateTime = transaction.getLastUpdateTime();
        long currentVersion = transaction.getVersion();

        transaction.updateTime();
        transaction.updateVersion();

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE " + getTableName() + " SET " +
                    "IS_DELETE = 1 WHERE GLOBAL_TX_ID = ? AND BRANCH_QUALIFIER = ? AND VERSION = ?");

            builder.append(StringUtils.isNotEmpty(domain) ? " AND DOMAIN = ?" : "");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setString(1, transaction.getXid().getGlobalTransactionId());
            stmt.setString(2, transaction.getXid().getBranchQualifier());
            stmt.setLong(3, currentVersion);

            if (StringUtils.isNotEmpty(domain)) {
                stmt.setString(4, domain);
            }
            logger.info("doLogicDelete sql:{}",stmt.toString());
            int result = stmt.executeUpdate();

            return result;

        } catch (Throwable e) {
            transaction.setLastUpdateTime(lastUpdateTime);
            transaction.setVersion(currentVersion);
            throw new TransactionIOException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }

    protected Transaction doFindOne(TransactionXid xid) {
        List<TransactionXid> transactionXids = new ArrayList<TransactionXid>();
        transactionXids.add(xid);
        List<Transaction> transactions = doFind(transactionXids);

        if (!CollectionUtils.isEmpty(transactions)) {
            return transactions.get(0);
        }
        return null;
    }

    @Override
    protected List<Transaction> doFindAllUnmodifiedSince(java.util.Date date) {

        List<Transaction> transactions = new ArrayList<Transaction>();

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();

            builder.append("SELECT GLOBAL_TX_ID, BRANCH_QUALIFIER, CONTENT,STATUS,TRANSACTION_TYPE,CREATE_TIME,LAST_UPDATE_TIME,RETRIED_COUNT,VERSION,IS_SEND_EMAIL");
            builder.append(StringUtils.isNotEmpty(domain) ? ",DOMAIN" : "");
            builder.append("  FROM " + getTableName() + " WHERE LAST_UPDATE_TIME < ?");
            builder.append(" AND IS_DELETE = 0 AND IS_SEND_EMAIL = 0 ");
            builder.append(StringUtils.isNotEmpty(domain) ? " AND DOMAIN = ?" : "");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setTimestamp(1, new Timestamp(date.getTime()));

            if (StringUtils.isNotEmpty(domain)) {
                stmt.setString(2, domain);
            }

            ResultSet resultSet = stmt.executeQuery();

            this.constructTransactions(resultSet, transactions);
        } catch (Throwable e) {
            throw new TransactionIOException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }

        return transactions;
    }

    protected List<Transaction> doFind(List<TransactionXid> xids) {

        List<Transaction> transactions = new ArrayList<Transaction>();

        if (CollectionUtils.isEmpty(xids)) {
            return transactions;
        }

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT GLOBAL_TX_ID, BRANCH_QUALIFIER, CONTENT,STATUS,TRANSACTION_TYPE,CREATE_TIME,LAST_UPDATE_TIME,RETRIED_COUNT,VERSION,IS_SEND_EMAIL");
            builder.append(StringUtils.isNotEmpty(domain) ? ",DOMAIN" : "");
            builder.append("  FROM " + getTableName() + " WHERE");

            if (!CollectionUtils.isEmpty(xids)) {
                for (TransactionXid xid : xids) {
                    builder.append(" ( GLOBAL_TX_ID = ? AND BRANCH_QUALIFIER = ? ) OR");
                }

                builder.delete(builder.length() - 2, builder.length());
            }

            builder.append(StringUtils.isNotEmpty(domain) ? " AND DOMAIN = ?" : "");

            stmt = connection.prepareStatement(builder.toString());

            int i = 0;

            for (TransactionXid xid : xids) {
                stmt.setString(++i, xid.getGlobalTransactionId());
                stmt.setString(++i, xid.getBranchQualifier());
            }

            if (StringUtils.isNotEmpty(domain)) {
                stmt.setString(++i, domain);
            }

            ResultSet resultSet = stmt.executeQuery();

            this.constructTransactions(resultSet, transactions);
        } catch (Throwable e) {
            throw new TransactionIOException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }

        return transactions;
    }

    protected void constructTransactions(ResultSet resultSet, List<Transaction> transactions) throws SQLException {
        while (resultSet.next()) {
            byte[] transactionBytes = resultSet.getBytes(3);
            Transaction transaction = (Transaction) serializer.deserialize(transactionBytes);
            transaction.changeStatus(TransactionStatus.valueOf(resultSet.getInt(4)));
            transaction.setLastUpdateTime(resultSet.getDate(7));
            transaction.setVersion(resultSet.getLong(9));
            transaction.resetRetriedCount(resultSet.getInt(8));
            transaction.setIsSendEmail(resultSet.getInt(10));
            transactions.add(transaction);
        }
    }


    protected Connection getConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new TransactionIOException(e);
        }
    }

    protected void releaseConnection(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            throw new TransactionIOException(e);
        }
    }

    private void closeStatement(Statement stmt) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (Exception ex) {
            throw new TransactionIOException(ex);
        }
    }

    private String getTableName() {
        return StringUtils.isNotEmpty(tbSuffix) ? "TCC_TRANSACTION" + tbSuffix : "TCC_TRANSACTION";
    }
}
