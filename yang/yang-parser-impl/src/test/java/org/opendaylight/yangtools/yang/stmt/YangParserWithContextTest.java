/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.stmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DeviateKind;
import org.opendaylight.yangtools.yang.model.api.Deviation;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.MustDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.model.api.type.UnsignedIntegerTypeDefinition;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;

public class YangParserWithContextTest {
    private static final URI T1_NS = URI.create("urn:simple.demo.test1");
    private static final URI T2_NS = URI.create("urn:simple.demo.test2");
    private static final URI T3_NS = URI.create("urn:simple.demo.test3");
    private final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static Date rev;

    private static final YangStatementSourceImpl BAR = new YangStatementSourceImpl("/model/bar.yang", false);
    private static final YangStatementSourceImpl BAZ = new YangStatementSourceImpl("/model/baz.yang", false);
    private static final YangStatementSourceImpl FOO = new YangStatementSourceImpl("/model/foo.yang", false);
    private static final YangStatementSourceImpl SUBFOO = new YangStatementSourceImpl("/model/subfoo.yang", false);

    private static final YangStatementSourceImpl[] IETF = new YangStatementSourceImpl[] {
            new YangStatementSourceImpl("/ietf/iana-afn-safi@2012-06-04.yang", false),
            new YangStatementSourceImpl("/ietf/iana-if-type@2012-06-05.yang", false),
            new YangStatementSourceImpl("/ietf/iana-timezones@2012-07-09.yang", false),
            new YangStatementSourceImpl("/ietf/ietf-inet-types@2010-09-24.yang", false),
            new YangStatementSourceImpl("/ietf/ietf-yang-types@2010-09-24.yang", false),
            new YangStatementSourceImpl("/ietf/network-topology@2013-07-12.yang", false),
            new YangStatementSourceImpl("/ietf/network-topology@2013-10-21.yang", false) };

    @BeforeClass
    public static void init() throws ParseException {
        final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        rev = simpleDateFormat.parse("2013-06-18");
    }

    @Test
    public void testTypeFromContext() throws Exception {

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        final YangStatementSourceImpl types = new YangStatementSourceImpl("/types/custom-types-test@2012-4-4.yang",
                false);
        final YangStatementSourceImpl test1 = new YangStatementSourceImpl("/context-test/test1.yang", false);

        StmtTestUtils.addSources(reactor, IETF);
        StmtTestUtils.addSources(reactor, types, test1);

        final SchemaContext context = reactor.buildEffective();

        final Module module = context.findModuleByName("test1",
                SimpleDateFormatUtil.getRevisionFormat().parse("2013-06-18"));
        assertNotNull(module);

        final LeafSchemaNode leaf = (LeafSchemaNode) module.getDataChildByName(QName.create(module.getQNameModule(),
                "id"));

        assertTrue(leaf.getType() instanceof UnsignedIntegerTypeDefinition);
        final UnsignedIntegerTypeDefinition leafType = (UnsignedIntegerTypeDefinition) leaf.getType();
        QName qname = leafType.getQName();
        assertEquals(URI.create("urn:simple.demo.test1"), qname.getNamespace());
        assertEquals(simpleDateFormat.parse("2013-06-18"), qname.getRevision());
        assertEquals("port-number", qname.getLocalName());

        final UnsignedIntegerTypeDefinition leafBaseType = leafType.getBaseType();
        qname = leafBaseType.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:ietf-inet-types"), qname.getNamespace());
        assertEquals(simpleDateFormat.parse("2010-09-24"), qname.getRevision());
        assertEquals("port-number", qname.getLocalName());

        final UnsignedIntegerTypeDefinition dscpExt = (UnsignedIntegerTypeDefinition) TestUtils.findTypedef(
                module.getTypeDefinitions(), "dscp-ext");
        final List<RangeConstraint> ranges = dscpExt.getRangeConstraints();
        assertEquals(1, ranges.size());
        final RangeConstraint range = ranges.get(0);
        assertEquals(0, range.getMin().intValue());
        assertEquals(63, range.getMax().intValue());
    }

