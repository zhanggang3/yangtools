/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.stmt.rfc6020;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.model.api.Rfc6020Mapping;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ContactStatement;
import org.opendaylight.yangtools.yang.parser.spi.SubstatementValidator;
import org.opendaylight.yangtools.yang.parser.spi.meta.AbstractDeclaredStatement;
import org.opendaylight.yangtools.yang.parser.spi.meta.AbstractStatementSupport;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.effective.ContactEffectiveStatementImpl;

public class ContactStatementImpl extends AbstractDeclaredStatement<String> implements ContactStatement{
    private static final SubstatementValidator SUBSTATEMENT_VALIDATOR = SubstatementValidator.builder(Rfc6020Mapping
            .CONTACT)
            .build();

    protected ContactStatementImpl(StmtContext<String, ContactStatement,?> context) {
        super(context);
    }

    public static class Definition extends AbstractStatementSupport<String,ContactStatement,EffectiveStatement<String,ContactStatement>> {

        public Definition() {
            super(Rfc6020Mapping.CONTACT);
        }

        @Override
        public String parseArgumentValue(StmtContext<?, ?, ?> ctx, String value) {
            return value;
        }

        @Override
        public ContactStatement createDeclared(StmtContext<String, ContactStatement, ?> ctx) {
            return new ContactStatementImpl(ctx);
        }

        @Override
        public EffectiveStatement<String, ContactStatement> createEffective(StmtContext<String, ContactStatement, EffectiveStatement<String, ContactStatement>> ctx) {
            return new ContactEffectiveStatementImpl(ctx);
        }

        @Override
        public void onFullDefinitionDeclared(StmtContext.Mutable<String, ContactStatement,
                EffectiveStatement<String, ContactStatement>> stmt) {
            super.onFullDefinitionDeclared(stmt);
            SUBSTATEMENT_VALIDATOR.validate(stmt);
        }
    }

    @Nonnull @Override
    public String getText() {
        return rawArgument();
    }
}
