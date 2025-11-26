package com.farao_community.farao.rao_example;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

public class Main {
    public static void main(String[] args) {
        // Import network from UCTE file
        String networkFilename = "12Nodes.uct";
        Network network = Network.read(networkFilename, Main.class.getResourceAsStream("/%s".formatted(networkFilename)));

        // Initialise CRAC
        Crac crac = CracFactory.findDefault().create("crac");

        // Create instants
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative", InstantKind.CURATIVE);

        // Add contingency
        crac.newContingency()
            .withId("contingency")
            .withContingencyElement("DDE2AA1  NNL3AA1  1", ContingencyElementType.LINE)
            .add();

        // Add FlowCNECs
        crac.newFlowCnec()
            .withId("NNL2AA1  BBE3AA1  1 - preventive")
            .withInstant("preventive")
            .withOptimized()
            .withNetworkElement("NNL2AA1  BBE3AA1  1")
            .newThreshold()
            .withMin(-410d)
            .withMax(+410d)
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("NNL2AA1  BBE3AA1  1 - outage")
            .withInstant("outage")
            .withOptimized()
            .withContingency("contingency")
            .withNetworkElement("NNL2AA1  BBE3AA1  1")
            .newThreshold()
            .withMin(-1000d)
            .withMax(+1000d)
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .add()
            .add();

        crac.newFlowCnec()
            .withId("NNL2AA1  BBE3AA1  1 - curative")
            .withInstant("curative")
            .withOptimized()
            .withContingency("contingency")
            .withNetworkElement("NNL2AA1  BBE3AA1  1")
            .newThreshold()
            .withMin(-410d)
            .withMax(+410d)
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .add()
            .add();

        // Add PST range action (PRA + CRA)
        IidmPstHelper iidmPstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst-range-action")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap())
            .newTapRange()
            .withMinTap(-16)
            .withMaxTap(16)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();

        // Add auto terminals connection action
        crac.newNetworkAction()
            .withId("terminals-connection-action")
            .newTerminalsConnectionAction()
            .withNetworkElement("NNL2AA1  BBE3AA1  2")
            .withActionType(ActionType.CLOSE)
            .add()
            .newTerminalsConnectionAction()
            .withNetworkElement("NNL2AA1  BBE3AA1  3")
            .withActionType(ActionType.CLOSE)
            .add()
            .newOnContingencyStateUsageRule()
            .withInstant("curative")
            .withContingency("contingency")
            .add()
            .add();

        // RAO Parameters setting
        RaoParameters raoParameters = new RaoParameters();
        OpenRaoSearchTreeParameters searchTreeParameters = new OpenRaoSearchTreeParameters();

        // Enable DC mode for load-flow & sensitivity computations
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(true);
        SensitivityAnalysisParameters sensitivityAnalysisParameters = new SensitivityAnalysisParameters();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);

        // Set "OpenLoadFlow" as load-flow provider
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
        loadFlowAndSensitivityParameters.setLoadFlowProvider("OpenLoadFlow");
        loadFlowAndSensitivityParameters.setSensitivityWithLoadFlowParameters(sensitivityAnalysisParameters);
        searchTreeParameters.setLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);

        // Ask the RAO to maximize minimum margin in MW, and to stop when network is secure (i.e. when margins are positive)
        ObjectiveFunctionParameters objectiveFunctionParameters = new ObjectiveFunctionParameters();
        objectiveFunctionParameters.setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        objectiveFunctionParameters.setUnit(Unit.MEGAWATT);

        // Enable "APPROXIMATED_INTEGERS" in PST optimization, for better accuracy
        SearchTreeRaoRangeActionsOptimizationParameters rangeActionsParameters = new SearchTreeRaoRangeActionsOptimizationParameters();
        rangeActionsParameters.setPstModel(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        searchTreeParameters.setRangeActionsOptimizationParameters(rangeActionsParameters);

        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, searchTreeParameters);

        // Run RAO
        RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        RaoResult raoResult = Rao.find().run(raoInputBuilder.build(), raoParameters);

        System.exit(0);
    }
}