    @Test
    public void testUsesFromContext() throws Exception {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        final YangStatementSourceImpl test2 = new YangStatementSourceImpl("/context-test/test2.yang", false);
        StmtTestUtils.addSources(reactor, BAZ, FOO, BAR, SUBFOO, test2);
        final SchemaContext context = reactor.buildEffective();

        final Module testModule = context.findModuleByName("test2",
                SimpleDateFormatUtil.getRevisionFormat().parse("2013-06-18"));
        assertNotNull(testModule);

        final Module contextModule = context.findModuleByNamespace(URI.create("urn:opendaylight.baz")).iterator()
                .next();
        assertNotNull(contextModule);
        final Set<GroupingDefinition> groupings = contextModule.getGroupings();
        assertEquals(1, groupings.size());
        final GroupingDefinition grouping = groupings.iterator().next();

        // get node containing uses
        final ContainerSchemaNode peer = (ContainerSchemaNode) testModule.getDataChildByName(QName.create(
                testModule.getQNameModule(), "peer"));
        final ContainerSchemaNode destination = (ContainerSchemaNode) peer.getDataChildByName(QName.create(
                testModule.getQNameModule(), "destination"));

        // check uses
        final Set<UsesNode> uses = destination.getUses();
        assertEquals(1, uses.size());

        // check uses process
        final AnyXmlSchemaNode data_u = (AnyXmlSchemaNode) destination.getDataChildByName(QName.create(
                testModule.getQNameModule(), "data"));
        assertNotNull(data_u);
        assertTrue(data_u.isAddedByUses());

        final AnyXmlSchemaNode data_g = (AnyXmlSchemaNode) grouping.getDataChildByName(QName.create(
                contextModule.getQNameModule(), "data"));
        assertNotNull(data_g);
        assertFalse(data_g.isAddedByUses());
        assertFalse(data_u.equals(data_g));

        final ChoiceSchemaNode how_u = (ChoiceSchemaNode) destination.getDataChildByName(QName.create(
                testModule.getQNameModule(), "how"));
        assertNotNull(how_u);
        assertTrue(how_u.isAddedByUses());

        final ChoiceSchemaNode how_g = (ChoiceSchemaNode) grouping.getDataChildByName(QName.create(
                contextModule.getQNameModule(), "how"));
        assertNotNull(how_g);
        assertFalse(how_g.isAddedByUses());
        assertFalse(how_u.equals(how_g));

        final LeafSchemaNode address_u = (LeafSchemaNode) destination.getDataChildByName(QName.create(
                testModule.getQNameModule(), "address"));
        assertNotNull(address_u);
        assertTrue(address_u.isAddedByUses());

        final LeafSchemaNode address_g = (LeafSchemaNode) grouping.getDataChildByName(QName.create(
                contextModule.getQNameModule(), "address"));
        assertNotNull(address_g);
        assertFalse(address_g.isAddedByUses());
        assertFalse(address_u.equals(address_g));

        final ContainerSchemaNode port_u = (ContainerSchemaNode) destination.getDataChildByName(QName.create(
                testModule.getQNameModule(), "port"));
        assertNotNull(port_u);
        assertTrue(port_u.isAddedByUses());

        final ContainerSchemaNode port_g = (ContainerSchemaNode) grouping.getDataChildByName(QName.create(
                contextModule.getQNameModule(), "port"));
        assertNotNull(port_g);
        assertFalse(port_g.isAddedByUses());
        assertFalse(port_u.equals(port_g));

        final ListSchemaNode addresses_u = (ListSchemaNode) destination.getDataChildByName(QName.create(
                testModule.getQNameModule(), "addresses"));
        assertNotNull(addresses_u);
        assertTrue(addresses_u.isAddedByUses());

        final ListSchemaNode addresses_g = (ListSchemaNode) grouping.getDataChildByName(QName.create(
                contextModule.getQNameModule(), "addresses"));
        assertNotNull(addresses_g);
        assertFalse(addresses_g.isAddedByUses());
        assertFalse(addresses_u.equals(addresses_g));

        // grouping defined by 'uses'
        final Set<GroupingDefinition> groupings_u = destination.getGroupings();
        assertEquals(1, groupings_u.size());
        final GroupingDefinition grouping_u = groupings_u.iterator().next();
        assertTrue(grouping_u.isAddedByUses());

        // grouping defined in 'grouping' node
        final Set<GroupingDefinition> groupings_g = grouping.getGroupings();
        assertEquals(1, groupings_g.size());
        final GroupingDefinition grouping_g = groupings_g.iterator().next();
        assertFalse(grouping_g.isAddedByUses());
        assertFalse(grouping_u.equals(grouping_g));

        final List<UnknownSchemaNode> nodes_u = destination.getUnknownSchemaNodes();
        assertEquals(1, nodes_u.size());
        final UnknownSchemaNode node_u = nodes_u.get(0);
        assertTrue(node_u.isAddedByUses());

        final List<UnknownSchemaNode> nodes_g = grouping.getUnknownSchemaNodes();
        assertEquals(1, nodes_g.size());
        final UnknownSchemaNode node_g = nodes_g.get(0);
        assertFalse(node_g.isAddedByUses());
        assertFalse(node_u.equals(node_g));
    }

