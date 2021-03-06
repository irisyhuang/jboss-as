/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cmp.jdbc2.keygen;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.CreateException;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc2.CreateCommand;
import org.jboss.as.cmp.jdbc2.JDBCStoreManager2;
import org.jboss.as.cmp.jdbc2.PersistentContext;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.logging.Logger;

/**
 * Abstract create command
 *
 * @author <a href="mailto:jep@worldleaguesports.com">Jesper Pedersen</a>
 * @version <tt>$Revision: 82920 $</tt>
 */
public abstract class AbstractCreateCommand
        implements CreateCommand {
    protected JDBCEntityBridge2 entityBridge;
    protected Logger log;
    protected JDBCCMPFieldBridge2 pkField;
    protected String pkSql;

    public void init(JDBCStoreManager2 manager) {
        this.entityBridge = (JDBCEntityBridge2) manager.getEntityBridge();
        this.log = Logger.getLogger(getClass().getName() + "." + entityBridge.getEntityName());

        final JDBCFieldBridge[] pkFields = entityBridge.getPrimaryKeyFields();
        if (pkFields.length > 1) {
            throw CmpMessages.MESSAGES.entityCommandCanNotBeUsedWithCompositePk();
        }

        this.pkField = (JDBCCMPFieldBridge2) pkFields[0];
        this.pkSql = "";
    }

    public Object execute(Method m, Object[] args, CmpEntityBeanContext ctx) throws CreateException {
        Object pk;
        PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
        if (ctx.getPrimaryKeyUnchecked() == null) {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("executing sql: " + pkSql);
                }

                con = entityBridge.getDataSource().getConnection();
                ps = con.prepareStatement(pkSql);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    throw CmpMessages.MESSAGES.pkSqlReturnedNoResults(pkSql);
                }

                pk = pkField.loadArgumentResults(rs, 1);
                pctx.setFieldValue(pkField.getRowIndex(), pk);
                pk = entityBridge.extractPrimaryKeyFromInstance(ctx);
            } catch (SQLException e) {
                throw CmpMessages.MESSAGES.failedToExecutePkSql(e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }

            if (pk == null) {
                throw CmpMessages.MESSAGES.pkIsNullForCreatedInstance();
            }

            pctx.setPk(pk);
        } else {
            // insert-after-ejb-post-create
            try {
                pctx.flush();
            } catch (SQLException e) {
                if ("23000".equals(e.getSQLState())) {
                    throw CmpMessages.MESSAGES.uniqueKeyViolation(ctx.getPrimaryKeyUnchecked());
                } else {
                    throw CmpMessages.MESSAGES.failedToCreateInstance(ctx.getPrimaryKeyUnchecked(), e);
                }
            }
            pk = ctx.getPrimaryKeyUnchecked();
        }
        return pk;
    }
}