    @Test
    public void testUsesRefineFromContext() throws Exception {

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        final YangStatementSourceImpl test2 = new YangStatementSourceImpl("/context-test/test2.yang", false);
        StmtTestUtils.addSources(reactor, BAZ, FOO, BAR, SUBFOO, test2);
        final SchemaContext context = reactor.buildEffective();

        final Module module = context.findModuleByName("test2",
                SimpleDateFormatUtil.getRevisionFormat().parse("2013-06-18"));
        assertNotNull(module);
        final ContainerSchemaNode peer = (ContainerSchemaNode) module.getDataChildByName(QName.create(
                module.getQNameModule(), "peer"));
        final ContainerSchemaNode destination = (ContainerSchemaNode) peer.getDataChildByName(QName.create(
                module.getQNameModule(), "destination"));
        final Set<UsesNode> usesNodes = destination.getUses();
        assertEquals(1, usesNodes.size());
        final UsesNode usesNode = usesNodes.iterator().next();

        // test grouping path
        final List<QName> path = new ArrayList<>();
        final QName qname = QName.create(URI.create("urn:opendaylight.baz"), simpleDateFormat.parse("2013-02-27"),
                "target");
        path.add(qname);
        final SchemaPath expectedPath = SchemaPath.create(path, true);
        assertEquals(expectedPath, usesNode.getGroupingPath());

        // test refine
        final Map<SchemaPath, SchemaNode> refines = usesNode.getRefines();
        assertEquals(3, refines.size());

        LeafSchemaNode refineLeaf = null;
        ContainerSchemaNode refineContainer = null;
        ListSchemaNode refineList = null;
        for (final Map.Entry<SchemaPath, SchemaNode> entry : refines.entrySet()) {
            final SchemaNode value = entry.getValue();
            if (value instanceof LeafSchemaNode) {
                refineLeaf = (LeafSchemaNode) value;
            } else if (value instanceof ContainerSchemaNode) {
                refineContainer = (ContainerSchemaNode) value;
            } else if (value instanceof ListSchemaNode) {
                refineList = (ListSchemaNode) value;
            }
        }

        // leaf address
        assertNotNull(refineLeaf);
        assertEquals("address", refineLeaf.getQName().getLocalName());
        assertEquals("description of address defined by refine", refineLeaf.getDescription());
        assertEquals("address reference added by refine", refineLeaf.getReference());
        assertFalse(refineLeaf.isConfiguration());
        assertTrue(refineLeaf.getConstraints().isMandatory());
        final Set<MustDefinition> leafMustConstraints = refineLeaf.getConstraints().getMustConstraints();
        assertEquals(1, leafMustConstraints.size());
        final MustDefinition leafMust = leafMustConstraints.iterator().next();
        assertEquals("ifType != 'ethernet' or (ifType = 'ethernet' and ifMTU = 1500)", leafMust.toString());

        // container port
        assertNotNull(refineContainer);
        final Set<MustDefinition> mustConstraints = refineContainer.getConstraints().getMustConstraints();
        assertTrue(mustConstraints.isEmpty());
        assertEquals("description of port defined by refine", refineContainer.getDescription());
        assertEquals("port reference added by refine", refineContainer.getReference());
        assertFalse(refineContainer.isConfiguration());
        assertTrue(refineContainer.isPresenceContainer());

        // list addresses
        assertNotNull(refineList);
        assertEquals("description of addresses defined by refine", refineList.getDescription());
        assertEquals("addresses reference added by refine", refineList.getReference());
        assertFalse(refineList.isConfiguration());
        assertEquals(2, (int) refineList.getConstraints().getMinElements());
        assertEquals(12, (int) refineList.getConstraints().getMaxElements());
    }

    @Test
    public void testIdentity() throws Exception {

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        final YangStatementSourceImpl types = new YangStatementSourceImpl("/types/custom-types-test@2012-4-4.yang",
                false);
        final YangStatementSourceImpl test3 = new YangStatementSourceImpl("/context-test/test3.yang", false);

        StmtTestUtils.addSources(reactor, IETF);
        StmtTestUtils.addSources(reactor, types, test3);
        final SchemaContext context = reactor.buildEffective();

        final Module module = context.findModuleByName("test3",
                SimpleDateFormatUtil.getRevisionFormat().parse("2013-06-18"));
        assertNotNull(module);

        final Set<IdentitySchemaNode> identities = module.getIdentities();
        assertEquals(1, identities.size());

        final IdentitySchemaNode identity = identities.iterator().next();
        final QName idQName = identity.getQName();
        assertEquals(URI.create("urn:simple.demo.test3"), idQName.getNamespace());
        assertEquals(simpleDateFormat.parse("2013-06-18"), idQName.getRevision());
        assertEquals("pt", idQName.getLocalName());

        final IdentitySchemaNode baseIdentity = identity.getBaseIdentity();
        final QName idBaseQName = baseIdentity.getQName();
        assertEquals(URI.create("urn:custom.types.demo"), idBaseQName.getNamespace());
        assertEquals(simpleDateFormat.parse("2012-04-16"), idBaseQName.getRevision());
        assertEquals("service-type", idBaseQName.getLocalName());
    }

    @Test
    public void testUnknownNodes() throws Exception {

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        final YangStatementSourceImpl types = new YangStatementSourceImpl("/types/custom-types-test@2012-4-4.yang",
                false);
        final YangStatementSourceImpl test3 = new YangStatementSourceImpl("/context-test/test3.yang", false);

        StmtTestUtils.addSources(reactor, IETF);
        StmtTestUtils.addSources(reactor, types, test3);

        final SchemaContext context = reactor.buildEffective();

        final Module module = context.findModuleByName("test3",
                SimpleDateFormatUtil.getRevisionFormat().parse("2013-06-18"));
        assertNotNull(module);

        final ContainerSchemaNode network = (ContainerSchemaNode) module.getDataChildByName(QName.create(
                module.getQNameModule(), "network"));
        final List<UnknownSchemaNode> unknownNodes = network.getUnknownSchemaNodes();
        assertEquals(1, unknownNodes.size());

        final UnknownSchemaNode un = unknownNodes.get(0);
        final QName unType = un.getNodeType();
        assertEquals(URI.create("urn:custom.types.demo"), unType.getNamespace());
        assertEquals(simpleDateFormat.parse("2012-04-16"), unType.getRevision());
        assertEquals("mountpoint", unType.getLocalName());
        assertEquals("point", un.getNodeParameter());
        assertNotNull(un.getExtensionDefinition());
    }

    @Test
    public void testAugment() throws Exception {
        final StatementStreamSource resource = new YangStatementSourceImpl("/context-augment-test/test4.yang", false);
        final StatementStreamSource test1 = new YangStatementSourceImpl("/context-augment-test/test1.yang", false);
        final StatementStreamSource test2 = new YangStatementSourceImpl("/context-augment-test/test2.yang", false);
        final StatementStreamSource test3 = new YangStatementSourceImpl("/context-augment-test/test3.yang", false);

        final SchemaContext context = TestUtils.parseYangSources(resource, test1, test2, test3);
        final Set<Module> modules = context.getModules();
        assertNotNull(modules);

        final Module t4 = TestUtils.findModule(modules, "test4");
        final ContainerSchemaNode interfaces = (ContainerSchemaNode) t4.getDataChildByName(QName.create(
                t4.getQNameModule(), "interfaces"));
        final ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName(QName.create(t4.getQNameModule(),
                "ifEntry"));

        // test augmentation process
        final ContainerSchemaNode augmentHolder = (ContainerSchemaNode) ifEntry.getDataChildByName(QName.create(T3_NS,
                rev, "augment-holder"));
        assertNotNull(augmentHolder);
        final DataSchemaNode ds0 = augmentHolder.getDataChildByName(QName.create(T2_NS, rev, "ds0ChannelNumber"));
        assertNotNull(ds0);
        final DataSchemaNode interfaceId = augmentHolder.getDataChildByName(QName.create(T2_NS, rev, "interface-id"));
        assertNotNull(interfaceId);
        final DataSchemaNode higherLayerIf = augmentHolder.getDataChildByName(QName.create(T2_NS, rev,
                "higher-layer-if"));
        assertNotNull(higherLayerIf);
        final ContainerSchemaNode schemas = (ContainerSchemaNode) augmentHolder.getDataChildByName(QName.create(T2_NS,
                rev, "schemas"));
        assertNotNull(schemas);
        assertNotNull(schemas.getDataChildByName(QName.create(T1_NS, rev, "id")));

        // test augment target after augmentation: check if it is same instance
        final ListSchemaNode ifEntryAfterAugment = (ListSchemaNode) interfaces.getDataChildByName(QName.create(
                t4.getQNameModule(), "ifEntry"));
        assertTrue(ifEntry == ifEntryAfterAugment);
    }

    @Test
    public void testDeviation() throws Exception {

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        final YangStatementSourceImpl bar = new YangStatementSourceImpl("/model/bar.yang", false);
        final YangStatementSourceImpl deviationTest = new YangStatementSourceImpl("/context-test/deviation-test.yang",
                false);

        StmtTestUtils.addSources(reactor, bar, deviationTest);
        final SchemaContext context = reactor.buildEffective();

        final Module testModule = context.findModuleByName("deviation-test", SimpleDateFormatUtil.getRevisionFormat()
                .parse("2013-02-27"));
        assertNotNull(testModule);

        final Set<Deviation> deviations = testModule.getDeviations();
        assertEquals(1, deviations.size());
        final Deviation dev = deviations.iterator().next();

        assertEquals("system/user ref", dev.getReference());

        final URI expectedNS = URI.create("urn:opendaylight.bar");
        final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final Date expectedRev = simpleDateFormat.parse("2013-07-03");
        final List<QName> path = new ArrayList<>();
        path.add(QName.create(expectedNS, expectedRev, "interfaces"));
        path.add(QName.create(expectedNS, expectedRev, "ifEntry"));
        final SchemaPath expectedPath = SchemaPath.create(path, true);

        assertEquals(expectedPath, dev.getTargetPath());
        assertEquals(DeviateKind.ADD, dev.getDeviates().iterator().next().getDeviateType());
    }

}
